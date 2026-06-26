package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.wealthtracker.util.findActivity
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.wealthtracker.ui.components.AnimatedPriceText
import com.example.wealthtracker.ui.components.AppodealBanner
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.data.UserPreferences
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.ui.DebtViewModel
import com.example.wealthtracker.data.local.DebtEntity
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.ui.components.RatingPromptDialog
import com.example.wealthtracker.ui.components.RatingPromptManager
import com.example.wealthtracker.ui.components.MarketIndex
import com.example.wealthtracker.ui.components.ReturnsInsightsCard
import com.example.wealthtracker.ui.components.PortfolioAllocationSection
import androidx.compose.material.icons.filled.CreditCard
import android.util.Log
import com.example.wealthtracker.network.StocksApiProvider
import com.example.wealthtracker.util.MarketHours
import com.example.wealthtracker.util.MarketWebSocket
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.wealthtracker.util.LocalActivity

// In-memory cache for country-specific market indices used in daily return calculation
private object DashboardMarketCache {
    var indices: List<MarketIndex> = emptyList()
}

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    debtViewModel: DebtViewModel,
    onAddClick: () -> Unit,
    onOpenInvestments: () -> Unit,
    onOpenCalculators: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenDebt: () -> Unit = {},
    onAddDebt: () -> Unit = onOpenDebt
) {
    val allItems by viewModel.investments.collectAsState()
    val filtered by viewModel.filteredInvestments.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val debts by debtViewModel.debts.collectAsState()
    val totalDebt = debts.sumOf { it.outstandingBalance }

    val ctx = LocalContext.current
    val reminderManager = remember { com.example.wealthtracker.data.ReminderManager.getInstance(ctx) }

    LaunchedEffect(allItems) {
        reminderManager.syncRemindersFromInvestments(allItems)
        reminderManager.refreshSnoozedReminders()
    }

    val activity = remember(ctx) {
        ctx as? android.app.Activity
            ?: (ctx as? androidx.appcompat.view.ContextThemeWrapper)?.baseContext as? android.app.Activity
    }

    var vizType by remember { mutableStateOf("Pie") }

    var showRatingPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        if (RatingPromptManager.shouldShowPrompt(ctx)) {
            showRatingPrompt = true
            RatingPromptManager.markPromptShown(ctx)
        }
    }

    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) { kotlinx.coroutines.delay(2000); backPressedOnce = false }
    }
    BackHandler(enabled = true) {
        if (!backPressedOnce) {
            backPressedOnce = true
            Toast.makeText(ctx, "Press back again to exit", Toast.LENGTH_SHORT).show()
        } else {
            activity?.finishAffinity() ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    // ── State ──────────────────────────────────────────────────────
    var isAmountHidden by rememberSaveable { mutableStateOf(false) }
    var marketIndices by remember { mutableStateOf(DashboardMarketCache.indices) }
    var showReportsSheet by remember { mutableStateOf(false) }

    val activityLifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
    var isForegrounded by remember { mutableStateOf(true) }
    DisposableEffect(activityLifecycle) {
        val lc = activityLifecycle ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { isForegrounded = true;  Log.d("DashboardMarket", "foregrounded — resuming polls") }
                Lifecycle.Event.ON_STOP  -> { isForegrounded = false; Log.d("DashboardMarket", "backgrounded — pausing polls") }
                else -> {}
            }
        }
        lc.addObserver(observer)
        onDispose { lc.removeObserver(observer) }
    }

    val countryCode = remember { UserPreferences(ctx).getCountryCode() }
    val symbolPairs = remember(countryCode) { marketSymbolsForCountry(countryCode) }

    LaunchedEffect(isForegrounded) {
        if (isForegrounded) {
            MarketWebSocket.updateSymbols(symbolPairs.map { it.first })
            MarketWebSocket.connect()
            Log.d("DashboardMarket", "WS reconnecting after foreground")
        }
    }

    val wsTicks by MarketWebSocket.ticks.collectAsState()
    LaunchedEffect(wsTicks) {
        if (wsTicks.isEmpty() || marketIndices.isEmpty()) return@LaunchedEffect
        val updated = marketIndices.map { idx ->
            val tick = wsTicks[idx.symbol] ?: return@map idx
            idx.copy(
                price = tick.price.toDouble(),
                change = tick.change.toDouble(),
                changePercent = tick.changePercent.toDouble()
            )
        }
        if (updated != marketIndices) {
            marketIndices = updated
            DashboardMarketCache.indices = updated
        }
    }

    LaunchedEffect(Unit) {
        val symbolToName = symbolPairs.toMap()

        suspend fun fetch() {
            runCatching {
                StocksApiProvider.ensureCrumb()
                val symbolStr = symbolPairs.joinToString(",") { it.first }
                val quoteItems = StocksApiProvider.service.quotes(symbolStr).quoteResponse.result
                val fetched = quoteItems.mapNotNull { item ->
                    val sym = item.symbol ?: return@mapNotNull null
                    val name = symbolToName[sym] ?: return@mapNotNull null
                    val price = item.regularMarketPrice ?: return@mapNotNull null
                    MarketIndex(
                        symbol = sym, name = name, price = price,
                        change = item.regularMarketChange ?: 0.0,
                        changePercent = item.regularMarketChangePercent ?: 0.0,
                        preMarketPrice = item.preMarketPrice ?: 0.0,
                        preMarketChange = item.preMarketChange ?: 0.0,
                        preMarketChangePercent = item.preMarketChangePercent ?: 0.0,
                        postMarketPrice = item.postMarketPrice ?: 0.0,
                        postMarketChange = item.postMarketChange ?: 0.0,
                        postMarketChangePercent = item.postMarketChangePercent ?: 0.0,
                        marketState = item.marketState ?: "REGULAR"
                    )
                }
                val ordered = symbolPairs.mapNotNull { (sym, _) -> fetched.find { it.symbol == sym } }
                if (ordered.isNotEmpty()) {
                    DashboardMarketCache.indices = ordered
                    marketIndices = ordered
                }
            }.onFailure { e ->
                val tag = if (e is java.net.UnknownHostException) "no network (Doze/offline)" else "fetch failed"
                Log.e("DashboardMarket", "$tag — ${e::class.simpleName}: ${e.message}")
            }
        }

        // Initial fetch (use cache if available)
        if (marketIndices.isEmpty()) fetch()

        while (true) {
            val delayMs = when {
                !isForegrounded                        -> 5 * 60 * 1000L   // background: Doze-friendly
                MarketHours.isMarketOpen(countryCode) -> 60_000L
                else                                   -> 15 * 60 * 1000L
            }
            delay(delayMs)
            if (isForegrounded) fetch()
        }
    }

    // ── Derived stats ──────────────────────────────────────────────
    val totalInvested = allItems.sumOf { it.amount }
    val stocksTotal = allItems.filter { it.investmentType in listOf("Equity", "Stocks") }.sumOf { it.amount }
    val mfTotal = allItems.filter { it.investmentType == "Mutual Fund" }.sumOf { it.amount }
    val fdDailyAccrual = allItems.filter { it.investmentType == "FD" }
        .sumOf { it.amount * (it.fdRate ?: 0.0) / 36500.0 }

    val primaryIdxChangePct   = marketIndices.firstOrNull()?.changePercent ?: 0.0
    val secondaryIdxChangePct = marketIndices.getOrNull(1)?.changePercent ?: primaryIdxChangePct

    val dailyReturnINR = fdDailyAccrual + stocksTotal * (primaryIdxChangePct / 100.0) + mfTotal * (secondaryIdxChangePct / 100.0) * 0.85
    val dailyReturnPct = if (totalInvested > 0) dailyReturnINR / totalInvested * 100.0 else 0.0

    val assetTypeCount = allItems.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().size
    val diversificationLabel = when {
        assetTypeCount >= 5 -> "Excellent"
        assetTypeCount >= 4 -> "Good"
        assetTypeCount >= 3 -> "Fair"
        assetTypeCount >= 2 -> "Low Diversification"
        else -> "Very Low"
    }

    val stocksPct = if (totalInvested > 0) stocksTotal / totalInvested * 100.0 else 0.0
    val riskLevel = when { stocksPct >= 70 -> "High Risk"; stocksPct >= 40 -> "Moderate"; else -> "Low Risk" }
    val riskProfile = when { stocksPct >= 70 -> "Aggressive"; stocksPct >= 40 -> "Balanced"; else -> "Conservative" }

    val healthScore = remember(allItems) { computeHealthScore(allItems) }
    val sparklinePoints = remember(allItems) { buildSparklinePoints(allItems) }

    // ── UI ─────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                greetingText(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(id = com.ss.wealthtracker.R.string.title_dashboard),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenReminders) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                val act = com.example.wealthtracker.util.LocalActivity.current
                if (act != null) {
                    androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        AppodealBanner(activity = act, modifier = Modifier.fillMaxWidth().height(50.dp))
                    }
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ① Portfolio card with health score
                PortfolioHeroCard(
                    total = totalInvested,
                    totalDebt = totalDebt,
                    assetCount = allItems.size,
                    isAmountHidden = isAmountHidden,
                    onToggleHidden = { isAmountHidden = !isAmountHidden },
                    dailyReturnINR = dailyReturnINR,
                    dailyReturnPct = dailyReturnPct,
                    sparklinePoints = sparklinePoints,
                    healthScore = healthScore
                )

                // ② Quick actions strip
                QuickActionsStrip(
                    onAddAsset = onAddClick,
                    onAddDebt = onAddDebt,
                    onCalculators = onOpenCalculators,
                    onReports = { showReportsSheet = true }
                )

                // ③ Market carousel
                if (marketIndices.isNotEmpty()) {
                    MarketCarousel(indices = marketIndices)
                }

                // ④ Smart Insights (EnhancedInsights has its own header)
                if (allItems.isNotEmpty()) {
                    com.example.wealthtracker.ui.components.EnhancedInsights(
                        investments = allItems,
                        currentFilter = typeFilter
                    )
                    com.example.wealthtracker.ui.components.PortfolioAnalysisCard(investments = allItems)
                }

                // ⑥ Filters — moved below insights
                if (allItems.isNotEmpty()) {
                    Text(
                        "Portfolio Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val counts = allItems
                            .groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }
                            .mapValues { it.value.size }
                        val filters = listOf("All") + counts.keys.sorted()
                        filters.forEach { key ->
                            val selected = (typeFilter ?: "All") == key
                            val count = if (key == "All") allItems.size else counts[key] ?: 0
                            val display = if (key == "FD") "FD" else key
                            CompactFilterChip(
                                selected = selected,
                                onClick = { viewModel.setTypeFilter(if (key == "All") null else key) },
                                label = "$display ($count)"
                            )
                        }
                    }
                }

                // ⑦ Asset-specific cards (filtered list)
                if (allItems.isNotEmpty()) {
                    AssetSpecificCards(
                        investments = if (typeFilter.isNullOrBlank()) allItems else filtered,
                        filter = typeFilter,
                        onAddClick = onAddClick
                    )
                }

                // ⑧ Recent Activity
                val recentItems = allItems.sortedByDescending { it.createdAt }.take(4)
                if (recentItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onOpenInvestments) { Text("View All", style = MaterialTheme.typography.labelMedium) }
                    }
                    RecentActivityRow(items = recentItems)
                }

                // ⑨ Portfolio allocation chart
                PortfolioAllocationSection(investments = allItems)

                // ⑩ Reminders
                val activeReminders by reminderManager.activeReminders.collectAsState()
                com.example.wealthtracker.ui.components.DashboardRemindersSection(
                    reminders = activeReminders,
                    onGotIt = { reminderManager.dismissReminder(it.id) },
                    onRemindLater = { reminderManager.snoozeReminder(it.id) },
                    onViewAll = onOpenReminders
                )

                // ⑪ Returns insights
                ReturnsInsightsCard(investments = allItems)

                // Native ad (news-feed style) between insights and debt tracker
                com.example.wealthtracker.ui.components.AppodealNativeNewsFeed()

                // ⑫ Debt tracker entry
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDebt() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Debt Tracker", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Track your loans & EMIs", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }

                // ⑬ Allocation chart
                val cfg = LocalConfiguration.current
                val isWide = cfg.screenWidthDp >= 700
                val recentSrc = if (typeFilter.isNullOrBlank()) allItems else filtered
                val recent = recentSrc.sortedByDescending { it.createdAt }.take(5)

                if (isWide) {
                    Row(Modifier.fillMaxWidth().height(500.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            ChartSectionDashboard(items = filtered, filter = typeFilter, vizType = vizType, onVizTypeChange = { vizType = it })
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            if (recent.isNotEmpty()) {
                                Text(stringResource(id = com.ss.wealthtracker.R.string.recent_investments), style = MaterialTheme.typography.titleLarge)
                                HorizontalDivider()
                                LazyColumn(modifier = Modifier.fillMaxHeight(), contentPadding = PaddingValues(bottom = 96.dp)) {
                                    items(recent) { e -> InvestmentListItem(e) }
                                }
                            }
                        }
                    }
                } else {
                    ChartSectionDashboard(items = filtered, filter = typeFilter, vizType = vizType, onVizTypeChange = { vizType = it })
                    if (recent.isNotEmpty()) {
                        Text(stringResource(id = com.ss.wealthtracker.R.string.recent_investments), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider()
                        recent.forEach { e -> InvestmentListItem(e) }
                    }
                }
            }
        }
    }

    if (showRatingPrompt) {
        RatingPromptDialog(
            onDismiss = { showRatingPrompt = false },
            onRated = { RatingPromptManager.markUserRated(ctx); showRatingPrompt = false }
        )
    }

    if (showReportsSheet) {
        com.example.wealthtracker.ui.components.ReportsBottomSheet(
            investments = allItems,
            onDismiss = { showReportsSheet = false }
        )
    }
}

// ─── Pure helpers ──────────────────────────────────────────────────────────────

private fun greetingText(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when { h < 12 -> "Good Morning"; h < 17 -> "Good Afternoon"; else -> "Good Evening" }
}

private fun marketSymbolsForCountry(countryCode: String): List<Pair<String, String>> = when (countryCode) {
    "IN" -> listOf(
        "^NSEI"    to "NIFTY 50",     "^BSESN"   to "SENSEX",
        "^NSEBANK" to "BANK NIFTY",   "^CNXIT"   to "NIFTY IT",
        "^CNXFMCG" to "NIFTY FMCG",  "^CNXAUTO" to "NIFTY Auto",
        "^GSPC"    to "S&P 500",      "^IXIC"    to "NASDAQ"
    )
    "US" -> listOf(
        "^GSPC"  to "S&P 500",     "^IXIC"  to "NASDAQ",
        "^DJI"   to "DOW Jones",   "^RUT"   to "Russell 2000",
        "^VIX"   to "VIX",         "^GSPTSE" to "TSX",
        "^FTSE"  to "FTSE 100",    "^N225"  to "Nikkei 225"
    )
    "GB" -> listOf(
        "^FTSE"  to "FTSE 100",    "^FTMC"  to "FTSE 250",
        "^GDAXI" to "DAX",         "^FCHI"  to "CAC 40",
        "^GSPC"  to "S&P 500",     "^IXIC"  to "NASDAQ"
    )
    "JP" -> listOf(
        "^N225"   to "Nikkei 225", "^TPX"   to "TOPIX",
        "^HSI"    to "Hang Seng",  "000001.SS" to "Shanghai",
        "^GSPC"   to "S&P 500",    "^IXIC"  to "NASDAQ"
    )
    "AU" -> listOf(
        "^AXJO"  to "ASX 200",     "^AORD"  to "All Ords",
        "^NZ50"  to "NZX 50",      "^HSI"   to "Hang Seng",
        "^GSPC"  to "S&P 500",     "^IXIC"  to "NASDAQ"
    )
    "NZ" -> listOf(
        "^NZ50"  to "NZX 50",      "^AXJO"  to "ASX 200",
        "^HSI"   to "Hang Seng",   "^GSPC"  to "S&P 500",
        "^IXIC"  to "NASDAQ"
    )
    "SG" -> listOf(
        "^STI"      to "STI",       "^HSI"      to "Hang Seng",
        "000001.SS" to "Shanghai",  "^N225"     to "Nikkei 225",
        "^GSPC"     to "S&P 500",   "^IXIC"     to "NASDAQ"
    )
    "CA" -> listOf(
        "^GSPTSE" to "TSX",         "^GSPC"  to "S&P 500",
        "^IXIC"   to "NASDAQ",      "^DJI"   to "DOW Jones",
        "^FTSE"   to "FTSE 100"
    )
    "KR" -> listOf(
        "^KS11"  to "KOSPI",        "^KQ11"  to "KOSDAQ",
        "000001.SS" to "Shanghai",  "^N225"  to "Nikkei 225",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ"
    )
    "CN" -> listOf(
        "000001.SS" to "Shanghai",  "399001.SZ" to "Shenzhen",
        "^HSI"      to "Hang Seng", "^KS11"     to "KOSPI",
        "^N225"     to "Nikkei 225","^GSPC"     to "S&P 500"
    )
    "HK" -> listOf(
        "^HSI"      to "Hang Seng", "000001.SS" to "Shanghai",
        "399001.SZ" to "Shenzhen",  "^N225"     to "Nikkei 225",
        "^GSPC"     to "S&P 500",   "^IXIC"     to "NASDAQ"
    )
    "TW" -> listOf(
        "^TWII"     to "Taiwan",    "^HSI"      to "Hang Seng",
        "000001.SS" to "Shanghai",  "^N225"     to "Nikkei 225",
        "^GSPC"     to "S&P 500",   "^IXIC"     to "NASDAQ"
    )
    "DE", "FR", "IT", "ES", "NL", "BE", "AT", "IE", "FI", "PT", "GR" -> listOf(
        "^GDAXI"    to "DAX",       "^FCHI"     to "CAC 40",
        "^STOXX50E" to "EURO STOXX 50", "^FTSE" to "FTSE 100",
        "^SSMI"     to "SMI",       "^GSPC"     to "S&P 500",
        "^IXIC"     to "NASDAQ",    "^N225"     to "Nikkei 225"
    )
    "CH" -> listOf(
        "^SSMI"  to "SMI",          "^GDAXI" to "DAX",
        "^FCHI"  to "CAC 40",       "^FTSE"  to "FTSE 100",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ"
    )
    "SE" -> listOf(
        "^OMX"   to "OMX Stockholm","^GDAXI" to "DAX",
        "^FTSE"  to "FTSE 100",     "^GSPC"  to "S&P 500",
        "^IXIC"  to "NASDAQ"
    )
    "NO" -> listOf(
        "OBX.OL" to "OBX",         "^GDAXI" to "DAX",
        "^FTSE"  to "FTSE 100",     "^GSPC"  to "S&P 500",
        "^IXIC"  to "NASDAQ"
    )
    "DK" -> listOf(
        "^OMXC25" to "OMX Copenhagen","^GDAXI" to "DAX",
        "^FTSE"   to "FTSE 100",      "^GSPC"  to "S&P 500",
        "^IXIC"   to "NASDAQ"
    )
    "PL" -> listOf(
        "^WIG20" to "WIG20",        "^GDAXI" to "DAX",
        "^FTSE"  to "FTSE 100",     "^GSPC"  to "S&P 500",
        "^IXIC"  to "NASDAQ"
    )
    "CZ" -> listOf(
        "^PX"    to "Prague PX",    "^GDAXI" to "DAX",
        "^FTSE"  to "FTSE 100",     "^GSPC"  to "S&P 500"
    )
    "BR" -> listOf(
        "^BVSP"  to "BOVESPA",      "^MXX"   to "IPC Mexico",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^DJI"   to "DOW Jones"
    )
    "MX" -> listOf(
        "^MXX"   to "IPC Mexico",   "^BVSP"  to "BOVESPA",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^DJI"   to "DOW Jones"
    )
    "AR" -> listOf(
        "^MERV"  to "MERVAL",       "^BVSP"  to "BOVESPA",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ"
    )
    "ZA" -> listOf(
        "^J203.JO" to "JSE All Share","^EGX30" to "EGX 30",
        "^GSPC"    to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^FTSE"    to "FTSE 100"
    )
    "EG" -> listOf(
        "^EGX30" to "EGX 30",       "^J203.JO" to "JSE All Share",
        "^GSPC"  to "S&P 500",      "^FTSE"    to "FTSE 100"
    )
    "SA" -> listOf(
        "^TASI"  to "TADAWUL",      "^ADI"   to "Abu Dhabi",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^FTSE"  to "FTSE 100"
    )
    "AE" -> listOf(
        "^ADI"   to "Abu Dhabi",    "^TASI"  to "TADAWUL",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^FTSE"  to "FTSE 100"
    )
    "MY" -> listOf(
        "^KLSE"  to "KLCI",         "^STI"   to "STI",
        "^JKSE"  to "Jakarta",      "^HSI"   to "Hang Seng",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ"
    )
    "TH" -> listOf(
        "^SET.BK" to "SET",         "^KLSE"  to "KLCI",
        "^STI"    to "STI",         "^HSI"   to "Hang Seng",
        "^GSPC"   to "S&P 500",     "^IXIC"  to "NASDAQ"
    )
    "ID" -> listOf(
        "^JKSE"  to "Jakarta",      "^KLSE"  to "KLCI",
        "^STI"   to "STI",          "^HSI"   to "Hang Seng",
        "^GSPC"  to "S&P 500",      "^IXIC"  to "NASDAQ"
    )
    "PH" -> listOf(
        "PSEi.PS" to "PSEi",        "^STI"   to "STI",
        "^JKSE"   to "Jakarta",     "^HSI"   to "Hang Seng",
        "^GSPC"   to "S&P 500",     "^IXIC"  to "NASDAQ"
    )
    "PK" -> listOf(
        "^KSE100" to "KSE 100",     "^BSESN" to "SENSEX",
        "^NSEI"   to "NIFTY 50",    "^HSI"   to "Hang Seng",
        "^GSPC"   to "S&P 500"
    )
    "IL" -> listOf(
        "^TA125.TA" to "Tel Aviv 125","^GDAXI" to "DAX",
        "^FTSE"    to "FTSE 100",     "^GSPC"  to "S&P 500",
        "^IXIC"    to "NASDAQ"
    )
    "RU" -> listOf(
        "IMOEX.ME" to "MOEX Russia","^GDAXI" to "DAX",
        "^GSPC"    to "S&P 500",    "^IXIC"  to "NASDAQ"
    )
    else -> listOf(
        "^GSPC"     to "S&P 500",      "^IXIC"  to "NASDAQ",
        "^DJI"      to "DOW Jones",    "^GDAXI" to "DAX",
        "^FTSE"     to "FTSE 100",     "^N225"  to "Nikkei 225",
        "^STOXX50E" to "EURO STOXX 50","^HSI"   to "Hang Seng"
    )
}

private fun computeHealthScore(items: List<InvestmentEntity>): Int {
    if (items.isEmpty()) return 0
    val total = items.sumOf { it.amount }.coerceAtLeast(1.0)
    val byType = items.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }

    // Diversification (0–30)
    val divScore = when (byType.size) { 1 -> 0; 2 -> 10; 3 -> 18; 4 -> 25; else -> 30 }

    // Concentration (0–30): lower top-type share is better
    val maxShare = byType.maxOfOrNull { (_, v) -> v.sumOf { it.amount } / total } ?: 1.0
    val concScore = when {
        maxShare < 0.40 -> 30; maxShare < 0.55 -> 22; maxShare < 0.70 -> 14
        maxShare < 0.85 -> 8; else -> 2
    }

    // Safety net (0–20): has at least one conservative asset (India + international equivalents)
    val hasSafety = byType.keys.any { it in listOf(
        "FD", "PPF", "EPF", "NPS",                                      // India
        "CD", "401k", "IRA", "403b", "T-Bill", "Treasury", "Bonds",    // US / universal
        "GIC", "TFSA", "RRSP",                                          // Canada
        "ISA", "SIPP",                                                   // UK
        "Fixed Income", "Government Bonds",                              // Universal
        "Health Insurance", "Term Insurance"                             // Universal
    ) }
    val safetyScore = if (hasSafety) 20 else 0

    // Invested amount (0–20): normalized to ₹10L
    val sizeScore = (total / 1_000_000.0 * 20.0).coerceAtMost(20.0).toInt()

    return (divScore + concScore + safetyScore + sizeScore).coerceIn(0, 100)
}

private fun healthLabel(score: Int) = when {
    score >= 80 -> "Healthy"
    score >= 65 -> "Good"
    score >= 50 -> "Growing"
    score >= 35 -> "Building Up"
    else -> "Getting Started"
}

private fun healthColor(score: Int) = when {
    score >= 65 -> Color(0xFF16A34A); score >= 50 -> Color(0xFFD97706); else -> Color(0xFFDC2626)
}

private fun buildSparklinePoints(allItems: List<InvestmentEntity>): List<Float> {
    if (allItems.size < 2) return listOf(20f, 45f, 30f, 60f, 50f, 80f, 70f, 100f)
    var cumulative = 0.0
    return allItems.sortedBy { it.createdAt }.map { cumulative += it.amount; cumulative.toFloat() }.takeLast(10)
}

private fun dashTimeAgo(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "Just now"; diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d ago"
        else -> "${diff / (7 * 86_400_000L)}w ago"
    }
}

// ─── Portfolio Hero Card ───────────────────────────────────────────────────────

@Composable
private fun PortfolioHeroCard(
    total: Double,
    totalDebt: Double,
    assetCount: Int,
    isAmountHidden: Boolean,
    onToggleHidden: () -> Unit,
    dailyReturnINR: Double,
    dailyReturnPct: Double,
    sparklinePoints: List<Float>,
    healthScore: Int
) {
    // Color matches what is printed: if %.2f rounds to 0.00, show grey
    val returnPositive = dailyReturnPct >= 0.005
    val returnNegative = dailyReturnPct <= -0.005
    val returnColor = when {
        returnPositive -> Color(0xFF16A34A)
        returnNegative -> Color(0xFFDC2626)
        else           -> Color(0xFF9CA3AF)
    }
    val returnSign = if (returnPositive) "+" else ""
    val hColor = healthColor(healthScore)
    val hLabel = healthLabel(healthScore)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Top row: value + sparkline
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Label + eye
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Portfolio Value", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier.size(18.dp).clickable(onClick = onToggleHidden),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isAmountHidden) "🙈" else "👁", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    // Big amount
                    Text(
                        if (isAmountHidden) "₹ ••••••" else FormatUtils.formatINR(total),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (assetCount > 0) {
                        Text(
                            "$assetCount ${if (assetCount == 1) "Holding" else "Holdings"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Daily gain row
                    if (total > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null,
                                tint = returnColor, modifier = Modifier.size(14.dp))
                            Text(
                                buildString {
                                    append(returnSign)
                                    if (!isAmountHidden) append(FormatUtils.formatINRShort(dailyReturnINR))
                                    else append("••••")
                                    append("  $returnSign${String.format("%.2f", dailyReturnPct)}%")
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = returnColor
                            )
                            Text("Today", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Compact sparkline
                Sparkline(
                    points = sparklinePoints,
                    lineColor = Color(0xFF2563EB),
                    modifier = Modifier.size(width = 76.dp, height = 44.dp)
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Portfolio Health Score row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Portfolio Health", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$healthScore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = hColor)
                        Text("/100", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(color = hColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                            Text(hLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = hColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                // Solid fill bar — no thumb/dot
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(hColor.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((healthScore / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(hColor)
                    )
                }
            }

            // Net Worth breakdown (always visible, even when debt = 0)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            val netWorth = total - totalDebt
            val netWorthColor = when {
                netWorth > 0 -> Color(0xFF16A34A)
                netWorth < 0 -> Color(0xFFDC2626)
                else         -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetWorthCell(label = "Assets",    value = if (isAmountHidden) "••••" else FormatUtils.formatINRShort(total),      valueColor = MaterialTheme.colorScheme.onSurface)
                Box(Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                NetWorthCell(label = "Debt",      value = if (isAmountHidden) "••••" else FormatUtils.formatINRShort(totalDebt),   valueColor = if (totalDebt > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                NetWorthCell(label = "Net Worth", value = if (isAmountHidden) "••••" else FormatUtils.formatINRShort(netWorth),    valueColor = netWorthColor, emphasized = true)
            }
        }
    }
}

@Composable
private fun NetWorthCell(
    label: String,
    value: String,
    valueColor: Color,
    emphasized: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (emphasized)
                MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            else
                MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

// ─── Sparkline ─────────────────────────────────────────────────────────────────

@Composable
private fun Sparkline(points: List<Float>, lineColor: Color = Color(0xFF2563EB), modifier: Modifier = Modifier) {
    val fillColor = lineColor.copy(alpha = 0.15f)
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val minV = points.min(); val maxV = points.max()
        val range = (maxV - minV).coerceAtLeast(1f); val pad = h * 0.1f
        fun xOf(i: Int) = i / (points.size - 1f) * w
        fun yOf(v: Float) = h - pad - (v - minV) / range * (h - 2 * pad)
        val linePath = Path()
        linePath.moveTo(xOf(0), yOf(points[0]))
        for (i in 1 until points.size) {
            val cx = (xOf(i - 1) + xOf(i)) / 2f
            linePath.cubicTo(cx, yOf(points[i - 1]), cx, yOf(points[i]), xOf(i), yOf(points[i]))
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val fillPath = Path()
        fillPath.addPath(linePath)
        fillPath.lineTo(w, h); fillPath.lineTo(0f, h); fillPath.close()
        drawPath(fillPath, fillColor)
    }
}

// ─── Quick Actions Strip ───────────────────────────────────────────────────────

@Composable
private fun QuickActionsStrip(
    onAddAsset: () -> Unit,
    onAddDebt: () -> Unit,
    onCalculators: () -> Unit,
    onReports: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickStripItem(Icons.Default.Add,                    Color(0xFF6366F1), "Add Asset",  onAddAsset)
            QuickStripItem(Icons.Default.CreditCard,             Color(0xFF8B5CF6), "Add Debt",   onAddDebt)
            QuickStripItem(Icons.Default.Calculate,              Color(0xFFEA580C), "Calculator", onCalculators)
            QuickStripItem(Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF16A34A), "Reports",    onReports)
        }
    }
}

@Composable
private fun QuickStripItem(icon: ImageVector, iconBg: Color, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ─── Market Carousel ───────────────────────────────────────────────────────────

@Composable
private fun MarketCarousel(indices: List<MarketIndex>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(indices) { index -> MarketCarouselCard(index) }
    }
}

@Composable
private fun MarketCarouselCard(index: MarketIndex) {
    // Determine which session's data to display
    val isPreMarket  = index.marketState in listOf("PRE", "PREPRE")
    val isPostMarket = index.marketState in listOf("POST", "POSTPOST")
    val hasPreData   = isPreMarket  && index.preMarketPrice  > 0.0
    val hasPostData  = isPostMarket && index.postMarketPrice > 0.0

    val displayPrice  = when { hasPreData -> index.preMarketPrice;  hasPostData -> index.postMarketPrice;  else -> index.price }
    val displayChange = when { hasPreData -> index.preMarketChange; hasPostData -> index.postMarketChange; else -> index.change }
    val displayPct    = when { hasPreData -> index.preMarketChangePercent; hasPostData -> index.postMarketChangePercent; else -> index.changePercent }

    val isPositive = displayChange >= 0
    val changeColor = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
    val changeBg   = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    val returnSign = if (isPositive) "+" else ""

    // Session badge
    val badgeText  = when { isPreMarket -> "PRE"; isPostMarket -> "AH"; else -> null }
    val badgeColor = when { isPreMarket -> Color(0xFFF59E0B); isPostMarket -> Color(0xFF6366F1); else -> Color.Transparent }

    var showSessionInfo by remember { mutableStateOf(false) }

    if (showSessionInfo && badgeText != null) {
        AlertDialog(
            onDismissRequest = { showSessionInfo = false },
            confirmButton = {
                TextButton(onClick = { showSessionInfo = false }) { Text("Got it") }
            },
            title = {
                Text(if (badgeText == "PRE") "Pre-Market" else "After Hours (AH)")
            },
            text = {
                Text(
                    if (badgeText == "PRE")
                        "Pre-market trading happens before the official exchange opens (typically 4:00–9:30 AM ET for US markets). Prices may be more volatile due to lower trading volume."
                    else
                        "After-hours trading happens after the official market close (typically 4:00–8:00 PM ET for US markets). Prices can differ significantly from the regular session."
                )
            }
        )
    }

    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Name + badge + info icon on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    index.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (badgeText != null) {
                    Spacer(Modifier.width(2.dp))
                    Surface(color = badgeColor, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "What is $badgeText?",
                        tint = badgeColor,
                        modifier = Modifier.size(11.dp).clickable { showSessionInfo = true }
                    )
                }
            }
            AnimatedPriceText(
                text     = String.format("%.2f", displayPrice),
                isRising = isPositive,
                style    = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Surface(color = changeBg, shape = RoundedCornerShape(6.dp)) {
                Text(
                    "$returnSign${String.format("%.2f", displayChange)}  ($returnSign${String.format("%.2f", displayPct)}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = changeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ─── Stats Cards (number first) ────────────────────────────────────────────────

@Composable
private fun StatsCardsGrid(
    total: Double,
    investmentCount: Int,
    diversificationLabel: String,
    assetTypeCount: Int,
    riskLevel: String,
    riskProfile: String,
    dailyReturnPct: Double,
    dailyReturnINR: Double,
    isAmountHidden: Boolean
) {
    // Color matches what is printed: if %.2f rounds to 0.00, show grey
    val returnPositive = dailyReturnPct >= 0.005
    val returnNegative = dailyReturnPct <= -0.005
    val returnSign = if (returnPositive) "+" else ""

    // Semantic colors — each card communicates status, not just style
    val divColor = when {
        assetTypeCount >= 4 -> Color(0xFF16A34A)   // green: well diversified
        assetTypeCount >= 2 -> Color(0xFFF59E0B)   // amber: needs work
        else                -> Color(0xFFDC2626)   // red: single asset
    }
    val riskColor = when (riskLevel) {
        "Low Risk"  -> Color(0xFF16A34A)
        "Moderate"  -> Color(0xFFF59E0B)
        else        -> Color(0xFFDC2626)
    }
    val returnColor = when {
        returnPositive -> Color(0xFF16A34A)
        returnNegative -> Color(0xFFDC2626)
        else           -> Color(0xFF9CA3AF)  // grey for zero
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                iconBg = Color(0xFFDCFCE7), iconTint = Color(0xFF16A34A), icon = Icons.Default.Savings,
                value = if (isAmountHidden) "₹ ••••" else FormatUtils.formatINRShort(total),
                valueColor = Color(0xFF16A34A),
                label = "Portfolio Value",
                subtitle = "$investmentCount investments"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                iconBg = divColor.copy(alpha = 0.12f), iconTint = divColor, icon = Icons.Default.AccountBalance,
                value = diversificationLabel,
                valueColor = divColor,
                label = "Diversification",
                subtitle = "$assetTypeCount asset types"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                iconBg = riskColor.copy(alpha = 0.12f), iconTint = riskColor, icon = Icons.AutoMirrored.Filled.TrendingUp,
                value = riskLevel,
                valueColor = riskColor,
                label = "Risk Profile",
                subtitle = riskProfile
            )
            StatCard(
                modifier = Modifier.weight(1f),
                iconBg = returnColor.copy(alpha = 0.12f), iconTint = returnColor,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                value = if (total > 0) "$returnSign${String.format("%.2f", dailyReturnPct)}%" else "N/A",
                valueColor = returnColor,
                label = "Today's Return",
                subtitle = if (total > 0 && !isAmountHidden) "$returnSign${FormatUtils.formatINRShort(dailyReturnINR)}" else ""
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    iconBg: Color,
    iconTint: Color,
    icon: ImageVector,
    value: String,
    valueColor: Color,
    label: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            // Number FIRST — titleSmall keeps text values from dominating number values
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = valueColor, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
    }
}

// ─── Compact Filter Chip ──────────────────────────────────────────────────────

@Composable
private fun CompactFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(21.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.height(42.dp)
    ) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Asset-Specific Cards ─────────────────────────────────────────────────────

@Composable
private fun AssetSpecificCards(
    investments: List<InvestmentEntity>,
    filter: String?,
    onAddClick: () -> Unit
) {
    val displayItems = investments.sortedByDescending { it.amount }.take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (displayItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No assets yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onAddClick) { Text("+ Add your first investment") }
                }
            } else {
                displayItems.forEachIndexed { i, item ->
                    AssetRow(item = item)
                    if (i < displayItems.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(item: InvestmentEntity) {
    val name = when (item.investmentType) {
        "FD" -> item.bankName ?: "Fixed Deposit"
        "Equity", "Stocks" -> item.stockName?.takeIf { it.isNotBlank() } ?: "Stocks"
        "Mutual Fund" -> item.type.ifBlank { "Mutual Fund" }
        "Gold" -> item.goldType?.takeIf { it.isNotBlank() } ?: "Gold"
        "Health Insurance" -> item.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Health Insurance"
        else -> item.type.ifBlank { item.investmentType }
    }
    val subtitle = when (item.investmentType) {
        "FD" -> item.fdRate?.let { "${"%.1f".format(it)}% p.a." } ?: ""
        "Equity", "Stocks" -> item.stockDate?.takeIf { it.isNotBlank() } ?: ""
        "Mutual Fund" -> item.mfDate?.takeIf { it.isNotBlank() } ?: ""
        "Health Insurance" -> item.hiRenewalDate?.let { "Renews $it" } ?: ""
        else -> ""
    }
    val typeLabel = if (item.investmentType == "Equity") "Stocks" else item.investmentType
    val iconBg = when (typeLabel) {
        "Stocks" -> Color(0xFFDBEAFE); "FD", "PPF", "EPF", "NPS" -> Color(0xFFDCFCE7)
        "Mutual Fund" -> Color(0xFFF3E8FF); "Gold" -> Color(0xFFFFFBEB)
        else -> Color(0xFFF1F5F9)
    }
    val iconTint = when (typeLabel) {
        "Stocks" -> Color(0xFF1D4ED8); "FD", "PPF", "EPF", "NPS" -> Color(0xFF16A34A)
        "Mutual Fund" -> Color(0xFF7C3AED); "Gold" -> Color(0xFFD97706)
        else -> Color(0xFF64748B)
    }
    val icon = when (typeLabel) {
        "FD", "PPF", "EPF", "NPS" -> Icons.Default.AccountBalance
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(FormatUtils.formatINRShort(item.amount), style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ─── Recent Activity Row ──────────────────────────────────────────────────────

@Composable
private fun RecentActivityRow(items: List<InvestmentEntity>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item -> RecentActivityCard(item = item) }
    }
}

@Composable
private fun RecentActivityCard(item: InvestmentEntity) {
    val typeLabel = when (item.investmentType) {
        "FD" -> "Added FD"; "Equity", "Stocks" -> "Added Stocks"; "Mutual Fund" -> "SIP Added"
        "Gold" -> "Added Gold"; "PPF" -> "Added PPF"; "EPF" -> "Added EPF"; "NPS" -> "Added NPS"
        else -> "Added Asset"
    }
    val typeKey = if (item.investmentType == "Equity") "Stocks" else item.investmentType
    val iconBg = when (typeKey) {
        "FD", "PPF", "EPF", "NPS" -> Color(0xFFDCFCE7); "Stocks", "Mutual Fund" -> Color(0xFFDBEAFE)
        "Gold" -> Color(0xFFFFFBEB); else -> Color(0xFFF3E8FF)
    }
    val iconTint = when (typeKey) {
        "FD", "PPF", "EPF", "NPS" -> Color(0xFF16A34A); "Stocks", "Mutual Fund" -> Color(0xFF1D4ED8)
        "Gold" -> Color(0xFFD97706); else -> Color(0xFF7C3AED)
    }
    val icon = when (typeKey) {
        "FD", "PPF", "EPF", "NPS" -> Icons.Default.AccountBalance
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
    val subtitle = when (typeKey) {
        "FD" -> item.bankName ?: "Fixed Deposit"; "Stocks" -> item.stockName ?: "Stocks"
        "Mutual Fund" -> item.type.ifBlank { "Mutual Fund" }; else -> typeKey
    }

    Card(
        modifier = Modifier.width(148.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(typeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(FormatUtils.formatINRShort(item.amount), style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(dashTimeAgo(item.createdAt), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ─── Investment list (wide layout) ────────────────────────────────────────────

@Composable
private fun InvestmentListItem(e: InvestmentEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val icon = when (e.investmentType) {
            "FD" -> Icons.Default.AccountBalance; "Mutual Fund" -> Icons.AutoMirrored.Filled.TrendingUp
            "PPF", "EPF", "NPS" -> Icons.Default.AccountBalance; "Gold" -> Icons.Default.Savings
            "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp; else -> Icons.Default.Savings
        }
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            val title = when (e.investmentType) {
                "FD" -> stringResource(id = com.ss.wealthtracker.R.string.fixed_deposit)
                "Equity", "Stocks" -> e.stockName?.takeIf { it.isNotBlank() } ?: "Stocks"
                "Mutual Fund" -> e.type.ifBlank { "Mutual Fund" }
                "Gold" -> e.goldType?.takeIf { it.isNotBlank() } ?: "Gold"
                "Health Insurance" -> e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Health Insurance"
                else -> if (e.investmentType == "Others") e.type.ifBlank { "Others" } else e.investmentType
            }
            Text(title, fontWeight = FontWeight.SemiBold)
            val bits = mutableListOf<String>()
            when (e.investmentType) {
                "FD" -> { e.bankName?.takeIf { it.isNotBlank() }?.let { bits += it }; e.fdRate?.let { bits += "${"%.2f".format(it)}%" }; e.fdMaturityDate?.takeIf { it.isNotBlank() }?.let { bits += "Matures $it" } }
                "Equity", "Stocks" -> e.stockDate?.takeIf { it.isNotBlank() }?.let { bits += it }
                "Mutual Fund" -> e.mfDate?.takeIf { it.isNotBlank() }?.let { bits += it }
                else -> {}
            }
            if (bits.isNotEmpty()) Text(bits.joinToString(" • "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(FormatUtils.formatINRShort(e.amount), fontWeight = FontWeight.Bold)
    }
}

// ─── Chart section ────────────────────────────────────────────────────────────

@Composable
private fun IconAsset(assetPath: String, contentDesc: String? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val loader = remember { ImageLoader.Builder(ctx).components { add(SvgDecoder.Factory()) }.build() }
    val req = remember(assetPath) { ImageRequest.Builder(ctx).data("file:///android_asset/$assetPath").build() }
    AsyncImage(model = req, imageLoader = loader, contentDescription = contentDesc, modifier = Modifier.size(18.dp))
}

private fun parseUiDateDashboard(s: String): java.time.LocalDate? = runCatching {
    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH))
}.getOrNull()

private fun formatAddedOnDashboard(d: java.time.LocalDate): String =
    d.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH))

@Composable
private fun ChartSectionDashboard(items: List<InvestmentEntity>, filter: String?, vizType: String, onVizTypeChange: (String) -> Unit) {
    fun truncateLabel(s: String, max: Int = 12): String = if (s.length <= max) s else s.take(max - 1) + "…"
    val data = run {
        val unknown = "Unknown"; val unknownBank = "Unknown Bank"
        val grouped: Map<String, List<InvestmentEntity>> = when (filter?.let { if (it == "Equity") "Stocks" else it }) {
            "FD" -> items.groupBy { val b = it.bankName ?: unknownBank; if (b.isBlank() || b == "Others" || b == "Others (Custom Bank)") unknownBank else b }
            "Stocks" -> items.groupBy { (it.stockName ?: it.type).ifBlank { unknown } }
            "Gold" -> items.groupBy { (it.goldType ?: "Gold").ifBlank { "Gold" } }
            "Mutual Fund" -> items.groupBy { it.type.ifBlank { unknown } }
            "Health Insurance" -> items.groupBy { (it.hiPolicyName ?: unknown).ifBlank { unknown } }
            "PPF" -> items.groupBy { (it.ppfFy ?: "PPF").ifBlank { "PPF" } }
            "NPS" -> items.groupBy { (it.npsTier ?: "NPS").ifBlank { "NPS" } }
            else -> items.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }
        }
        val pairs = grouped.mapNotNull { (key, entities) ->
            if (key.isBlank() || entities.isEmpty()) return@mapNotNull null
            val total = entities.sumOf { e -> e.amount }
            if (total <= 0 || !total.isFinite()) return@mapNotNull null
            key to total
        }.sortedByDescending { it.second }
        if (pairs.size <= 10) pairs else pairs.take(10) + listOf("Others" to pairs.drop(10).sumOf { it.second })
    }
    val colors = listOf(0xFFF44336.toInt(), 0xFF4CAF50.toInt(), 0xFFFBC02D.toInt(), 0xFF2196F3.toInt(), 0xFF9C27B0.toInt(), 0xFFFF9800.toInt())
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)) {
            if (data.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = com.ss.wealthtracker.R.string.overall_allocation), style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        val selBg = MaterialTheme.colorScheme.primary; val selTxt = MaterialTheme.colorScheme.onPrimary; val unselTxt = MaterialTheme.colorScheme.onSurfaceVariant
                        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (vizType == "Pie") selBg else Color.Transparent).clickable { if (vizType != "Pie") onVizTypeChange("Pie") }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(stringResource(id = com.ss.wealthtracker.R.string.viz_pie), color = if (vizType == "Pie") selTxt else unselTxt, style = MaterialTheme.typography.labelMedium)
                        }
                        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (vizType == "Bar") selBg else Color.Transparent).clickable { if (vizType != "Bar") onVizTypeChange("Bar") }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(stringResource(id = com.ss.wealthtracker.R.string.viz_bar), color = if (vizType == "Bar") selTxt else unselTxt, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (data.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Text(stringResource(id = com.ss.wealthtracker.R.string.no_data_to_visualize), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (vizType == "Pie") {
                var animateOnToggle by remember(items, filter, vizType) { mutableStateOf(true) }
                val composeColors = colors.map { Color(it) }
                val centerText = when (filter) { "FD" -> "Fixed\nDeposits"; "Stocks" -> "Stock\nPortfolio"; "Mutual Fund" -> "Mutual\nFunds"; else -> "Total\nInvestments" }
                com.example.wealthtracker.ui.charts.SafePieChart(data = data, colors = composeColors, modifier = Modifier.fillMaxWidth(), showLegend = true, animationEnabled = animateOnToggle, centerText = centerText, onSliceClick = null)
                LaunchedEffect(items, filter, vizType) { animateOnToggle = false }
            } else {
                var barAnim by remember(items, filter, vizType) { mutableStateOf(true) }
                AndroidView(
                    factory = { context -> com.example.wealthtracker.ui.charts.ChartUtils.createBarChart(context, onSurface) },
                    update = { view ->
                        val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                        com.example.wealthtracker.ui.charts.ChartUtils.bindBarData(view, data.map { truncateLabel(it.first) }, data.map { (it.second / total * 100.0).toFloat() }, colors, onSurface, barAnim)
                        view.data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getBarLabel(barEntry: com.github.mikephil.charting.data.BarEntry?): String {
                                val raw = (barEntry?.y ?: 0f).toDouble()
                                return FormatUtils.formatPercent(if (raw > 0.0 && raw < 0.1) 0.1 else kotlin.math.round(raw * 10.0) / 10.0)
                            }
                        })
                        if (barAnim) barAnim = false
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                    data.take(10).forEachIndexed { idx, (label, value) ->
                        val pctRaw = value / total * 100.0
                        if (pctRaw < 0.5) return@forEachIndexed
                        val pct = if (pctRaw > 0.0 && pctRaw < 0.1) 0.1 else kotlin.math.round(pctRaw * 10.0) / 10.0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) { drawRect(color = Color(colors[idx % colors.size])) }
                            Spacer(Modifier.width(6.dp))
                            Text("${truncateLabel(label)}: ${FormatUtils.formatPercent(pct)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
