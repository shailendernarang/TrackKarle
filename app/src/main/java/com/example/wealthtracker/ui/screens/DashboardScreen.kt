package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.google.android.gms.ads.AdSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.util.FormatUtils

private data class Reminder(
    val id: String,
    val title: String,
    val investmentType: String,
    val action: String,
    val whenText: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onAddClick: () -> Unit,
    onOpenInvestments: () -> Unit,
    onOpenCalculators: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val allItems by viewModel.investments.collectAsState()
    val filtered by viewModel.filteredInvestments.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(ctx) { 
        ctx as? android.app.Activity ?: (ctx as? androidx.appcompat.view.ContextThemeWrapper)?.baseContext as? android.app.Activity
    }
    val prefs = remember { ctx.getSharedPreferences("reminders_prefs", android.content.Context.MODE_PRIVATE) }
    var showReminderCarousel by rememberSaveable { mutableStateOf(true) }
    var startupShown by rememberSaveable { mutableStateOf(prefs.getInt("startup_shown", 0)) }

    var vizType by remember { mutableStateOf("Pie") }
    val cfgTop = LocalConfiguration.current
    val isWideTop = cfgTop.screenWidthDp >= 700

    // Double-back-to-exit from home (Dashboard)
    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000)
            backPressedOnce = false
        }
    }
    BackHandler(enabled = true) {
        if (!backPressedOnce) {
            backPressedOnce = true
            Toast.makeText(ctx, "Press back again to exit", Toast.LENGTH_SHORT).show()
        } else {
            activity?.finishAffinity() ?: run {
                // Fallback if activity is null
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
    
    // Upcoming reminders: build list (FD <90d, Insurance <60d)
    val fdSoonList = allItems.filter { it.investmentType == "FD" }.mapNotNull { e ->
        e.fdMaturityDate?.let { d -> parseUiDateDashboard(d)?.let { it to e } }
    }.filter { (date, _) ->
        val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), date).toInt()
        days in 0..90
    }.sortedBy { it.first }
    val hiSoonList = allItems.filter { it.investmentType == "Health Insurance" }.mapNotNull { e ->
        e.hiRenewalDate?.let { d -> parseUiDateDashboard(d)?.let { it to e } }
    }.filter { (date, _) ->
        val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), date).toInt()
        days in 0..60
    }.sortedBy { it.first }
    val ackSet = remember { prefs.getStringSet("ack_ids", emptySet())?.toMutableSet() ?: mutableSetOf() }
    // Capture colors outside remember to avoid composable reads inside remember
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val reminders = remember(allItems) {
        buildList {
            fdSoonList.forEach { (d, e) ->
                add(Reminder("FD:${e.id}", e.bankName ?: "Fixed Deposit", "Fixed Deposit", "Maturity", formatAddedOnDashboard(d), primaryColor))
            }
            hiSoonList.forEach { (d, e) ->
                add(Reminder("HI:${e.id}", e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Health Insurance", "Health Insurance", "Renewal", formatAddedOnDashboard(d), tertiaryColor))
            }
        }
            .filter { it.id !in ackSet }
            .takeIf { it.isNotEmpty() } ?: emptyList()
    }
    val canShow = showReminderCarousel && reminders.isNotEmpty() && startupShown < 3
    
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = com.ss.wealthtracker.R.string.title_dashboard)) },
                actions = {
                    IconButton(onClick = { viewModel.consumeMessage(); onOpenInvestments() }) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = stringResource(id = com.ss.wealthtracker.R.string.cta_view_investments))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(id = com.ss.wealthtracker.R.string.settings))
                    }
                }
            )
        },
        bottomBar = {
            androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AdMob Adaptive Anchored banner (collapse on no fill)
                    run {
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
                            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                                override fun onAdLoaded() { 
                                    adLoaded = true 
                                }
                                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) { 
                                    adLoaded = false
                                    android.util.Log.e("DashboardAd", "Ad failed to load: ${error.message} (Code: ${error.code})")
                                }
                            }
                            adView.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                            
                            onDispose {
                                adView.destroy()
                            }
                        }
                        
                        if (adLoaded) {
                            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = com.ss.wealthtracker.R.string.privacy_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isWideTop) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FloatingActionButton(onClick = onOpenCalculators) {
                        Icon(Icons.Default.Calculate, contentDescription = "Open Calculators")
                    }
                    ExtendedFloatingActionButton(
                        onClick = onAddClick,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(id = com.ss.wealthtracker.R.string.btn_add_investment)) }
                    )
                }
            } else {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = com.ss.wealthtracker.R.string.btn_add_investment))
                }
            }
        }
    ) { inner ->
        val contentModifier = if (isWideTop) {
            Modifier.fillMaxSize().padding(inner).padding(16.dp)
        } else {
            Modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState())
        }
        
        Box(Modifier.fillMaxSize()) {
            Column(contentModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Filter chips row (dynamic)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = allItems.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.mapValues { it.value.size }
                val filters = listOf("All") + counts.keys.sorted()
                filters.forEach { key ->
                    val selected = (typeFilter ?: "All") == key
                    val count = if (key == "All") allItems.size else counts[key] ?: 0
                    val display = if (key == "FD") "Fixed Deposit" else key
                    val icon = when (key) {
                        "All" -> Icons.Default.Savings
                        "FD" -> Icons.Default.AccountBalance
                        "Mutual Fund", "NPS", "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp
                        "Gold" -> Icons.Default.Savings
                        else -> Icons.Default.Savings
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setTypeFilter(if (key == "All") null else key) },
                        label = { Text("$display (${FormatUtils.formatInt(count)})") },
                        leadingIcon = { Icon(icon, contentDescription = null) }
                    )
                }
            }

            // Summary cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(id = com.ss.wealthtracker.R.string.label_total_amount),
                    value = FormatUtils.formatINR(allItems.sumOf { it.amount }),
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(id = com.ss.wealthtracker.R.string.label_investments),
                    value = FormatUtils.formatInt(allItems.size),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            val cfg = LocalConfiguration.current
            val isWide = cfg.screenWidthDp >= 700
            val insightItems = if (typeFilter.isNullOrBlank()) allItems else filtered
            val recentSrc = if (typeFilter.isNullOrBlank()) allItems else filtered
            val recent = recentSrc.sortedByDescending { it.createdAt }.take(5)

            if (isWide) {
                // Two vertical halves: top (chart + recent side-by-side), bottom (insights full width)
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    // Top half
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Overall Allocation (left)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            ChartSectionDashboard(items = filtered, filter = typeFilter, vizType = vizType, onVizTypeChange = { vizType = it })
                        }
                        // Recent Investments (right)
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            if (recent.isNotEmpty()) {
                                Text(stringResource(id = com.ss.wealthtracker.R.string.recent_investments), style = MaterialTheme.typography.titleLarge)
                                HorizontalDivider()
                                LazyColumn(modifier = Modifier.fillMaxHeight(), contentPadding = PaddingValues(bottom = 12.dp)) {
                                    items(recent) { e ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            val icon = when (e.investmentType) {
                                                "FD" -> Icons.Default.AccountBalance
                                                "Mutual Fund" -> Icons.AutoMirrored.Filled.TrendingUp
                                                "PPF", "EPF", "NPS" -> Icons.Default.AccountBalance
                                                "Gold" -> Icons.Default.Savings
                                                "Health Insurance", "Term Insurance" -> Icons.Default.Savings
                                                "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp
                                                else -> Icons.Default.Savings
                                            }
                                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                val title = when (e.investmentType) {
                                                    "FD" -> stringResource(id = com.ss.wealthtracker.R.string.fixed_deposit)
                                                    "Equity", "Stocks" -> (e.stockName?.takeIf { it.isNotBlank() } ?: "Stocks")
                                                    "Mutual Fund" -> e.type.ifBlank { "Mutual Fund" }
                                                    "Gold" -> e.goldType?.takeIf { it.isNotBlank() } ?: "Gold"
                                                    "Health Insurance" -> e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Health Insurance"
                                                    else -> if (e.investmentType == "Others") e.type.ifBlank { "Others" } else e.investmentType
                                                }
                                                Text(title, fontWeight = FontWeight.SemiBold)
                                                val subtitleBits = mutableListOf<String>()
                                                when (e.investmentType) {
                                                    "FD" -> { e.bankName?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.fdRate?.let { subtitleBits += "${String.format(java.util.Locale.ENGLISH, "%.2f", it)}%" }; e.fdMaturityDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += "Maturity $it" } }
                                                    "Equity", "Stocks" -> { e.stockDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                                    "Mutual Fund" -> { e.mfDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                                    "Gold" -> { e.goldDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                                    "PPF" -> { e.ppfFy?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.ppfDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                                    "NPS" -> { e.npsTier?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.npsDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                                    "Health Insurance" -> { e.hiRenewalDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += "Renews $it" } }
                                                    else -> {}
                                                }
                                                if (subtitleBits.isNotEmpty()) {
                                                    Text(subtitleBits.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(FormatUtils.formatINRShort(e.amount), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    InsightsSection(allItems = insightItems, currentFilter = typeFilter)
                }
            } else {
                // Phone layout: stacked sections
                ChartSectionDashboard(items = filtered, filter = typeFilter, vizType = vizType, onVizTypeChange = { vizType = it })
                // Insights
                InsightsSection(allItems = insightItems, currentFilter = typeFilter)
                // Recent investments
                if (recent.isNotEmpty()) {
                    Text(stringResource(id = com.ss.wealthtracker.R.string.recent_investments), style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()
                    recent.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (e.investmentType) {
                                "FD" -> Icons.Default.AccountBalance
                                "Mutual Fund" -> Icons.AutoMirrored.Filled.TrendingUp
                                "PPF", "EPF", "NPS" -> Icons.Default.AccountBalance
                                "Gold" -> Icons.Default.Savings
                                "Health Insurance", "Term Insurance" -> Icons.Default.Savings
                                "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp
                                else -> Icons.Default.Savings
                            }
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                val title = when (e.investmentType) {
                                    "FD" -> stringResource(id = com.ss.wealthtracker.R.string.fixed_deposit)
                                    "Equity", "Stocks" -> (e.stockName?.takeIf { it.isNotBlank() } ?: "Stocks")
                                    "Mutual Fund" -> e.type.ifBlank { "Mutual Fund" }
                                    "Gold" -> e.goldType?.takeIf { it.isNotBlank() } ?: "Gold"
                                    "Health Insurance" -> e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Health Insurance"
                                    else -> if (e.investmentType == "Others") e.type.ifBlank { "Others" } else e.investmentType
                                }
                                Text(title, fontWeight = FontWeight.SemiBold)
                                val subtitleBits = mutableListOf<String>()
                                when (e.investmentType) {
                                    "FD" -> { e.bankName?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.fdRate?.let { subtitleBits += "${String.format(java.util.Locale.ENGLISH, "%.2f", it)}%" }; e.fdMaturityDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += "Maturity $it" } }
                                    "Equity", "Stocks" -> { e.stockDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                    "Mutual Fund" -> { e.mfDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                    "Gold" -> { e.goldDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                    "PPF" -> { e.ppfFy?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.ppfDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                    "NPS" -> { e.npsTier?.takeIf { it.isNotBlank() }?.let { subtitleBits += it }; e.npsDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += it } }
                                    "Health Insurance" -> { e.hiRenewalDate?.takeIf { it.isNotBlank() }?.let { subtitleBits += "Renews $it" } }
                                    else -> {}
                                }
                                if (subtitleBits.isNotEmpty()) {
                                    Text(subtitleBits.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(FormatUtils.formatINRShort(e.amount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    }
    
    // Reminder overlay - covers entire screen including topBar/bottomBar
    if (canShow) {
        LaunchedEffect(Unit) {
            startupShown += 1
            prefs.edit().putInt("startup_shown", startupShown).apply()
        }
        val pager = rememberPagerState(pageCount = { reminders.size })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with close button
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Reminder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showReminderCarousel = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Pager content
                    HorizontalPager(state = pager) { idx ->
                        val r = reminders[idx]
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Title with investment type badge
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = r.color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        r.investmentType,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = r.color,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                r.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Due date card with action label
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = r.color.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "${r.action} Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            r.whenText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = r.color
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = r.color,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Page indicators
                    if (reminders.size > 1) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(reminders.size) { i ->
                                val active = pager.currentPage == i
                                Box(
                                    Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (active) 8.dp else 6.dp)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showReminderCarousel = false
                                Toast.makeText(ctx, "We'll remind you next time you open the app", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Later")
                        }
                        Button(
                            onClick = {
                                val current = reminders[pager.currentPage]
                                ackSet += current.id
                                prefs.edit().putStringSet("ack_ids", ackSet).apply()
                                if (pager.currentPage == reminders.lastIndex) {
                                    showReminderCarousel = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Got it")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun InsightsSection(allItems: List<InvestmentEntity>, currentFilter: String?) {
    if (allItems.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    val title = if (currentFilter.isNullOrBlank()) "Insights" else "Insights · " + (if (currentFilter == "Equity") "Stocks" else currentFilter)
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    val items = allItems.map { e -> if (e.investmentType == "Equity") e.copy(investmentType = "Stocks") else e }

    // Row 1: compact headline insights
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filteredType = currentFilter?.let { if (it == "Equity") "Stocks" else it }
        if (filteredType.isNullOrBlank()) {
            val byType = items.groupBy { it.investmentType }
                .mapValues { it.value.sumOf { v -> v.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
            byType.forEach { (label, amt) ->
                // Skip generic widgets we already show dedicated cards for
                if (label == "Stocks" || label == "Health Insurance") return@forEach
                val icon = when (label) {
                    "FD", "Fixed Deposit" -> "icons/bank.svg"
                    "Stocks" -> "icons/stock.svg"
                    "Gold" -> "icons/gold.svg"
                    "Health Insurance" -> "icons/shield.svg"
                    else -> null
                }
                SmallInsightCard(title = label, value = FormatUtils.formatINR(amt), iconAsset = icon)
            }

            val fds = items.filter { it.investmentType == "FD" }
            if (fds.isNotEmpty()) {
                val avgRate = fds.mapNotNull { it.fdRate }.average().takeIf { !it.isNaN() }
                if (avgRate != null) SmallInsightCard(title = "FD Avg Rate", value = "${"%.2f".format(avgRate)}%", iconAsset = "icons/bank.svg")
                val banks = fds.mapNotNull { it.bankName }.distinct().size
                if (banks > 0) SmallInsightCard(title = "FD Banks", value = FormatUtils.formatInt(banks), iconAsset = "icons/bank.svg")
            }

            val stocks = items.filter { it.investmentType == "Stocks" }
            if (stocks.isNotEmpty()) {
                val invested = stocks.sumOf { it.amount }
                SmallInsightCard(title = "Stocks (${FormatUtils.formatInt(stocks.size)})", value = FormatUtils.formatINR(invested), iconAsset = "icons/stock.svg")
                val avgInv = invested / stocks.size
                SmallInsightCard(title = "Avg/Stock", value = FormatUtils.formatINRShort(avgInv), iconAsset = "icons/stock.svg")
            }

            val hi = items.filter { it.investmentType == "Health Insurance" }
            if (hi.isNotEmpty()) {
                val total = hi.sumOf { it.amount }
                SmallInsightCard(title = "HI Premium", value = FormatUtils.formatINRShort(total), iconAsset = "icons/shield.svg")
                val soonest = hi.mapNotNull { e -> e.hiRenewalDate?.let { d -> parseUiDateDashboard(d)?.let { it to e } } }.minByOrNull { it.first }
                soonest?.let { (date, _) -> SmallInsightCard(title = "Next Renewal", value = formatAddedOnDashboard(date), iconAsset = "icons/shield.svg") }
            }
        } else when (filteredType) {
            "FD" -> {
                val fds = items
                val total = fds.sumOf { it.amount }
                SmallInsightCard(title = "Total in FD", value = FormatUtils.formatINR(total), iconAsset = "icons/bank.svg")
                SmallInsightCard(title = "FD Count", value = FormatUtils.formatInt(fds.size), iconAsset = "icons/bank.svg")
                val avgRate = fds.mapNotNull { it.fdRate }.average().takeIf { !it.isNaN() }
                if (avgRate != null) SmallInsightCard(title = "Avg Rate", value = "${"%.2f".format(avgRate)}%", iconAsset = "icons/bank.svg")
                val maxRate = fds.mapNotNull { it.fdRate }.maxOrNull()
                if (maxRate != null) SmallInsightCard(title = "Highest Rate", value = "${"%.2f".format(maxRate)}%", iconAsset = "icons/bank.svg")
            }
            "Stocks" -> {
                val stocks = items
                SmallInsightCard(title = "Count", value = FormatUtils.formatInt(stocks.size), iconAsset = "icons/stock.svg")
                val total = stocks.sumOf { it.amount }
                SmallInsightCard(title = "Invested", value = FormatUtils.formatINR(total), iconAsset = "icons/stock.svg")
                val avgInv = total / stocks.size.coerceAtLeast(1)
                SmallInsightCard(title = "Avg/Stock", value = FormatUtils.formatINRShort(avgInv), iconAsset = "icons/stock.svg")
                val top = stocks.groupBy { ((it.stockName ?: it.type).ifBlank { "Unknown" }) }
                    .mapValues { it.value.sumOf { v -> v.amount } }
                    .toList().sortedByDescending { it.second }.firstOrNull()?.first
                if (!top.isNullOrBlank()) SmallInsightCard(title = "Top Holding", value = top, iconAsset = "icons/stock.svg")
            }
            "Health Insurance" -> {
                val hi = items
                val total = hi.sumOf { it.amount }
                SmallInsightCard(title = "Policies", value = FormatUtils.formatInt(hi.size), iconAsset = "icons/shield.svg")
                SmallInsightCard(title = "Total Premium", value = FormatUtils.formatINR(total), iconAsset = "icons/shield.svg")
                val avg = total / hi.size.coerceAtLeast(1)
                SmallInsightCard(title = "Avg Premium", value = FormatUtils.formatINRShort(avg), iconAsset = "icons/shield.svg")
                val next = hi.mapNotNull { e -> e.hiRenewalDate?.let { d -> parseUiDateDashboard(d)?.let { it to e } } }.minByOrNull { it.first }
                next?.let { (d, _) -> SmallInsightCard(title = "Next Renewal", value = formatAddedOnDashboard(d), iconAsset = "icons/shield.svg") }
            }
            "Mutual Fund" -> {
                val total = items.sumOf { it.amount }
                SmallInsightCard(title = "Total", value = FormatUtils.formatINR(total))
                SmallInsightCard(title = "Funds", value = FormatUtils.formatInt(items.size))
                val avg = total / items.size.coerceAtLeast(1)
                SmallInsightCard(title = "Avg/Fund", value = FormatUtils.formatINRShort(avg))
            }
            "Gold" -> {
                val total = items.sumOf { it.amount }
                SmallInsightCard(title = "Total Gold", value = FormatUtils.formatINR(total), iconAsset = "icons/gold.svg")
                SmallInsightCard(title = "Holdings", value = FormatUtils.formatInt(items.size), iconAsset = "icons/gold.svg")
                val avg = total / items.size.coerceAtLeast(1)
                SmallInsightCard(title = "Avg/Holding", value = FormatUtils.formatINRShort(avg), iconAsset = "icons/gold.svg")
            }
            "PPF", "EPF", "NPS" -> {
                val total = items.sumOf { it.amount }
                SmallInsightCard(title = "Total", value = FormatUtils.formatINR(total))
                SmallInsightCard(title = "Accounts", value = FormatUtils.formatInt(items.size))
                val avg = total / items.size.coerceAtLeast(1)
                SmallInsightCard(title = "Avg/Account", value = FormatUtils.formatINRShort(avg))
            }
            "Term Insurance" -> {
                val total = items.sumOf { it.amount }
                SmallInsightCard(title = "Policies", value = FormatUtils.formatInt(items.size), iconAsset = "icons/shield.svg")
                SmallInsightCard(title = "Total Premium", value = FormatUtils.formatINR(total), iconAsset = "icons/shield.svg")
            }
            else -> {
                val total = items.sumOf { it.amount }
                SmallInsightCard(title = "Total", value = FormatUtils.formatINR(total))
                SmallInsightCard(title = "Count", value = FormatUtils.formatInt(items.size))
            }
        }
    }

    // Row 2: more insights (concentration, diversification, upcoming events)
    val totalAmt = items.sumOf { it.amount }.coerceAtLeast(1.0)
    val filteredType = currentFilter?.let { if (it == "Equity") "Stocks" else it }
    val analysisLines = mutableListOf<String>()
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filteredType.isNullOrBlank()) {
            val byType = items.groupBy { it.investmentType }.mapValues { it.value.sumOf { v -> v.amount } }.toList().sortedByDescending { it.second }
            val topShare = if (byType.isNotEmpty()) byType.first().second / totalAmt * 100.0 else 0.0
            val hhi = byType.fold(0.0) { acc, (_, v) -> val s = v / totalAmt; acc + s * s } // 0..1
            val conc = when {
                hhi >= 0.25 -> "High"
                hhi >= 0.15 -> "Moderate"
                else -> "Low"
            }
            SmallInsightCard("Top Type Share", String.format(java.util.Locale.ENGLISH, "%.1f%%", topShare.coerceAtMost(100.0)))
            SmallInsightCard("Concentration", conc)
            val distinct = buildSet {
                items.filter { it.investmentType == "Stocks" }.mapNotNullTo(this) { it.stockName?.takeIf { n -> n.isNotBlank() } ?: it.type }
                items.filter { it.investmentType == "Mutual Fund" }.mapTo(this) { it.type.ifBlank { "Mutual Fund" } }
                items.filter { it.investmentType == "FD" }.mapNotNullTo(this) { it.bankName ?: "Unknown Bank" }
                items.filter { it.investmentType == "Gold" }.mapTo(this) { it.goldType ?: "Gold" }
            }.size
            SmallInsightCard("Diversification", "${FormatUtils.formatInt(distinct)} assets")
            val fdSoon = items.filter { it.investmentType == "FD" }.mapNotNull { it.fdMaturityDate?.let { d -> parseUiDateDashboard(d) } }.count {
                val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it).toInt()
                days in 0..90
            }
            val hiSoon = items.filter { it.investmentType == "Health Insurance" }.mapNotNull { it.hiRenewalDate?.let { d -> parseUiDateDashboard(d) } }.count {
                val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it).toInt()
                days in 0..60
            }
            if (fdSoon > 0) SmallInsightCard("FDs <${FormatUtils.formatInt(90)}d", FormatUtils.formatInt(fdSoon))
            if (hiSoon > 0) SmallInsightCard("Renewals <${FormatUtils.formatInt(60)}d", FormatUtils.formatInt(hiSoon))
            analysisLines += "Portfolio concentration: $conc. Top type covers ${FormatUtils.formatPercent(topShare)} of invested amount."
            if (fdSoon > 0 || hiSoon > 0) analysisLines += "Upcoming: ${FormatUtils.formatInt(fdSoon)} FD maturities within ${FormatUtils.formatInt(90)}d, ${FormatUtils.formatInt(hiSoon)} insurance renewals within ${FormatUtils.formatInt(60)}d."
        } else when (filteredType) {
            "Stocks" -> {
                val stocks = items
                val byName = stocks.groupBy { (it.stockName ?: it.type).ifBlank { "Unknown" } }.mapValues { it.value.sumOf { v -> v.amount } }.toList().sortedByDescending { it.second }
                val uniq = byName.size
                val topShare = if (byName.isNotEmpty()) byName.first().second / totalAmt * 100.0 else 0.0
                SmallInsightCard("Unique Stocks", FormatUtils.formatInt(uniq), iconAsset = "icons/stock.svg")
                SmallInsightCard("Top Holding %", FormatUtils.formatPercent(topShare.coerceAtMost(100.0)), iconAsset = "icons/stock.svg")
                val top3Share = byName.take(3).sumOf { it.second } / totalAmt * 100.0
                SmallInsightCard("Top 3 Cover", FormatUtils.formatPercent(top3Share.coerceAtMost(100.0)), iconAsset = "icons/stock.svg")
                analysisLines += "Stocks: ${FormatUtils.formatInt(uniq)} holdings. Top holding ${FormatUtils.formatPercent(topShare)}; top 3 cover ${FormatUtils.formatPercent(top3Share)}."
            }
            "FD" -> {
                val fds = items
                val avgRate = fds.mapNotNull { it.fdRate }.average().takeIf { !it.isNaN() }
                val nearest = fds.mapNotNull { it.fdMaturityDate?.let { d -> parseUiDateDashboard(d) } } .minOrNull()
                val soon = fds.mapNotNull { it.fdMaturityDate?.let { d -> parseUiDateDashboard(d) } }.count {
                    val days = java.time.Period.between(java.time.LocalDate.now(), it).days
                    days in 0..90
                }
                avgRate?.let { SmallInsightCard("Avg Rate", FormatUtils.formatPercent(it), iconAsset = "icons/bank.svg") }
                nearest?.let { SmallInsightCard("Nearest Maturity", formatAddedOnDashboard(it), iconAsset = "icons/bank.svg") }
                if (soon > 0) SmallInsightCard("Maturing <${FormatUtils.formatInt(90)}d", FormatUtils.formatInt(soon), iconAsset = "icons/bank.svg")
                analysisLines += "FDs: ${FormatUtils.formatInt(fds.size)} deposits; ${FormatUtils.formatInt(soon)} maturing within ${FormatUtils.formatInt(90)}d." + (if (avgRate != null) " Avg rate ${FormatUtils.formatPercent(avgRate)}." else "")
            }
            "Mutual Fund" -> {
                val byFund = items.groupBy { it.type.ifBlank { "Unknown" } }.mapValues { it.value.sumOf { v -> v.amount } }.toList().sortedByDescending { it.second }
                val uniq = byFund.size
                val topShare = if (byFund.isNotEmpty()) byFund.first().second / totalAmt * 100.0 else 0.0
                SmallInsightCard("Unique Funds", FormatUtils.formatInt(uniq))
                SmallInsightCard("Top Fund %", FormatUtils.formatPercent(topShare.coerceAtMost(100.0)))
                val amounts = items.map { it.amount }
                if (amounts.isNotEmpty()) {
                    val min = amounts.minOrNull() ?: 0.0
                    val max = amounts.maxOrNull() ?: 0.0
                    if (min != max) SmallInsightCard("Investment Range", "${FormatUtils.formatINRShort(min)}-${FormatUtils.formatINRShort(max)}")
                }
                val top3Share = byFund.take(3).sumOf { it.second } / totalAmt * 100.0
                SmallInsightCard("Top 3 Cover", FormatUtils.formatPercent(top3Share.coerceAtMost(100.0)))
                analysisLines += "MF: ${FormatUtils.formatInt(uniq)} schemes. Top fund concentration ${FormatUtils.formatPercent(topShare)}; top 3 cover ${FormatUtils.formatPercent(top3Share)}."
            }
            "Gold" -> {
                val byType = items.groupBy { (it.goldType ?: "Gold").ifBlank { "Gold" } }.mapValues { it.value.sumOf { v -> v.amount } }.toList().sortedByDescending { it.second }
                val uniq = byType.size
                val topShare = if (byType.isNotEmpty()) byType.first().second / totalAmt * 100.0 else 0.0
                SmallInsightCard("Gold Types", FormatUtils.formatInt(uniq), iconAsset = "icons/gold.svg")
                SmallInsightCard("Top Type %", FormatUtils.formatPercent(topShare.coerceAtMost(100.0)), iconAsset = "icons/gold.svg")
                val amounts = items.map { it.amount }
                if (amounts.isNotEmpty()) {
                    val min = amounts.minOrNull() ?: 0.0
                    val max = amounts.maxOrNull() ?: 0.0
                    if (min != max) SmallInsightCard("Investment Range", "${FormatUtils.formatINRShort(min)}-${FormatUtils.formatINRShort(max)}", iconAsset = "icons/gold.svg")
                }
                val oldest = items.minByOrNull { it.createdAt }
                oldest?.let { SmallInsightCard("First Purchase", formatAddedOnDashboard(java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), iconAsset = "icons/gold.svg") }
                analysisLines += "Gold: ${FormatUtils.formatInt(uniq)} types. Top type ${FormatUtils.formatPercent(topShare)}."
            }
            "PPF", "EPF", "NPS" -> {
                val amounts = items.map { it.amount }
                if (amounts.isNotEmpty()) {
                    val min = amounts.minOrNull() ?: 0.0
                    val max = amounts.maxOrNull() ?: 0.0
                    if (min != max) SmallInsightCard("Contribution Range", "${FormatUtils.formatINRShort(min)}-${FormatUtils.formatINRShort(max)}")
                }
                val oldest = items.minByOrNull { it.createdAt }
                oldest?.let { SmallInsightCard("Oldest Account", formatAddedOnDashboard(java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())) }
                val recent = items.filter { 
                    val days = java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        java.time.LocalDate.now()
                    )
                    days <= 365
                }.size
                if (recent > 0) SmallInsightCard("Added This Year", FormatUtils.formatInt(recent))
                analysisLines += "${filteredType}: ${FormatUtils.formatInt(items.size)} accounts."
            }
            "Term Insurance" -> {
                val amounts = items.map { it.amount }
                if (amounts.isNotEmpty()) {
                    val min = amounts.minOrNull() ?: 0.0
                    val max = amounts.maxOrNull() ?: 0.0
                    if (min != max) SmallInsightCard("Premium Range", "${FormatUtils.formatINRShort(min)}-${FormatUtils.formatINRShort(max)}", iconAsset = "icons/shield.svg")
                }
                val oldest = items.minByOrNull { it.createdAt }
                oldest?.let { SmallInsightCard("Oldest Policy", formatAddedOnDashboard(java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), iconAsset = "icons/shield.svg") }
                analysisLines += "Term Insurance: ${FormatUtils.formatInt(items.size)} policies."
            }
            "Health Insurance" -> {
                val soon = items.mapNotNull { it.hiRenewalDate?.let { d -> parseUiDateDashboard(d) } }.count {
                    val days = java.time.Period.between(java.time.LocalDate.now(), it).days
                    days in 0..60
                }
                if (soon > 0) SmallInsightCard("Renewals <${FormatUtils.formatInt(60)}d", FormatUtils.formatInt(soon), iconAsset = "icons/shield.svg")
                val amounts = items.map { it.amount }
                if (amounts.isNotEmpty()) {
                    val min = amounts.minOrNull() ?: 0.0
                    val max = amounts.maxOrNull() ?: 0.0
                    if (min != max) SmallInsightCard("Premium Range", "${FormatUtils.formatINRShort(min)}-${FormatUtils.formatINRShort(max)}", iconAsset = "icons/shield.svg")
                }
                val oldest = items.minByOrNull { it.createdAt }
                oldest?.let { SmallInsightCard("Oldest Policy", formatAddedOnDashboard(java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), iconAsset = "icons/shield.svg") }
                analysisLines += "Insurance: ${FormatUtils.formatInt(items.size)} policies; ${FormatUtils.formatInt(soon)} renewals within ${FormatUtils.formatInt(60)}d."
            }
            else -> {}
        }
    }
    // Text summary
    if (analysisLines.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Analysis", style = MaterialTheme.typography.titleMedium)
                analysisLines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun SmallInsightCard(title: String, value: String, leadingIcon: ImageVector? = null, iconAsset: String? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.widthIn(min = 140.dp).padding(vertical = 2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    leadingIcon != null -> Icon(leadingIcon, contentDescription = null)
                    iconAsset != null -> IconAsset(iconAsset)
                }
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun IconAsset(assetPath: String, contentDesc: String? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val loader = remember {
        ImageLoader.Builder(ctx)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val req = remember(assetPath) {
        ImageRequest.Builder(ctx)
            .data("file:///android_asset/$assetPath")
            .build()
    }
    AsyncImage(model = req, imageLoader = loader, contentDescription = contentDesc, modifier = Modifier.size(18.dp))
}

private fun parseUiDateDashboard(s: String): java.time.LocalDate? = runCatching {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
    java.time.LocalDate.parse(s, fmt)
}.getOrNull()

private fun formatAddedOnDashboard(d: java.time.LocalDate): String = d.format(
    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
)
@Composable
private fun FdRatesSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val data: List<Triple<String, String, String>> = remember {
        runCatching {
            val json = ctx.assets.open("fd_rates.json").bufferedReader().use { it.readText() }
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Triple(o.getString("bank"), o.getString("rate"), o.optString("tenure", ""))
            }
        }.getOrDefault(emptyList())
    }
    if (data.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(id = com.ss.wealthtracker.R.string.compare_fd_rates), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            data.take(8).forEach { (bank, rate, tenure) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(bank, fontWeight = FontWeight.SemiBold)
                        if (tenure.isNotBlank()) Text(tenure, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(rate, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(id = com.ss.wealthtracker.R.string.fd_rates_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(2.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.offset(y = (-1).dp)) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            }
        }
    }
}


@Composable
private fun ChartSectionDashboard(items: List<InvestmentEntity>, filter: String?, vizType: String, onVizTypeChange: (String) -> Unit) {
    fun truncateLabel(s: String, max: Int = 12): String = if (s.length <= max) s else s.take(max - 1) + "…"
    val data = run {
        val unknown = "Unknown"
        val unknownBank = "Unknown Bank"
        val grouped: Map<String, List<InvestmentEntity>> = when (val f = filter?.let { if (it == "Equity") "Stocks" else it }) {
            "FD" -> items.groupBy { (it.bankName ?: unknownBank).ifBlank { unknownBank } }
            // Fallback to display 'type' when stockName is missing to avoid blank labels
            "Stocks" -> items.groupBy { ((it.stockName ?: it.type).ifBlank { unknown }) }
            "Gold" -> items.groupBy { (it.goldType ?: "Gold").ifBlank { "Gold" } }
            // Use display 'type' field for MF names to avoid missing mfName property
            "Mutual Fund" -> items.groupBy { it.type.ifBlank { unknown } }
            "Health Insurance" -> items.groupBy { (it.hiPolicyName ?: unknown).ifBlank { unknown } }
            "PPF" -> items.groupBy { (it.ppfFy ?: "PPF").ifBlank { "PPF" } }
            "NPS" -> items.groupBy { (it.npsTier ?: "NPS").ifBlank { "NPS" } }
            null, "" -> items.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }
            else -> items.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }
        }
        val pairs = grouped.map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
        val limit = 10
        if (pairs.size <= limit) pairs else {
            val top = pairs.take(limit)
            val othersSum = pairs.drop(limit).sumOf { it.second }
            top + listOf("Others" to othersSum)
        }
    }
    // Distinct darker palette
    val colors = listOf(
        0xFFF44336.toInt(), // red
        0xFF4CAF50.toInt(), // green
        0xFFFBC02D.toInt(), // yellow (amber darken)
        0xFF2196F3.toInt(), // blue
        0xFF9C27B0.toInt(), // purple
        0xFFFF9800.toInt()  // orange
    )

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)) {
            // Resolve dynamic text colors based on theme
            val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(id = com.ss.wealthtracker.R.string.overall_allocation), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedBg = MaterialTheme.colorScheme.primary
                    val selectedText = MaterialTheme.colorScheme.onPrimary
                    val unselectedBg = Color.Transparent
                    val unselectedText = MaterialTheme.colorScheme.onSurfaceVariant
                    // Pie segment
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (vizType == "Pie") selectedBg else unselectedBg)
                            .clickable { if (vizType != "Pie") onVizTypeChange("Pie") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(id = com.ss.wealthtracker.R.string.viz_pie), color = if (vizType == "Pie") selectedText else unselectedText, style = MaterialTheme.typography.labelMedium)
                    }
                    // Bar segment
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (vizType == "Bar") selectedBg else unselectedBg)
                            .clickable { if (vizType != "Bar") onVizTypeChange("Bar") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(id = com.ss.wealthtracker.R.string.viz_bar), color = if (vizType == "Bar") selectedText else unselectedText, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (data.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text(stringResource(id = com.ss.wealthtracker.R.string.no_data_to_visualize), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (vizType == "Pie") {
                var selectedLabel by remember(items, filter) { mutableStateOf<String?>(null) }
                var animateOnToggle by remember(items, filter, vizType) { mutableStateOf(true) }
                AndroidView(
                    factory = { context ->
                        com.example.wealthtracker.ui.charts.ChartUtils.createPieChart(context, onSurface)
                    },
                    update = { view ->
                        val first = view.data == null
                        com.example.wealthtracker.ui.charts.ChartUtils.bindPieData(view, data, colors, animateOnToggle)
                        if (first) {
                            val txtPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = onSurface
                                textSize = 40f
                                typeface = android.graphics.Typeface.create("montserrat", android.graphics.Typeface.BOLD)
                            }
                            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = onSurfaceVariant
                                strokeWidth = 6f
                            }
                            val chartRef = view
                            view.marker = object : com.github.mikephil.charting.components.IMarker {
                                private var label: String = ""
                                override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: com.github.mikephil.charting.highlight.Highlight?) {
                                    val pe = e as? com.github.mikephil.charting.data.PieEntry ?: return
                                    val ds = chartRef.data?.getDataSetByIndex(0) as? com.github.mikephil.charting.data.PieDataSet
                                    val denom = (ds?.values?.sumOf { it.value.toDouble() } ?: 0.0).coerceAtLeast(1.0)
                                    val pctRaw = (pe.value.toDouble() / denom * 100.0)
                                    val pct = if (pctRaw > 0.0 && pctRaw < 0.1) 0.1 else kotlin.math.round(pctRaw * 10.0) / 10.0
                                    label = "${pe.label}: ${FormatUtils.formatPercent(pct)}"
                                }
                                override fun draw(canvas: android.graphics.Canvas?, posX: Float, posY: Float) {
                                    if (canvas == null || label.isBlank()) return
                                    val centerX = chartRef.centerOffsets.x
                                    val dir = if (posX >= centerX) 1f else -1f
                                    val length = 150f
                                    val startX = posX
                                    val startY = posY
                                    val endX = posX + dir * length
                                    val endY = posY
                                    canvas.drawLine(startX, startY, endX, endY, linePaint)
                                    val textW = txtPaint.measureText(label)
                                    val textX = if (dir > 0) endX + 12f else endX - 12f - textW
                                    canvas.drawText(label, textX, endY + 10f, txtPaint)
                                }
                                override fun getOffset(): com.github.mikephil.charting.utils.MPPointF = com.github.mikephil.charting.utils.MPPointF(0f, 0f)
                                override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): com.github.mikephil.charting.utils.MPPointF = getOffset()
                            }
                            view.setDrawMarkers(true)
                            view.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                    selectedLabel = (e as? com.github.mikephil.charting.data.PieEntry)?.label
                                }
                                override fun onNothingSelected() { selectedLabel = null }
                            })
                        }
                        if (animateOnToggle) animateOnToggle = false
                    },
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
                Spacer(Modifier.height(8.dp))
                // Legend with percentages (hide < 0.5%)
                val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    data.take(10).forEachIndexed { idx, (label, value) ->
                        val pctRaw = (value / total * 100.0)
                        if (pctRaw < 0.5) return@forEachIndexed
                        val pct = if (pctRaw > 0.0 && pctRaw < 0.1) 0.1 else kotlin.math.round(pctRaw * 10.0) / 10.0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
                                drawRect(color = Color(colors[idx % colors.size]))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${truncateLabel(label)}: ${FormatUtils.formatPercent(pct)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                var barAnimateOnToggle by remember(items, filter, vizType) { mutableStateOf(true) }
                AndroidView(
                    factory = { context ->
                        com.example.wealthtracker.ui.charts.ChartUtils.createBarChart(context, onSurface)
                    },
                    update = { view ->
                        val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                        val pctValues = data.map { (it.second / total * 100.0).toFloat() }
                        val labels = data.map { truncateLabel(it.first) }
                        com.example.wealthtracker.ui.charts.ChartUtils.bindBarData(view, labels, pctValues, colors, onSurface, barAnimateOnToggle)
                        view.data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getBarLabel(barEntry: com.github.mikephil.charting.data.BarEntry?): String {
                                val raw = (barEntry?.y ?: 0f).toDouble()
                                val pct = if (raw > 0.0 && raw < 0.1) 0.1 else kotlin.math.round(raw * 10.0) / 10.0
                                return FormatUtils.formatPercent(pct)
                            }
                        })
                        if (barAnimateOnToggle) barAnimateOnToggle = false
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                    data.take(10).forEachIndexed { idx, (label, value) ->
                        val pctRaw = (value / total * 100.0)
                        if (pctRaw < 0.5) return@forEachIndexed
                        val pct = if (pctRaw > 0.0 && pctRaw < 0.1) 0.1 else kotlin.math.round(pctRaw * 10.0) / 10.0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
                                drawRect(color = Color(colors[idx % colors.size]))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${truncateLabel(label)}: ${FormatUtils.formatPercent(pct)}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
