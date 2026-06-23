package com.example.wealthtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.XirrCalculator
import java.util.Locale

@Composable
fun ReturnsInsightsCard(
    investments: List<InvestmentEntity>,
    modifier: Modifier = Modifier
) {
    if (investments.isEmpty()) return

    val fdsWithRate = investments.filter { it.investmentType == "FD" && it.fdRate != null && it.fdRate > 0 }
    val totalFdAmount = fdsWithRate.sumOf { it.amount }
    val weightedFdRate = if (fdsWithRate.isNotEmpty() && totalFdAmount > 0) {
        XirrCalculator.weightedAvgRate(fdsWithRate.map { it.amount to (it.fdRate ?: 0.0) })
    } else null

    val realReturnPct = weightedFdRate?.let { rate ->
        XirrCalculator.realReturn(rate / 100.0) * 100.0
    }

    var benchmarksExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Returns & Benchmarks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (weightedFdRate == null) {
                Text(
                    "Add FD investments with interest rate to see returns analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricChip(
                        label = "Your FD Rate",
                        value = String.format(Locale.ENGLISH, "%.1f%%", weightedFdRate),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (realReturnPct != null) {
                        MetricChip(
                            label = "Real Return",
                            value = String.format(Locale.ENGLISH, "%.1f%%", realReturnPct),
                            color = if (realReturnPct >= 0) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                TextButton(
                    onClick = { benchmarksExpanded = !benchmarksExpanded },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(if (benchmarksExpanded) "Hide benchmarks" else "Show benchmarks")
                }

                if (benchmarksExpanded) {
                    val benchmarks = listOf(
                        "Nifty 50 (10Y CAGR)" to 14.0,
                        "Sensex (10Y CAGR)" to 13.5,
                        "PPF (current)" to 7.1,
                        "Avg FD (SBI/HDFC)" to 7.0,
                        "Inflation (CPI)" to 6.0,
                        "Your FD Rate" to weightedFdRate
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        benchmarks.forEach { (label, bRate) ->
                            BenchmarkRow(
                                label = label,
                                rate = bRate,
                                userRate = weightedFdRate,
                                isUserEntry = label == "Your FD Rate"
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Benchmark data is indicative. Historical returns don't guarantee future performance.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BenchmarkRow(
    label: String,
    rate: Double,
    userRate: Double,
    isUserEntry: Boolean
) {
    val beats = !isUserEntry && userRate > rate
    val lags = !isUserEntry && userRate <= rate
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            fontWeight = if (isUserEntry) FontWeight.Bold else FontWeight.Normal
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                String.format(Locale.ENGLISH, "%.1f%%", rate),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (!isUserEntry) {
                Surface(
                    color = if (beats) Color(0xFF22C55E).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (beats) "beat" else "lag",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (beats) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
