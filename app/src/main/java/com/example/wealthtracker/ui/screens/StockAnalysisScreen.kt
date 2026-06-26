package com.example.wealthtracker.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wealthtracker.network.AssetProfile
import com.example.wealthtracker.network.StocksApiProvider
import com.example.wealthtracker.ui.components.AnimatedPriceText
import com.example.wealthtracker.ui.components.AppodealBanner
import com.example.wealthtracker.ui.components.MarketIndicesMarquee
import com.example.wealthtracker.util.LocalActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URI
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// ── Data models ────────────────────────────────────────────────────────────────

data class NewsItem(val title: String, val link: String, val source: String, val pubDate: Long = 0L)

data class Suggestion(
    val symbol: String,
    val name: String,
    val mcCode: String? = null,
    val mcExchange: String? = null
)

data class StockQuoteDetail(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val prevClose: Double,
    val volume: Long,
    val marketCap: Long,
    val currency: String,
    val marketState: String,
    val exchange: String
)

data class StockReturns(
    val day1: Double?,
    val week1: Double?,
    val month1: Double?,
    val month6: Double?,
    val year1: Double?
)

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun parseRssDate(s: String?): Long {
    if (s.isNullOrBlank()) return 0L
    val formats = arrayOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss zzz")
    for (fmt in formats) {
        runCatching { return SimpleDateFormat(fmt, Locale.ENGLISH).parse(s.trim())?.time ?: 0L }
    }
    return 0L
}

private fun timeAgo(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000L           -> "just now"
        diff < 3_600_000L        -> "${diff / 60_000}m ago"
        diff < 86_400_000L       -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L   -> "${diff / 86_400_000}d ago"
        else                     -> SimpleDateFormat("dd MMM", Locale.ENGLISH).format(java.util.Date(epochMs))
    }
}

private fun prettySource(domain: String): String = when {
    domain.contains("moneycontrol")     -> "Moneycontrol"
    domain.contains("economictimes") || domain.contains("indiatimes") -> "Economic Times"
    domain.contains("livemint")         -> "Livemint"
    domain.contains("businesstoday")    -> "Business Today"
    domain.contains("reuters")          -> "Reuters"
    domain.contains("bloomberg")        -> "Bloomberg"
    domain.contains("ndtv")             -> "NDTV"
    domain.contains("cnbctv18")         -> "CNBC TV18"
    domain.contains("zeebiz")           -> "Zee Business"
    domain.contains("financialexpress") -> "Financial Express"
    domain.contains("thehindu")         -> "The Hindu"
    domain.contains("google")           -> "Google News"
    else -> domain.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: domain
}

private fun formatVolume(v: Long): String = when {
    v >= 1_000_000_000 -> String.format("%.2fB", v / 1_000_000_000.0)
    v >= 1_000_000     -> String.format("%.2fM", v / 1_000_000.0)
    v >= 1_000         -> String.format("%.1fK", v / 1_000.0)
    else               -> v.toString()
}

private fun currencyPrefix(currency: String) = when (currency) {
    "INR" -> "₹"; "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"
    "JPY" -> "¥"; "CNY" -> "¥"; "AUD" -> "A$"; "CAD" -> "CA$"
    "SGD" -> "S$"; "HKD" -> "HK$"; "KRW" -> "₩"; "CHF" -> "Fr"
    "INDEX" -> ""   // index values are points, not a currency
    else  -> "$currency "
}

private fun formatPrice(price: Double, currency: String): String {
    val pfx = currencyPrefix(currency)
    return if (currency == "JPY") "$pfx${String.format("%,.0f", price)}"
    else "$pfx${String.format("%,.2f", price)}"
}

private fun formatMarketCap(cap: Long, currency: String): String {
    val isINR = currency == "INR"
    return when {
        isINR && cap >= 10_000_000_000L  -> String.format("₹%.0f Cr", cap / 10_000_000.0)
        isINR && cap >= 10_000_000L      -> String.format("₹%.1f Cr", cap / 10_000_000.0)
        !isINR && cap >= 1_000_000_000L  -> String.format("$%.2fB", cap / 1_000_000_000.0)
        !isINR && cap >= 1_000_000L      -> String.format("$%.2fM", cap / 1_000_000.0)
        else                             -> NumberFormat.getInstance().format(cap)
    }
}

private fun List<Pair<Long, Double>>.returnForDays(days: Int): Double? {
    val cutoffSecs = System.currentTimeMillis() / 1000L - days * 86400L
    val refPrice = firstOrNull { it.first >= cutoffSecs }?.second ?: return null
    val lastPrice = lastOrNull()?.second ?: return null
    return if (refPrice > 0) (lastPrice - refPrice) / refPrice * 100.0 else null
}

// ── Main screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAnalysisScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Search state
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }

    // Selected stock state
    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    var selectedName   by remember { mutableStateOf<String?>(null) }
    var quoteDetail    by remember { mutableStateOf<StockQuoteDetail?>(null) }
    var assetProfile   by remember { mutableStateOf<AssetProfile?>(null) }

    // Chart state
    var chartPoints    by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var basePoints     by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) } // 1Y for returns
    var prevClose      by remember { mutableStateOf<Double?>(null) }
    var range          by remember { mutableStateOf("1mo") }
    var interval       by remember { mutableStateOf("1d") }

    // UI state
    var isLoading      by remember { mutableStateOf(false) }
    var error          by remember { mutableStateOf<String?>(null) }
    var showFullscreen by remember { mutableStateOf(false) }

    // News
    var marketNews    by remember { mutableStateOf(emptyList<NewsItem>()) }
    var top10News     by remember { mutableStateOf(emptyList<NewsItem>()) }
    var pennyNews     by remember { mutableStateOf(emptyList<NewsItem>()) }
    var relatedNews   by remember { mutableStateOf(emptyList<NewsItem>()) }
    var relatedNewsMc by remember { mutableStateOf(emptyList<NewsItem>()) }

    // Recents
    val prefs = ctx.getSharedPreferences("stocks_prefs", android.content.Context.MODE_PRIVATE)
    fun loadRecents(): List<Suggestion> {
        val raw = prefs.getString("recent_list", null) ?: return emptyList()
        return raw.split(';').mapNotNull { e ->
            val p = e.split("::", limit = 2)
            if (p.size == 2) Suggestion(p[0], p[1]) else null
        }
    }
    fun saveRecents(list: List<Suggestion>) {
        prefs.edit().putString("recent_list", list.joinToString(";") { it.symbol + "::" + it.name }).apply()
    }
    var recent by remember { mutableStateOf(loadRecents()) }

    // ── RSS fetch ──────────────────────────────────────────────────────────────

    suspend fun fetchRss(url: String): List<NewsItem> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val conn = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 4000; readTimeout = 4000
            }
            conn.getInputStream().use { stream ->
                val xpp = XmlPullParserFactory.newInstance().newPullParser()
                xpp.setInput(stream, null)
                val out = mutableListOf<NewsItem>()
                var inItem = false; var title: String? = null; var link: String? = null; var pubDateStr: String? = null
                var ev = xpp.eventType
                while (ev != XmlPullParser.END_DOCUMENT) {
                    when (ev) {
                        XmlPullParser.START_TAG -> {
                            val n = xpp.name
                            if (n.equals("item", true)) inItem = true
                            if (inItem && n.equals("title", true)) title = xpp.nextText()
                            if (inItem && n.equals("link", true)) link = xpp.nextText()
                            if (inItem && n.equals("pubDate", true)) pubDateStr = xpp.nextText()
                        }
                        XmlPullParser.END_TAG -> {
                            if (xpp.name.equals("item", true)) {
                                val t = title; val l = link
                                if (!t.isNullOrBlank() && !l.isNullOrBlank()) {
                                    val src = URI(l).host?.removePrefix("www.") ?: ""
                                    out += NewsItem(t.trim(), l.trim(), src, parseRssDate(pubDateStr))
                                }
                                inItem = false; title = null; link = null; pubDateStr = null
                            }
                        }
                    }
                    ev = xpp.next()
                }
                out.take(15)
            }
        }.getOrElse { emptyList() }
    }

    suspend fun fetchRelatedNews(q: String) = fetchRss(
        "https://news.google.com/rss/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}&hl=en-IN&gl=IN&ceid=IN:en"
    )

    suspend fun fetchRelatedMcNews(q: String) = fetchRss(
        "https://news.google.com/rss/search?q=${java.net.URLEncoder.encode("site:moneycontrol.com $q", "UTF-8")}&hl=en-IN&gl=IN&ceid=IN:en"
    )

    // ── Moneycontrol suggest ───────────────────────────────────────────────────

    suspend fun mcSuggest(q: String): List<Suggestion> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val link = "https://www.moneycontrol.com/mccode/common/autosuggestion_solr.php?classic=true&query=" +
                    java.net.URLEncoder.encode(q, "UTF-8") + "&type=1&format=json"
            val json = URL(link).openStream().use { it.readBytes().decodeToString() }
            com.google.gson.JsonParser.parseString(json).asJsonArray.mapNotNull { el ->
                val o = el.asJsonObject
                val name   = o["full_name"]?.asString ?: o["label"]?.asString ?: o["name"]?.asString
                val symbol = o["symbol"]?.asString ?: o["value"]?.asString
                val code   = o["sc_id"]?.asString ?: o["sid"]?.asString
                val exch   = o["ex"]?.asString ?: o["exchange"]?.asString
                if (!name.isNullOrBlank() && !symbol.isNullOrBlank()) Suggestion(symbol, name, code, exch) else null
            }
        }.getOrElse { emptyList() }
    }

    // ── Chart refresh ──────────────────────────────────────────────────────────

    suspend fun refreshChartFor(symbol: String, r: String, itv: String) {
        val chart = runCatching { StocksApiProvider.service.chart(symbol, range = r, interval = itv) }.getOrNull()
        chart?.let {
            val res   = it.chart.result.firstOrNull()
            val times = res?.timestamp ?: emptyList()
            val closes = res?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            val pts = times.zip(closes).mapNotNull { (t, c) -> if (c != null) t to c else null }
            if (pts.isNotEmpty()) {
                chartPoints = pts
                prevClose = prevClose ?: res?.meta?.chartPreviousClose
            }
        }
    }

    // ── Full search + load ─────────────────────────────────────────────────────

    fun search(symOrName: String) {
        if (symOrName.isBlank()) return
        scope.launch {
            isLoading = true; error = null
            chartPoints = emptyList(); basePoints = emptyList()
            quoteDetail = null; assetProfile = null

            // Resolve symbol
            val resolvedSymbol: String? = run {
                val q = symOrName.trim()
                if (q.contains('.') || q.startsWith("^")) q else {
                    val res = runCatching { StocksApiProvider.service.search(q, quotesCount = 20) }.getOrNull()
                    res?.quotes?.firstOrNull { it.symbol?.endsWith(".NS") == true }?.symbol
                        ?: res?.quotes?.firstOrNull()?.symbol ?: "${q.uppercase()}.NS"
                }
            }

            if (resolvedSymbol.isNullOrBlank()) { error = "Symbol not found"; isLoading = false; return@launch }
            selectedSymbol = resolvedSymbol
            selectedName   = resolvedSymbol

            StocksApiProvider.ensureCrumb()

            // Parallel: display chart, 1Y base chart, quote detail
            val chartJob   = async { runCatching { StocksApiProvider.service.chart(resolvedSymbol, range, interval) }.getOrNull() }
            val baseJob    = async { runCatching { StocksApiProvider.service.chart(resolvedSymbol, "1y", "1d") }.getOrNull() }
            val quoteJob   = async { runCatching { StocksApiProvider.service.quotes(resolvedSymbol).quoteResponse.result.firstOrNull() }.getOrNull() }

            val chart   = chartJob.await()
            val base    = baseJob.await()
            val quote   = quoteJob.await()
            val profile = null

            // Chart points
            val r    = chart?.chart?.result?.firstOrNull()
            val times  = r?.timestamp ?: emptyList()
            val closes = r?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            chartPoints = times.zip(closes).mapNotNull { (t, c) -> if (c != null) t to c else null }
            prevClose   = r?.meta?.chartPreviousClose ?: r?.meta?.previousClose

            // 1Y base for returns
            val rb     = base?.chart?.result?.firstOrNull()
            val timesB = rb?.timestamp ?: emptyList()
            val closesB = rb?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            basePoints = timesB.zip(closesB).mapNotNull { (t, c) -> if (c != null) t to c else null }

            // Quote detail
            if (quote != null) {
                val name = quote.longName ?: quote.shortName ?: resolvedSymbol
                selectedName = name
                quoteDetail = StockQuoteDetail(
                    symbol      = resolvedSymbol,
                    name        = name,
                    price       = quote.regularMarketPrice ?: 0.0,
                    change      = quote.regularMarketChange ?: 0.0,
                    changePercent = quote.regularMarketChangePercent ?: 0.0,
                    open        = quote.regularMarketOpen ?: 0.0,
                    high        = quote.regularMarketDayHigh ?: 0.0,
                    low         = quote.regularMarketDayLow ?: 0.0,
                    prevClose   = quote.regularMarketPreviousClose ?: 0.0,
                    volume      = quote.regularMarketVolume ?: 0L,
                    marketCap   = quote.marketCap ?: 0L,
                    currency    = quote.financialCurrency ?: if (resolvedSymbol.startsWith("^")) "INDEX" else "USD",
                    marketState = quote.marketState ?: "REGULAR",
                    exchange    = quote.exchange ?: ""
                )
            }

            assetProfile = profile

            // Save recents
            val newItem = Suggestion(resolvedSymbol, selectedName ?: resolvedSymbol)
            val merged  = (listOf(newItem) + recent.filterNot { it.symbol.equals(resolvedSymbol, true) }).take(4)
            recent = merged; saveRecents(merged)
            prefs.edit().putString("last_symbol", resolvedSymbol).putString("last_name", selectedName).apply()

            // News (parallel)
            val qNews = selectedName ?: resolvedSymbol
            val newsJobs = awaitAll(
                async { fetchRelatedNews(qNews) },
                async { fetchRelatedMcNews(qNews) }
            )
            relatedNews   = newsJobs[0]
            relatedNewsMc = newsJobs[1]

            if (chart == null) error = "Failed to load chart"
            else if (chartPoints.isEmpty()) error = "No chart data. News available below."

            isLoading = false
        }
    }

    // ── Side effects ───────────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        val gnMarket = async { fetchRss("https://news.google.com/rss/search?q=India%20stock%20market&hl=en-IN&gl=IN&ceid=IN:en") }
        val mcTop    = async { fetchRss("https://www.moneycontrol.com/rss/MCtopnews.xml") }
        val mcMkt    = async { fetchRss("https://www.moneycontrol.com/rss/marketreports.xml") }
        marketNews = (gnMarket.await() + mcTop.await() + mcMkt.await()).distinctBy { it.link }.shuffled()
        top10News  = fetchRss("https://news.google.com/rss/search?q=top%2010%20stocks%20India&hl=en-IN&gl=IN&ceid=IN:en")
        pennyNews  = fetchRss("https://news.google.com/rss/search?q=top%2010%20penny%20stocks%20India&hl=en-IN&gl=IN&ceid=IN:en")
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { suggestions = emptyList(); return@LaunchedEffect }
        delay(300)
        val mc    = mcSuggest(q)
        val yahoo = runCatching { StocksApiProvider.service.search(q, quotesCount = 20) }.getOrNull()
        val mapped = yahoo?.quotes?.mapNotNull {
            val sym = it.symbol; val n = it.longname ?: it.shortname
            if (!sym.isNullOrBlank() && !n.isNullOrBlank()) Suggestion(sym, n) else null
        } ?: emptyList()
        suggestions = (mc + mapped).distinctBy { it.symbol }.take(12)
    }

    // ── Compute returns ────────────────────────────────────────────────────────

    val returns = remember(basePoints, quoteDetail) {
        if (basePoints.isEmpty()) return@remember null
        val qd = quoteDetail
        StockReturns(
            day1   = qd?.changePercent,
            week1  = basePoints.returnForDays(7),
            month1 = basePoints.returnForDays(30),
            month6 = basePoints.returnForDays(180),
            year1  = basePoints.returnForDays(365)
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────────

    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                title = {
                    Text(
                        text = selectedName?.let { it } ?: "Stocks & Analysis",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            // Market index cards with sparklines
            MarketIndicesMarquee(modifier = Modifier.fillMaxWidth())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ── Search or stock chip ──────────────────────────────────────
                if (selectedSymbol.isNullOrBlank()) {
                    SearchSection(
                        query = query, onQueryChange = { query = it },
                        suggestions = suggestions,
                        onItemClick = { s ->
                            selectedSymbol = s.symbol; selectedName = s.name
                            suggestions = emptyList(); query = ""
                            search(s.symbol)
                            focusManager.clearFocus(); keyboard?.hide()
                        },
                        onSearch = { search(query.trim()); focusManager.clearFocus(); keyboard?.hide() },
                        onClear = { query = "" },
                        recent = recent
                    )
                } else {
                    FilterChip(
                        selected = true,
                        onClick = {
                            selectedSymbol = null; selectedName = null
                            chartPoints = emptyList(); quoteDetail = null
                            relatedNews = emptyList(); relatedNewsMc = emptyList(); error = null
                        },
                        label = { Text("${selectedName ?: selectedSymbol} ($selectedSymbol)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Loading / error
                AnimatedVisibility(isLoading) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                         style = MaterialTheme.typography.bodySmall)
                }

                // ── Stock detail sections ─────────────────────────────────────
                if (!selectedSymbol.isNullOrBlank()) {

                    // Stock header
                    if (quoteDetail != null) {
                        StockHeaderCard(quoteDetail!!)
                    } else if (chartPoints.isNotEmpty()) {
                        FallbackHeader(selectedSymbol!!, selectedName, chartPoints)
                    }

                    // Chart
                    if (chartPoints.isNotEmpty()) {
                        PriceChartCard(
                            chartPoints   = chartPoints,
                            symbol        = selectedSymbol!!,
                            range         = range,
                            prevClose     = prevClose,
                            currency      = quoteDetail?.currency ?: "USD",
                            onFullscreen  = { showFullscreen = true },
                            onRangeChange = { r, i ->
                                range = r; interval = i
                                scope.launch { refreshChartFor(selectedSymbol!!, r, i) }
                            }
                        )

                        // Returns row
                        returns?.let { ReturnsCard(it) }
                    }

                    // About section
                    assetProfile?.let { AboutCard(it) }

                    // Related news
                    if (relatedNews.isNotEmpty() || relatedNewsMc.isNotEmpty()) {
                        CompactNewsSection(
                            title = "Related News · ${selectedName ?: selectedSymbol}",
                            items = (relatedNews + relatedNewsMc).distinctBy { it.link }.take(8)
                        )
                    }
                }

                // Market news
                CompactNewsSection("Market News", marketNews)

                // Ad
                LocalActivity.current?.let { act ->
                    AppodealBanner(activity = act, modifier = Modifier.fillMaxWidth().height(50.dp))
                }

                CompactNewsSection("Top 10 Stocks", top10News)
                CompactNewsSection("Top 10 Penny Stocks", pennyNews)

            } // end scroll Column
        }
    }

    // ── Fullscreen chart dialog ────────────────────────────────────────────────
    if (showFullscreen && chartPoints.isNotEmpty()) {
        FullscreenChartDialog(
            chartPoints   = chartPoints,
            symbol        = selectedSymbol ?: "",
            range         = range,
            prevClose     = prevClose,
            quoteDetail   = quoteDetail,
            returns       = returns,
            onDismiss     = { showFullscreen = false },
            onRangeChange = { r, i ->
                range = r; interval = i
                scope.launch { refreshChartFor(selectedSymbol!!, r, i) }
            }
        )
    }
}

// ── Stock header card ──────────────────────────────────────────────────────────

@Composable
private fun StockHeaderCard(qd: StockQuoteDetail) {
    val isPositive  = qd.changePercent >= 0
    val changeColor = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
    val changeBg    = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Symbol + market state
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(qd.symbol, style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(qd.name, style = MaterialTheme.typography.titleMedium,
                         fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                MarketStateBadge(qd.marketState)
            }

            // Price row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedPriceText(
                    text     = formatPrice(qd.price, qd.currency),
                    isRising = qd.changePercent >= 0,
                    style    = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(changeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            String.format("%+.2f", qd.change),
                            style = MaterialTheme.typography.bodyMedium,
                            color = changeColor, fontWeight = FontWeight.Bold
                        )
                        Text(
                            String.format("%+.2f%%", qd.changePercent),
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor
                        )
                    }
                }
            }

            // OHLCV grid
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MetricsGrid(qd)
        }
    }
}

@Composable
private fun MetricsGrid(qd: StockQuoteDetail) {
    val curr = qd.currency

    val metrics = listOf(
        "Day Open"   to formatPrice(qd.open, curr),
        "Day High"   to formatPrice(qd.high, curr),
        "Day Low"    to formatPrice(qd.low, curr),
        "Prev Close" to formatPrice(qd.prevClose, curr),
        "Volume"     to formatVolume(qd.volume),
        "Mkt Cap"    to if (qd.marketCap > 0) formatMarketCap(qd.marketCap, curr) else "—"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodySmall,
                             fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketStateBadge(state: String) {
    val (label, bg, fg) = when (state) {
        "PRE"     -> Triple("PRE",     Color(0xFFFEF9C3), Color(0xFFCA8A04))
        "REGULAR" -> Triple("OPEN",    Color(0xFFDCFCE7), Color(0xFF16A34A))
        "POST"    -> Triple("AFTER",   Color(0xFFE0F2FE), Color(0xFF0284C7))
        else      -> Triple("CLOSED",  Color(0xFFF1F5F9), Color(0xFF64748B))
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FallbackHeader(symbol: String, name: String?, pts: List<Pair<Long, Double>>) {
    val last  = pts.lastOrNull()?.second
    val first = pts.firstOrNull()?.second
    val chg   = if (last != null && first != null) last - first else null
    val pct   = if (chg != null && first != null && first != 0.0) chg / first * 100.0 else null
    val up    = (pct ?: 0.0) >= 0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name ?: symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(last?.let { "₹${String.format("%,.2f", it)}" } ?: "—",
                 style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(if (up) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    String.format("%+.2f (%s)", chg ?: 0.0, pct?.let { String.format("%.2f%%", it) } ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (up) Color(0xFF16A34A) else Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Price chart card ───────────────────────────────────────────────────────────

@Composable
fun PriceChartCard(
    chartPoints: List<Pair<Long, Double>>,
    symbol: String,
    range: String,
    prevClose: Double?,
    onFullscreen: () -> Unit,
    onRangeChange: (String, String) -> Unit,
    chartHeight: Int = 280,
    currency: String = "USD"
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Price Chart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onFullscreen, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Fullscreen, "Expand chart",
                         modifier = Modifier.size(20.dp),
                         tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Chart
            SmoothLineChart(
                chartPoints = chartPoints,
                symbol      = symbol,
                range       = range,
                prevClose   = prevClose,
                currency    = currency,
                modifier    = Modifier.fillMaxWidth().height(chartHeight.dp)
            )

            // Range pills
            RangePills(range = range, onSelect = onRangeChange)
        }
    }
}

@Composable
fun SmoothLineChart(
    chartPoints: List<Pair<Long, Double>>,
    symbol: String,
    range: String,
    prevClose: Double?,
    modifier: Modifier = Modifier,
    currency: String = "USD"
) {
    val closes = chartPoints.map { it.second }
    val isPositive = (closes.lastOrNull() ?: 0.0) >= (closes.firstOrNull() ?: 0.0)
    val lineColorInt = if (isPositive) android.graphics.Color.parseColor("#16A34A")
                       else            android.graphics.Color.parseColor("#DC2626")

    AndroidView(
        modifier = modifier,
        factory  = { context ->
            LineChart(context).apply {
                description.isEnabled          = false
                axisRight.isEnabled            = false
                legend.isEnabled               = false
                xAxis.position                 = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.textSize                 = 9f
                xAxis.textColor                = android.graphics.Color.GRAY
                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor             = android.graphics.Color.argb(30, 0, 0, 0)
                axisLeft.textSize              = 9f
                axisLeft.textColor             = android.graphics.Color.GRAY
                isHighlightPerTapEnabled       = true
                isHighlightPerDragEnabled      = true
                setTouchEnabled(true)
                setPinchZoom(false)
                setScaleEnabled(false)
                extraBottomOffset              = 4f
            }
        },
        update = { chart ->
            // Previous close limit line
            chart.axisLeft.removeAllLimitLines()
            if (prevClose != null && prevClose > 0) {
                chart.axisLeft.addLimitLine(LimitLine(prevClose.toFloat(), "").apply {
                    lineWidth      = 1f
                    lineColor      = android.graphics.Color.argb(80, 100, 100, 100)
                    enableDashedLine(6f, 4f, 0f)
                })
            }

            // Build gradient fill
            val gradColor1 = if (isPositive) android.graphics.Color.argb(80, 22, 163, 74)
                             else            android.graphics.Color.argb(80, 220, 38, 38)
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(gradColor1, android.graphics.Color.TRANSPARENT)
            )

            val entries  = chartPoints.mapIndexed { i, p -> Entry(i.toFloat(), p.second.toFloat()) }
            val dataset  = LineDataSet(entries, symbol).apply {
                color              = lineColorInt
                lineWidth          = 2f
                mode               = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity     = 0.15f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillDrawable       = gradient
                highLightColor     = android.graphics.Color.GRAY
                highlightLineWidth = 1.2f
                enableDashedHighlightLine(6f, 4f, 0f)
                setDrawHorizontalHighlightIndicator(false)
                setDrawVerticalHighlightIndicator(true)
            }

            // X-axis labels
            val sdf = when (range) {
                "1d"  -> SimpleDateFormat("HH:mm", Locale.ENGLISH)
                "5d"  -> SimpleDateFormat("EEE", Locale.ENGLISH)
                else  -> SimpleDateFormat("dd MMM", Locale.ENGLISH)
            }
            val labels = chartPoints.mapIndexed { i, (ts, _) ->
                if (i % maxOf(1, chartPoints.size / 5) == 0)
                    runCatching { sdf.format(java.util.Date(ts * 1000)) }.getOrDefault("")
                else ""
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.granularity    = 1f
            chart.xAxis.labelCount     = 5

            // Custom tooltip marker
            val bgPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(220, 30, 30, 30) }
            val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 30f }
            val pad = 10f
            val r   = RectF()
            var markerText = ""
            val marker = object : com.github.mikephil.charting.components.IMarker {
                override fun refreshContent(e: Entry?, highlight: com.github.mikephil.charting.highlight.Highlight?) {
                    if (e == null) return
                    val idx = e.x.toInt().coerceIn(0, chartPoints.size - 1)
                    val ts  = chartPoints[idx].first * 1000
                    val lbl = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH).format(java.util.Date(ts))
                    markerText = "$lbl  ${currencyPrefix(currency)}${String.format("%.2f", e.y)}"
                }
                override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
                    if (canvas == null || markerText.isEmpty()) return
                    val w     = txtPaint.measureText(markerText)
                    val h     = txtPaint.fontMetrics.let { it.bottom - it.top }
                    val cx    = posX
                    val cy    = posY - 20f - h
                    r.set(cx - w / 2 - pad, cy - pad, cx + w / 2 + pad, cy + h + pad / 2)
                    val bounds = chart.viewPortHandler.contentRect
                    val dx = when {
                        r.left  < bounds.left  -> bounds.left  - r.left
                        r.right > bounds.right -> bounds.right - r.right
                        else -> 0f
                    }
                    val dy = if (r.top < bounds.top) bounds.top - r.top else 0f
                    r.offset(dx, dy)
                    canvas.drawRoundRect(r, 8f, 8f, bgPaint)
                    canvas.drawText(markerText, r.left + pad, r.top + pad - txtPaint.fontMetrics.top, txtPaint)
                }
                override fun getOffset() = com.github.mikephil.charting.utils.MPPointF(0f, 0f)
                override fun getOffsetForDrawingAtPoint(x: Float, y: Float) = getOffset()
            }
            chart.marker = marker
            chart.setDrawMarkers(true)
            chart.data = LineData(dataset)
            chart.animateX(500, Easing.EaseInOutQuart)
            chart.invalidate()
        }
    )
}

// ── Range pills ────────────────────────────────────────────────────────────────

@Composable
fun RangePills(range: String, onSelect: (String, String) -> Unit) {
    val opts = listOf("1D" to ("1d" to "5m"), "1W" to ("5d" to "15m"),
                      "1M" to ("1mo" to "1d"), "6M" to ("6mo" to "1d"), "1Y" to ("1y" to "1d"))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        opts.forEach { (label, ri) ->
            val selected = range == ri.first
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(ri.first, ri.second) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style    = MaterialTheme.typography.labelMedium,
                    color    = if (selected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Returns card ───────────────────────────────────────────────────────────────

@Composable
fun ReturnsCard(returns: StockReturns) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Returns", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(
                    "1D" to returns.day1,
                    "1W" to returns.week1,
                    "1M" to returns.month1,
                    "6M" to returns.month6,
                    "1Y" to returns.year1
                ).forEach { (label, pct) ->
                    ReturnCell(label, pct)
                }
            }
        }
    }
}

@Composable
private fun ReturnCell(label: String, pct: Double?) {
    val isPositive = (pct ?: 0.0) >= 0
    val color = when {
        pct == null    -> MaterialTheme.colorScheme.onSurfaceVariant
        pct >= 0       -> Color(0xFF16A34A)
        else           -> Color(0xFFDC2626)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = pct?.let { String.format("%+.2f%%", it) } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = color, fontWeight = FontWeight.SemiBold
        )
    }
}

// ── About card ─────────────────────────────────────────────────────────────────

@Composable
private fun AboutCard(profile: AssetProfile) {
    var expanded by remember { mutableStateOf(false) }
    val summary = profile.longBusinessSummary
        ?.replace(Regex("<[^>]+>"), "")
        ?.replace(Regex("\\s+"), " ")
        ?.trim() ?: return
    val chips = listOf(profile.sector, profile.industry, profile.country).filterNotNull()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("About", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Pre-truncate the string instead of using maxLines inside a verticalScroll Column.
            // maxLines with a long paragraph reports the full paragraph height in unbounded layout,
            // creating a large invisible gap above the visible lines.
            val displayText = if (expanded || summary.length <= 160) summary
                              else summary.take(160).trimEnd() + "…"
            Text(
                text  = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (summary.length > 160) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = if (expanded) "Show less" else "Read more",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        }
    }
}

// ── Fullscreen chart dialog ────────────────────────────────────────────────────

@Composable
private fun FullscreenChartDialog(
    chartPoints: List<Pair<Long, Double>>,
    symbol: String,
    range: String,
    prevClose: Double?,
    quoteDetail: StockQuoteDetail?,
    returns: StockReturns?,
    onDismiss: () -> Unit,
    onRangeChange: (String, String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toolbar
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            quoteDetail?.name ?: symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(symbol, style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }

                quoteDetail?.let {
                    val isPos = it.changePercent >= 0
                    val changeColor = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatPrice(it.price, it.currency), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    String.format("%+.2f", it.change),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = changeColor, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    String.format("%+.2f%%", it.changePercent),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = changeColor
                                )
                            }
                        }
                    }
                }

                // Large chart
                SmoothLineChart(
                    chartPoints = chartPoints, symbol = symbol,
                    range = range, prevClose = prevClose,
                    currency = quoteDetail?.currency ?: "USD",
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                )

                RangePills(range = range, onSelect = onRangeChange)

                returns?.let { ReturnsCard(it) }

                quoteDetail?.let { MetricsGrid(it) }
            }
        }
    }
}

// ── Compact news section ───────────────────────────────────────────────────────

@Composable
private fun CompactNewsSection(title: String, items: List<NewsItem>) {
    if (items.isEmpty()) return
    val ctx = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                     fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${items.size}", style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))

            // Regular Column — avoids LazyColumn-inside-verticalScroll measurement corruption
            Column(modifier = Modifier.fillMaxWidth()) {
                val capped = items.take(8)
                capped.forEachIndexed { idx, item ->
                    CompactNewsRow(item) {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    if (idx < capped.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactNewsRow(item: NewsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(top = 5.dp).size(4.dp)
                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                 style = MaterialTheme.typography.bodySmall, lineHeight = 15.sp)
            val src  = prettySource(item.source)
            val time = timeAgo(item.pubDate)
            Text(
                text  = if (time.isNotEmpty()) "$src · $time" else src,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

// ── Search section ─────────────────────────────────────────────────────────────

@Composable
fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<Suggestion>,
    onItemClick: (Suggestion) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    recent: List<Suggestion>
) {
    val focusManager = LocalFocusManager.current
    val keyboard     = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value         = query,
            onValueChange = onQueryChange,
            label         = { Text("Search company or symbol") },
            singleLine    = true,
            leadingIcon   = { Icon(Icons.Default.Search, null) },
            trailingIcon  = {
                if (query.isNotEmpty()) IconButton(onClick = { onClear(); focusManager.clearFocus(); keyboard?.hide() }) {
                    Icon(Icons.Default.Close, "Clear")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(); focusManager.clearFocus(); keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (suggestions.isNotEmpty()) {
            Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                ) {
                    items(suggestions, key = { it.symbol }) { s ->
                        ListItem(
                            headlineContent   = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(s.symbol, style = MaterialTheme.typography.bodySmall) },
                            leadingContent    = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth().clickable {
                                onItemClick(s); focusManager.clearFocus(); keyboard?.hide()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Recently Viewed",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.take(4).forEach { s ->
                        AssistChip(
                            onClick = { onQueryChange(s.symbol); onSearch(); focusManager.clearFocus(); keyboard?.hide() },
                            label   = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    }
}
