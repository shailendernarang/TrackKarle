package com.example.wealthtracker.ui.screens

import com.example.wealthtracker.util.findActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.ui.components.AppBottomNavBar
import com.example.wealthtracker.ui.components.AppodealNativeNewsFeed
import com.ss.wealthtracker.R
import kotlin.math.pow

// ── Data ─────────────────────────────────────────────────────────────────────

data class FdRateRow(val bank: String, val tenure: String, val nonSeniorRate: Double, val seniorRate: Double)
data class EmiMonth(val month: Int, val emi: Double, val principal: Double, val interest: Double, val balance: Double)

private data class CalcTab(val id: String, val emoji: String, val label: String, val sub: String)
private val CALC_TABS = listOf(
    CalcTab("fd",      "🏦", "FD",      "Fixed Deposit"),
    CalcTab("sip",     "📈", "SIP",     "Mutual Fund"),
    CalcTab("lumpsum", "💰", "Lumpsum", "One-Time"),
    CalcTab("ppf",     "🏛️", "PPF",     "Govt Scheme"),
    CalcTab("emi",     "💳", "EMI",     "Loan"),
    CalcTab("tax",     "📋", "Tax",     "Old vs New"),
)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun loadFdRates(ctx: android.content.Context): List<FdRateRow> =
    runCatching {
        ctx.resources.openRawResource(R.raw.fd_rates_30_banks).bufferedReader()
            .useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val p = line.split(",")
                    if (p.size >= 4) FdRateRow(
                        p[0].trim(), p[1].trim(),
                        p[2].toDoubleOrNull() ?: return@mapNotNull null,
                        p[3].toDoubleOrNull() ?: return@mapNotNull null
                    ) else null
                }.toList()
            }
    }.getOrDefault(emptyList())

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

private fun parseINR(input: String): Double = input.replace(",", "").trim().toDoubleOrNull() ?: 0.0

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
        while (rem.length > 2) { remSb.insert(0, "," + rem.takeLast(2)); rem = rem.dropLast(2) }
        if (rem.isNotEmpty()) remSb.insert(0, rem)
        sb.append(remSb).append(",").append(last3)
    } else sb.append(intPart)
    if (decPart.isNotEmpty()) sb.append(".").append(decPart.take(2))
    return sb.toString()
}

private fun formatCrLakh(amount: Double): String = when {
    amount.isNaN() || amount.isInfinite() || amount == 0.0 -> "₹0"
    amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)} Cr"
    amount >= 1_00_000    -> "₹${"%.2f".format(amount / 1_00_000)} L"
    else                  -> "₹${formatIndianNumber(amount.toLong().toString())}"
}

// ── Shared UI ─────────────────────────────────────────────────────────────────

private val CardGradient  = Brush.linearGradient(listOf(Color(0xFF0D3B8E), Color(0xFF6A1B9A)))
private val GreenGradient = Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))

@Composable
private fun CalcCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) { content() }
}

@Composable
private fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = CardGradient,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(brush).padding(20.dp)
    ) { Column(content = content) }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun StatPair(
    label1: String, val1: String,
    label2: String, val2: String,
    color1: Color = Color.White,
    color2: Color = Color.White
) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label1, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
            Text(val1, color = color1, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(label2, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
            Text(val2, color = color2, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AmountField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(formatIndianNumber(it)) },
        label = { Text(label) },
        prefix = { Text("₹ ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RateField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        suffix = { Text(" % p.a.") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

/** Pill-style segmented selector — same pattern as Yrs/Mos in EMI */
@Composable
private fun PillSelector(
    options: List<Pair<String, String>>,   // key → displayLabel
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (key, label) ->
            val sel = selected == key
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    onBack: () -> Unit = {},
    initialTab: String? = null,
    showBack: Boolean = true,
    onGoHome: () -> Unit = {},
    onGoPortfolio: () -> Unit = {},
    onGoSettings: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val defaultId = when (initialTab?.lowercase()) {
        "fd"                               -> "fd"
        "sip"                              -> "sip"
        "lumpsum", "lump-sum", "lump- sum" -> "lumpsum"
        "ppf", "epf"                       -> "ppf"
        "emi"                              -> "emi"
        "tax"                              -> "tax"
        else                               -> "fd"
    }
    var selectedId by remember { mutableStateOf(defaultId) }
    val ctx = LocalContext.current
    val fdRateRows = remember { loadFdRates(ctx) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧮", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculators", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    if (showBack) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomNavBar(
                currentRoute = "calculators",
                onGoHome = onGoHome,
                onGoPortfolio = onGoPortfolio,
                onGoCalculators = {},
                onGoSettings = onGoSettings,
                onAddClick = onAddClick
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.background)
        ) {
            // ── Tab chip grid ──────────────────────────────────────────────
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    CALC_TABS.chunked(3).forEachIndexed { ri, row ->
                        if (ri > 0) Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { tab ->
                                CalcChip(
                                    tab = tab,
                                    selected = selectedId == tab.id,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedId = tab.id }
                                )
                            }
                        }
                    }
                }
            }

            // ── Calculator content ─────────────────────────────────────────
            when (selectedId) {
                "fd"      -> FdCalculator(fdRateRows)
                "sip"     -> SipCalculator()
                "lumpsum" -> LumpsumCalculator()
                "ppf"     -> PpfEpfCalculator()
                "emi"     -> EmiCalculator()
                "tax"     -> TaxCalculator()
            }
        }
    }
}

@Composable
private fun CalcChip(tab: CalcTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg  = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg  = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val sub = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(tab.emoji, fontSize = 14.sp)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(tab.label, color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp)
            Text(tab.sub, color = sub, fontSize = 9.5.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── FD Calculator ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdCalculator(rateRows: List<FdRateRow>) {
    var principal    by remember { mutableStateOf("") }
    var rate         by remember { mutableStateOf("") }
    var years        by remember { mutableStateOf("1") }
    var compounding  by remember { mutableStateOf("Quarterly") }
    var selectedBank by remember { mutableStateOf("") }
    var senior       by remember { mutableStateOf(false) }
    var showSheet    by remember { mutableStateOf(false) }
    var bankSearch   by remember { mutableStateOf("") }

    val banks = remember(rateRows) { rateRows.map { it.bank }.distinct().sorted() }
    val currentTenure = remember(years) {
        val y = years.toDoubleOrNull() ?: 1.0
        when { y <= 1.0 -> "1y"; y <= 1.33 -> "1y 4m"; y <= 1.5 -> "1y 6m"; y <= 2.0 -> "2y"; y <= 3.0 -> "3y"; else -> "5y" }
    }
    val autoRate = remember(selectedBank, currentTenure, senior, rateRows) {
        val matches = rateRows.filter { it.bank == selectedBank && normalizeTenure(it.tenure) == currentTenure }
        if (matches.isEmpty()) null
        else if (senior) matches.maxByOrNull { it.seniorRate }?.seniorRate
        else matches.maxByOrNull { it.nonSeniorRate }?.nonSeniorRate
    }
    LaunchedEffect(autoRate) { autoRate?.let { rate = "%.2f".format(it) } }

    val p       = parseINR(principal)
    val r       = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val t       = years.toDoubleOrNull() ?: 0.0
    val n       = when (compounding) { "Monthly" -> 12.0; "Half-yearly" -> 2.0; "Yearly" -> 1.0; else -> 4.0 }
    val maturity = if (p > 0 && r > 0 && t > 0) p * (1 + r / n).pow(n * t) else 0.0
    val interest = (maturity - p).coerceAtLeast(0.0)

    // Bank picker bottom sheet
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false; bankSearch = "" }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Select Bank & Rate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bankSearch,
                    onValueChange = { bankSearch = it },
                    placeholder = { Text("Search bank or institution…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val filteredBanks = remember(bankSearch, banks) {
                    if (bankSearch.isBlank()) banks else banks.filter { it.contains(bankSearch, ignoreCase = true) }
                }
                val sheetRates = remember(rateRows, currentTenure, senior) {
                    rateRows.filter { normalizeTenure(it.tenure) == currentTenure }
                        .associateBy({ it.bank }) { if (senior) it.seniorRate else it.nonSeniorRate }
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(filteredBanks) { bank ->
                        val bankRate = sheetRates[bank]
                        ListItem(
                            headlineContent = { Text(bank, fontWeight = FontWeight.Medium) },
                            trailingContent = {
                                if (bankRate != null) Text(
                                    "${"%.2f".format(bankRate)}%",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingContent = {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(bank.first().toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            },
                            modifier = Modifier.clickable {
                                selectedBank = bank
                                bankRate?.let { rate = "%.2f".format(it) }
                                showSheet = false; bankSearch = ""
                            }
                        )
                        HorizontalDivider(Modifier.padding(start = 56.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Bank card ──────────────────────────────────────────────────────
        item {
            CalcCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (selectedBank.isBlank()) "🏦" else selectedBank.first().toString(),
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedBank.isBlank()) "Select a bank" else selectedBank,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                        if (rate.isNotBlank() && selectedBank.isNotBlank()) {
                            Text(
                                "$rate% p.a. · ${if (senior) "Senior" else "Regular"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "Tap Change to pick a bank",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                            )
                        }
                    }
                    TextButton(onClick = { showSheet = true }) {
                        Text("Change", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Senior citizen ─────────────────────────────────────────────────
        item {
            CalcCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Senior Citizen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Extra +0.5% interest rate",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                        )
                    }
                    Switch(checked = senior, onCheckedChange = { senior = it })
                }
            }
        }

        // ── Inputs ────────────────────────────────────────────────────────
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Principal Amount
                    AmountField("Principal Amount", principal) { principal = it }

                    // Tenure  +  Interest Rate (side by side)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = years,
                            onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Tenure") },
                            suffix = { Text(" yrs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        RateField(
                            label = "Interest Rate",
                            value = rate,
                            modifier = Modifier.weight(1f)
                        ) { rate = it }
                    }

                    // Compounding — pill segmented selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Compounding",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PillSelector(
                            options = listOf(
                                "Monthly"    to "Monthly",
                                "Quarterly"  to "Quarterly",
                                "Half-yearly" to "Half-Yr",
                                "Yearly"     to "Yearly"
                            ),
                            selected = compounding,
                            onSelect = { compounding = it }
                        )
                    }
                }
            }
        }

        // ── Native ad ──────────────────────────────────────────────────────
        item { AppodealNativeNewsFeed() }

        // ── Result card ────────────────────────────────────────────────────
        item {
            AnimatedVisibility(visible = maturity > 0, enter = fadeIn(), exit = fadeOut()) {
                GradientCard {
                    Text("Maturity Amount", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatCrLakh(maturity), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(Modifier.height(14.dp))
                    StatPair(
                        "Invested", formatCrLakh(p),
                        "Est. Returns", "+${formatCrLakh(interest)}",
                        color2 = Color(0xFF81C784)
                    )
                    Spacer(Modifier.height(8.dp))
                    StatPair("Tenure", "$years yrs", "Rate", "$rate%")
                }
            }
        }

        // ── FD rates comparison ────────────────────────────────────────────
        item {
            FdRatesComparison(
                rateRows = rateRows,
                currentTenure = currentTenure,
                senior = senior,
                selectedBank = selectedBank,
                onSelectBank = { bank, r -> selectedBank = bank; rate = "%.2f".format(r) }
            )
        }
    }
}

@Composable
private fun FdRatesComparison(
    rateRows: List<FdRateRow>,
    currentTenure: String,
    senior: Boolean,
    selectedBank: String,
    onSelectBank: (String, Double) -> Unit
) {
    val sorted = remember(rateRows, currentTenure, senior) {
        rateRows.filter { normalizeTenure(it.tenure) == currentTenure }
            .sortedByDescending { if (senior) it.seniorRate else it.nonSeniorRate }
    }
    if (sorted.isEmpty()) return
    CalcCard {
        Column(Modifier.padding(14.dp)) {
            Text(
                "FD Rates · $currentTenure",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            sorted.forEach { row ->
                val r = if (senior) row.seniorRate else row.nonSeniorRate
                val isSel = row.bank == selectedBank
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSelectBank(row.bank, r) }
                        .padding(vertical = 9.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(row.bank.first().toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(row.bank, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${"%.2f".format(r)}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (isSel) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                if (row != sorted.last()) HorizontalDivider(Modifier.padding(start = 48.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("Rates are indicative — verify with bank websites", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

// ── SIP Calculator ────────────────────────────────────────────────────────────

@Composable
fun SipCalculator() {
    var monthly by remember { mutableStateOf("") }
    var rate    by remember { mutableStateOf("12") }
    var years   by remember { mutableStateOf("10") }
    val m = parseINR(monthly)
    val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0 / 12.0
    val n = (years.toDoubleOrNull() ?: 0.0) * 12.0
    val future   = if (r == 0.0) m * n else m * ((1 + r).pow(n) - 1) * (1 + r) / r
    val invested = m * n
    val gain     = (future - invested).coerceAtLeast(0.0)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField("Monthly Investment", monthly) { monthly = it }
                    RateField("Expected Return", rate) { rate = it }
                    OutlinedTextField(
                        value = years,
                        onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tenure") }, suffix = { Text(" yrs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { AppodealNativeNewsFeed() }
        item {
            AnimatedVisibility(visible = future > 0, enter = fadeIn(), exit = fadeOut()) {
                GradientCard {
                    Text("Future Value", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatCrLakh(future), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(Modifier.height(14.dp))
                    StatPair("Invested", formatCrLakh(invested), "Est. Gain", "+${formatCrLakh(gain)}", color2 = Color(0xFF81C784))
                    Spacer(Modifier.height(8.dp))
                    StatRow("Tenure", "$years yrs")
                    if (future > 0 && invested > 0) {
                        val cagr = ((future / invested).pow(1.0 / (years.toDoubleOrNull() ?: 1.0)) - 1) * 100
                        StatRow("CAGR (approx)", "${"%.1f".format(cagr)}%")
                    }
                }
            }
        }
    }
}

// ── Lumpsum Calculator ────────────────────────────────────────────────────────

@Composable
fun LumpsumCalculator() {
    var amount by remember { mutableStateOf("") }
    var rate   by remember { mutableStateOf("12") }
    var years  by remember { mutableStateOf("10") }
    val p      = parseINR(amount)
    val r      = (rate.toDoubleOrNull() ?: 0.0) / 100.0
    val n      = years.toDoubleOrNull() ?: 0.0
    val future = if (p > 0 && r > 0 && n > 0) p * (1 + r).pow(n) else 0.0
    val gain   = (future - p).coerceAtLeast(0.0)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField("Investment Amount", amount) { amount = it }
                    RateField("Expected Return", rate) { rate = it }
                    OutlinedTextField(
                        value = years,
                        onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tenure") }, suffix = { Text(" yrs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item { AppodealNativeNewsFeed() }
        item {
            AnimatedVisibility(visible = future > 0, enter = fadeIn(), exit = fadeOut()) {
                GradientCard {
                    Text("Future Value", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatCrLakh(future), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(Modifier.height(14.dp))
                    StatPair("Invested", formatCrLakh(p), "Est. Gain", "+${formatCrLakh(gain)}", color2 = Color(0xFF81C784))
                    Spacer(Modifier.height(8.dp))
                    StatRow("Tenure", "$years yrs")
                    StatRow("Rate", "$rate% p.a.")
                }
            }
        }
    }
}

// ── PPF / EPF Calculator ──────────────────────────────────────────────────────

@Composable
fun PpfEpfCalculator() {
    var yearly by remember { mutableStateOf("") }
    var rate   by remember { mutableStateOf("7.1") }
    var years  by remember { mutableStateOf("15") }
    val y = parseINR(yearly); val r = (rate.toDoubleOrNull() ?: 0.0) / 100.0; val n = years.toIntOrNull() ?: 0
    var future = 0.0; repeat(n) { future = (future + y) * (1 + r) }
    val invested = y * n; val gain = (future - invested).coerceAtLeast(0.0)
    val eligible = y.coerceAtMost(150000.0)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CalcCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏛️", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("PPF current rate: 7.1% p.a.", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Tax-free returns · Section 80C eligible", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField("Yearly Contribution", yearly) { yearly = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RateField("Interest Rate", rate, modifier = Modifier.weight(1f)) { rate = it }
                        OutlinedTextField(
                            value = years,
                            onValueChange = { years = it.filter { c -> c.isDigit() } },
                            label = { Text("Tenure") }, suffix = { Text(" yrs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item { AppodealNativeNewsFeed() }
        item {
            AnimatedVisibility(visible = future > 0, enter = fadeIn(), exit = fadeOut()) {
                GradientCard(brush = GreenGradient) {
                    Text("Maturity Value", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatCrLakh(future), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(Modifier.height(14.dp))
                    StatPair("Invested", formatCrLakh(invested), "Tax-Free Gain", "+${formatCrLakh(gain)}", color2 = Color(0xFFA5D6A7))
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    StatRow("80C Eligible", "₹${formatIndianNumber(eligible.toLong().toString())} / ₹1,50,000")
                }
            }
        }
    }
}

// ── EMI Calculator ────────────────────────────────────────────────────────────

@Composable
fun EmiCalculator() {
    var loanType   by remember { mutableStateOf("Home") }
    var amount     by remember { mutableStateOf("") }
    var rate       by remember { mutableStateOf("8.5") }
    var tenure     by remember { mutableStateOf("20") }
    var tenureUnit by remember { mutableStateOf("Yrs") }

    LaunchedEffect(loanType) {
        rate = when (loanType) { "Home" -> "8.5"; "Car" -> "9.5"; else -> "12.0" }
    }

    val principal = parseINR(amount)
    val r         = (rate.toDoubleOrNull() ?: 0.0) / 100.0 / 12.0
    val months    = if (tenureUnit == "Yrs") (tenure.toDoubleOrNull() ?: 0.0) * 12 else (tenure.toDoubleOrNull() ?: 0.0)
    val n         = months.toLong()
    val emi       = if (r == 0.0 || n == 0L) 0.0
                    else principal * r * (1 + r).pow(n.toDouble()) / ((1 + r).pow(n.toDouble()) - 1)
    val totalPayment  = emi * n
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)
    val interestPct   = if (totalPayment > 0) totalInterest / totalPayment * 100 else 0.0

    val schedule = remember(emi, principal, r, n) {
        if (emi <= 0 || n <= 0) emptyList()
        else {
            var balance = principal
            (1..n).map { month ->
                val intPart  = balance * r
                val prinPart = (emi - intPart).coerceAtLeast(0.0)
                balance -= prinPart
                EmiMonth(month.toInt(), emi, prinPart, intPart, balance.coerceAtLeast(0.0))
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Loan type
        item {
            PillSelector(
                options = listOf("Home" to "Home Loan", "Car" to "Car Loan", "Personal" to "Personal"),
                selected = loanType,
                onSelect = { loanType = it }
            )
        }

        // Inputs
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField("Loan Amount", amount) { amount = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RateField("Interest Rate", rate, modifier = Modifier.weight(1f)) { rate = it }
                        OutlinedTextField(
                            value = tenure,
                            onValueChange = { tenure = it.filter { c -> c.isDigit() } },
                            label = { Text("Tenure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Yrs / Mos toggle
                    PillSelector(
                        options = listOf("Yrs" to "Years", "Mos" to "Months"),
                        selected = tenureUnit,
                        onSelect = { tenureUnit = it }
                    )
                }
            }
        }

        // Native ad
        item { AppodealNativeNewsFeed() }

        // Result
        item {
            AnimatedVisibility(visible = emi > 0, enter = fadeIn(), exit = fadeOut()) {
                GradientCard {
                    Text("Monthly EMI", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatCrLakh(emi), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(Modifier.height(14.dp))
                    StatPair("Principal", formatCrLakh(principal), "Total Interest", "+${formatCrLakh(totalInterest)}", color2 = Color(0xFFEF9A9A))
                    Spacer(Modifier.height(8.dp))
                    StatPair("Total Payment", formatCrLakh(totalPayment), "Interest Burden", "${"%.1f".format(interestPct)}%", color2 = Color(0xFFFFCC80))
                }
            }
        }

        // Amortization
        if (schedule.isNotEmpty()) {
            item {
                Text(
                    "AMORTIZATION SCHEDULE",
                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp
                )
            }
            item {
                CalcCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Mo", Modifier.width(32.dp), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text("EMI",       Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text("Principal", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text("Interest",  Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                        val display = schedule.take(12) + if (schedule.size > 12) listOf(schedule.last()) else emptyList()
                        display.forEachIndexed { idx, row ->
                            if (idx == 12 && schedule.size > 13) {
                                Text("· · ·  ${schedule.size - 12} more months",
                                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(row.month.toString(), Modifier.width(32.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCrLakh(row.emi),       Modifier.weight(1f), fontSize = 11.sp)
                                Text(formatCrLakh(row.principal), Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(formatCrLakh(row.interest),  Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 11.sp, color = Color(0xFFE57373))
                            }
                            if (idx < display.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ── Tax Calculator ─────────────────────────────────────────────────────────────

private fun computeOldRegimeTax(taxable: Double): Double {
    if (taxable <= 0) return 0.0
    var tax = 0.0
    if (taxable > 1000000.0) tax += (taxable - 1000000.0) * 0.30
    if (taxable > 500000.0)  tax += (minOf(taxable, 1000000.0) - 500000.0) * 0.20
    if (taxable > 250000.0)  tax += (minOf(taxable, 500000.0) - 250000.0) * 0.05
    if (taxable <= 500000.0) tax = 0.0
    return tax * 1.04
}

private fun computeNewRegimeTax(taxable: Double): Double {
    if (taxable <= 0) return 0.0
    var tax = 0.0
    if (taxable > 2400000.0) tax += (taxable - 2400000.0) * 0.30
    if (taxable > 2000000.0) tax += (minOf(taxable, 2400000.0) - 2000000.0) * 0.25
    if (taxable > 1600000.0) tax += (minOf(taxable, 2000000.0) - 1600000.0) * 0.20
    if (taxable > 1200000.0) tax += (minOf(taxable, 1600000.0) - 1200000.0) * 0.15
    if (taxable > 800000.0)  tax += (minOf(taxable, 1200000.0) - 800000.0) * 0.10
    if (taxable > 400000.0)  tax += (minOf(taxable, 800000.0) - 400000.0) * 0.05
    if (taxable <= 1200000.0) tax = 0.0
    return tax * 1.04
}

@Composable
fun TaxCalculator() {
    var grossIncome   by remember { mutableStateOf("") }
    var deductions80C by remember { mutableStateOf("150000") }
    var deductions80D by remember { mutableStateOf("25000") }
    var hra           by remember { mutableStateOf("0") }
    var showOldDed    by remember { mutableStateOf(false) }

    val gross       = parseINR(grossIncome)
    val stdOld      = 50000.0; val stdNew = 75000.0
    val totalOldDed = parseINR(deductions80C).coerceAtMost(150000.0) + parseINR(deductions80D).coerceAtMost(25000.0) + parseINR(hra) + stdOld
    val taxableOld  = (gross - totalOldDed).coerceAtLeast(0.0)
    val taxableNew  = (gross - stdNew).coerceAtLeast(0.0)
    val oldTax      = computeOldRegimeTax(taxableOld)
    val newTax      = computeNewRegimeTax(taxableNew)
    val savings     = oldTax - newTax
    val newIsBetter = newTax <= oldTax
    val showBetter  = newIsBetter && (oldTax > 0 || newTax > 0) && kotlin.math.abs(savings) >= 1.0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner
        item {
            CalcCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("FY 2025-26", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Compares Old vs New tax regime and recommends the better one.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Income input
        item {
            CalcCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountField("Annual Gross Income", grossIncome) { grossIncome = it }
                    // Old regime deductions (collapsible)
                    Row(
                        Modifier.fillMaxWidth().clickable { showOldDed = !showOldDed }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Deductions (Old Regime)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(
                                if (!showOldDed) "Tap to add 80C, 80D, HRA" else "80C · 80D · HRA",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = showOldDed) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AmountField("80C – Investments (max ₹1.5L)", deductions80C) { deductions80C = it }
                            AmountField("80D – Health Insurance (max ₹25K)", deductions80D) { deductions80D = it }
                            AmountField("HRA / Other Deductions", hra) { hra = it }
                        }
                    }
                }
            }
        }

        if (gross > 0) {
            item { AppodealNativeNewsFeed() }
            // Regime comparison
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Old
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)
                    ) {
                        Column {
                            Text("Old Regime", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formatCrLakh(oldTax), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("Taxable: ${formatCrLakh(taxableOld)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    // New
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (showBetter) Color(0xFF1565C0) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("New Regime", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (showBetter) Color.White else MaterialTheme.colorScheme.onSurface)
                                if (showBetter) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF43A047)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                        Text("BETTER", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(formatCrLakh(newTax), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (showBetter) Color.White else MaterialTheme.colorScheme.onSurface)
                            Text("Taxable: ${formatCrLakh(taxableNew)}", color = if (showBetter) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Savings banner
            if (oldTax > 0 || newTax > 0) {
                item {
                    CalcCard {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (newIsBetter) "✅" else "ℹ️", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                val abs = kotlin.math.abs(savings)
                                Text(
                                    when {
                                        abs < 1 -> "Both regimes have the same tax"
                                        newIsBetter -> "New Regime saves you ${formatCrLakh(savings)}"
                                        else -> "Old Regime saves you ${formatCrLakh(-savings)}"
                                    },
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                )
                                Text(
                                    if (newIsBetter) "Fewer deductions needed, simpler filing" else "Higher deductions give you better savings",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // New regime breakdown
            item { SectionHeader("TAX BREAKDOWN · NEW REGIME") }
            item {
                val baseTax = run {
                    var t = 0.0
                    if (taxableNew > 2400000.0) t += (taxableNew - 2400000.0) * 0.30
                    if (taxableNew > 2000000.0) t += (minOf(taxableNew, 2400000.0) - 2000000.0) * 0.25
                    if (taxableNew > 1600000.0) t += (minOf(taxableNew, 2000000.0) - 1600000.0) * 0.20
                    if (taxableNew > 1200000.0) t += (minOf(taxableNew, 1600000.0) - 1200000.0) * 0.15
                    if (taxableNew > 800000.0)  t += (minOf(taxableNew, 1200000.0) - 800000.0) * 0.10
                    if (taxableNew > 400000.0)  t += (minOf(taxableNew, 800000.0) - 400000.0) * 0.05
                    if (taxableNew <= 1200000.0) t = 0.0
                    t
                }
                CalcCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaxRow("Gross Income",             formatCrLakh(gross))
                        TaxRow("Standard Deduction",       "-₹75,000", Color(0xFF2E7D32))
                        HorizontalDivider()
                        TaxRow("Taxable Income",           formatCrLakh(taxableNew), weight = FontWeight.SemiBold)
                        TaxRow("Income Tax",               formatCrLakh(baseTax))
                        if (taxableNew <= 1200000.0 && gross > 0) TaxRow("Rebate u/s 87A", "-${formatCrLakh(baseTax)}", Color(0xFF2E7D32))
                        TaxRow("Health & Edu Cess (4%)",   formatCrLakh(baseTax * 0.04))
                        HorizontalDivider()
                        TaxRow("Total Tax Payable",        formatCrLakh(newTax), weight = FontWeight.Bold)
                    }
                }
            }

            // Old regime breakdown
            item { SectionHeader("TAX BREAKDOWN · OLD REGIME") }
            item {
                val baseOldTax = if (taxableOld <= 500000.0) 0.0 else run {
                    var t = 0.0
                    if (taxableOld > 1000000.0) t += (taxableOld - 1000000.0) * 0.30
                    if (taxableOld > 500000.0)  t += (minOf(taxableOld, 1000000.0) - 500000.0) * 0.20
                    if (taxableOld > 250000.0)  t += (minOf(taxableOld, 500000.0) - 250000.0) * 0.05
                    t
                }
                CalcCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaxRow("Gross Income",             formatCrLakh(gross))
                        TaxRow("Standard Deduction",       "-₹50,000", Color(0xFF2E7D32))
                        TaxRow("80C + 80D + HRA",          "-${formatCrLakh(totalOldDed - stdOld)}", Color(0xFF2E7D32))
                        HorizontalDivider()
                        TaxRow("Taxable Income",           formatCrLakh(taxableOld), weight = FontWeight.SemiBold)
                        TaxRow("Income Tax",               formatCrLakh(baseOldTax))
                        TaxRow("Health & Edu Cess (4%)",   formatCrLakh(baseOldTax * 0.04))
                        HorizontalDivider()
                        TaxRow("Total Tax Payable",        formatCrLakh(oldTax), weight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp
    )
}

@Composable
private fun TaxRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface, weight: FontWeight = FontWeight.Normal) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = weight)
    }
}
