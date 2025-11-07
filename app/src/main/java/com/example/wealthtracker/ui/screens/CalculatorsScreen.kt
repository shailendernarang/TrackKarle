package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.wealthtracker.R
import com.google.android.gms.ads.AdSize
import kotlin.math.pow

// ------------------------ DATA + HELPERS ------------------------

private data class FdRateRow(val bank: String, val tenure: String, val nonSeniorRate: Double, val seniorRate: Double)

private fun loadFdRates(ctx: android.content.Context): List<FdRateRow> {
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

private fun normalizeTenure(raw: String): String {
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

private val TENURE_ORDER = listOf("1y", "1y 4m", "1y 6m", "2y", "3y", "5y")

// ------------------------ MAIN SCREEN ------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalculatorsScreen(onBack: () -> Unit = {}, initialTab: String? = null, showBack: Boolean = true) {
    val tabs = listOf("SIP", "Lumpsum", "FD", "PPF/EPF")
    val initialIndex = when (initialTab?.lowercase()) {
        "fd" -> 2
        "lumpsum" -> 1
        "ppf", "epf" -> 3
        else -> 0
    }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculators") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            // AdMob Adaptive Anchored banner at top
            val ctx = LocalContext.current
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
            Spacer(Modifier.height(8.dp))
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = pagerState.currentPage == i,
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        text = { Text(t) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().height(1200.dp)) {
                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> SipCalculator()
                        1 -> LumpsumCalculator()
                        2 -> FdCalculator()
                        else -> PpfEpfCalculator()
                    }
                }
            }
        }
    }
}

// ------------------------ REUSABLE UI ------------------------

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(formatIndianNumber(new)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun parseINR(input: String): Double =
    input.replace(",", "").trim().toDoubleOrNull() ?: 0.0

private fun formatIndianNumber(input: String): String {
    val clean = input.replace(",", "").trim()
    if (clean.isBlank()) return ""
    val parts = clean.split('.')
    var intPart = parts.getOrNull(0)?.filter { it.isDigit() } ?: ""
    val decPart = parts.getOrNull(1)?.filter { it.isDigit() } ?: ""
    if (intPart.isBlank()) return ""

    val sb = StringBuilder()
    val n = intPart.length
    if (n > 3) {
        val last3 = intPart.takeLast(3)
        var rem = intPart.dropLast(3)
        val remSb = StringBuilder()
        while (rem.length > 2) {
            remSb.insert(0, "," + rem.takeLast(2))
            rem = rem.dropLast(2)
        }
        if (rem.isNotEmpty()) remSb.insert(0, rem)
        sb.append(remSb).append(",").append(last3)
    } else sb.append(intPart)

    if (decPart.isNotEmpty()) sb.append(".").append(decPart.take(2))
    return sb.toString()
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

// ------------------------ SIP ------------------------

@Composable
fun SipCalculator() {
    var monthly by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }

    val m = parseINR(monthly)
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0 / 12.0
    val n = (years.toDoubleOrNull() ?: 0.0) * 12.0
    val future = if (r == 0.0) m * n else m * ((1 + r).pow(n) - 1) * (1 + r) / r
    val invested = m * n
    val gain = (future - invested).coerceAtLeast(0.0)

    Section("SIP Calculator") {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            NumberField("Monthly Investment (₹)", monthly) { monthly = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Expected Return (% p.a.)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = years,
                onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Tenure (years)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ResultRow("Total Invested", "₹ ${formatIndianNumber(invested.toLong().toString())}")
            ResultRow("Future Value", "₹ ${formatIndianNumber(future.toLong().toString())}")
            ResultRow("Estimated Gain", "₹ ${formatIndianNumber(gain.toLong().toString())}")
        }
    }
}

// ------------------------ LUMPSUM ------------------------

@Composable
fun LumpsumCalculator() {
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }

    val p = parseINR(amount)
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val n = (years.toDoubleOrNull() ?: 0.0)
    val future = p * (1 + r).pow(n)
    val gain = (future - p).coerceAtLeast(0.0)

    Section("Lumpsum Calculator") {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            NumberField("Amount (₹)", amount) { amount = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Expected Return (% p.a.)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = years,
                onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Tenure (years)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ResultRow("Future Value", "₹ ${formatIndianNumber(future.toLong().toString())}")
            ResultRow("Estimated Gain", "₹ ${formatIndianNumber(gain.toLong().toString())}")
        }
    }
}

// ------------------------ FD CALCULATOR ------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FdCalculator() {

    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var compounding by remember { mutableStateOf("Quarterly") }
    var selectedTenure by remember { mutableStateOf("1y") }
    var selectedBank by remember { mutableStateOf("") }
    var senior by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val rateRows = remember { loadFdRates(ctx) }
    val banks = remember(rateRows) { rateRows.map { it.bank }.distinct().sorted() }
    val tenures = remember(rateRows) {
        rateRows.map { normalizeTenure(it.tenure) }
            .distinct()
            .sortedWith(
                compareBy(
                    { TENURE_ORDER.indexOf(it).let { i -> if (i == -1) Int.MAX_VALUE else i } },
                    { it }
                )
            )
    }

    val selectedRate = remember(selectedBank, selectedTenure, senior, rateRows) {
        val norm = normalizeTenure(selectedTenure)
        val matches = rateRows.filter { it.bank == selectedBank && normalizeTenure(it.tenure) == norm }
        if (matches.isEmpty()) null else {
            val pick = if (senior) matches.maxByOrNull { it.seniorRate }
            else matches.maxByOrNull { it.nonSeniorRate }
            pick?.let { if (senior) it.seniorRate else it.nonSeniorRate }
        }
    }

    LaunchedEffect(selectedRate) {
        selectedRate?.let { rate = "%.2f".format(it) }
    }

    val p = parseINR(principal)
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val t = (years.toDoubleOrNull() ?: 0.0)
    val n = when (compounding) {
        "Monthly" -> 12
        "Quarterly" -> 4
        "Half-yearly" -> 2
        else -> 1
    }.toDouble()

    val maturity = p * (1 + r / n).pow(n * t)
    val interest = (maturity - p).coerceAtLeast(0.0)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {

        item {
            Section("FD Maturity Calculator") {

                Column {

                    Text("Bank & Rate", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))

                    var bankExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = bankExpanded, onExpandedChange = { bankExpanded = !bankExpanded }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = if (selectedBank.isBlank()) "Select Bank" else selectedBank,
                            onValueChange = {},
                            label = { Text("Bank") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = bankExpanded, onDismissRequest = { bankExpanded = false }) {
                            banks.forEach { b ->
                                DropdownMenuItem(text = { Text(b) }, onClick = {
                                    selectedBank = b
                                    bankExpanded = false
                                })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    var tenureExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = tenureExpanded, onExpandedChange = { tenureExpanded = !tenureExpanded }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedTenure,
                            onValueChange = {},
                            label = { Text("Tenure") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenureExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = tenureExpanded, onDismissRequest = { tenureExpanded = false }) {
                            tenures.forEach { tOpt ->
                                DropdownMenuItem(
                                    text = { Text(tOpt) },
                                    onClick = {
                                        selectedTenure = tOpt
                                        tenureExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Senior citizen")
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = senior, onCheckedChange = { senior = it })
                        Spacer(Modifier.width(12.dp))
                        selectedRate?.let { sr ->
                            AssistChip(onClick = { rate = "%.2f".format(sr) }, label = { Text("Use ${"%.2f".format(sr)}%") })
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    NumberField("Principal (₹)", principal) { principal = it }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Interest Rate (% p.a.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = years,
                        onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tenure (years)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    var freqExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = freqExpanded, onExpandedChange = { freqExpanded = !freqExpanded }) {
                        OutlinedTextField(
                            readOnly = true,
                            value = compounding,
                            onValueChange = {},
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = freqExpanded, onDismissRequest = { freqExpanded = false }) {
                            listOf("Monthly", "Quarterly", "Half-yearly", "Yearly").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        compounding = it
                                        freqExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    ResultRow("Maturity Amount", "₹ ${formatIndianNumber(maturity.toLong().toString())}")
                    ResultRow("Interest Earned", "₹ ${formatIndianNumber(interest.toLong().toString())}")
                }
            }
            Spacer(Modifier.height(4.dp))
            FdRatesComparison(
                selectedTenure = selectedTenure,
                senior = senior,
                onUseRate = { used -> rate = "%.2f".format(used) }
            )
        }
    }
}

// ------------------------ FD RATES GRID ------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdRatesComparison(selectedTenure: String, senior: Boolean, onUseRate: (Double) -> Unit) {

    val ctx = LocalContext.current
    val rateRows = remember { loadFdRates(ctx) }
    val norm = normalizeTenure(selectedTenure)

    var sortExpanded by remember { mutableStateOf(false) }
    var sortLabel by remember { mutableStateOf("Rate: High → Low") }

    val sorted = remember(rateRows, norm, senior, sortLabel) {
        val list = rateRows.filter { normalizeTenure(it.tenure) == norm }
        when (sortLabel) {
            "Name: A → Z" -> list.sortedBy { it.bank.lowercase() }
            "Name: Z → A" -> list.sortedByDescending { it.bank.lowercase() }
            "Rate: Low → High" -> list.sortedBy { if (senior) it.seniorRate else it.nonSeniorRate }
            else -> list.sortedByDescending { if (senior) it.seniorRate else it.nonSeniorRate }
        }
    }

    Spacer(Modifier.height(4.dp))

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("FD Rates ($norm)", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = !sortExpanded }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = sortLabel,
                        onValueChange = {},
                        label = { Text("Sort") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier.menuAnchor().widthIn(min = 200.dp)
                    )
                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        listOf("Name: A → Z", "Name: Z → A", "Rate: High → Low", "Rate: Low → High").forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                sortLabel = opt
                                sortExpanded = false
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxWidth().height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted) { row ->
                    val rTxt = if (senior) row.seniorRate else row.nonSeniorRate

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(8.dp).fillMaxWidth()) {
                            Text(row.bank, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(row.tenure, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text("${"%.2f".format(rTxt)}%", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            AssistChip(onClick = { onUseRate(rTxt) }, label = { Text("Use rate") })
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("Source: Offline sample — verify with bank websites for latest rates")
        }
    }
}

// ------------------------ PPF / EPF ------------------------

@Composable
fun PpfEpfCalculator() {
    var yearly by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("7.1") }
    var years by remember { mutableStateOf("15") }

    val y = parseINR(yearly)
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val n = years.toIntOrNull() ?: 0

    var future = 0.0
    repeat(n) { future = (future + y) * (1 + r) }

    val invested = y * n
    val gain = (future - invested).coerceAtLeast(0.0)
    val limit = 150000.0
    val eligible = y.coerceAtMost(limit)

    Section("PPF/EPF Projection & 80C Tracker") {
        Column(Modifier.verticalScroll(rememberScrollState())) {

            NumberField("Yearly Contribution (₹)", yearly) { yearly = it }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Expected Return (% p.a.)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = years,
                onValueChange = { years = it.filter { c -> c.isDigit() } },
                label = { Text("Tenure (years)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            ResultRow("Total Invested", "₹ ${formatIndianNumber(invested.toLong().toString())}")
            ResultRow("Future Value", "₹ ${formatIndianNumber(future.toLong().toString())}")
            ResultRow("Estimated Gain", "₹ ${formatIndianNumber(gain.toLong().toString())}")

            Divider(Modifier.padding(vertical = 8.dp))

            ResultRow(
                "80C Eligible (est.)",
                "₹ ${formatIndianNumber(eligible.toLong().toString())} / ₹ 1,50,000"
            )
        }
    }
}
