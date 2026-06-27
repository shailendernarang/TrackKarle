package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.util.FormatUtils

private val RvBlue = Color(0xFF155DFC)
private val GradStart = Color(0xFF2563EB)
private val GradEnd = Color(0xFF7C3AED)
private val AmberWarningText = Color(0xFF92400E)
private val AmberWarningIcon = Color(0xFFF59E0B)

private fun emojiForType(type: String): String = when (type) {
    "FD" -> "🏦"; "Mutual Fund" -> "📈"; "PPF" -> "🏛️"; "NPS" -> "🏛️"
    "Gold", "SGB" -> "🪙"; "Health Insurance" -> "🏥"; "Term Insurance" -> "🛡️"
    "Stocks", "Equity" -> "📊"; "Real Estate" -> "🏠"; "Crypto" -> "₿"
    "EPF" -> "🏦"; "Bonds" -> "📜"; else -> "💼"
}

private fun bgForType(type: String): Color = when (type) {
    "FD" -> Color(0xFFEFF6FF); "Mutual Fund" -> Color(0xFFECFDF5)
    "PPF", "NPS", "EPF" -> Color(0xFFECFEFF); "Gold", "SGB" -> Color(0xFFFEF3C7)
    "Health Insurance", "Term Insurance" -> Color(0xFFFEF2F2)
    "Stocks", "Equity" -> Color(0xFFFFFBEB); "Real Estate" -> Color(0xFFF0FDF4)
    "Crypto" -> Color(0xFFFFF7ED); "Bonds" -> Color(0xFFF5F3FF); else -> Color(0xFFF5F3FF)
}

private fun categoryFor(type: String): String = when (type) {
    "FD" -> "Banking"; "Mutual Fund" -> "Mutual Funds"
    "PPF", "NPS", "EPF" -> "Retirement"; "Gold", "SGB" -> "Gold"
    "Health Insurance", "Term Insurance" -> "Insurance"
    "Stocks", "Equity" -> "Stocks & ETFs"; "Real Estate" -> "Real Estate"
    "Crypto" -> "Crypto"; "Bonds" -> "Bonds"; else -> "Alternative"
}

@Composable
private fun DetailRow(label: String, value: String) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val title = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = muted)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInvestmentScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val pending by viewModel.pendingInvestment.collectAsState()
    var confirmed by remember { mutableStateOf(false) }
    var cachedEntity by remember { mutableStateOf<InvestmentEntity?>(null) }

    LaunchedEffect(pending) {
        if (pending != null) {
            cachedEntity = pending
        } else if (!confirmed) {
            onBack()
        }
    }

    val entity = cachedEntity ?: return

    // Theme-aware semantic colors
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val titleColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val amberBg = if (isDark) Color(0xFF2A1800) else Color(0xFFFFFBEB)
    val amberBorder = if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
    val amberText = if (isDark) Color(0xFFFDE68A) else AmberWarningText

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text("Review Investment", style = MaterialTheme.typography.titleMedium)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
            )
        },
        containerColor = background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val invType = entity.investmentType ?: "Others"

            // Investment header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgForType(invType)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emojiForType(invType), style = MaterialTheme.typography.titleLarge)
                    }
                    Column {
                        Text(entity.type, style = MaterialTheme.typography.titleSmall, color = titleColor)
                        Text(categoryFor(invType), style = MaterialTheme.typography.bodySmall, color = mutedColor)
                    }
                }
            }

            // Amount card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Amount Invested", style = MaterialTheme.typography.bodySmall, color = mutedColor)
                    Text(
                        text = FormatUtils.formatCurrency(entity.amount),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = titleColor
                    )
                }
            }

            // Details card
            val details = buildList {
                when (invType) {
                    "FD" -> {
                        entity.bankName?.let { add("Institution" to it) }
                        entity.fdStartDate?.let { add("Start Date" to it) }
                        entity.fdMaturityDate?.let { add("Maturity Date" to it) }
                        entity.fdRate?.let { add("Interest Rate" to "${String.format("%.2f", it)}% p.a.") }
                    }
                    "Mutual Fund" -> {
                        entity.mfDate?.let { add("Purchase Date" to it) }
                    }
                    "PPF" -> {
                        entity.ppfFy?.let { add("Financial Year" to it) }
                        entity.ppfDate?.let { add("Maturity Date" to it) }
                    }
                    "NPS" -> {
                        entity.npsTier?.let { add("Tier" to it) }
                        entity.npsDate?.let { add("Date" to it) }
                    }
                    "Gold", "SGB" -> {
                        entity.goldType?.let { add("Gold Type" to it) }
                        entity.goldDate?.let { add("Purchase Date" to it) }
                    }
                    "Health Insurance", "Term Insurance" -> {
                        entity.hiPolicyName?.let { add("Policy Name" to it) }
                        entity.hiRenewalDate?.let { add("Renewal Date" to it) }
                    }
                    "Stocks", "Equity" -> {
                        entity.stockName?.let { add("Stock / Fund" to it) }
                        entity.stockDate?.let { add("Purchase Date" to it) }
                    }
                }
            }

            if (details.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "ALL DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = mutedColor,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        details.forEachIndexed { index, (label, value) ->
                            DetailRow(label = label, value = value)
                            if (index < details.lastIndex) {
                                HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // Warning note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(amberBg)
                    .border(1.dp, amberBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarningIcon,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Text(
                    "Please verify all details before confirming. Investment data cannot be edited after saving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = amberText
                )
            }

            Spacer(Modifier.height(4.dp))

            // Confirm & Save gradient button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(GradStart, GradEnd)))
                    .clickable {
                        confirmed = true
                        viewModel.confirmSavePending()
                        onConfirm()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Confirm & Save", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
            }

            // Edit Details text button
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = RvBlue, modifier = Modifier.size(16.dp))
                    Text("Edit Details", style = MaterialTheme.typography.titleSmall, color = RvBlue)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
