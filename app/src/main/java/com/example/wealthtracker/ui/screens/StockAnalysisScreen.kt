package com.example.wealthtracker.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
 
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import com.example.wealthtracker.network.StocksApiProvider
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

data class NewsItem(val title: String, val link: String, val source: String)

// Promote shared models to top level so helper composables can use them
data class Suggestion(val symbol: String, val name: String, val mcCode: String? = null, val mcExchange: String? = null)
// Marquee removed: no QuoteDisplay or Alpha key needed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAnalysisScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }

    var selectedSymbol by remember { mutableStateOf<String?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var chartPoints by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }

    var range by remember { mutableStateOf("1mo") }
    var interval by remember { mutableStateOf("1d") }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showSma20 by remember { mutableStateOf(false) }
    var showSma50 by remember { mutableStateOf(false) }
    var showRsi by remember { mutableStateOf(false) }

    var marketNews by remember { mutableStateOf(emptyList<NewsItem>()) }
    var top10News by remember { mutableStateOf(emptyList<NewsItem>()) }
    var pennyNews by remember { mutableStateOf(emptyList<NewsItem>()) }
    var relatedNews by remember { mutableStateOf(emptyList<NewsItem>()) }
    var relatedNewsMc by remember { mutableStateOf(emptyList<NewsItem>()) }

    // Simple prefs to persist last-searched symbol
    val prefs = LocalContext.current.getSharedPreferences("stocks_prefs", android.content.Context.MODE_PRIVATE)
    // Recent searches (symbol + name), last 4
    fun loadRecents(): List<Suggestion> {
        val raw = prefs.getString("recent_list", null) ?: return emptyList()
        return raw.split(';').mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            if (parts.size == 2) Suggestion(parts[0], parts[1]) else null
        }
    }
    fun saveRecents(list: List<Suggestion>) {
        val s = list.joinToString(";") { it.symbol + "::" + it.name }
        prefs.edit().putString("recent_list", s).apply()
    }
    var recent by remember { mutableStateOf(loadRecents()) }

    // RSS fetch
    suspend fun fetchRss(url: String): List<NewsItem> =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val conn = URL(url).openConnection().apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                conn.getInputStream().use { stream ->
                    val fac = XmlPullParserFactory.newInstance()
                    val xpp = fac.newPullParser()
                    xpp.setInput(stream, null)

                    val out = mutableListOf<NewsItem>()
                    var event = xpp.eventType
                    var inItem = false
                    var title: String? = null
                    var link: String? = null

                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                val n = xpp.name
                                if (n.equals("item", true)) inItem = true
                                if (inItem && n.equals("title", true)) title = xpp.nextText()
                                if (inItem && n.equals("link", true)) link = xpp.nextText()
                            }
                            XmlPullParser.END_TAG -> {
                                if (xpp.name.equals("item", true)) {
                                    val t = title
                                    val l = link
                                    if (!t.isNullOrBlank() && !l.isNullOrBlank()) {
                                        val src = URI(l).host?.removePrefix("www.") ?: ""
                                        out += NewsItem(t.trim(), l.trim(), src)
                                    }
                                    inItem = false
                                    title = null
                                    link = null
                                }
                            }
                        }
                        event = xpp.next()
                    }
                    out.take(15)
                }
            }.getOrElse { emptyList() }
        }

    // Related news for a company/keyword using Google News RSS
    suspend fun fetchRelatedNews(query: String): List<NewsItem> {
        if (query.isBlank()) return emptyList()
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://news.google.com/rss/search?q=$q&hl=en-IN&gl=IN&ceid=IN:en"
        return fetchRss(url)
    }

    // Moneycontrol-focused related news using Google News site filter
    suspend fun fetchRelatedMcNews(query: String): List<NewsItem> {
        if (query.isBlank()) return emptyList()
        val q = java.net.URLEncoder.encode("site:moneycontrol.com $query", "UTF-8")
        val url = "https://news.google.com/rss/search?q=$q&hl=en-IN&gl=IN&ceid=IN:en"
        return fetchRss(url)
    }

    fun search(symOrName: String) {
        if (symOrName.isBlank()) return
        scope.launch {
            isLoading = true
            error = null
            chartPoints = emptyList()

            // Resolve symbol
            val resolvedSymbol: String? = run {
                val q = symOrName.trim()
                if (q.contains('.') || q.startsWith("^")) q else {
                    val res = runCatching {
                        StocksApiProvider.service.search(query = q, quotesCount = 20)
                    }.getOrNull()

                    val bestNs = res?.quotes?.firstOrNull { it.symbol?.endsWith(".NS") == true }?.symbol
                    bestNs ?: res?.quotes?.firstOrNull()?.symbol ?: "${q.uppercase()}.NS"
                }
            }

            if (resolvedSymbol.isNullOrBlank()) {
                error = "Symbol not found"
                isLoading = false
                return@launch
            }
            selectedSymbol = resolvedSymbol
            selectedName = resolvedSymbol

            val svc = StocksApiProvider.service
            val chart =
                runCatching { svc.chart(resolvedSymbol, range, interval) }.getOrNull()

            if (chart != null) {
                val r = chart.chart.result.firstOrNull()
                val times = r?.timestamp ?: emptyList()
                val closes = r?.indicators?.quote?.firstOrNull()?.close ?: emptyList()

                chartPoints = times.zip(closes).mapNotNull { (t, c) ->
                    if (c != null) t to c else null
                }
                // Save last selection
                prefs.edit().putString("last_symbol", resolvedSymbol).putString("last_name", selectedName).apply()
                // Update recents (most recent first, unique by symbol, keep 4)
                val newItem = Suggestion(resolvedSymbol!!, selectedName ?: resolvedSymbol)
                val merged = (listOf(newItem) + recent.filterNot { it.symbol.equals(resolvedSymbol, true) }).take(4)
                recent = merged
                saveRecents(merged)
                // Fetch related news for selected
                val qForNews = selectedName ?: resolvedSymbol
                relatedNews = fetchRelatedNews(qForNews ?: resolvedSymbol)
                relatedNewsMc = fetchRelatedMcNews(qForNews ?: resolvedSymbol)
            } else {
                error = "Failed to fetch chart"
            }

            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val gnMarket = fetchRss("https://news.google.com/rss/search?q=India%20stock%20market&hl=en-IN&gl=IN&ceid=IN:en")
        val mcTop = fetchRss("https://www.moneycontrol.com/rss/MCtopnews.xml")
        val mcMarket = fetchRss("https://www.moneycontrol.com/rss/marketreports.xml")
        marketNews = (gnMarket + mcTop + mcMarket).distinctBy { it.link }.shuffled()

        top10News = fetchRss("https://news.google.com/rss/search?q=top%2010%20stocks%20India&hl=en-IN&gl=IN&ceid=IN:en")
        pennyNews = fetchRss("https://news.google.com/rss/search?q=top%2010%20penny%20stocks%20India&hl=en-IN&gl=IN&ceid=IN:en")

        // Do not auto-load last searched symbol; wait for explicit user action
    }

    // Moneycontrol Suggest
    suspend fun mcSuggest(q: String): List<Suggestion> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val link =
                    "https://www.moneycontrol.com/mccode/common/autosuggestion_solr.php?classic=true&query=" +
                            java.net.URLEncoder.encode(q, "UTF-8") +
                            "&type=1&format=json"

                val json = URL(link).openStream().use { it.readBytes().decodeToString() }
                val arr = com.google.gson.JsonParser.parseString(json).asJsonArray

                arr.mapNotNull { el ->
                    val o = el.asJsonObject
                    val name = o["full_name"]?.asString ?: o["label"]?.asString ?: o["name"]?.asString
                    val symbol = o["symbol"]?.asString ?: o["value"]?.asString
                    val code = o["sc_id"]?.asString ?: o["sid"]?.asString
                    val exch = o["ex"]?.asString ?: o["exchange"]?.asString

                    if (!name.isNullOrBlank()) Suggestion(symbol ?: name, name, code, exch)
                    else null
                }
            }.getOrElse { emptyList() }
        }
    }

    // Lightweight chart refresh for range changes without clearing chart or showing global loader
    suspend fun refreshChartFor(symbol: String, r: String, itv: String) {
        val svc = StocksApiProvider.service
        val chart = runCatching { svc.chart(symbol, range = r, interval = itv) }.getOrNull()
        chart?.let {
            val res = it.chart.result.firstOrNull()
            val times = res?.timestamp ?: emptyList()
            val closes = res?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            val pts = times.zip(closes).mapNotNull { (t, c) -> if (c != null) t to c else null }
            if (pts.isNotEmpty()) {
                chartPoints = pts
            }
        }
    }

    // Marquee Google helpers removed

    

    // Suggestion fetch debounce
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(300)

        val mc = mcSuggest(q)

        val yahoo = runCatching {
            StocksApiProvider.service.search(query = q, quotesCount = 20)
        }.getOrNull()

        val mapped = yahoo?.quotes?.mapNotNull {
            val sym = it.symbol
            val n = it.longname ?: it.shortname
            if (!sym.isNullOrBlank() && !n.isNullOrBlank()) Suggestion(sym, n) else null
        } ?: emptyList()

        val merged = (mc + mapped).distinctBy { it.symbol }.take(12)
        suggestions = merged
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text("Stocks and Analysis") }
            )
        }
    ) { inner ->

        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Marquee removed

            // Search + suggestions
            SearchSection(
                query = query,
                onQueryChange = { query = it },
                suggestions = suggestions,
                onItemClick = { s ->
                    selectedName = s.name
                    search(s.symbol)
                },
                onSearch = { search(query.trim()) },
                onClear = { query = "" },
                recent = recent
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            // Header
            if (!selectedSymbol.isNullOrBlank() && chartPoints.isNotEmpty()) {
                SelectedHeader(selectedSymbol!!, selectedName, chartPoints)
            }

            if (chartPoints.isNotEmpty()) {
                PriceChart(chartPoints, selectedSymbol ?: "", range, showSma20, showSma50)
                // Range filters under the graph
                RangeFilters(
                    range = range,
                    interval = interval,
                    onChange = { r, i ->
                        range = r; interval = i
                        selectedSymbol?.let { sym ->
                            scope.launch { refreshChartFor(sym, r, i) }
                        }
                    }
                )
            }

            if (showRsi && chartPoints.isNotEmpty()) {
                RsiChart(chartPoints)
            }

            // Related news for selected instrument (split 3 + 3)
            if (!selectedSymbol.isNullOrBlank()) {
                val title = "Related News " + (selectedName?.let { "- $it" } ?: "")
                Text(title.trim(), style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        NewsListAll(relatedNews.take(3))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        NewsListAll(relatedNewsMc.take(3))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // News blocks
            NewsSection("Market News", marketNews)
            // AdMob banner between Market News and Top 10 Stocks; render only when loaded
            run {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var adLoaded by remember { mutableStateOf(false) }
                val adView = remember(ctx) {
                    com.google.android.gms.ads.AdView(ctx).apply {
                        adUnitId = "ca-app-pub-4934815537317220/1418248826"
                    }
                }
                DisposableEffect(Unit) {
                    val dm = ctx.resources.displayMetrics
                    val adWidthDp = (dm.widthPixels / dm.density).toInt()
                    val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthDp)
                    adView.setAdSize(adaptiveSize)
                    adView.adListener = object : AdListener() {
                        override fun onAdLoaded() { adLoaded = true }
                        override fun onAdFailedToLoad(error: LoadAdError) { 
                            adLoaded = false
                            android.util.Log.e("StockAnalysisAd", "Ad failed: ${error.message} (${error.code})")
                        }
                    }
                    adView.loadAd(AdRequest.Builder().build())
                    onDispose {
                        adView.destroy()
                    }
                }
                if (adLoaded) {
                    AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
                }
            }
            NewsSection("Top 10 Stocks - News", top10News)
            NewsSection("Top 10 Penny Stocks - News", pennyNews)
        }
    }
} // THIS BRACE FIXES YOUR FILE


@Composable
private fun NewsSection(title: String, items: List<NewsItem>) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        NewsGrid(items.take(12))
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun NewsListAll(items: List<NewsItem>) {
    val ctx = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.link }) { n ->
            ListItem(
                headlineContent = { Text(n.title) },
                supportingContent = {
                    Text(n.source, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(n.link))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    }
            )
            HorizontalDivider()
        }
    }
}

// MarqueeSection removed

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
    val keyboard = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search company") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onClear()
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearch()
                focusManager.clearFocus()
                keyboard?.hide()
            }),
            modifier = Modifier.fillMaxWidth()
        )
        // Suggestions list
        if (suggestions.isNotEmpty()) {
            Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(suggestions, key = { it.symbol }) { s ->
                        ListItem(
                            headlineContent = { Text(s.name) },
                            supportingContent = { Text(s.symbol) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemClick(s)
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        // Removed under-field CTAs; rely on IME Search and trailing X
        // Recent search chips (last 4); tap fills and searches immediately
        if (recent.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recent.take(4).forEach { s ->
                    AssistChip(
                        onClick = {
                            onQueryChange(s.symbol)
                            onSearch()
                            focusManager.clearFocus()
                            keyboard?.hide()
                        },
                        label = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedHeader(symbol: String, name: String?, chartPoints: List<Pair<Long, Double>>) {
    val displayName = name ?: symbol
    val last = chartPoints.lastOrNull()?.second
    val first = chartPoints.firstOrNull()?.second
    val chg = if (last != null && first != null) last - first else null
    val pct = if (chg != null && first != null && first != 0.0) (chg / first) * 100.0 else null
    val up = (pct ?: 0.0) >= 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(displayName, style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(last?.let { "₹" + String.format("%.2f", it) } ?: "—", style = MaterialTheme.typography.headlineLarge)
            AssistChip(
                onClick = {},
                label = { Text(String.format("%+.2f (%s)", chg ?: 0.0, pct?.let { String.format("%.2f%%", it) } ?: "—")) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (up) Color(0xFF2E7D32) else MaterialTheme.colorScheme.errorContainer,
                    labelColor = if (up) Color.White else MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    }
}

@Composable
private fun PriceChart(
    chartPoints: List<Pair<Long, Double>>,
    symbol: String,
    range: String,
    showSma20: Boolean,
    showSma50: Boolean
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                legend.isEnabled = false
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(true)
                isHighlightPerTapEnabled = true
                isHighlightPerDragEnabled = true
            }
        },
        update = { chart ->
            fun sma(values: List<Double>, period: Int): List<Float?> {
                if (values.size < period) return List(values.size) { null }
                val res = MutableList<Float?>(values.size) { null }
                var sum = 0.0
                for (i in values.indices) {
                    sum += values[i]
                    if (i >= period) sum -= values[i - period]
                    if (i >= period - 1) res[i] = (sum / period).toFloat()
                }
                return res
            }
            val closes = chartPoints.map { it.second }
            val base = chartPoints.mapIndexed { idx, p -> Entry(idx.toFloat(), p.second.toFloat()) }
            val dataSets = mutableListOf<LineDataSet>()
            val main = LineDataSet(base, symbol).apply {
                color = android.graphics.Color.BLUE
                setDrawCircles(false)
                lineWidth = 1.8f
            }
            dataSets.add(main)
            if (showSma20) {
                val s20 = sma(closes, 20)
                val e20 = s20.mapIndexedNotNull { i, v -> v?.let { Entry(i.toFloat(), it) } }
                dataSets.add(LineDataSet(e20, "SMA20").apply {
                    color = android.graphics.Color.MAGENTA
                    setDrawCircles(false)
                    lineWidth = 1.3f
                })
            }
            if (showSma50) {
                val s50 = sma(closes, 50)
                val e50 = s50.mapIndexedNotNull { i, v -> v?.let { Entry(i.toFloat(), it) } }
                dataSets.add(LineDataSet(e50, "SMA50").apply {
                    color = android.graphics.Color.GREEN
                    setDrawCircles(false)
                    lineWidth = 1.3f
                })
            }
            val sdf = when (range) {
                "1d" -> SimpleDateFormat("HH:mm", Locale.ENGLISH)
                "5d" -> SimpleDateFormat("EEE HH:mm", Locale.ENGLISH)
                else -> SimpleDateFormat("dd MMM", Locale.ENGLISH)
            }
            val labels = chartPoints.map { p ->
                runCatching { sdf.format(java.util.Date(p.first * 1000)) }.getOrDefault("")
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.granularity = 1f
            chart.xAxis.labelRotationAngle = 0f
            chart.data = LineData(dataSets as List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>)
            // On-chart marker for value/date
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(200, 33, 33, 33) }
            val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 32f
            }
            val pad = 12f
            val r = RectF()
            var markerText: String = ""
            val marker = object : com.github.mikephil.charting.components.IMarker {
                override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: com.github.mikephil.charting.highlight.Highlight?) {
                    if (e == null) return
                    val idx = e.x.toInt().coerceIn(0, chartPoints.size - 1)
                    val ts = chartPoints[idx].first * 1000
                    val sdfLbl = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH)
                    markerText = sdfLbl.format(java.util.Date(ts)) + "  •  ₹" + String.format("%.2f", e.y)
                }
                override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
                    if (canvas == null || markerText.isEmpty()) return
                    val w = txtPaint.measureText(markerText)
                    val h = txtPaint.fontMetrics.let { it.bottom - it.top }
                    val halfW = w / 2f
                    val cx = posX
                    val cy = posY - 24f - h
                    r.set(cx - halfW - pad, cy - pad, cx + halfW + pad, cy + h + pad / 2f)
                    // Keep inside content bounds
                    val bounds = chart.viewPortHandler.contentRect
                    val dx = when {
                        r.left < bounds.left -> bounds.left - r.left
                        r.right > bounds.right -> bounds.right - r.right
                        else -> 0f
                    }
                    val dy = if (r.top < bounds.top) bounds.top - r.top else 0f
                    r.offset(dx, dy)
                    canvas.drawRoundRect(r, 12f, 12f, bgPaint)
                    canvas.drawText(markerText, r.left + pad, r.top + pad - txtPaint.fontMetrics.top, txtPaint)
                }
                override fun getOffset(): com.github.mikephil.charting.utils.MPPointF = com.github.mikephil.charting.utils.MPPointF(0f, 0f)
                override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): com.github.mikephil.charting.utils.MPPointF = com.github.mikephil.charting.utils.MPPointF(0f, 0f)
            }
            chart.marker = marker
            chart.setDrawMarkers(true)
            chart.invalidate()
        }
    )
}

@Composable
private fun RangeFilters(
    range: String,
    interval: String,
    onChange: (String, String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val opts = listOf(
            "1D" to ("1d" to "5m"),
            "1W" to ("5d" to "15m"),
            "1M" to ("1mo" to "1d"),
            "6M" to ("6mo" to "1d"),
            "1Y" to ("1y" to "1d")
        )
        opts.forEach { (label, ri) ->
            val sel = (range == ri.first && interval == ri.second)
            FilterChip(
                selected = sel,
                onClick = { onChange(ri.first, ri.second) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun RsiChart(chartPoints: List<Pair<Long, Double>>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                legend.isEnabled = false
                axisLeft.axisMinimum = 0f
                axisLeft.axisMaximum = 100f
                axisLeft.removeAllLimitLines()
                axisLeft.addLimitLine(LimitLine(70f, ""))
                axisLeft.addLimitLine(LimitLine(30f, ""))
            }
        },
        update = { chart ->
            fun rsi(values: List<Double>, period: Int = 14): List<Float?> {
                if (values.size <= period) return List(values.size) { null }
                val gains = MutableList(values.size) { 0.0 }
                val losses = MutableList(values.size) { 0.0 }
                for (i in 1 until values.size) {
                    val diff = values[i] - values[i - 1]
                    if (diff >= 0) gains[i] = diff else losses[i] = -diff
                }
                var avgGain = gains.subList(1, period + 1).average()
                var avgLoss = losses.subList(1, period + 1).average()
                val res = MutableList<Float?>(values.size) { null }
                var rsiVal = if (avgLoss == 0.0) 100f else {
                    val rs = avgGain / avgLoss
                    (100 - 100 / (1 + rs)).toFloat()
                }
                res[period] = rsiVal
                for (i in period + 1 until values.size) {
                    avgGain = (avgGain * (period - 1) + gains[i]) / period
                    avgLoss = (avgLoss * (period - 1) + losses[i]) / period
                    rsiVal = if (avgLoss == 0.0) 100f else {
                        val rs = avgGain / avgLoss
                        (100 - 100 / (1 + rs)).toFloat()
                    }
                    res[i] = rsiVal
                }
                return res
            }
            val closes = chartPoints.map { it.second }
            val r = rsi(closes)
            val entries = r.mapIndexedNotNull { i, v -> v?.let { Entry(i.toFloat(), it) } }
            val ds = LineDataSet(entries, "RSI").apply {
                color = android.graphics.Color.RED
                setDrawCircles(false)
                lineWidth = 1.3f
            }
            chart.data = LineData(ds)
            chart.invalidate()
        }
    )
}

@Composable
private fun NewsGrid(items: List<NewsItem>) {
    val ctx = LocalContext.current
    val icons = remember { mutableStateMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>() }

    fun host(url: String): String =
        runCatching { URI(url).host?.removePrefix("www.") ?: "" }.getOrDefault("")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.link }) { n ->
            val host = host(n.link)
            val bmp = icons[host]

            LaunchedEffect(host) {
                if (icons.containsKey(host)) return@LaunchedEffect
                val fetched = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val u = URL("https://www.google.com/s2/favicons?domain=$host&sz=64")
                        BitmapFactory.decodeStream(u.openStream())?.asImageBitmap()
                    }.getOrNull()
                }
                icons[host] = fetched
            }

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(n.link))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(host, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        n.title,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}