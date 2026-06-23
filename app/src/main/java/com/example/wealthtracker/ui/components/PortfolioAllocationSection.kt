package com.example.wealthtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.charts.SafePieChart
import com.example.wealthtracker.util.FormatUtils

@Composable
fun PortfolioAllocationSection(
    investments: List<InvestmentEntity>,
    modifier: Modifier = Modifier
) {
    if (investments.isEmpty()) return

    val grouped = investments
        .groupBy { if (it.investmentType == "Equity") "Stocks" else it.investmentType }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .filter { it.value > 0 }
        .toList()
        .sortedByDescending { it.second }

    if (grouped.isEmpty()) return

    val palette = listOf(
        Color(0xFF6366F1),
        Color(0xFF22C55E),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFFEC4899),
        Color(0xFF8B5CF6),
        Color(0xFF14B8A6),
        Color(0xFFF97316),
        Color(0xFF64748B)
    )

    val totalAmount = grouped.sumOf { it.second }
    val centerText = FormatUtils.formatINRShort(totalAmount)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Asset Allocation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            SafePieChart(
                data = grouped,
                colors = palette,
                modifier = Modifier.fillMaxWidth(),
                showLegend = true,
                animationEnabled = true,
                centerText = centerText,
                onSliceClick = null
            )
        }
    }
}
