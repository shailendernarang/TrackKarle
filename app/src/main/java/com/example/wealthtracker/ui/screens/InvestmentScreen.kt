package com.example.wealthtracker.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.LocalContext
import com.example.wealthtracker.util.findActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.wealthtracker.ui.components.EditInvestmentBottomSheet
import com.example.wealthtracker.ui.components.ModernSnackbarHost
import com.example.wealthtracker.ui.components.showSuccessSnackbar
import com.example.wealthtracker.ui.components.showDeleteSnackbar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.analytics.AnalyticsManager
import com.example.wealthtracker.analytics.TrackScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.IndianBanks
import com.example.wealthtracker.util.InvestmentTypes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.Color as GColor
import java.io.File
import java.io.FileOutputStream
import com.ss.wealthtracker.R
import com.example.wealthtracker.util.AverageRatesProvider
import java.net.URL
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.wealthtracker.ui.components.AppodealBanner
import com.ss.wealthtracker.BuildConfig
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

private fun loadFdRatesQuick(ctx: android.content.Context): List<FdRateRow> {
    return runCatching {
        ctx.resources.openRawResource(R.raw.fd_rates_30_banks)
            .bufferedReader()
            .useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        val bank = parts[0].trim()
                        val tenure = parts[1].trim()
                        val non = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                        val sen = parts[3].toDoubleOrNull() ?: return@mapNotNull null
                        FdRateRow(bank, tenure, non, sen)
                    } else null
                }.toList()
            }
    }.getOrDefault(emptyList())
}

// Mutual Fund name suggestions via public API (api.mfapi.in)
private suspend fun fetchMfSuggest(q: String): List<String> = withContext(kotlinx.coroutines.Dispatchers.IO) {
    runCatching {
        val link = "https://api.mfapi.in/mf/search?q=" + java.net.URLEncoder.encode(q, "UTF-8")
        val json = URL(link).openStream().use { it.readBytes().decodeToString() }
        val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
        val list = arr.mapNotNull { el -> el.asJsonObject["schemeName"]?.asString }.take(15)
        Log.d("MF_API", "q='$q' results=${list.size} first='${list.firstOrNull() ?: ""}'")
        list
    }.onFailure { e -> Log.e("MF_API", "error for q='$q'", e) }.getOrDefault(emptyList())
}

// Stocks autocomplete via StocksApiProvider (Moneycontrol/Yahoo backed)
private suspend fun fetchStockSuggest(q: String): List<String> = withContext(kotlinx.coroutines.Dispatchers.IO) {
    runCatching {
        val res = com.example.wealthtracker.network.StocksApiProvider.service.search(query = q, quotesCount = 15)
        val names = res.quotes?.mapNotNull { it.longname ?: it.shortname ?: it.symbol } ?: emptyList()
        Log.d("STOCK_API", "q='$q' results=${names.size} first='${names.firstOrNull() ?: ""}'")
        names.take(15)
    }.onFailure { e -> Log.e("STOCK_API", "error for q='$q'", e) }.getOrDefault(emptyList())
}

private fun normalizeTenureQuick(raw: String): String {
    val s = raw.trim().lowercase()
    return when {
        s.contains("1y 4m") -> "1y 4m"
        s.contains("1y 6m") || s.startsWith("2y") -> "2y"
        s.startsWith("1y") -> "1y"
        s.startsWith("3y") -> "3y"
        s.startsWith("5y") -> "5y"
        else -> raw
    }
}

private val TENURE_ORDER_QUICK = listOf("1y", "1y 4m", "1y 6m", "2y", "3y", "5y")

private fun formatIndianNumber(input: String): String {
    val clean = input.replace(",", "").trim()
    if (clean.isBlank()) return ""
    val parts = clean.split('.')
    val intPart = parts.getOrNull(0)?.filter { it.isDigit() } ?: ""
    val decPart = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
    val num = intPart.toLongOrNull() ?: return ""
    val dfs = DecimalFormatSymbols(Locale("en", "IN"))
    val df = DecimalFormat("#,##,##0").apply { decimalFormatSymbols = dfs }
    var res = df.format(num)
    if (decPart.isNotEmpty()) res += "." + decPart.take(2)
    return res
}

// Encrypted backup/restore removed: database is now encrypted at rest via SQLCipher.

@Composable
private fun ChartSection(
    items: List<InvestmentEntity>,
    filter: String?,
    vizType: String,
    onTypeChange: (String) -> Unit
) {
    val data: List<Pair<String, Double>> = remember(items, filter) {
        val grouped = if (filter == "FD") items.groupBy { it.bankName ?: "Others" } else items.groupBy { it.investmentType }
        grouped.map { it.key to it.value.sumOf { e -> e.amount } }.sortedByDescending { it.second }
    }
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
    ).map { it.toArgb() }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Visualization", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { onTypeChange("Pie") }, enabled = vizType != "Pie") { Text("Pie") }
                TextButton(onClick = { onTypeChange("Bar") }, enabled = vizType != "Bar") { Text("Bar") }
            }
            if (data.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No data to visualize", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Resolve dynamic text colors based on theme
                val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
                var animateOnce by remember(items, filter, vizType) { mutableStateOf(true) }
                if (vizType == "Pie") {
                    // Use new safe Compose pie chart instead of problematic MPAndroidChart
                    val composeColors = colors.map { Color(it) }
                    val centerText = when (filter) {
                        "FD" -> "Fixed\nDeposits"
                        "Stocks" -> "Stock\nPortfolio"
                        "Mutual Fund" -> "Mutual\nFunds"
                        else -> "Total\nInvestments"
                    }
                    
                    com.example.wealthtracker.ui.charts.SafePieChart(
                        data = data,
                        colors = composeColors,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        showLegend = false, // Legend shown separately below
                        animationEnabled = animateOnce,
                        centerText = centerText,
                        onSliceClick = null  // Disable tap to avoid interfering with scroll
                    )
                } else {
                    AndroidView(
                        factory = { context ->
                            com.example.wealthtracker.ui.charts.ChartUtils.createBarChart(context, onSurface)
                        },
                        update = { view ->
                            val labels = data.map { it.first }
                            val values = data.map { it.second.toFloat() }
                            com.example.wealthtracker.ui.charts.ChartUtils.bindBarData(view, labels, values, colors, onSurface, animateOnce)
                            view.xAxis.setLabelCount(labels.size, false)
                            view.setVisibleXRangeMaximum(6f)
                            if (animateOnce) animateOnce = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                
                // Reset animation flag
                LaunchedEffect(items, filter, vizType) {
                    animateOnce = false
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                data.take(10).forEachIndexed { idx, (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) { drawRect(color = Color(colors[idx % colors.size])) }
                        Spacer(Modifier.width(6.dp))
                        Text("$label: ${FormatUtils.formatINR(value)}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            
        }
    }
}

private fun formatAddedOn(ts: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM\u2019yy", Locale.ENGLISH)
        sdf.format(Date(ts))
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun TotalsBar(items: List<InvestmentEntity>) {
    androidx.compose.material3.Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Investment", style = MaterialTheme.typography.titleMedium)
                val total = items.sumOf { it.amount }
                Text(
                    FormatUtils.formatINR(total),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Total Investment Count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    FormatUtils.formatInt(items.size),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onOpenDashboard: () -> Unit = {},
    onOpenCalculators: () -> Unit = {},
    onOpenStocks: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBack: () -> Unit = {},
    showBack: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    requireDeviceLock: Boolean = false,
    onToggleRequireDeviceLock: () -> Unit = {},
    useHindiNumerals: Boolean = false,
    onToggleHindiNumerals: () -> Unit = {}
) {
    val items by viewModel.filteredInvestments.collectAsState()
    val allItems by viewModel.investments.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<InvestmentEntity?>(null) }
    var showEditBottomSheet by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(TextFieldValue("")) }
    var invType by remember { mutableStateOf(if (InvestmentTypes.all.contains("FD")) "FD" else InvestmentTypes.all.first()) }
    var othersName by remember { mutableStateOf("") }
    var mfName by remember { mutableStateOf("") }
    var mfSuggest by remember { mutableStateOf(listOf<String>()) }
    var mfExpanded by remember { mutableStateOf(false) }
    var mfJustSelected by remember { mutableStateOf(false) }
    var mfCommittedName by remember { mutableStateOf<String?>(null) }
    var mfHasFocus by remember { mutableStateOf(false) }
    var bankExpanded by remember { mutableStateOf(false) }
    var selectedBank: String? by remember { mutableStateOf(null) }
    var customBankName by remember { mutableStateOf("") }
    // FD extra fields
    var fdStartDate by remember { mutableStateOf("") }
    var fdMaturityDate by remember { mutableStateOf("") }
    var showFdStartPicker by remember { mutableStateOf(false) }
    var showFdMaturityPicker by remember { mutableStateOf(false) }
    var fdRate by remember { mutableStateOf("") }
    // Gold
    var goldType by remember { mutableStateOf<String?>(null) }
    var goldDate by remember { mutableStateOf("") }
    var showGoldDatePicker by remember { mutableStateOf(false) }
    // PPF
    var ppfFy by remember { mutableStateOf<String?>(null) }
    var ppfDate by remember { mutableStateOf("") }
    var showPpfDatePicker by remember { mutableStateOf(false) }
    // NPS
    var npsTier by remember { mutableStateOf<String?>(null) }
    var npsDate by remember { mutableStateOf("") }
    var showNpsDatePicker by remember { mutableStateOf(false) }
    // Health Insurance
    var hiPolicyName by remember { mutableStateOf("") }
    var hiRenewalDate by remember { mutableStateOf("") }
    var showHiRenewalPicker by remember { mutableStateOf(false) }
    // Stocks (Equity)
    var stockName by remember { mutableStateOf("") }
    var stockSuggest by remember { mutableStateOf(listOf<String>()) }
    var stockExpanded by remember { mutableStateOf(false) }
    var stockJustSelected by remember { mutableStateOf(false) }
    var stockCommittedName by remember { mutableStateOf<String?>(null) }
    var stockHasFocus by remember { mutableStateOf(false) }
    var stockDate by remember { mutableStateOf("") }
    var showStockDatePicker by remember { mutableStateOf(false) }
    var mfDate by remember { mutableStateOf("") }
    var showMfDatePicker by remember { mutableStateOf(false) }

    var highlightId by remember { mutableStateOf<Long?>(null) }
    var pendingAddHighlight by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val analytics = remember(context) { AnalyticsManager(context) }
    TrackScreen(screenName = "Investment", analyticsManager = analytics)

    LaunchedEffect(Unit) {
        analytics.logInvestmentCreationStarted(invType)
    }

    LaunchedEffect(invType) {
        analytics.logInvestmentTypeSelected(invType)
    }

    // Log FD bank selection
    LaunchedEffect(selectedBank, invType) {
        if (invType == "FD" && selectedBank != null) {
            analytics.logFeatureUsed(featureName = "fd_bank_selected", context = "Investment")
        }
    }

    // Log investment added on success
    LaunchedEffect(message) {
        if (message == "Added") {
            analytics.logInvestmentAdded(invType)
        }
    }

    // Debounced suggestions for MF and Stocks
    LaunchedEffect(mfName, invType, mfJustSelected, mfCommittedName, mfHasFocus) {
        if (mfJustSelected) { mfJustSelected = false; return@LaunchedEffect }
        // If the field shows a committed selection, do not fetch or expand
        if (invType == "Mutual Fund" && mfCommittedName != null && mfName.trim() == mfCommittedName!!.trim()) {
            mfSuggest = emptyList(); mfExpanded = false; return@LaunchedEffect
        }
        val q = mfName.trim()
        if (invType == "Mutual Fund" && q.length >= 2 && mfHasFocus) {
            delay(250)
            val latest = mfName.trim()
            if (invType == "Mutual Fund" && latest == q) {
                mfSuggest = fetchMfSuggest(q)
                mfExpanded = mfSuggest.isNotEmpty()
            }
        } else {
            mfSuggest = emptyList()
            mfExpanded = false
        }
    }
    LaunchedEffect(stockName, invType, stockJustSelected, stockCommittedName, stockHasFocus) {
        if (stockJustSelected) { stockJustSelected = false; return@LaunchedEffect }
        if ((invType == "Equity" || invType == "Stocks") && stockCommittedName != null && stockName.trim() == stockCommittedName!!.trim()) {
            stockSuggest = emptyList(); stockExpanded = false; return@LaunchedEffect
        }
        val q = stockName.trim()
        if ((invType == "Equity" || invType == "Stocks") && q.length >= 2 && stockHasFocus) {
            delay(250)
            val latest = stockName.trim()
            if ((invType == "Equity" || invType == "Stocks") && latest == q) {
                stockSuggest = fetchStockSuggest(q)
                stockExpanded = stockSuggest.isNotEmpty()
            }
        } else {
            stockSuggest = emptyList()
            stockExpanded = false
        }
    }

    var attemptedAdd by remember { mutableStateOf(false) }
    val amountError = attemptedAdd && (amount.text.isBlank() || amount.text.replace(",", "").toDoubleOrNull()?.let { it <= 0.0 } != false)
    val bankError = attemptedAdd && invType == "FD" && (selectedBank == null)
    val othersNameError = attemptedAdd && invType == "Others" && othersName.isBlank()
    val mfNameError = false

    // Overflow menu removed; use dedicated Settings icon in top bar
    var lastDeleted by remember { mutableStateOf<InvestmentEntity?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val uiScope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // One-year estimate helpers (scoped)
    data class FdQuick(val bank: String, val tenure: String, val rate: Double)
    fun loadFdRatesQuick(ctx: android.content.Context): List<FdQuick> {
        return runCatching {
            ctx.resources.openRawResource(R.raw.fd_rates_30_banks)
                .bufferedReader()
                .useLines { lines ->
                    lines.drop(1).mapNotNull { line ->
                        val parts = line.split(",")
                        if (parts.size >= 3) {
                            val bank = parts[0].trim()
                            val tenure = parts[1].trim()
                            val nonSenior = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                            FdQuick(bank, tenure, nonSenior)
                        } else null
                    }.toList()
                }
        }.getOrDefault(emptyList())
    }
    val fdRates = remember { loadFdRatesQuick(ctx) }
    var avgRates by remember { mutableStateOf(AverageRatesProvider.defaults) }
    LaunchedEffect(Unit) {
        // Fetch live averages with cache fallback; ignore failures
        avgRates = AverageRatesProvider.getAverages(ctx)
    }
    fun parseTenureMonths(text: String): Int? {
        val t = text.trim().lowercase()
        val range = Regex("(\\d+)\\s*[-to]+\\s*(\\d+)\\s*(year|yr|y|month|m)").find(t)
        if (range != null) {
            val a = range.groupValues[1].toIntOrNull()
            val b = range.groupValues[2].toIntOrNull()
            val unit = range.groupValues[3]
            if (a != null && b != null) {
                val mid = (a + b) / 2.0
                return if (unit.startsWith("y")) (mid * 12).toInt() else mid.toInt()
            }
        }
        val m = Regex("(\\d+)(?:\\s*)(year|yr|y|month|m)").find(t)
        if (m != null) {
            val n = m.groupValues[1].toIntOrNull() ?: return null
            val unit = m.groupValues[2]
            return if (unit.startsWith("y")) n * 12 else n
        }
        val m2 = Regex("(\\d+)(y|yr|m)").find(t)
        if (m2 != null) {
            val n = m2.groupValues[1].toIntOrNull() ?: return null
            val unit = m2.groupValues[2]
            return if (unit.startsWith("y")) n * 12 else n
        }
        return null
    }
    fun findFdRate1Y(bank: String?): Double? {
        if (bank.isNullOrBlank()) return null
        val rows = fdRates.filter { it.bank.equals(bank, ignoreCase = true) }
        if (rows.isEmpty()) return null
        val oneYear = rows.firstOrNull { r ->
            val t = r.tenure.lowercase()
            t.contains("1y") || t.contains("1 y") || t.contains("12m") || t.contains("12 m") || t.contains("1 year") || t.contains("12 month")
        }
        if (oneYear != null) return oneYear.rate
        // Fallback: choose tenure closest to 12 months
        val scored = rows.mapNotNull { r ->
            val months = parseTenureMonths(r.tenure) ?: return@mapNotNull null
            kotlin.math.abs(months - 12) to r.rate
        }
        return scored.minByOrNull { it.first }?.second
    }


    fun findFdRateByDuration(bank: String?, months: Int): Double? {
        if (bank.isNullOrBlank()) return null
        val rows = fdRates.filter { it.bank.equals(bank, ignoreCase = true) }
        if (rows.isEmpty()) return null
        val scored = rows.mapNotNull { r ->
            val m = parseTenureMonths(r.tenure) ?: return@mapNotNull null
            kotlin.math.abs(m - months) to r.rate
        }
        if (scored.isEmpty()) return null
        return scored.minByOrNull { it.first }?.second
    }

    fun parseUiDate(s: String): java.time.LocalDate? {
        if (s.isBlank()) return null
        return try {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)
            java.time.LocalDate.parse(s, fmt)
        } catch (_: Throwable) { null }
    }

    LaunchedEffect(invType, selectedBank, fdStartDate, fdMaturityDate) {
        if (invType == "FD" && !selectedBank.isNullOrBlank()) {
            val start = parseUiDate(fdStartDate)
            val end = parseUiDate(fdMaturityDate)
            val newRate = if (start != null && end != null && end.isAfter(start)) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
                val monthsApprox = kotlin.math.max(1, kotlin.math.round(days / 30.4).toInt())
                findFdRateByDuration(selectedBank, monthsApprox) ?: findFdRate1Y(selectedBank)
            } else {
                findFdRate1Y(selectedBank)
            }
            if (newRate != null) fdRate = String.format(java.util.Locale.ENGLISH, "%.2f", newRate)
        }
    }
    fun computeOneYearEstimate(amountStr: String, type: String, bank: String?): Pair<Double, Double>? {
        val amt = amountStr.replace(",", "").toDoubleOrNull() ?: return null
        if (amt <= 0.0) return null
        val resolvedRate: Double? = if (type == "FD") {
            findFdRate1Y(bank) ?: avgRates["FD"]
        } else {
            avgRates[type]
        }
        val rate = resolvedRate ?: return null
        if (rate <= 0.0) return null
        val projected = amt * (1 + rate / 100.0)
        return projected to rate
    }

    // Preserve cursor while applying Indian grouping formatting
    fun formatIndianNumberTF(input: TextFieldValue): TextFieldValue {
        val formatted = formatIndianNumber(input.text)
        return TextFieldValue(formatted, selection = TextRange(formatted.length))
    }

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompact = cfg.screenWidthDp < 600

    Scaffold(
        snackbarHost = { 
            Box(modifier = Modifier.imePadding()) {
                ModernSnackbarHost(
                    hostState = snackbar,
                    bottomPadding = 88.dp // Position above FAB
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (items.isNotEmpty()) {
                    TotalsBar(items)
                }
                androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Appodeal banner ad — no padding so it fills edge-to-edge
                        val activity = com.example.wealthtracker.util.LocalActivity.current
                        if (activity != null) AppodealBanner(
                            activity = activity,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                        Text(
                            "Private • No cloud sync • Offline",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.money_transfer))
                        val iconSize = if (isCompact) 40.dp else 56.dp
                        Box(modifier = Modifier.height(iconSize).width(iconSize)) {
                            if (comp != null) {
                                LottieAnimation(
                                    composition = comp,
                                    iterations = Int.MAX_VALUE,
                                    isPlaying = true,
                                    speed = 1.2f
                                )
                            }
                        }
                        Column {
                            Text(stringResource(R.string.title_investments), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.subtitle_investments), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    val ctx = LocalContext.current
                    var exportMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
                    }
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Export")
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Share CSV") },
                            onClick = {
                                exportMenu = false
                                val f = createCsvFile(ctx, items)
                                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(intent, "Share CSV").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(chooser)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            onClick = {
                                exportMenu = false
                                val f = createPdfFile(ctx, items)
                                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(intent, "Share PDF").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(chooser)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            // Speed dial: quick access actions
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                        ExtendedFloatingActionButton(
                            onClick = { onOpenDashboard(); expanded = false },
                            icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                            text = { Text(stringResource(id = R.string.title_dashboard)) }
                        )
                        ExtendedFloatingActionButton(
                            onClick = { onOpenCalculators(); expanded = false },
                            icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                            text = { Text("Calculators") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = { onOpenStocks(); expanded = false },
                            icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                            text = { Text("Stock Analysis") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { expanded = !expanded },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        }
    ) { inner ->
        // Snackbar bridge
        LaunchedEffect(message) {
            message?.let {
                snackbar.showSnackbar(it)
                viewModel.consumeMessage()
            }
        }
        // Highlight logic: after a successful add, highlight the top item
        val listState = rememberLazyListState()
        LaunchedEffect(items.size, pendingAddHighlight) {
            if (pendingAddHighlight && items.isNotEmpty()) {
                listState.animateScrollToItem(0)
                delay(50)
                highlightId = items.first().id
                pendingAddHighlight = false
                showCelebration = true
            }
        }
        // Celebration visibility will be controlled by Lottie playback completion
        Box(modifier = Modifier.fillMaxSize()) {
            val contentModifier = if (isCompact) {
                Modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState()).imePadding()
            } else {
                Modifier.fillMaxSize().padding(inner).padding(16.dp)
            }
            val listBring = remember { BringIntoViewRequester() }
            var didBring by remember { mutableStateOf(false) }
            Column(
                modifier = contentModifier
            ) {
            // Visualization moved below the Add section as requested
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { tf -> amount = formatIndianNumberTF(tf) },
                            label = { 
                                Text(
                                    stringResource(R.string.label_amount),
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            placeholder = {
                                val symbol = FormatUtils.getCurrencySymbol()
                                val ph = when (invType) {
                                    "FD" -> "$symbol 1,00,000"
                                    "Mutual Fund" -> "$symbol 50,000"
                                    "Stocks", "Equity" -> "$symbol 25,000"
                                    "Gold" -> "$symbol 10,000"
                                    "PPF", "EPF", "NPS" -> "$symbol 1,50,000"
                                    "Health Insurance", "Term Insurance" -> "$symbol 15,000"
                                    else -> "$symbol 10,000"
                                }
                                Text(
                                    ph,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            isError = amountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp, max = 72.dp) // Increased height for better comma visibility
                        )
                    }
                    if (amountError) {
                        Text(stringResource(R.string.error_enter_valid_amount), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize() // Smooth animation for FD-specific fields
                    ) {
                        Text(stringResource(R.string.label_investment_type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InvestmentTypes.all.forEach { t ->
                                val selectedAdd = invType == t
                                FilterChip(
                                    selected = selectedAdd,
                                    onClick = { invType = t; if (t != "FD") selectedBank = null },
                                    label = { Text(if (t == "FD") stringResource(R.string.fixed_deposit) else if (t == "Equity") "Stocks" else t) },
                                    leadingIcon = {
                                        val icon = when (t) {
                                            "FD" -> Icons.Default.AccountBalance
                                            "Mutual Fund", "NPS", "Equity" -> Icons.AutoMirrored.Filled.TrendingUp
                                            "Gold" -> Icons.Default.Savings
                                            "PPF", "EPF" -> Icons.Default.AccountBalance
                                            "Term Insurance", "Health Insurance" -> Icons.Default.Savings
                                            else -> Icons.Default.Savings
                                        }
                                        Icon(icon, contentDescription = null)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        if (invType == "Others") {
                            OutlinedTextField(
                                value = othersName,
                                onValueChange = { othersName = it },
                                label = { Text(stringResource(R.string.name_investment)) },
                                placeholder = { Text(stringResource(R.string.hint_investment_name)) },
                                isError = othersNameError,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 56.dp)
                            )
                            if (othersNameError) {
                                Text(
                                    stringResource(R.string.error_enter_name_others),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // One-year estimate panel (above CTA)
                    val estimate = computeOneYearEstimate(amount.text, invType, selectedBank)
                    if (estimate != null) {
                        val (proj, rate) = estimate
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("1-year estimate", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${FormatUtils.formatINR(proj)} at ${String.format(Locale.ENGLISH, "%.2f", rate)}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val sub = if (invType == "FD" && selectedBank != null) "FD · $selectedBank" else invType
                                if (!sub.isNullOrBlank()) {
                                    Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    AnimatedVisibility(visible = invType == "FD") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val clickSrc = remember { MutableInteractionSource() }
                            OutlinedTextField(
                                value = selectedBank ?: "Select Bank",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_bank)) },
                                isError = bankError,
                                readOnly = true,
                                trailingIcon = { 
                                    IconButton(onClick = { bankExpanded = true }) { 
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null) 
                                    } 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(indication = null, interactionSource = clickSrc) { bankExpanded = true }
                            )
                            // Show custom bank name field when Others is selected
                            AnimatedVisibility(visible = selectedBank == "Others") {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = customBankName,
                                        onValueChange = { customBankName = it },
                                        label = { Text("Enter Bank Name") },
                                        placeholder = { Text("e.g., Chase Bank, HSBC, etc.") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "FD") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = fdStartDate,
                                onValueChange = {},
                                label = { Text("Start Date") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showFdStartPicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showFdStartPicker = true }
                            )
                            Spacer(Modifier.height(8.dp))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = fdMaturityDate,
                                onValueChange = {},
                                label = { Text("Maturity Date") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showFdMaturityPicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showFdMaturityPicker = true }
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = fdRate,
                                onValueChange = { fdRate = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Interest Rate (%)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showFdStartPicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showFdStartPicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            fdStartDate = sdf.format(Date(millis))
                                        }
                                        showFdStartPicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showFdStartPicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                            if (showFdMaturityPicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showFdMaturityPicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            fdMaturityDate = sdf.format(Date(millis))
                                        }
                                        showFdMaturityPicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showFdMaturityPicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "Mutual Fund") {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.zIndex(2f)) {
                                OutlinedTextField(
                                    value = mfName,
                                    onValueChange = {
                                        mfName = it
                                        if (mfCommittedName != null && it.trim() != mfCommittedName!!.trim()) mfCommittedName = null
                                        mfExpanded = mfHasFocus && it.trim().length >= 2
                                    },
                                    label = { Text("Fund Name") },
                                    placeholder = { Text("Fund Name (optional)") },
                                    singleLine = true,
                                    isError = mfNameError,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { f -> mfHasFocus = f.isFocused }
                                )
                            }
                            
                            if (mfExpanded && mfSuggest.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(2f)
                                ) {
                                    Column {
                                        mfSuggest.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                onClick = {
                                                    mfName = name
                                                    mfCommittedName = name
                                                    mfExpanded = false
                                                    mfSuggest = emptyList()
                                                    mfJustSelected = true
                                                    mfHasFocus = false
                                                    focusManager.clearFocus()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = mfDate,
                                onValueChange = {},
                                label = { Text("Date (optional)") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showMfDatePicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showMfDatePicker = true }
                            )
                            if (showMfDatePicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showMfDatePicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            mfDate = sdf.format(Date(millis))
                                        }
                                        showMfDatePicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showMfDatePicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "Gold") {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            var goldMenu by remember { mutableStateOf(false) }
                            OutlinedTextField(value = goldType ?: "Select Type", onValueChange = {}, label = { Text("Type") }, readOnly = true, trailingIcon = { IconButton(onClick = { goldMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } }, modifier = Modifier.fillMaxWidth().clickable { goldMenu = true })
                            DropdownMenu(expanded = goldMenu, onDismissRequest = { goldMenu = false }) {
                                listOf("24K Coins/Bars", "22K Jewellery", "Sovereign Gold Bond", "Gold ETF", "Digital Gold", "Gold Mutual Fund", "Others").forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { goldType = t; goldMenu = false }) }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = goldDate,
                                onValueChange = {},
                                label = { Text("Date (optional)") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showGoldDatePicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showGoldDatePicker = true }
                            )
                            if (showGoldDatePicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showGoldDatePicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            goldDate = sdf.format(Date(millis))
                                        }
                                        showGoldDatePicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showGoldDatePicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "PPF") {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            var fyMenu by remember { mutableStateOf(false) }
                            OutlinedTextField(value = ppfFy ?: "Select FY", onValueChange = {}, label = { Text("FY") }, readOnly = true, trailingIcon = { IconButton(onClick = { fyMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } }, modifier = Modifier.fillMaxWidth().clickable { fyMenu = true })
                            DropdownMenu(expanded = fyMenu, onDismissRequest = { fyMenu = false }) {
                                listOf("FY 2023-24", "FY 2024-25", "FY 2025-26").forEach { f -> DropdownMenuItem(text = { Text(f) }, onClick = { ppfFy = f; fyMenu = false }) }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = ppfDate,
                                onValueChange = {},
                                label = { Text("Date (optional)") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showPpfDatePicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showPpfDatePicker = true }
                            )
                            if (showPpfDatePicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showPpfDatePicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            ppfDate = sdf.format(Date(millis))
                                        }
                                        showPpfDatePicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showPpfDatePicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "NPS") {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            var tierMenu by remember { mutableStateOf(false) }
                            OutlinedTextField(value = npsTier ?: "Select Tier", onValueChange = {}, label = { Text("Tier") }, readOnly = true, trailingIcon = { IconButton(onClick = { tierMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } }, modifier = Modifier.fillMaxWidth().clickable { tierMenu = true })
                            DropdownMenu(expanded = tierMenu, onDismissRequest = { tierMenu = false }) {
                                listOf("Tier I (retirement account)", "Tier II (optional savings)", "Others").forEach { t ->
                                    DropdownMenuItem(text = { Text(t) }, onClick = { npsTier = t; tierMenu = false })
                                }
                            }
                            if (!npsTier.isNullOrBlank()) {
                                val info = if (npsTier!!.startsWith("Tier I")) "Long-term retirement account with withdrawal restrictions." else if (npsTier!!.startsWith("Tier II")) "Flexible savings account with free withdrawals." else "Other plan type."
                                Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = npsDate,
                                onValueChange = {},
                                label = { Text("Date (optional)") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showNpsDatePicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth().clickable { showNpsDatePicker = true }
                            )
                            if (showNpsDatePicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showNpsDatePicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            npsDate = sdf.format(Date(millis))
                                        }
                                        showNpsDatePicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showNpsDatePicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "Health Insurance") {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = hiPolicyName, onValueChange = { hiPolicyName = it }, label = { Text("Policy Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = hiRenewalDate,
                                onValueChange = {},
                                label = { Text("Renewal Date") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showHiRenewalPicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showHiRenewalPicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showHiRenewalPicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            hiRenewalDate = sdf.format(Date(millis))
                                        }
                                        showHiRenewalPicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showHiRenewalPicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = invType == "Equity") {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.zIndex(2f)) {
                                OutlinedTextField(
                                    value = stockName,
                                    onValueChange = {
                                        stockName = it
                                        if (stockCommittedName != null && it.trim() != stockCommittedName!!.trim()) stockCommittedName = null
                                        stockExpanded = stockHasFocus && it.trim().length >= 2
                                    },
                                    label = { Text("Stock Name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { f -> stockHasFocus = f.isFocused }
                                )
                            }
                            if (stockExpanded && stockSuggest.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(2f)
                                ) {
                                    Column {
                                        stockSuggest.forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(s, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                onClick = {
                                                    stockName = s
                                                    stockCommittedName = s
                                                    stockExpanded = false
                                                    stockSuggest = emptyList()
                                                    stockJustSelected = true
                                                    stockHasFocus = false
                                                    focusManager.clearFocus()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = stockDate,
                                onValueChange = {},
                                label = { Text("Date (optional)") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { showStockDatePicker = true }) { Icon(Icons.Filled.DateRange, contentDescription = null) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (showStockDatePicker) {
                                val dpState = rememberDatePickerState()
                                DatePickerDialog(onDismissRequest = { showStockDatePicker = false }, confirmButton = {
                                    TextButton(onClick = {
                                        dpState.selectedDateMillis?.let { millis ->
                                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                            stockDate = sdf.format(Date(millis))
                                        }
                                        showStockDatePicker = false
                                    }) { Text("OK") }
                                }, dismissButton = { TextButton(onClick = { showStockDatePicker = false }) { Text("Cancel") } }) {
                                    DatePicker(state = dpState)
                                }
                            }
                        }
                    }
                    if (invType == "FD" && bankError) {
                        Text(stringResource(R.string.error_select_bank_fd), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(onClick = {
                            attemptedAdd = true
                            focusManager.clearFocus()
                            keyboard?.hide()
                            val amountValue = amount.text.replace(",", "").toDoubleOrNull()
                            val canAdd = amountValue != null && amountValue > 0.0 &&
                                !(invType == "FD" && selectedBank == null) &&
                                !(invType == "Others" && othersName.isBlank())
                            if (canAdd) {
                                val displayType = when (invType) {
                                    "Others" -> othersName
                                    "Mutual Fund" -> mfName.ifBlank { "Mutual Fund" }
                                    "Equity" -> stockName.ifBlank { "Stocks" }
                                    else -> invType
                                }
                                // Determine final bank name (use custom if Others selected)
                                val finalBankName = if (selectedBank == "Others" && customBankName.isNotBlank()) {
                                    customBankName
                                } else {
                                    selectedBank
                                }
                                val entity = com.example.wealthtracker.data.local.InvestmentEntity(
                                    type = displayType,
                                    amount = amountValue!!,
                                    investmentType = if (invType == "Equity" || invType == "Stocks") "Stocks" else invType,
                                    bankName = if (invType == "FD") finalBankName else null,
                                    fdStartDate = if (invType == "FD") fdStartDate.takeIf { it.isNotBlank() } else null,
                                    fdMaturityDate = if (invType == "FD") fdMaturityDate.takeIf { it.isNotBlank() } else null,
                                    fdRate = if (invType == "FD") fdRate.toDoubleOrNull() else null,
                                    fdTenure = null,
                                    mfDate = if (invType == "Mutual Fund") mfDate.takeIf { it.isNotBlank() } else null,
                                    ppfFy = if (invType == "PPF") ppfFy else null,
                                    ppfDate = if (invType == "PPF") ppfDate.takeIf { it.isNotBlank() } else null,
                                    npsTier = if (invType == "NPS") npsTier else null,
                                    npsDate = if (invType == "NPS") npsDate.takeIf { it.isNotBlank() } else null,
                                    goldType = if (invType == "Gold") goldType else null,
                                    goldDate = if (invType == "Gold") goldDate.takeIf { it.isNotBlank() } else null,
                                    hiPolicyName = if (invType == "Health Insurance") hiPolicyName.takeIf { it.isNotBlank() } else null,
                                    hiRenewalDate = if (invType == "Health Insurance") hiRenewalDate.takeIf { it.isNotBlank() } else null,
                                    stockName = if (invType == "Equity" || invType == "Stocks") stockName.takeIf { it.isNotBlank() } else null,
                                    stockDate = if (invType == "Equity" || invType == "Stocks") stockDate.takeIf { it.isNotBlank() } else null
                                )
                                viewModel.addInvestmentFull(entity)
                                // Clear inputs
                                amount = TextFieldValue("")
                                selectedBank = null
                                customBankName = ""
                                fdStartDate = ""; fdMaturityDate = ""; fdRate = ""
                                mfName = ""; mfDate = ""
                                ppfFy = null; ppfDate = ""
                                npsTier = null; npsDate = ""
                                goldType = null; goldDate = ""
                                hiPolicyName = ""; hiRenewalDate = ""
                                stockName = ""; stockDate = ""; stockSuggest = emptyList(); stockExpanded = false
                                attemptedAdd = false
                                pendingAddHighlight = true
                                showCelebration = true
                                viewModel.setTypeFilter(invType)
                            }
                        }) { Text(stringResource(id = R.string.btn_add_investment), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = allItems.groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.mapValues { it.value.size }
                val dynamicTypes = counts.keys.sorted()
                val filters = listOf("All") + dynamicTypes
                filters.forEach { key ->
                    val selected = (typeFilter ?: "All") == key
                    val count = if (key == "All") allItems.size else counts[key] ?: 0
                    val display = when (key) {
                        "FD" -> "Fixed Deposit"
                        "Equity" -> "Stocks"
                        else -> key
                    }
                    val icon = when (key) {
                        "All" -> Icons.Default.Savings
                        "FD" -> Icons.Default.AccountBalance
                        "Mutual Fund", "NPS", "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp
                        "Gold" -> Icons.Default.Savings
                        "PPF", "EPF" -> Icons.Default.AccountBalance
                        "Term Insurance", "Health Insurance" -> Icons.Default.Savings
                        else -> Icons.Default.MoreVert
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setTypeFilter(if (key == "All") null else key) },
                        label = { Text("$display (${FormatUtils.formatInt(count)})") },
                        leadingIcon = { Icon(icon, contentDescription = null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.section_your_investments), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                var confirmDeleteAll by remember { mutableStateOf(false) }
                if (items.isNotEmpty()) {
                    TextButton(onClick = { confirmDeleteAll = true }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                    if (confirmDeleteAll) {
                        AlertDialog(
                            onDismissRequest = { confirmDeleteAll = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    confirmDeleteAll = false
                                    // If filtered list equals all items, delete everything; otherwise delete only filtered ids
                                    val currentIds = items.map { it.id }
                                    val allIds = allItems.map { it.id }
                                    if (currentIds.size == allIds.size) {
                                        viewModel.deleteAll()
                                    } else {
                                        viewModel.deleteByIds(currentIds)
                                    }
                                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
                            text = { Text("Delete all investments? This action cannot be undone.") }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.empty_no_investments), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                if (isCompact) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp).bringIntoViewRequester(listBring)) {
                        items.forEach { item ->
                            Box(modifier = Modifier) {
                                InvestmentRow(
                                    entity = item,
                                    onDelete = {
                                        val toDelete = item
                                        viewModel.delete(toDelete)
                                        lastDeleted = toDelete
                                        uiScope.launch {
                                            val res = snackbar.showSnackbar(
                                                message = ctx.getString(R.string.snackbar_deleted),
                                                actionLabel = ctx.getString(R.string.action_undo),
                                                withDismissAction = true,
                                                duration = androidx.compose.material3.SnackbarDuration.Short
                                            )
                                            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                lastDeleted?.let { viewModel.reAdd(it) }
                                                lastDeleted = null
                                            } else {
                                                snackbar.currentSnackbarData?.dismiss()
                                            }
                                        }
                                    },
                                    onEdit = {
                                        editing = item
                                        showEditBottomSheet = true
                                    },
                                    highlight = (highlightId == item.id),
                                    onHighlightConsumed = { if (highlightId == item.id) highlightId = null }
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        state = listState
                    ) {
                        items(items, key = { item -> item.id }) { item ->
                            Box(modifier = Modifier) {
                                InvestmentRow(
                                    entity = item,
                                    onDelete = {
                                        val toDelete = item
                                        viewModel.delete(toDelete)
                                        lastDeleted = toDelete
                                        uiScope.launch {
                                            val res = snackbar.showSnackbar(
                                                message = ctx.getString(R.string.snackbar_deleted),
                                                actionLabel = ctx.getString(R.string.action_undo),
                                                withDismissAction = true,
                                                duration = androidx.compose.material3.SnackbarDuration.Short
                                            )
                                            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                lastDeleted?.let { viewModel.reAdd(it) }
                                                lastDeleted = null
                                            } else {
                                                snackbar.currentSnackbarData?.dismiss()
                                            }
                                        }
                                    },
                                    onEdit = {
                                        editing = item
                                        showEditBottomSheet = true
                                    },
                                    highlight = (highlightId == item.id),
                                    onHighlightConsumed = { if (highlightId == item.id) highlightId = null }
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            // Removed legacy export/import dialogs; sharing now generates files directly
        }
            // Celebration overlay (does not affect layout)
            if (showCelebration) {
                val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
                if (comp != null) {
                    val animState = com.airbnb.lottie.compose.animateLottieCompositionAsState(
                        composition = comp,
                        iterations = 1,
                        isPlaying = true,
                        restartOnPlay = true,
                        speed = 2f
                    )
                    LaunchedEffect(animState.progress) {
                        if (animState.progress >= 0.99f) showCelebration = false
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = 0.95f }
                            .zIndex(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LottieAnimation(
                            composition = comp,
                            progress = { animState.progress },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            
            // Scrim for FAB menu - inside content so FAB renders on top
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            expanded = false
                        }
                )
            }
        }
    }
    
    // Modern Edit Bottom Sheet
    if (showEditBottomSheet && editing != null) {
        EditInvestmentBottomSheet(
            investment = editing!!,
            onDismiss = { 
                showEditBottomSheet = false
                editing = null
            },
            onSave = { updatedInvestment ->
                viewModel.updateInvestment(updatedInvestment)
                showEditBottomSheet = false
                editing = null
                uiScope.launch {
                    snackbar.showSuccessSnackbar("Investment updated successfully")
                }
            },
            onDelete = {
                val toDelete = editing!!
                viewModel.delete(toDelete)
                showEditBottomSheet = false
                editing = null
                uiScope.launch {
                    snackbar.showDeleteSnackbar(
                        itemName = toDelete.investmentType,
                        onUndo = { viewModel.reAdd(toDelete) }
                    )
                }
            }
        )
    }
    
    // Bank Selection Bottom Sheet
    if (bankExpanded) {
        ModalBottomSheet(
            onDismissRequest = { bankExpanded = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Select Bank",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val banksFromCsv = fdRates.map { it.bank }.distinct().sorted()
                    items(banksFromCsv) { bank ->
                        ListItem(
                            headlineContent = { Text(bank) },
                            leadingContent = { BankAvatar(bank) },
                            modifier = Modifier.clickable {
                                selectedBank = bank
                                bankExpanded = false
                            }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_others)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedBank = "Others"
                                bankExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualizationDialog(
    items: List<InvestmentEntity>,
    filter: String?,
    vizType: String,
    onTypeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val data: List<Pair<String, Double>> = remember(items, filter) {
        val grouped = if (filter == "FD") {
            items.groupBy { it.bankName ?: "Others" }
        } else if (filter == null || filter == "All") {
            items.groupBy { it.investmentType }
        } else {
            items.groupBy { it.investmentType }
        }
        grouped.map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
    }
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onTypeChange("Pie") }, enabled = vizType != "Pie") { Text("Pie") }
                TextButton(onClick = { onTypeChange("Bar") }, enabled = vizType != "Bar") { Text("Bar") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = { Text("Visualization") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                )
                if (vizType == "Pie") {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        var startAngle = -90f
                        data.forEachIndexed { idx, (_, value) ->
                            val sweep = ((value / total) * 360f).toFloat()
                            drawArc(
                                color = colors[idx % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true
                            )
                            startAngle += sweep
                        }
                    }
                } else {
                    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        val barCount = data.size.coerceAtLeast(1)
                        val space = 12f
                        val barWidth = (size.width - space * (barCount + 1)) / barCount
                        data.forEachIndexed { idx, (_, v) ->
                            val h = (v / maxVal * size.height.toDouble()).toFloat()
                            val left = space + idx * (barWidth + space)
                            drawRect(
                                color = colors[idx % colors.size],
                                topLeft = androidx.compose.ui.geometry.Offset(left, size.height - h),
                                size = androidx.compose.ui.geometry.Size(barWidth, h)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.take(8).forEachIndexed { idx, (label, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(14.dp).height(14.dp)) {
                                drawRect(color = colors[idx % colors.size])
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("$label — ${FormatUtils.formatINR(value)}")
                        }
                    }
                    if (data.size > 8) {
                        Text("+${data.size - 8} more...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    )
}

// CSV generation and sharing helpers
private fun createCsvFile(context: android.content.Context, items: List<InvestmentEntity>): File {
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(cacheDir, "investments.csv")
    val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()
    val (header, rows) = when (singleType) {
        "FD" -> {
            val h = "S.No,Bank,Rate,Maturity,Amount\n"
            val r = items.mapIndexed { idx, e ->
                listOf(
                    idx + 1,
                    e.bankName ?: "FD",
                    e.fdRate?.let { String.format(java.util.Locale.ENGLISH, "%.2f%%", it) } ?: "",
                    e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt),
                    FormatUtils.formatINR(e.amount)
                ).joinToString(",") { s -> val str = s.toString(); if (str.contains(",")) '"' + str.replace("\"", "\"\"") + '"' else str }
            }
            h to r
        }
        "Health Insurance" -> {
            val h = "S.No,Policy,Renewal,Amount\n"
            val r = items.mapIndexed { idx, e ->
                listOf(idx + 1, e.hiPolicyName ?: "", e.hiRenewalDate ?: formatAddedOn(e.createdAt), FormatUtils.formatINR(e.amount))
                    .joinToString(",") { s -> val str = s.toString(); if (str.contains(",")) '"' + str.replace("\"", "\"\"") + '"' else str }
            }
            h to r
        }
        "Stocks" -> {
            val h = "S.No,Stock,Date,Amount\n"
            val r = items.mapIndexed { idx, e ->
                listOf(idx + 1, (e.stockName ?: e.type).ifBlank { "Stocks" }, e.stockDate ?: formatAddedOn(e.createdAt), FormatUtils.formatINR(e.amount))
                    .joinToString(",") { s -> val str = s.toString(); if (str.contains(",")) '"' + str.replace("\"", "\"\"") + '"' else str }
            }
            h to r
        }
        else -> {
            val h = "S.No,Type,Name,Date,Amount\n"
            val r = items.mapIndexed { idx, e ->
                val (name, dateStr) = when (e.investmentType) {
                    "FD" -> ((e.bankName ?: "Fixed Deposit") to (e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "Health Insurance" -> ((e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Insurance") to (e.hiRenewalDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "Mutual Fund" -> (e.type.ifBlank { "Mutual Fund" } to (e.mfDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "Gold" -> ((e.goldType?.takeIf { it.isNotBlank() } ?: "Gold") to (e.goldDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "PPF" -> ((e.ppfFy?.takeIf { it.isNotBlank() } ?: "PPF") to (e.ppfDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "NPS" -> ((e.npsTier?.takeIf { it.isNotBlank() } ?: "NPS") to (e.npsDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    "Equity", "Stocks" -> (((e.stockName?.takeIf { it.isNotBlank() } ?: e.type).ifBlank { "Stocks" }) to (e.stockDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)))
                    else -> (e.type.ifBlank { e.investmentType } to formatAddedOn(e.createdAt))
                }
                listOf(idx + 1, e.investmentType, name, dateStr, FormatUtils.formatINR(e.amount)).joinToString(",") { s ->
                    val str = s.toString(); if (str.contains(",")) '"' + str.replace("\"", "\"\"") + '"' else str
                }
            }
            h to r
        }
    }
    val body = rows.joinToString("\n")
    val total = items.sumOf { it.amount }
    val footerCols = header.substringBefore('\n').split(',').size
    val totalRow = buildList {
        repeat(footerCols - 2) { add("") }
        add("Total")
        add(FormatUtils.formatINR(total))
    }.joinToString(",") { v -> val s = v.toString(); if (s.contains(",")) '"' + s.replace("\"", "\"\"") + '"' else s }
    file.writeText(header + body + "\n" + totalRow)
    return file
}

private fun createPdfFile(context: android.content.Context, items: List<InvestmentEntity>): File {
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(cacheDir, "investments.pdf")
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    fun startPage(pageNumber: Int): Triple<PdfDocument.Page, android.graphics.Canvas, FloatArray> {
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()
        // Dynamic columns based on filter/type
        val colX = when (singleType) {
            "FD" -> floatArrayOf(40f, 60f, 250f, 380f, 480f, 555f) // #, Bank, Rate, Maturity, Amount, right edge
            "Health Insurance" -> floatArrayOf(40f, 60f, 360f, 460f, 520f, 555f) // #, Policy, Renewal, Amount, right edge
            "Stocks" -> floatArrayOf(40f, 60f, 360f, 460f, 520f, 555f) // #, Stock, Date, Amount, right edge
            else -> floatArrayOf(40f, 60f, 210f, 380f, 480f, 555f) // #, Type, Name, Date, Amount, right edge
        }
        return Triple(page, canvas, colX)
    }

    val boldTf = ResourcesCompat.getFont(context, R.font.montserrat_bold) ?: Typeface.DEFAULT_BOLD
    val regTf = ResourcesCompat.getFont(context, R.font.montserrat_regular) ?: Typeface.DEFAULT
    val titlePaint = Paint().apply {
        color = GColor.parseColor("#2E7D32")
        textSize = 20f
        typeface = boldTf
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        color = GColor.BLACK
        textSize = 12f
        typeface = boldTf
        isAntiAlias = true
    }
    val textPaint = Paint().apply {
        color = GColor.BLACK
        textSize = 12f
        typeface = regTf
        isAntiAlias = true
    }
    fun Drawable.toBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        setBounds(0, 0, c.width, c.height)
        draw(c)
        return bmp
    }

    val appIconDrawable: Drawable? = try { context.packageManager.getApplicationIcon(context.packageName) } catch (_: Exception) { null }
    val appIconBitmap: Bitmap? = appIconDrawable?.toBitmap()

    var pageNum = 1
    var (page, canvas, colX) = startPage(pageNum)

    fun drawHeader(): Float {
        // small icon snapshot at top-right
        appIconBitmap?.let { bmp ->
            val size = 32
            val left = pageW - 40 - size
            val top = 24
            val rect = android.graphics.Rect(0,0,bmp.width,bmp.height)
            val dst = android.graphics.Rect(left, top, left+size, top+size)
            canvas.drawBitmap(bmp, rect, dst, null)
        }
        var y = 60f
        canvas.drawText("TrackKaro - Investments", 40f, y, titlePaint)
        y += 28f
        val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()
        canvas.drawText("#", colX[0], y, headerPaint)
        when (singleType) {
            "FD" -> {
                canvas.drawText("Bank", colX[1], y, headerPaint)
                canvas.drawText("Rate", colX[2], y, headerPaint)
                canvas.drawText("Maturity", colX[3], y, headerPaint)
                canvas.drawText("Amount", colX[4], y, headerPaint)
            }
            "Health Insurance" -> {
                canvas.drawText("Policy", colX[1], y, headerPaint)
                canvas.drawText("Renewal", colX[2], y, headerPaint)
                canvas.drawText("Amount", colX[3], y, headerPaint)
            }
            "Stocks" -> {
                canvas.drawText("Stock", colX[1], y, headerPaint)
                canvas.drawText("Date", colX[2], y, headerPaint)
                canvas.drawText("Amount", colX[3], y, headerPaint)
            }
            else -> {
                canvas.drawText("Type", colX[1], y, headerPaint)
                canvas.drawText("Name", colX[2], y, headerPaint)
                canvas.drawText("Date", colX[3], y, headerPaint)
                canvas.drawText("Amount", colX[4], y, headerPaint)
            }
        }
        y += 12f
        canvas.drawLine(colX[0], y, colX[5], y, textPaint)
        y += 16f
        return y
    }

    var y = drawHeader()

    fun drawTextWrapped(s: String, x: Float, yStart: Float, maxW: Float, paint: Paint): Float {
        if (s.isBlank()) { canvas.drawText("", x, yStart, paint); return 0f }
        val words = s.split(' ')
        var line = StringBuilder()
        var y = yStart
        var used = 0f
        for (w in words) {
            val test = if (line.isEmpty()) w else line.toString() + " " + w
            val width = paint.measureText(test)
            if (width <= maxW) {
                line = StringBuilder(test)
            } else {
                canvas.drawText(line.toString(), x, y, paint)
                y += 14f
                used += 14f
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
        }
        return used
    }

    items.forEachIndexed { idx, e ->
        if (y > pageH - 60f) { // leave more bottom margin
            doc.finishPage(page)
            pageNum += 1
            val triple = startPage(pageNum)
            page = triple.first
            canvas = triple.second
            colX = triple.third
            y = drawHeader()
        }
        val name = when (e.investmentType) {
            "FD" -> e.bankName ?: "Fixed Deposit"
            "Health Insurance" -> e.hiPolicyName?.takeIf { it.isNotBlank() } ?: "Insurance"
            "Mutual Fund" -> e.type.ifBlank { "Mutual Fund" }
            "Gold" -> e.goldType?.takeIf { it.isNotBlank() } ?: "Gold"
            "PPF" -> e.ppfFy?.takeIf { it.isNotBlank() } ?: "PPF"
            "NPS" -> e.npsTier?.takeIf { it.isNotBlank() } ?: "NPS"
            "Equity", "Stocks" -> (e.stockName?.takeIf { it.isNotBlank() } ?: e.type).ifBlank { "Stocks" }
            else -> e.type.ifBlank { e.investmentType }
        }
        val dateStr = when (e.investmentType) {
            "FD" -> e.fdMaturityDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "Health Insurance" -> e.hiRenewalDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "Mutual Fund" -> e.mfDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "Gold" -> e.goldDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "PPF" -> e.ppfDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "NPS" -> e.npsDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            "Equity", "Stocks" -> e.stockDate?.takeIf { it.isNotBlank() } ?: formatAddedOn(e.createdAt)
            else -> formatAddedOn(e.createdAt)
        }
        canvas.drawText((idx + 1).toString(), colX[0], y, textPaint)
        val singleType = items.map { if (it.investmentType == "Equity") "Stocks" else it.investmentType }.distinct().singleOrNull()
        when (singleType) {
            "FD" -> {
                canvas.drawText(name, colX[1], y, textPaint)
                canvas.drawText(e.fdRate?.let { String.format(java.util.Locale.ENGLISH, "%.2f%%", it) } ?: "", colX[2], y, textPaint)
                canvas.drawText(dateStr, colX[3], y, textPaint)
                canvas.drawText(FormatUtils.formatINR(e.amount), colX[4], y, textPaint)
            }
            "Health Insurance" -> {
                // wrap policy name if long
                val used = drawTextWrapped(name, colX[1], y, (colX[2] - colX[1] - 6f), textPaint)
                canvas.drawText(dateStr, colX[2], y, textPaint)
                canvas.drawText(FormatUtils.formatINR(e.amount), colX[3], y, textPaint)
                if (used > 0f) y += used
            }
            "Stocks" -> {
                val used = drawTextWrapped(name, colX[1], y, (colX[2] - colX[1] - 6f), textPaint)
                canvas.drawText(dateStr, colX[2], y, textPaint)
                canvas.drawText(FormatUtils.formatINR(e.amount), colX[3], y, textPaint)
                if (used > 0f) y += used
            }
            else -> {
                canvas.drawText(e.investmentType, colX[1], y, textPaint)
                val used = drawTextWrapped(name, colX[2], y, (colX[3] - colX[2] - 6f), textPaint)
                canvas.drawText(dateStr, colX[3], y, textPaint)
                canvas.drawText(FormatUtils.formatINR(e.amount), colX[4], y, textPaint)
                if (used > 0f) y += used
            }
        }
        // increase row height and draw separator below text to avoid overlap
        val sepY = y + 6f
        canvas.drawLine(colX[0], sepY, colX[5], sepY, textPaint)
        y += 20f
    }

    // Totals only on the last page
    val total = items.sumOf { it.amount }
    if (y > pageH - 60f) {
        doc.finishPage(page)
        pageNum += 1
        val triple = startPage(pageNum)
        page = triple.first
        canvas = triple.second
        colX = triple.third
        y = drawHeader()
    }
    canvas.drawLine(colX[0], y, colX[5], y, textPaint)
    y += 18f
    val bold = Paint(textPaint).apply { typeface = boldTf }
    canvas.drawText("Total", colX[1], y, bold)
    canvas.drawText(FormatUtils.formatINR(total), colX[4], y, bold)
    y += 12f
    canvas.drawLine(colX[0], y, colX[5], y, textPaint)

    doc.finishPage(page)
    FileOutputStream(file).use { out -> doc.writeTo(out) }
    doc.close()
    return file
}

@Composable
private fun InvestmentRow(
    entity: InvestmentEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    highlight: Boolean,
    onHighlightConsumed: () -> Unit
) {
    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(highlight) {
        if (highlight) {
            flashing = true
            kotlinx.coroutines.delay(900)
            flashing = false
            onHighlightConsumed()
        }
    }
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (flashing) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        animationSpec = tween(durationMillis = 700), label = "row_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon per type
        val leadIcon = when (entity.investmentType) {
            "FD" -> Icons.Default.AccountBalance
            "Mutual Fund" -> Icons.AutoMirrored.Filled.TrendingUp
            "PPF", "EPF" -> Icons.Default.AccountBalance
            "NPS" -> Icons.Default.AccountBalance
            "Gold" -> Icons.Default.Savings
            "Health Insurance", "Term Insurance" -> Icons.Default.Savings
            "Equity", "Stocks" -> Icons.AutoMirrored.Filled.TrendingUp
            else -> Icons.Default.Savings
        }
        Icon(leadIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        // Left: optional check then type/name and optional bank
        // No icon during highlight; only a single fade background
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (entity.investmentType == "FD") "Fixed Deposit" else if (entity.investmentType == "Equity") "Stocks" else if (entity.investmentType == "Stocks") "Stocks" else entity.investmentType,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val details = mutableListOf<String>()
            when (entity.investmentType) {
                "FD" -> {
                    if (!entity.bankName.isNullOrBlank()) details += entity.bankName!!
                    val fdBits = listOfNotNull(
                        entity.fdRate?.let { "${String.format(Locale.ENGLISH, "%.2f", it)}%" },
                        entity.fdStartDate?.takeIf { it.isNotBlank() }?.let { "Start $it" },
                        entity.fdMaturityDate?.takeIf { it.isNotBlank() }?.let { "Maturity $it" }
                    )
                    if (fdBits.isNotEmpty()) details += fdBits.joinToString(" • ")
                }
                "Mutual Fund" -> {
                    if (entity.type.isNotBlank()) details += entity.type
                    if (!entity.mfDate.isNullOrBlank()) details += entity.mfDate
                }
                "PPF" -> {
                    val bits = listOfNotNull(
                        entity.ppfFy?.takeIf { it.isNotBlank() },
                        entity.ppfDate?.takeIf { it.isNotBlank() }?.let { "Date $it" }
                    )
                    if (bits.isNotEmpty()) details += bits.joinToString(" • ")
                }
                "NPS" -> {
                    val bits = listOfNotNull(
                        entity.npsTier?.takeIf { it.isNotBlank() },
                        entity.npsDate?.takeIf { it.isNotBlank() }?.let { "Date $it" }
                    )
                    if (bits.isNotEmpty()) details += bits.joinToString(" • ")
                }
                "Gold" -> {
                    val bits = listOfNotNull(
                        entity.goldType?.takeIf { it.isNotBlank() },
                        entity.goldDate?.takeIf { it.isNotBlank() }?.let { "Date $it" }
                    )
                    if (bits.isNotEmpty()) details += bits.joinToString(" • ")
                }
                "Health Insurance" -> {
                    if (!entity.hiPolicyName.isNullOrBlank()) details += entity.hiPolicyName
                    if (!entity.hiRenewalDate.isNullOrBlank()) details += "Renews ${entity.hiRenewalDate}"
                }
                "Equity", "Stocks" -> {
                    val name = entity.stockName?.takeIf { it.isNotBlank() } ?: entity.type
                    if (name.isNotBlank()) details += name
                    if (!entity.stockDate.isNullOrBlank()) details += entity.stockDate
                }
                "Others" -> {
                    if (entity.type.isNotBlank()) details += entity.type
                }
            }
            details.forEach { line ->
                if (line.isNotBlank()) {
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("Added on ${formatAddedOn(entity.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Right: amount then actions
        Text(
            FormatUtils.formatINRShort(entity.amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuExpanded = true }) { 
                Icon(Icons.Default.MoreVert, contentDescription = "More options") 
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuExpanded = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuExpanded = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun BankAvatar(bank: String) {
    val initials = bank.firstOrNull()?.uppercase() ?: "B"
    val fg = MaterialTheme.colorScheme.primary
    val bg = fg.copy(alpha = 0.18f)
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(28.dp)
            .padding(end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = bg)
        }
        Text(initials, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
