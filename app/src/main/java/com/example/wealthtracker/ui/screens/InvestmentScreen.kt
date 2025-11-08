package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.IndianBanks
import com.example.wealthtracker.util.InvestmentTypes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.ExperimentalComposeUiApi
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
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                AndroidView(
                factory = { context ->
                    if (vizType == "Pie") {
                        val chart = com.github.mikephil.charting.charts.PieChart(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                            description.isEnabled = false
                            legend.isEnabled = false
                            setUsePercentValues(false)
                            setDrawEntryLabels(false)
                            setHoleColor(android.graphics.Color.TRANSPARENT)
                            setTouchEnabled(false)
                            setExtraOffsets(8f, 8f, 8f, 8f)
                            setCenterTextColor(onSurface)
                        }
                        chart
                    } else {
                        val chart = com.github.mikephil.charting.charts.BarChart(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                            description.isEnabled = false
                            legend.isEnabled = false
                            axisRight.isEnabled = false
                            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                            xAxis.setDrawGridLines(false)
                            axisLeft.setDrawGridLines(true)
                            // Axis/label text colors for theme
                            xAxis.textColor = onSurface
                            axisLeft.textColor = onSurface
                            xAxis.setGranularity(1f)
                            xAxis.setLabelRotationAngle(-20f)
                            setFitBars(true)
                            setTouchEnabled(false)
                            setExtraOffsets(8f, 8f, 8f, 8f)
                        }
                        chart
                    }
                },
                update = { view ->
                    if (view is com.github.mikephil.charting.charts.PieChart) {
                        val entries = data.map { (label, value) -> com.github.mikephil.charting.data.PieEntry(value.toFloat(), label) }
                        val set = com.github.mikephil.charting.data.PieDataSet(entries, null).apply {
                            this.colors = colors
                            sliceSpace = 2f
                        }
                        view.data = com.github.mikephil.charting.data.PieData(set).apply { setDrawValues(false) }
                        view.invalidate()
                    } else if (view is com.github.mikephil.charting.charts.BarChart) {
                        val entries = data.mapIndexed { idx, (_, value) -> com.github.mikephil.charting.data.BarEntry(idx.toFloat(), value.toFloat()) }
                        val set = com.github.mikephil.charting.data.BarDataSet(entries, null).apply {
                            this.colors = colors
                        }
                        view.data = com.github.mikephil.charting.data.BarData(set).apply {
                            setDrawValues(true)
                            setValueTextSize(9f)
                            setValueTextColor(onSurface)
                            setBarWidth(0.6f)
                        }
                        // X labels
                        val labels = data.map { it.first }
                        view.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                val i = value.toInt()
                                return if (i in labels.indices) labels[i] else ""
                            }
                        }
                        view.xAxis.setLabelCount(labels.size, false)
                        view.setVisibleXRangeMaximum(6f)
                        view.invalidate()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                )
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
    var editType by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf(TextFieldValue("")) }
    var editInvType by remember { mutableStateOf(if (InvestmentTypes.all.contains("Others")) "Others" else InvestmentTypes.all.first()) }
    var editBank by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf(TextFieldValue("")) }
    var invType by remember { mutableStateOf(if (InvestmentTypes.all.contains("FD")) "FD" else InvestmentTypes.all.first()) }
    var othersName by remember { mutableStateOf("") }
    var bankExpanded by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<String?>(null) }

    var highlightId by remember { mutableStateOf<Long?>(null) }
    var pendingAddHighlight by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    val amountError = amount.text.isNotBlank() && (amount.text.replace(",", "").toDoubleOrNull()?.let { it <= 0.0 } != false)
    var attemptedAdd by remember { mutableStateOf(false) }
    val bankError = attemptedAdd && invType == "FD" && (selectedBank == null)
    val othersNameError = attemptedAdd && invType == "Others" && othersName.isBlank()

    // Overflow menu removed; use dedicated Settings icon in top bar
    var lastDeleted by remember { mutableStateOf<InvestmentEntity?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val uiScope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Preserve cursor while applying Indian grouping formatting
    fun formatIndianNumberTF(input: TextFieldValue): TextFieldValue {
        val formatted = formatIndianNumber(input.text)
        return TextFieldValue(formatted, selection = TextRange(formatted.length))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (items.isNotEmpty()) {
                    TotalsBar(items)
                }
                androidx.compose.material3.Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Private • No cloud sync • Offline",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Box(modifier = Modifier.height(56.dp).width(56.dp)) {
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
                    // Dashboard button with icon + text
                    FilledTonalButton(onClick = onOpenDashboard) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.cta_view_dashboard))
                    }
                    // Quick CTA to calculators
                    IconButton(onClick = onOpenCalculators) { Icon(Icons.Default.Calculate, contentDescription = "Calculators") }
                    // Settings icon
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        // No FAB; primary Add button exists in content
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner).padding(16.dp)
            ) {
            // Visualization moved below the Add section as requested
            // Filter chips row (dynamic based on all data with counts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = allItems.groupBy { it.investmentType }.mapValues { it.value.size }
                val dynamicTypes = counts.keys.sorted()
                val filters = listOf("All") + dynamicTypes
                filters.forEach { key ->
                    val selected = (typeFilter ?: "All") == key
                    val count = if (key == "All") allItems.size else counts[key] ?: 0
                    val display = when (key) {
                        "FD" -> "Fixed Deposit"
                        else -> key
                    }
                    val icon = when (key) {
                        "All" -> Icons.Default.Savings
                        "FD" -> Icons.Default.AccountBalance
                        "Mutual Fund", "NPS", "Equity" -> Icons.AutoMirrored.Filled.TrendingUp
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
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { tf -> amount = formatIndianNumberTF(tf) },
                            label = { Text(stringResource(R.string.label_amount)) },
                            singleLine = true,
                            isError = amountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (amountError) {
                        Text(stringResource(R.string.error_enter_valid_amount), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column {
                        Text(stringResource(R.string.label_investment_type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InvestmentTypes.all.forEach { t ->
                                val selectedAdd = invType == t
                                FilterChip(
                                    selected = selectedAdd,
                                    onClick = { invType = t; if (t != "FD") selectedBank = null },
                                    label = { Text(if (t == "FD") stringResource(R.string.fixed_deposit) else t) },
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
                                    }
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = invType == "Others",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = othersName,
                                    onValueChange = { othersName = it },
                                    label = { Text(stringResource(R.string.name_investment)) },
                                    placeholder = { Text(stringResource(R.string.hint_investment_name)) },
                                    isError = othersNameError,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
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
                        // Close inner Column
                    }
                    Spacer(Modifier.height(12.dp))
                    AnimatedVisibility(visible = invType == "FD") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val clickSrc = remember { MutableInteractionSource() }
                            OutlinedTextField(
                                value = selectedBank ?: "Select Bank",
                                onValueChange = {},
                                label = { Text(stringResource(R.string.label_bank)) },
                                isError = bankError,
                                readOnly = true,
                                trailingIcon = { IconButton(onClick = { bankExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(indication = null, interactionSource = clickSrc) { bankExpanded = true }
                            )
                            DropdownMenu(
                                expanded = bankExpanded,
                                onDismissRequest = { bankExpanded = false },
                                offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
                            ) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_others)) }, onClick = { selectedBank = "Others"; bankExpanded = false })
                                IndianBanks.all.forEach { bank ->
                                    DropdownMenuItem(leadingIcon = { BankAvatar(bank) }, text = { Text(bank) }, onClick = { selectedBank = bank; bankExpanded = false })
                                }
                            }
                        }
                    }
                    if (invType == "FD" && bankError) {
                        Text(stringResource(R.string.error_select_bank_fd), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            attemptedAdd = true
                            focusManager.clearFocus()
                            keyboard?.hide()
                            viewModel.addInvestment(amount.text.replace(",", ""), invType, selectedBank, if (invType == "Others") othersName else null)
                            val amountValue = amount.text.replace(",", "").toDoubleOrNull()
                            val canAdd = amountValue != null && amountValue > 0.0 &&
                                !(invType == "FD" && selectedBank == null) &&
                                !(invType == "Others" && othersName.isBlank())
                            if (canAdd) {
                                amount = TextFieldValue("")
                                selectedBank = null
                                attemptedAdd = false
                                if (invType == "Others") othersName = ""
                                pendingAddHighlight = true
                            }
                        }) { Text(stringResource(R.string.btn_add_investment)) }
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(onClick = { viewModel.deleteAll() }, enabled = items.isNotEmpty()) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_delete_all))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.section_your_investments), style = MaterialTheme.typography.titleMedium)
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    state = listState
                ) {
                    items(items, key = { item -> item.id }) { item ->
                        Box(modifier = Modifier.animateItem()) {
                            InvestmentRow(
                                entity = item,
                                onDelete = {
                                    val toDelete = item
                                    viewModel.delete(toDelete)
                                    lastDeleted = toDelete
                                    uiScope.launch {
                                        val res = snackbar.showSnackbar(ctx.getString(R.string.snackbar_deleted), actionLabel = ctx.getString(R.string.action_undo))
                                        if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            lastDeleted?.let { viewModel.reAdd(it) }
                                            lastDeleted = null
                                        }
                                    }
                                },
                                onEdit = {
                                    editing = item
                                    editType = item.type
                                    val amtStr = java.math.BigDecimal(item.amount).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                                    editAmount = formatIndianNumberTF(TextFieldValue(amtStr))
                                    editInvType = item.investmentType
                                    editBank = item.bankName
                                },
                                highlight = (highlightId == item.id),
                                onHighlightConsumed = { if (highlightId == item.id) highlightId = null }
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            // Edit Dialog
            if (editing != null) {
                var editBankExpanded by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { editing = null },
                    confirmButton = {
                        TextButton(onClick = {
                            editing?.let { current ->
                                viewModel.updateInvestment(current.id, editType, editAmount.text.replace(",", ""), editInvType, editBank)
                                uiScope.launch {
                                    delay(50)
                                    highlightId = current.id
                                }
                            }
                            editing = null
                        }) { Text(stringResource(R.string.action_save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
                    },
                    title = { Text(stringResource(R.string.dlg_title_edit_investment)) },
                    text = {
                        Column {
                            // Type chips in edit
                            Text(stringResource(R.string.label_investment_type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InvestmentTypes.all.forEach { t ->
                                    val selectedEdit = editInvType == t
                                    FilterChip(
                                        selected = selectedEdit,
                                        onClick = { editInvType = t; if (t != "FD") editBank = null },
                                        label = { Text(if (t == "FD") stringResource(R.string.fixed_deposit) else t) },
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
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (editInvType == "Others") {
                                OutlinedTextField(
                                    value = editType,
                                    onValueChange = { editType = it },
                                    label = { Text(stringResource(R.string.name_investment)) },
                                    placeholder = { Text(stringResource(R.string.hint_investment_name)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                            if (editInvType == "FD") {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    val clickSrc2 = remember { MutableInteractionSource() }
                                    OutlinedTextField(
                                        value = editBank ?: "Select Bank",
                                        onValueChange = {},
                                        label = { Text(stringResource(R.string.label_bank)) },
                                        readOnly = true,
                                        trailingIcon = { IconButton(onClick = { editBankExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) } },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(indication = null, interactionSource = clickSrc2) { editBankExpanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = editBankExpanded,
                                        onDismissRequest = { editBankExpanded = false },
                                        offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp)
                                    ) {
                                        DropdownMenuItem(text = { Text("Others") }, onClick = { editBank = "Others"; editBankExpanded = false })
                                        IndianBanks.all.forEach { bank ->
                                            DropdownMenuItem(leadingIcon = { BankAvatar(bank) }, text = { Text(bank) }, onClick = { editBank = bank; editBankExpanded = false })
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            // Removed display name field as requested
                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { tf -> editAmount = formatIndianNumberTF(tf) },
                                label = { Text("Amount") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Values saved via confirmButton closure
                        }
                    }
                )
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
                        data.forEachIndexed { idx, (label, value) ->
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
    val header = "S.No,Investment Type,Bank,Added On,Amount\n"
    val body = items.mapIndexed { idx, e ->
        listOf(idx + 1, e.investmentType, e.bankName ?: "", formatAddedOn(e.createdAt), FormatUtils.formatINR(e.amount)).joinToString(",") {
            // escape commas if needed
            val s = it.toString()
            if (s.contains(",")) '"' + s.replace("\"", "\"\"") + '"' else s
        }
    }.joinToString("\n")
    val total = items.sumOf { it.amount }
    val totalRow = listOf("", "Total", "", "", FormatUtils.formatINR(total)).joinToString(",") { v ->
        val s = v.toString(); if (s.contains(",")) '"' + s.replace("\"", "\"\"") + '"' else s
    }
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
        // Columns: #, Investment Type, Bank, Added On, Amount | colX[5] as right edge
        val colX = floatArrayOf(40f, 60f, 230f, 380f, 480f, 555f)
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

    fun drawWatermark() {
        appIconBitmap?.let { bmp ->
            val save = canvas.save()
            canvas.rotate(-25f, (pageW/2).toFloat(), (pageH/2).toFloat())
            val scale = 0.6f
            val w = (bmp.width * scale).toInt()
            val h = (bmp.height * scale).toInt()
            val left = (pageW/2 - w/2).toFloat()
            val top = (pageH/2 - h/2).toFloat()
            val p = Paint().apply { alpha = 40 }
            val rect = android.graphics.Rect(0,0,bmp.width,bmp.height)
            val dst = android.graphics.Rect(left.toInt(), top.toInt(), left.toInt()+w, top.toInt()+h)
            canvas.drawBitmap(bmp, rect, dst, p)
            canvas.restoreToCount(save)
        }
    }

    fun drawHeader(): Float {
        drawWatermark()
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
        canvas.drawText("TrackKarle - Investments", 40f, y, titlePaint)
        y += 28f
        canvas.drawText("#", colX[0], y, headerPaint)
        canvas.drawText("Investment Type", colX[1], y, headerPaint)
        canvas.drawText("Bank", colX[2], y, headerPaint)
        canvas.drawText("Added On", colX[3], y, headerPaint)
        canvas.drawText("Amount", colX[4], y, headerPaint)
        y += 12f
        canvas.drawLine(colX[0], y, colX[5], y, textPaint)
        y += 16f
        return y
    }

    var y = drawHeader()

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
        canvas.drawText((idx + 1).toString(), colX[0], y, textPaint)
        canvas.drawText(e.investmentType, colX[1], y, textPaint)
        canvas.drawText((e.bankName ?: "-"), colX[2], y, textPaint)
        canvas.drawText(formatAddedOn(e.createdAt), colX[3], y, textPaint)
        canvas.drawText(FormatUtils.formatINR(e.amount), colX[4], y, textPaint)
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
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: optional check then type/name and optional bank
        // No icon during highlight; only a single fade background
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (entity.investmentType == "FD") "Fixed Deposit" else entity.investmentType,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            val showSub = when (entity.investmentType) {
                "FD" -> !entity.bankName.isNullOrBlank()
                "Others" -> entity.type.isNotBlank()
                else -> false
            }
            if (showSub) {
                val sub = when (entity.investmentType) {
                    "FD" -> entity.bankName ?: ""
                    "Others" -> entity.type
                    else -> ""
                }
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Added on ${formatAddedOn(entity.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Right: amount then actions
        Text(
            FormatUtils.formatINR(entity.amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null) }
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
