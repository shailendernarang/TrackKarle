package com.example.wealthtracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query

// Yahoo Finance public APIs (unofficial). No API key required.
// Quotes: https://query1.finance.yahoo.com/v7/finance/quote?symbols=TCS.NS
// Chart:  https://query1.finance.yahoo.com/v8/finance/chart/TCS.NS?range=1mo&interval=1d

interface StocksService {
    @GET("v7/finance/quote")
    suspend fun quotes(@Query("symbols") symbols: String): QuoteResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun chart(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("range") range: String = "1mo",
        @Query("interval") interval: String = "1d"
    ): ChartResponse

    @GET("v10/finance/quoteSummary/{symbol}")
    suspend fun quoteSummary(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("modules") modules: String = "assetProfile"
    ): QuoteSummaryResponse

    // Search company names to resolve Yahoo symbols
    @GET("v1/finance/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0,
        @Query("lang") lang: String = "en-US",
        @Query("region") region: String = "US"
    ): SearchResponse
}

// --- DTOs (minimal fields used) ---

data class QuoteResponse(val quoteResponse: QuoteResult)

data class QuoteResult(val result: List<QuoteItem> = emptyList())

data class QuoteItem(
    val symbol: String?,
    val longName: String?,
    val shortName: String?,
    val regularMarketPrice: Double?,
    val regularMarketChange: Double?,
    val regularMarketChangePercent: Double?,
    val regularMarketPreviousClose: Double?,
    val regularMarketOpen: Double?,
    val regularMarketDayHigh: Double?,
    val regularMarketDayLow: Double?,
    val regularMarketVolume: Long?,
    val marketCap: Long?,
    val exchange: String?,
    val financialCurrency: String?,
    // Pre/post-market fields (null when market is in REGULAR session)
    val preMarketPrice: Double?,
    val preMarketChange: Double?,
    val preMarketChangePercent: Double?,
    val postMarketPrice: Double?,
    val postMarketChange: Double?,
    val postMarketChangePercent: Double?,
    // "PRE" | "REGULAR" | "POST" | "CLOSED" | "PREPRE" | "POSTPOST"
    val marketState: String?
)

data class ChartResponse(val chart: ChartResult)

data class ChartResult(val result: List<ChartEntry> = emptyList())

data class ChartEntry(
    val timestamp: List<Long> = emptyList(),
    val indicators: Indicators? = null,
    val meta: ChartMeta? = null
)

data class ChartMeta(
    val regularMarketPrice: Double? = null,
    val chartPreviousClose: Double? = null,
    val previousClose: Double? = null
)

data class Indicators(val quote: List<IndicatorQuote> = emptyList())

data class IndicatorQuote(
    val open: List<Double?> = emptyList(),
    val high: List<Double?> = emptyList(),
    val low: List<Double?> = emptyList(),
    val close: List<Double?> = emptyList(),
    val volume: List<Long?> = emptyList()
)

// --- Search response (minimal) ---
data class SearchResponse(val quotes: List<SearchQuote> = emptyList())
data class SearchQuote(
    val symbol: String?,
    val longname: String?,
    val shortname: String?,
    val exchDisp: String?
)

// --- Quote Summary (company profile) ---
data class QuoteSummaryResponse(
    val quoteSummary: QuoteSummaryResult? = null
)
data class QuoteSummaryResult(
    val result: List<QuoteSummaryEntry> = emptyList()
)
data class QuoteSummaryEntry(
    val assetProfile: AssetProfile? = null
)
data class AssetProfile(
    val longBusinessSummary: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val country: String? = null,
    val website: String? = null,
    val fullTimeEmployees: Long? = null
)

object StocksApiProvider {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    // ── Crumb / session ─────────────────────────────────────────────
    @Volatile private var crumb: String? = null
    private val crumbMutex = Mutex()

    // Shared cookie jar — session cookies from fc.yahoo.com must be reused
    // on every subsequent request to query1.finance.yahoo.com
    private val cookieStore = mutableListOf<okhttp3.Cookie>()
    private val cookieJar: okhttp3.CookieJar = object : okhttp3.CookieJar {
        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
            synchronized(cookieStore) { cookieStore.addAll(cookies) }
        }
        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
            synchronized(cookieStore) { return cookieStore.toList() }
        }
    }

    // Bare client used only for the two crumb-bootstrap requests (no crumb interceptor)
    private val bootstrapClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "*/*")
                        .build()
                )
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private suspend fun fetchCrumb(): String? = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1 — hit fc.yahoo.com so Yahoo sets the A1/A3 session cookies
            bootstrapClient.newCall(
                okhttp3.Request.Builder().url("https://fc.yahoo.com").build()
            ).execute().close()

            // Step 2 — exchange cookies for a crumb token
            val resp = bootstrapClient.newCall(
                okhttp3.Request.Builder()
                    .url("https://query1.finance.yahoo.com/v1/test/getcrumb")
                    .build()
            ).execute()

            resp.use { r ->
                val body = r.body?.string()?.trim()
                // Sanity-check: crumb is a short alphanumeric string, not HTML
                if (r.isSuccessful && !body.isNullOrBlank() && !body.startsWith("<")) body
                else null
            }
        }.getOrNull()
    }

    /** Call once before the first Yahoo Finance request (safe to call repeatedly). */
    suspend fun ensureCrumb() {
        if (crumb != null) return
        crumbMutex.withLock {
            if (crumb != null) return
            crumb = fetchCrumb()
        }
    }

    /** Force-refresh the crumb (call after a 401 response). */
    suspend fun refreshCrumb() {
        crumbMutex.withLock {
            crumb = null
            crumb = fetchCrumb()
        }
    }

    // ── Main HTTP client ─────────────────────────────────────────────
    private val httpClient: okhttp3.OkHttpClient by lazy {
        val builder = okhttp3.OkHttpClient.Builder()
            .cookieJar(cookieJar)   // reuse the session cookies obtained during crumb fetch
            .addInterceptor { chain ->
                val original = chain.request()
                // Append crumb to every Yahoo Finance request that doesn't already have it
                val url = crumb?.let { c ->
                    original.url.newBuilder().addQueryParameter("crumb", c).build()
                } ?: original.url
                val req = original.newBuilder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)

        if (com.ss.wealthtracker.BuildConfig.DEBUG) {
            appContext?.let { ctx ->
                builder.addInterceptor(
                    com.chuckerteam.chucker.api.ChuckerInterceptor.Builder(ctx)
                        .maxContentLength(250_000L)
                        .alwaysReadResponseBody(true)
                        .build()
                )
            }
        }

        builder.build()
    }

    private fun newRetrofit(baseUrl: String): retrofit2.Retrofit =
        retrofit2.Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

    private val defaultRetrofit by lazy { newRetrofit("https://query1.finance.yahoo.com/") }
    val service: StocksService by lazy { defaultRetrofit.create(StocksService::class.java) }

    fun forHost(baseUrl: String): StocksService = newRetrofit(baseUrl).create(StocksService::class.java)
}
