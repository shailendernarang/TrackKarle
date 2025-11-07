package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.InvestmentTypes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onAddClick: () -> Unit,
    onOpenInvestments: () -> Unit,
    onOpenCalculators: () -> Unit = {}
) {
    val allItems by viewModel.investments.collectAsState()
    val filtered by viewModel.filteredInvestments.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()

    var vizType by remember { mutableStateOf("Pie") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Dashboard") },
                actions = {
                    FilledTonalButton(onClick = onOpenInvestments) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("View Investments")
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
                    // AdMob Adaptive Anchored banner
                    run {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        val dm = ctx.resources.displayMetrics
                        val adWidthDp = (dm.widthPixels / dm.density).toInt()
                        val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthDp)
                        val adaptiveHeightDp = adaptiveSize.getHeightInPixels(ctx) / dm.density
                        AndroidView(
                            factory = { context ->
                                com.google.android.gms.ads.AdView(context).apply {
                                    setAdSize(adaptiveSize)
                                    adUnitId = "ca-app-pub-4934815537317220/1418248826"
                                    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(adaptiveHeightDp.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Private • No cloud sync • Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FloatingActionButton(onClick = onOpenCalculators) {
                    Icon(Icons.Default.Calculate, contentDescription = "Open Calculators")
                }
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Investment") }
                )
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Filter chips row (dynamic)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val counts = allItems.groupBy { it.investmentType }.mapValues { it.value.size }
                val filters = listOf("All") + counts.keys.sorted()
                filters.forEach { key ->
                    val selected = (typeFilter ?: "All") == key
                    val count = if (key == "All") allItems.size else counts[key] ?: 0
                    val display = if (key == "FD") "Fixed Deposit" else key
                    val icon = when (key) {
                        "All" -> Icons.Default.Savings
                        "FD" -> Icons.Default.AccountBalance
                        "Mutual Fund", "NPS", "Equity" -> Icons.Default.TrendingUp
                        "Gold" -> Icons.Default.Savings
                        else -> Icons.Default.Savings
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setTypeFilter(if (key == "All") null else key) },
                        label = { Text("$display ($count)") },
                        leadingIcon = { Icon(icon, contentDescription = null) }
                    )
                }
            }

            // Summary cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Amount",
                    value = FormatUtils.formatINR(allItems.sumOf { it.amount }),
                    color = MaterialTheme.colorScheme.primary
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Investments",
                    value = allItems.size.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            ChartSectionDashboard(items = filtered, filter = typeFilter, vizType = vizType, onVizTypeChange = { vizType = it })

            // Recent 5
            if (allItems.isNotEmpty()) {
                Text("Recent investments", style = MaterialTheme.typography.titleLarge)
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(allItems.sortedByDescending { it.createdAt }.take(5)) { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (e.investmentType == "FD") "Fixed Deposit" else e.investmentType, fontWeight = FontWeight.SemiBold)
                                val sub = e.bankName ?: if (e.investmentType == "Others") e.type else null
                                if (!sub.isNullOrBlank()) {
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(FormatUtils.formatINR(e.amount), fontWeight = FontWeight.Bold)
                        }
                        Divider()
                    }
                }
                // Removed trailing divider to avoid bottom line break appearance
            }
        }
    }
}

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
            Text("Compare FD Rates", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            data.take(8).forEach { (bank, rate, tenure) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(bank, fontWeight = FontWeight.SemiBold)
                        if (tenure.isNotBlank()) Text(tenure, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(rate, fontWeight = FontWeight.Bold)
                }
                Divider()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Source: Bank websites (sample) — see bank sites for latest rates",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

@Composable
private fun ChartSectionDashboard(items: List<InvestmentEntity>, filter: String?, vizType: String, onVizTypeChange: (String) -> Unit) {
    val data = run {
        val grouped = if (filter == "FD") {
            items.groupBy { (it.bankName ?: "Unknown Bank").ifBlank { "Unknown Bank" } }
        } else {
            items.groupBy { it.investmentType }
        }
        val pairs = grouped.map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
        if (pairs.size <= 5) pairs else {
            val top = pairs.take(5)
            val othersSum = pairs.drop(5).sumOf { it.second }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Overall Allocation", style = MaterialTheme.typography.titleMedium)
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
                        Text("Pie", color = if (vizType == "Pie") selectedText else unselectedText, style = MaterialTheme.typography.labelMedium)
                    }
                    // Bar segment
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (vizType == "Bar") selectedBg else unselectedBg)
                            .clickable { if (vizType != "Bar") onVizTypeChange("Bar") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Bar", color = if (vizType == "Bar") selectedText else unselectedText, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (data.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("No data to visualize", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (vizType == "Pie") {
                var selectedLabel by remember(items, filter) { mutableStateOf<String?>(null) }
                var animateOnToggle by remember(items, filter, vizType) { mutableStateOf(true) }
                AndroidView(
                    factory = { context ->
                        com.github.mikephil.charting.charts.PieChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            // We compute percentages ourselves from dataset values
                            setUsePercentValues(false)
                            // Remove labels on slices
                            setDrawEntryLabels(false)
                            setHoleColor(android.graphics.Color.TRANSPARENT)
                            setTouchEnabled(true)
                            setExtraOffsets(8f, 8f, 8f, 8f)
                            setCenterTextColor(android.graphics.Color.DKGRAY)
                            setCenterTextSize(14f)
                            setHighlightPerTapEnabled(true)
                        }
                    },
                    update = { view ->
                        val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                        // Initialize once
                        if (view.data == null) {
                            val entries = data.map { (label, value) -> com.github.mikephil.charting.data.PieEntry(value.toFloat(), label) }
                            val set = com.github.mikephil.charting.data.PieDataSet(entries, null).apply {
                                this.colors = colors
                                sliceSpace = 2f
                                selectionShift = 6f
                                // keep labels inside and never draw dataset values/lines
                                yValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
                                xValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
                            }
                            view.data = com.github.mikephil.charting.data.PieData(set).apply { setDrawValues(false) }
                            view.centerText = ""
                            // Custom marker to draw a small line and percentage text for selected slice only
                            val txtPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.DKGRAY
                                textSize = 40f
                                typeface = android.graphics.Typeface.create("montserrat", android.graphics.Typeface.BOLD)
                            }
                            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.DKGRAY
                                strokeWidth = 6f
                            }
                            val chartRef = view
                            view.marker = object : com.github.mikephil.charting.components.IMarker {
                                private var label: String = ""
                                override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: com.github.mikephil.charting.highlight.Highlight?) {
                                    val pe = e as? com.github.mikephil.charting.data.PieEntry ?: return
                                    val ds = chartRef.data?.getDataSetByIndex(0) as? com.github.mikephil.charting.data.PieDataSet
                                    val denom = (ds?.values?.sumOf { it.value.toDouble() } ?: 0.0).coerceAtLeast(1.0)
                                    val pct = (pe.value.toDouble() / denom * 100.0)
                                    label = "${pe.label}: ${"%.0f".format(pct)}%"
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
                                    // no animation on selection
                                }
                                override fun onNothingSelected() { selectedLabel = null }
                            })
                            // Play initial animation on first render if enabled
                            if (animateOnToggle) {
                                view.animateY(700, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
                                animateOnToggle = false
                            }
                            view.invalidate()
                        } else {
                            // Update or rebuild dataset based on filter changes
                            val set = view.data.getDataSetByIndex(0) as com.github.mikephil.charting.data.PieDataSet
                            val currentLabels = set.values.map { it.label }
                            val newLabels = data.map { it.first }
                            val needsRebuild = currentLabels.size != newLabels.size || currentLabels != newLabels
                            if (needsRebuild) {
                                val newEntries = data.map { (label, value) -> com.github.mikephil.charting.data.PieEntry(value.toFloat(), label) }
                                set.values = newEntries
                                set.colors = newLabels.mapIndexed { idx, _ -> colors[idx % colors.size] }
                            } else {
                                // Keep same order; update y values only
                                data.forEachIndexed { index, pair ->
                                    set.values[index] = com.github.mikephil.charting.data.PieEntry(pair.second.toFloat(), pair.first)
                                }
                            }
                            // Dataset always without values/lines; marker handles selection label+line
                            view.data.setDrawValues(false)
                            set.yValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
                            set.xValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
                            view.data.notifyDataChanged()
                            view.notifyDataSetChanged()
                            // Clear stray highlights when nothing is selected (prevents empty marker/lines)
                            if (selectedLabel == null) {
                                view.highlightValues(null)
                            }
                            // animate only on filter/toggle updates, not selection
                            if (animateOnToggle) {
                                view.animateY(700, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
                                animateOnToggle = false
                            }
                            view.invalidate()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
                Spacer(Modifier.height(8.dp))
                // Legend with percentages
                val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    data.take(10).forEachIndexed { idx, (label, value) ->
                        val pct = (value / total * 100.0)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
                                drawRect(color = Color(colors[idx % colors.size]))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("$label: ${"%.0f".format(pct)}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                var barAnimateOnToggle by remember(items, filter, vizType) { mutableStateOf(true) }
                AndroidView(
                    factory = { context ->
                        com.github.mikephil.charting.charts.BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            axisRight.isEnabled = false
                            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                            xAxis.setDrawGridLines(false)
                            axisLeft.setDrawGridLines(true)
                            xAxis.setGranularity(1f)
                            xAxis.setLabelRotationAngle(-20f)
                            setFitBars(true)
                            setTouchEnabled(false)
                            setExtraOffsets(12f, 12f, 12f, 12f)
                            // animation is controlled in update
                        }
                    },
                    update = { view ->
                        val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                        val entries = data.mapIndexed { idx, (_, value) ->
                            val pct = (value / total * 100.0).toFloat()
                            com.github.mikephil.charting.data.BarEntry(idx.toFloat(), pct)
                        }
                        val set = com.github.mikephil.charting.data.BarDataSet(entries, null).apply {
                            this.colors = colors
                        }
                        view.data = com.github.mikephil.charting.data.BarData(set).apply {
                            setDrawValues(true)
                            setValueTextSize(9f)
                            setValueTextColor(android.graphics.Color.DKGRAY)
                            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                override fun getBarLabel(barEntry: com.github.mikephil.charting.data.BarEntry?): String {
                                    return "${barEntry?.y?.toInt() ?: 0}%"
                                }
                            })
                            barWidth = 0.6f
                        }
                        val labels = data.map { it.first }
                        view.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                                val i = value.toInt()
                                return if (i in labels.indices) labels[i] else ""
                            }
                        }
                        if (barAnimateOnToggle) {
                            view.animateY(700, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
                            barAnimateOnToggle = false
                        }
                        view.invalidate()
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
                    data.take(10).forEachIndexed { idx, (label, value) ->
                        val pct = (value / total * 100.0)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.width(12.dp).height(12.dp)) {
                                drawRect(color = Color(colors[idx % colors.size]))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("$label: ${"%.0f".format(pct)}%", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
