package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.data.local.DebtEntity
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.DebtViewModel
import com.example.wealthtracker.ui.InvestmentViewModel
import androidx.compose.foundation.clickable
import com.example.wealthtracker.ui.components.AppBottomNavBar
import com.example.wealthtracker.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ── Colors ────────────────────────────────────────────────────────────────────

private val CardGradStart = Color(0xFF1E3A8A)
private val CardGradEnd   = Color(0xFF7C3AED)
private val GreenPos      = Color(0xFF22C55E)
private val RedNeg        = Color(0xFFEF4444)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun emojiForType(investmentType: String): String = when (investmentType) {
    "FD"                             -> "🏦"
    "Equity", "Stocks"               -> "📈"
    "Mutual Fund"                    -> "📊"
    "Gold"                           -> "🥇"
    "PPF", "EPF", "NPS"             -> "🏛️"
    "Term Insurance",
    "Health Insurance"               -> "🛡️"
    "Real Estate"                    -> "🏠"
    "Crypto"                         -> "₿"
    "GovernmentBond"                 -> "📜"
    else                             -> "💼"
}

private fun colorForType(investmentType: String): Color = when (investmentType) {
    "FD"                             -> Color(0xFFFFF7ED)
    "Equity", "Stocks"               -> Color(0xFFFFFBEB)
    "Mutual Fund"                    -> Color(0xFFEFF6FF)
    "Gold"                           -> Color(0xFFFEF9C3)
    "PPF", "EPF", "NPS"             -> Color(0xFFF0FDF4)
    "Term Insurance",
    "Health Insurance"               -> Color(0xFFFDF4FF)
    "Real Estate"                    -> Color(0xFFFFF1F2)
    "Crypto"                         -> Color(0xFFFFFBEB)
    else                             -> Color(0xFFF8FAFC)
}

private fun tagsForType(investmentType: String): List<String> = when (investmentType) {
    "FD"                             -> listOf("#safe", "#tax-saving")
    "Equity", "Stocks"               -> listOf("#high-risk")
    "Mutual Fund"                    -> listOf("#long-term")
    "Gold"                           -> listOf("#commodity")
    "PPF"                            -> listOf("#tax-saving", "#long-term")
    "EPF"                            -> listOf("#retirement")
    "NPS"                            -> listOf("#retirement", "#tax-saving")
    "Term Insurance",
    "Health Insurance"               -> listOf("#protection")
    "Real Estate"                    -> listOf("#illiquid")
    "Crypto"                         -> listOf("#high-risk", "#volatile")
    else                             -> listOf("#other")
}

private fun institutionLabel(e: InvestmentEntity): String {
    val name = when (e.investmentType) {
        "FD"                         -> e.bankName
        "Equity", "Stocks"           -> e.stockName
        "Mutual Fund"                -> e.type.takeIf { it.isNotBlank() }
        "PPF", "EPF", "NPS"         -> e.type.takeIf { it.isNotBlank() }
        "Term Insurance",
        "Health Insurance"           -> e.hiPolicyName ?: e.type
        "Gold"                       -> e.goldType ?: e.type
        else                         -> e.type.takeIf { it.isNotBlank() }
    }
    val date = when (e.investmentType) {
        "FD"           -> e.fdStartDate
        "Equity","Stocks" -> e.stockDate
        "Mutual Fund"  -> e.mfDate
        "Gold"         -> e.goldDate
        "PPF","EPF"    -> e.ppfDate
        "NPS"          -> e.npsDate
        "Term Insurance","Health Insurance" -> e.hiRenewalDate
        else           -> null
    }
    return listOfNotNull(name?.takeIf { it.isNotBlank() }, date?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifBlank { e.investmentType }
}

// Estimate current value from entity data
private fun estimatedCurrentValue(e: InvestmentEntity): Double {
    if (e.investmentType == "FD" && e.fdRate != null && e.fdRate > 0) {
        return e.amount * (1 + e.fdRate / 100)
    }
    return e.amount
}

// Progress bar fraction for investment cards
// - FD with start+maturity dates → time elapsed / total term
// - FD with rate only             → fdRate% as fraction (e.g. 6.9% → 0.069)
// - Everything else               → 0 (no price-tracking data)
private fun investmentProgress(e: InvestmentEntity): Float {
    val fdDateSdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    if (e.investmentType == "FD" &&
        !e.fdStartDate.isNullOrBlank() &&
        !e.fdMaturityDate.isNullOrBlank()
    ) {
        return try {
            val start = fdDateSdf.parse(e.fdStartDate!!) ?: return 0f
            val end   = fdDateSdf.parse(e.fdMaturityDate!!) ?: return 0f
            val now   = Date()
            if (end.time <= start.time) 0f
            else ((now.time - start.time).toFloat() / (end.time - start.time).toFloat()).coerceIn(0f, 1f)
        } catch (_: Exception) { 0f }
    }
    if (e.investmentType == "FD" && e.fdRate != null && e.fdRate > 0) {
        return (e.fdRate / 100.0).toFloat().coerceIn(0f, 1f)
    }
    return 0f
}

private fun formatShort(amount: Double): String = FormatUtils.formatINRShort(amount)

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: InvestmentViewModel,
    debtViewModel: DebtViewModel,
    onGoHome: () -> Unit,
    onGoCalculators: () -> Unit,
    onGoSettings: () -> Unit,
    onAddClick: () -> Unit,
) {
    val investments by viewModel.investments.collectAsState()
    val debts       by debtViewModel.debts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    var editingInvestment  by remember { mutableStateOf<InvestmentEntity?>(null) }
    var editingDebt        by remember { mutableStateOf<DebtEntity?>(null) }
    var deletingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var deletingDebt       by remember { mutableStateOf<DebtEntity?>(null) }

    // Net-worth math
    val totalInvested = investments.sumOf { it.amount }
    val totalCurrent  = investments.sumOf { estimatedCurrentValue(it) }
    val totalDebt     = debts.sumOf { it.outstandingBalance }
    val netWorth      = totalCurrent - totalDebt
    val totalReturn   = totalCurrent - totalInvested
    val returnPct     = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0
    val holdingCount  = investments.size + debts.size

    val tabLabels = listOf("All", "Investments", "Debts")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Portfolio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    Text(
                        "$holdingCount holdings",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            AppBottomNavBar(
                currentRoute = "portfolio",
                onGoHome = onGoHome,
                onGoPortfolio = {},
                onGoCalculators = onGoCalculators,
                onGoSettings = onGoSettings,
                onAddClick = onAddClick
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Net Worth card
            item {
                NetWorthCard(
                    netWorth       = netWorth,
                    totalReturn    = totalReturn,
                    returnPct      = returnPct,
                    totalInvested  = totalInvested,
                    totalCurrent   = totalCurrent,
                    totalDebt      = totalDebt,
                    modifier       = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Filter tabs
            item {
                FilterTabs(
                    tabs     = tabLabels,
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }

            // Investment items (tab 0 or 1)
            if (selectedTab != 2) {
                val visibleInvestments = investments
                if (visibleInvestments.isEmpty() && selectedTab == 1) {
                    item {
                        EmptyState(
                            message = "No investments yet",
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    items(visibleInvestments, key = { "inv_${it.id}" }) { entity ->
                        InvestmentCard(
                            entity  = entity,
                            onEdit  = { editingInvestment = entity },
                            onDelete = { deletingInvestment = entity },
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                        )
                    }
                }
            }

            // Debt items (tab 0 or 2)
            if (selectedTab != 1) {
                val visibleDebts = debts
                if (visibleDebts.isEmpty() && selectedTab == 2) {
                    item {
                        EmptyState(
                            message = "No debts tracked",
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    items(visibleDebts, key = { "debt_${it.id}" }) { debt ->
                        DebtCard(
                            debt     = debt,
                            onEdit   = { editingDebt = debt },
                            onDelete = { deletingDebt = debt },
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                        )
                    }
                }
            }

            // Empty all-tab state
            if (selectedTab == 0 && investments.isEmpty() && debts.isEmpty()) {
                item {
                    EmptyState(
                        message = "No investments or debts yet.\nTap + to add your first investment.",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }

    // ── Edit bottom sheet ─────────────────────────────────────────────────────
    editingInvestment?.let { entity ->
        EditInvestmentSheet(
            entity   = entity,
            onDismiss = { editingInvestment = null },
            onSave    = { updated ->
                viewModel.updateInvestment(updated)
                editingInvestment = null
            }
        )
    }

    // ── Delete investment confirmation ────────────────────────────────────────
    deletingInvestment?.let { entity ->
        DeleteConfirmDialog(
            title   = "Delete ${entity.investmentType}?",
            subtitle = institutionLabel(entity),
            amount  = entity.amount,
            onConfirm = {
                viewModel.delete(entity)
                deletingInvestment = null
            },
            onDismiss = { deletingInvestment = null }
        )
    }

    // ── Edit debt bottom sheet ────────────────────────────────────────────────
    editingDebt?.let { debt ->
        EditDebtSheet(
            debt      = debt,
            onDismiss = { editingDebt = null },
            onSave    = { updated ->
                debtViewModel.updateDebt(updated)
                editingDebt = null
            }
        )
    }

    // ── Delete debt confirmation ──────────────────────────────────────────────
    deletingDebt?.let { debt ->
        DeleteConfirmDialog(
            title   = "Delete ${debt.debtType}?",
            subtitle = debt.name,
            amount  = debt.outstandingBalance,
            onConfirm = {
                debtViewModel.delete(debt)
                deletingDebt = null
            },
            onDismiss = { deletingDebt = null }
        )
    }
}

// ── Net Worth Card ─────────────────────────────────────────────────────────────

@Composable
private fun NetWorthCard(
    netWorth: Double,
    totalReturn: Double,
    returnPct: Double,
    totalInvested: Double,
    totalCurrent: Double,
    totalDebt: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { if (!size.isEmpty()) drawRect(Brush.linearGradient(listOf(CardGradStart, CardGradEnd))) }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Net Worth",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Normal
            )
            Text(
                FormatUtils.formatINR(netWorth),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            // Returns row
            if (totalReturn != 0.0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isPos = totalReturn >= 0
                    Text(
                        text = "${if (isPos) "▲" else "▼"} ${FormatUtils.formatINR(abs(totalReturn))} " +
                                "(${FormatUtils.formatPercent(abs(returnPct))})",
                        fontSize = 13.sp,
                        color = if (isPos) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "overall returns",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCell("Invested",  formatShort(totalInvested), Modifier.weight(1f))
                StatCell("Current",   formatShort(totalCurrent),  Modifier.weight(1f))
                StatCell("Total Debt", formatShort(totalDebt),    Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ── Filter Tabs ───────────────────────────────────────────────────────────────

@Composable
private fun FilterTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected == index) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Investment Card ───────────────────────────────────────────────────────────

@Composable
private fun InvestmentCard(
    entity: InvestmentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = estimatedCurrentValue(entity)
    val gain         = currentValue - entity.amount
    val gainPct      = if (entity.amount > 0) (gain / entity.amount) * 100 else 0.0
    val isPositive   = gain >= 0
    val progress     = investmentProgress(entity)
    val tags         = tagsForType(entity.investmentType)
    val institution  = institutionLabel(entity)

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: icon + name/institution/tags + amount/pct
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Emoji icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorForType(entity.investmentType)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emojiForType(entity.investmentType), fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Name + institution + tags
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (entity.investmentType) {
                            "Equity", "Stocks" -> "Stock"
                            "Mutual Fund"      -> "SIP"
                            "FD"               -> "Fixed Deposit"
                            "PPF"              -> "PPF"
                            "NPS"              -> "NPS"
                            "EPF"              -> "EPF"
                            "Gold"             -> "Gold"
                            "Term Insurance"   -> "Term Insurance"
                            "Health Insurance" -> "Health Insurance"
                            "Crypto"           -> "Crypto"
                            "Real Estate"      -> "Real Estate"
                            else               -> entity.investmentType
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (institution.isNotBlank()) {
                        Text(
                            text = institution,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tag, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Amount + pct (right-aligned)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatShort(currentValue),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (gainPct != 0.0) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${FormatUtils.formatPercent(gainPct)}",
                            fontSize = 12.sp,
                            color = if (isPositive) GreenPos else RedNeg,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isPositive) GreenPos else RedNeg)
                )
            }

            // Invested + gain row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Invested: ${formatShort(entity.amount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (gain != 0.0) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${FormatUtils.formatINR(gain)}",
                        fontSize = 12.sp,
                        color = if (isPositive) GreenPos else RedNeg,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Edit + Delete row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onEdit,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick  = onDelete,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedNeg),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Debt Card ─────────────────────────────────────────────────────────────────

@Composable
private fun DebtCard(
    debt: DebtEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paidFraction = if (debt.principalAmount > 0)
        ((debt.principalAmount - debt.outstandingBalance) / debt.principalAmount).coerceIn(0.0, 1.0).toFloat()
    else 0f

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RedNeg.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💳", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        debt.debtType,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        debt.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RedNeg.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("#debt", fontSize = 11.sp, color = RedNeg)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatShort(debt.outstandingBalance),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RedNeg
                    )
                    Text(
                        "${FormatUtils.formatPercent(debt.interestRate)} p.a.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(paidFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(GreenPos)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Principal: ${formatShort(debt.principalAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "EMI: ${formatShort(debt.emiAmount)}/mo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = onEdit,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick  = onDelete,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = RedNeg),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Edit Investment Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditInvestmentSheet(
    entity: InvestmentEntity,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit
) {
    var amount   by remember { mutableStateOf(FormatUtils.formatAmountInput(entity.amount.toLong().toString())) }
    var bankName by remember { mutableStateOf(entity.bankName ?: "") }
    var fdRate   by remember { mutableStateOf(entity.fdRate?.toString() ?: "") }
    var stockName by remember { mutableStateOf(entity.stockName ?: entity.type) }
    var goldType by remember { mutableStateOf(entity.goldType ?: "") }
    var policyName by remember { mutableStateOf(entity.hiPolicyName ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit ${entity.investmentType}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Amount field (always shown)
            OutlinedTextField(
                value         = amount,
                onValueChange = { amount = FormatUtils.formatAmountInput(it) },
                label         = { Text("Amount Invested (${FormatUtils.getCurrencySymbol()})") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp)
            )

            // Type-specific fields
            when (entity.investmentType) {
                "FD" -> {
                    OutlinedTextField(
                        value = bankName, onValueChange = { bankName = it },
                        label = { Text("Institution") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fdRate, onValueChange = { fdRate = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Interest Rate (% p.a.)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                "Equity", "Stocks" -> {
                    OutlinedTextField(
                        value = stockName, onValueChange = { stockName = it },
                        label = { Text("Stock / Broker Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                "Gold" -> {
                    OutlinedTextField(
                        value = goldType, onValueChange = { goldType = it },
                        label = { Text("Gold Type (e.g. Physical, SGB)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                "Term Insurance", "Health Insurance" -> {
                    OutlinedTextField(
                        value = policyName, onValueChange = { policyName = it },
                        label = { Text("Policy Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                else -> {} // No extra fields for others
            }

            Button(
                onClick = {
                    val newAmount = FormatUtils.parseAmountInput(amount) ?: entity.amount
                    val updated = entity.copy(
                        amount     = newAmount,
                        bankName   = bankName.takeIf { it.isNotBlank() } ?: entity.bankName,
                        fdRate     = fdRate.toDoubleOrNull() ?: entity.fdRate,
                        stockName  = stockName.takeIf { it.isNotBlank() } ?: entity.stockName,
                        goldType   = goldType.takeIf { it.isNotBlank() } ?: entity.goldType,
                        hiPolicyName = policyName.takeIf { it.isNotBlank() } ?: entity.hiPolicyName
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Edit Debt Bottom Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDebtSheet(
    debt: DebtEntity,
    onDismiss: () -> Unit,
    onSave: (DebtEntity) -> Unit
) {
    var name         by remember { mutableStateOf(debt.name) }
    var outstanding  by remember { mutableStateOf(FormatUtils.formatAmountInput(debt.outstandingBalance.toLong().toString())) }
    var emi          by remember { mutableStateOf(FormatUtils.formatAmountInput(debt.emiAmount.toLong().toString())) }
    var interestRate by remember { mutableStateOf(debt.interestRate.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit ${debt.debtType}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name / Lender") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = outstanding,
                onValueChange = { outstanding = FormatUtils.formatAmountInput(it) },
                label = { Text("Outstanding Balance (${FormatUtils.getCurrencySymbol()})") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = emi,
                onValueChange = { emi = FormatUtils.formatAmountInput(it) },
                label = { Text("EMI Amount (${FormatUtils.getCurrencySymbol()})") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = interestRate,
                onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Interest Rate (% p.a.)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    onSave(
                        debt.copy(
                            name               = name.takeIf { it.isNotBlank() } ?: debt.name,
                            outstandingBalance = FormatUtils.parseAmountInput(outstanding) ?: debt.outstandingBalance,
                            emiAmount          = FormatUtils.parseAmountInput(emi) ?: debt.emiAmount,
                            interestRate       = interestRate.toDoubleOrNull() ?: debt.interestRate
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Delete Confirm Dialog ─────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    title: String,
    subtitle: String,
    amount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RedNeg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedNeg, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text  = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Text(FormatUtils.formatINR(amount), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = RedNeg),
                shape   = RoundedCornerShape(10.dp)
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ── Modifier extension for click without ripple ───────────────────────────────

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
