package com.example.wealthtracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.XirrCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MaturityCountdownSection(
    investments: List<InvestmentEntity>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val upcoming = investments
        .filter { it.investmentType == "FD" && !it.fdMaturityDate.isNullOrBlank() }
        .mapNotNull { inv ->
            val date = XirrCalculator.parseDate(inv.fdMaturityDate) ?: return@mapNotNull null
            val days = ChronoUnit.DAYS.between(today, date)
            if (days < 0) return@mapNotNull null
            Triple(inv, date, days)
        }
        .sortedBy { it.third }
        .take(5)

    if (upcoming.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Upcoming Maturities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            upcoming.forEach { (inv, maturityDate, days) ->
                MaturityCard(inv = inv, maturityDate = maturityDate, daysRemaining = days)
            }
        }
    }
}

@Composable
private fun MaturityCard(
    inv: InvestmentEntity,
    maturityDate: java.time.LocalDate,
    daysRemaining: Long
) {
    val badgeColor = when {
        daysRemaining < 15 -> MaterialTheme.colorScheme.error
        daysRemaining <= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }
    val badgeTextColor = when {
        daysRemaining < 15 -> MaterialTheme.colorScheme.onError
        else -> Color.White
    }

    val maturityAmount = if (inv.fdRate != null && inv.fdRate > 0 && inv.fdStartDate != null) {
        val start = XirrCalculator.parseDate(inv.fdStartDate)
        if (start != null) {
            val tenureYears = ChronoUnit.DAYS.between(start, maturityDate) / 365.0
            if (tenureYears > 0) inv.amount * Math.pow(1.0 + inv.fdRate / 100.0, tenureYears) else null
        } else null
    } else null

    val dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH)

    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                inv.bankName?.takeIf { it.isNotBlank() } ?: "FD",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                FormatUtils.formatINR(inv.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "In ${daysRemaining} days",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                maturityDate.format(dateFmt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (maturityAmount != null) {
                Text(
                    "≈ ${FormatUtils.formatINRShort(maturityAmount)} at maturity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
