package com.example.wealthtracker.network

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

    // Search company names to resolve Yahoo symbols
    @GET("v1/finance/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 10,
        @Query("newsCount") newsCount: Int = 0,
        @Query("lang") lang: String = "en-IN",
        @Query("region") region: String = "IN"
    ): SearchResponse
}

// --- DTOs (minimal fields used) ---

data class QuoteResponse(val quoteResponse: QuoteResult)

data class QuoteResult(val result: List<QuoteItem> = emptyList())

data class QuoteItem(
    val symbol: String?,
    val longName: String?,
    val regularMarketPrice: Double?,
    val regularMarketChange: Double?,
    val regularMarketChangePercent: Double?,
    val regularMarketPreviousClose: Double?
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

data class IndicatorQuote(val close: List<Double?> = emptyList())

// --- Search response (minimal) ---
data class SearchResponse(val quotes: List<SearchQuote> = emptyList())
data class SearchQuote(
    val symbol: String?,
    val longname: String?,
    val shortname: String?,
    val exchDisp: String?
)

object StocksApiProvider {
    private val httpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
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
