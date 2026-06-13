package com.example.wealthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.FormatUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class InsightCard(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val trend: String? = null,
    val isPositive: Boolean? = null
)

@Composable
fun EnhancedInsights(
    investments: List<InvestmentEntity>,
    currentFilter: String? = null,
    modifier: Modifier = Modifier
) {
    val insights = remember(investments, currentFilter) {
        generateInsights(investments, currentFilter)
    }
    
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Smart Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(insights) { insight ->
                InsightCardItem(insight = insight)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Additional detailed insights
        DetailedInsights(investments = investments, currentFilter = currentFilter)
    }
}

@Composable
private fun InsightCardItem(
    insight: InsightCard,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .widthIn(min = 140.dp, max = 180.dp)
            .wrapContentWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDarkTheme) {
                    insight.color.copy(alpha = 0.10f)
                } else {
                    insight.color.copy(alpha = 0.04f)
                }
            )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = null,
                tint = insight.color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = insight.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = insight.color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Text(
                text = insight.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            insight.trend?.let { trend ->
                Text(
                    text = trend,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (insight.isPositive == true) {
                        Color(0xFF4CAF50)
                    } else if (insight.isPositive == false) {
                        Color(0xFFFF9800)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailedInsights(
    investments: List<InvestmentEntity>,
    currentFilter: String? = null,
    modifier: Modifier = Modifier
) {
    val detailedInsights = remember(investments, currentFilter) {
        generateDetailedInsights(investments, currentFilter)
    }
    
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Portfolio Analysis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        detailedInsights.forEach { insight ->
            DetailedInsightItem(insight = insight)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailedInsightItem(
    insight: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDarkTheme) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = if (isDarkTheme) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

private fun generateInsights(investments: List<InvestmentEntity>, currentFilter: String? = null): List<InsightCard> {
    // Filter investments based on current filter
    val filteredInvestments = if (currentFilter.isNullOrBlank() || currentFilter == "All") {
        investments
    } else {
        investments.filter { 
            when (currentFilter) {
                "Stocks" -> it.investmentType == "Equity" || it.investmentType == "Stocks"
                else -> it.investmentType == currentFilter
            }
        }
    }
    if (filteredInvestments.isEmpty()) {
        val emptyMessage = if (currentFilter.isNullOrBlank() || currentFilter == "All") {
            "Add your first investment"
        } else {
            "No $currentFilter investments yet"
        }
        return listOf(
            InsightCard(
                title = if (currentFilter.isNullOrBlank() || currentFilter == "All") "Start Investing" else "Add $currentFilter",
                value = "₹0",
                subtitle = emptyMessage,
                icon = Icons.Default.Add,
                color = Color(0xFF2196F3)
            )
        )
    }
    
    val totalAmount = filteredInvestments.sumOf { it.amount }
    val investmentsByType = filteredInvestments.groupBy { it.investmentType }
    val avgInvestment = totalAmount / filteredInvestments.size
    
    // Calculate portfolio performance metrics using filtered data
    val fdInvestments = filteredInvestments.filter { it.investmentType == "Fixed Deposit" }
    val mfInvestments = filteredInvestments.filter { it.investmentType == "Mutual Fund" }
    val equityInvestments = filteredInvestments.filter { it.investmentType == "Equity" || it.investmentType == "Stocks" }
    val goldInvestments = filteredInvestments.filter { it.investmentType == "Gold" }
    
    // Risk assessment
    val highRiskAmount = equityInvestments.sumOf { it.amount }
    val mediumRiskAmount = mfInvestments.sumOf { it.amount }
    val lowRiskAmount = fdInvestments.sumOf { it.amount } + 
                       filteredInvestments.filter { it.investmentType == "PPF" }.sumOf { it.amount }
    
    val riskProfile = when {
        highRiskAmount > totalAmount * 0.6 -> "Aggressive"
        highRiskAmount > totalAmount * 0.4 -> "Moderate-Aggressive"
        mediumRiskAmount > totalAmount * 0.4 -> "Moderate"
        lowRiskAmount > totalAmount * 0.7 -> "Conservative"
        else -> "Balanced"
    }
    
    // Tax efficiency analysis
    val taxSavingAmount = filteredInvestments.filter { 
        it.investmentType in listOf("PPF", "NPS", "ELSS", "Tax Saving FD")
    }.sumOf { it.amount }
    
    val taxEfficiencyScore = (taxSavingAmount / totalAmount * 100).toInt()
    
    // Calculate diversification score
    val diversificationScore = when {
        investmentsByType.size >= 6 -> "Excellent"
        investmentsByType.size >= 4 -> "Good"
        investmentsByType.size >= 3 -> "Fair"
        else -> "Poor"
    }
    
    // Add filter-specific insights
    val filterSpecificTitle = if (currentFilter.isNullOrBlank() || currentFilter == "All") {
        "Portfolio Value"
    } else {
        "$currentFilter Value"
    }
    
    // Find largest investment type
    val largestType = investmentsByType.maxByOrNull { it.value.sumOf { inv -> inv.amount } }
    val largestTypePercentage = largestType?.let { 
        (it.value.sumOf { inv -> inv.amount } / totalAmount * 100).toInt()
    } ?: 0
    
    // Calculate recent activity (last 30 days)
    val thirtyDaysAgo = LocalDate.now().minusDays(30)
    val recentInvestments = investments.filter { investment ->
        try {
            val investmentDate = java.time.Instant.ofEpochMilli(investment.createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            investmentDate.isAfter(thirtyDaysAgo)
        } catch (e: Exception) {
            false
        }
    }
    
    // Calculate maturity insights for FDs (using existing fdInvestments variable)
    val maturingFDs = fdInvestments.filter { investment ->
        investment.fdMaturityDate?.let { maturityDate ->
            try {
                val maturity = LocalDate.parse(maturityDate)
                val daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), maturity)
                daysToMaturity in 1..90 // Maturing in next 90 days
            } catch (e: Exception) {
                false
            }
        } ?: false
    }
    
    val insights = mutableListOf<InsightCard>()
    
    // Portfolio/Filter Value
    insights.add(
        InsightCard(
            title = filterSpecificTitle,
            value = FormatUtils.formatINRShort(totalAmount),
            subtitle = "${filteredInvestments.size} investments",
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF4CAF50)
        )
    )
    
    // Filter-aware second card
    if (currentFilter.isNullOrBlank() || currentFilter == "All") {
        // Show diversification for all investments
        insights.add(
            InsightCard(
                title = "Diversification",
                value = diversificationScore,
                subtitle = "${investmentsByType.size} asset types",
                icon = Icons.Default.PieChart,
                color = Color(0xFF9C27B0)
            )
        )
    } else {
        // Show filter-specific rich metrics
        when (currentFilter) {
            "Fixed Deposit" -> {
                val avgFDRate = fdInvestments.mapNotNull { it.fdRate }.average()
                insights.add(
                    InsightCard(
                        title = "Avg FD Rate",
                        value = if (avgFDRate > 0) "${String.format("%.1f", avgFDRate)}%" else "N/A",
                        subtitle = "Interest rate",
                        icon = Icons.Default.Percent,
                        color = Color(0xFF4CAF50)
                    )
                )
            }
            "Mutual Fund" -> {
                insights.add(
                    InsightCard(
                        title = "MF Portfolio",
                        value = "${filteredInvestments.size}",
                        subtitle = "Fund diversity",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF2196F3)
                    )
                )
            }
            "Stocks", "Equity" -> {
                val equityPercent = (totalAmount / investments.sumOf { it.amount } * 100).toInt()
                insights.add(
                    InsightCard(
                        title = "Equity Weight",
                        value = "${equityPercent}%",
                        subtitle = "Of total portfolio",
                        icon = Icons.Default.ShowChart,
                        color = Color(0xFFF44336)
                    )
                )
            }
            "PPF" -> {
                insights.add(
                    InsightCard(
                        title = "Tax Savings",
                        value = FormatUtils.formatINRShort(totalAmount),
                        subtitle = "80C benefit",
                        icon = Icons.Default.AccountBalance,
                        color = Color(0xFF4CAF50)
                    )
                )
            }
            "Gold" -> {
                val goldPercent = (totalAmount / investments.sumOf { it.amount } * 100).toInt()
                insights.add(
                    InsightCard(
                        title = "Gold Weight",
                        value = "${goldPercent}%",
                        subtitle = "Inflation hedge",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFFD700)
                    )
                )
            }
            else -> {
                insights.add(
                    InsightCard(
                        title = "$currentFilter Count",
                        value = "${filteredInvestments.size}",
                        subtitle = "investments",
                        icon = Icons.Default.Tag,
                        color = Color(0xFF9C27B0)
                    )
                )
            }
        }
    }
    
    // Filter-aware third card
    if (currentFilter.isNullOrBlank() || currentFilter == "All") {
        // Show average investment for all
        insights.add(
            InsightCard(
                title = "Avg Investment",
                value = FormatUtils.formatINRShort(avgInvestment),
                subtitle = "Per investment",
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF2196F3)
            )
        )
    } else {
        // Show filter-specific third metric
        when (currentFilter) {
            "Fixed Deposit" -> {
                val maturingCount = fdInvestments.count { investment ->
                    investment.fdMaturityDate?.let { maturityDate ->
                        try {
                            val maturity = LocalDate.parse(maturityDate)
                            val daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), maturity)
                            daysToMaturity in 1..90
                        } catch (e: Exception) {
                            false
                        }
                    } ?: false
                }
                insights.add(
                    InsightCard(
                        title = "Maturing Soon",
                        value = "$maturingCount",
                        subtitle = "Next 90 days",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFF9800)
                    )
                )
            }
            "Mutual Fund" -> {
                insights.add(
                    InsightCard(
                        title = "Avg MF Size",
                        value = FormatUtils.formatINRShort(avgInvestment),
                        subtitle = "Per fund",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF2196F3)
                    )
                )
            }
            "Stocks", "Equity" -> {
                insights.add(
                    InsightCard(
                        title = "Risk Level",
                        value = "High",
                        subtitle = "Growth focused",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFF44336)
                    )
                )
            }
            "PPF" -> {
                val currentYear = LocalDate.now().year
                val ppfMaturityYear = currentYear + 15 // PPF has 15-year lock-in
                insights.add(
                    InsightCard(
                        title = "Maturity Year",
                        value = "$ppfMaturityYear",
                        subtitle = "15-year lock-in",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFF4CAF50)
                    )
                )
            }
            "Gold" -> {
                insights.add(
                    InsightCard(
                        title = "Hedge Value",
                        value = FormatUtils.formatINRShort(avgInvestment),
                        subtitle = "Avg holding",
                        icon = Icons.Default.Security,
                        color = Color(0xFFFFD700)
                    )
                )
            }
            else -> {
                insights.add(
                    InsightCard(
                        title = "Avg $currentFilter",
                        value = FormatUtils.formatINRShort(avgInvestment),
                        subtitle = "Per investment",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF2196F3)
                    )
                )
            }
        }
    }
    
    // Risk Profile
    insights.add(
        InsightCard(
            title = "Risk Profile",
            value = riskProfile,
            subtitle = "Investment style",
            icon = Icons.Default.TrendingUp,
            color = when (riskProfile) {
                "Aggressive" -> Color(0xFFF44336)
                "Moderate-Aggressive" -> Color(0xFFFF9800)
                "Moderate" -> Color(0xFF2196F3)
                "Conservative" -> Color(0xFF4CAF50)
                else -> Color(0xFF9C27B0)
            },
            trend = when (riskProfile) {
                "Aggressive" -> "High growth potential"
                "Conservative" -> "Low risk approach"
                else -> "Balanced strategy"
            },
            isPositive = true
        )
    )
    
    // Tax Efficiency
    if (taxEfficiencyScore > 0) {
        insights.add(
            InsightCard(
                title = "Tax Efficiency",
                value = "$taxEfficiencyScore%",
                subtitle = "Tax-saving investments",
                icon = Icons.Default.AccountBalance,
                color = Color(0xFF4CAF50),
                trend = when {
                    taxEfficiencyScore >= 20 -> "Excellent tax planning"
                    taxEfficiencyScore >= 10 -> "Good tax savings"
                    else -> "Consider tax-saving options"
                },
                isPositive = taxEfficiencyScore >= 10
            )
        )
    }
    
    // Largest Allocation
    if (largestType != null) {
        insights.add(
            InsightCard(
                title = "Top Allocation",
                value = "$largestTypePercentage%",
                subtitle = largestType.key,
                icon = Icons.Default.Star,
                color = Color(0xFF9C27B0),
                trend = if (largestTypePercentage > 50) "High concentration" else "Well balanced",
                isPositive = largestTypePercentage <= 50
            )
        )
    }
    
    // Liquidity Analysis
    val liquidAmount = investments.filter { 
        it.investmentType in listOf("Savings", "Liquid Fund", "Money Market")
    }.sumOf { it.amount }
    val liquidityRatio = (liquidAmount / totalAmount * 100).toInt()
    
    if (liquidityRatio > 0 || totalAmount > 50000) {
        insights.add(
            InsightCard(
                title = "Liquidity",
                value = if (liquidityRatio > 0) "$liquidityRatio%" else "Low",
                subtitle = "Emergency funds",
                icon = Icons.Default.Savings,
                color = Color(0xFF00BCD4),
                trend = when {
                    liquidityRatio >= 10 -> "Good emergency fund"
                    liquidityRatio >= 5 -> "Adequate liquidity"
                    else -> "Consider emergency fund"
                },
                isPositive = liquidityRatio >= 5
            )
        )
    }
    
    // Recent Activity
    if (recentInvestments.isNotEmpty()) {
        val recentAmount = recentInvestments.sumOf { it.amount }
        insights.add(
            InsightCard(
                title = "Recent Activity",
                value = FormatUtils.formatINRShort(recentAmount),
                subtitle = "Last 30 days",
                icon = Icons.Default.Schedule,
                color = Color(0xFF00BCD4),
                trend = "${recentInvestments.size} new investments",
                isPositive = true
            )
        )
    }
    
    // Maturity Alert
    if (maturingFDs.isNotEmpty()) {
        insights.add(
            InsightCard(
                title = "Maturing Soon",
                value = "${maturingFDs.size}",
                subtitle = "FDs in 90 days",
                icon = Icons.Default.Schedule,
                color = Color(0xFFFF9800),
                trend = "Action needed",
                isPositive = false
            )
        )
    }
    
    return insights
}

private fun generateDetailedInsights(investments: List<InvestmentEntity>, currentFilter: String? = null): List<String> {
    // Filter investments based on current filter
    val filteredInvestments = if (currentFilter.isNullOrBlank() || currentFilter == "All") {
        investments
    } else {
        investments.filter { 
            when (currentFilter) {
                "Stocks" -> it.investmentType == "Equity" || it.investmentType == "Stocks"
                else -> it.investmentType == currentFilter
            }
        }
    }
    if (filteredInvestments.isEmpty()) {
        val filterMessage = if (currentFilter.isNullOrBlank() || currentFilter == "All") {
            listOf(
                "🚀 Start your investment journey by adding your first investment",
                "📊 Consider diversifying across different asset classes for better risk management",
                "💰 Regular investments can help you build wealth over time",
                "🎯 Set clear financial goals to guide your investment strategy"
            )
        } else {
            listOf(
                "📊 No $currentFilter investments found in your portfolio",
                "💡 Consider adding $currentFilter investments for better diversification",
                "🎯 $currentFilter can be a valuable addition to your investment strategy",
                "📈 Explore $currentFilter options that match your risk profile"
            )
        }
        return filterMessage
    }
    
    val insights = mutableListOf<String>()
    val totalAmount = filteredInvestments.sumOf { it.amount }
    val investmentsByType = filteredInvestments.groupBy { it.investmentType }
    
    // Advanced portfolio analysis
    val fdInvestments = investments.filter { it.investmentType == "Fixed Deposit" }
    val mfInvestments = investments.filter { it.investmentType == "Mutual Fund" }
    val equityInvestments = investments.filter { it.investmentType == "Equity" || it.investmentType == "Stocks" }
    val ppfInvestments = investments.filter { it.investmentType == "PPF" }
    val npsInvestments = investments.filter { it.investmentType == "NPS" }
    
    // Risk-return analysis
    val highRiskAmount = equityInvestments.sumOf { it.amount }
    val mediumRiskAmount = mfInvestments.sumOf { it.amount }
    val lowRiskAmount = fdInvestments.sumOf { it.amount } + ppfInvestments.sumOf { it.amount }
    
    val highRiskPercent = (highRiskAmount / totalAmount * 100).toInt()
    val mediumRiskPercent = (mediumRiskAmount / totalAmount * 100).toInt()
    val lowRiskPercent = (lowRiskAmount / totalAmount * 100).toInt()
    
    // Risk profile analysis
    when {
        highRiskPercent > 60 -> insights.add("⚡ Aggressive portfolio with ${highRiskPercent}% in high-risk assets. Great for long-term growth!")
        highRiskPercent > 40 -> insights.add("📈 Moderate-aggressive approach with ${highRiskPercent}% in equities. Good growth potential.")
        mediumRiskPercent > 40 -> insights.add("⚖️ Balanced portfolio with ${mediumRiskPercent}% in mutual funds. Steady growth strategy.")
        lowRiskPercent > 70 -> insights.add("🛡️ Conservative approach with ${lowRiskPercent}% in safe assets. Consider some growth investments.")
        else -> insights.add("🎯 Well-balanced risk distribution across asset classes.")
    }
    
    // Diversification insights with emojis
    when (investmentsByType.size) {
        1 -> insights.add("🌱 Single asset type detected. Add mutual funds or stocks for better diversification")
        2 -> insights.add("📊 Good start with 2 asset types! Consider adding 1-2 more for optimal diversification")
        in 3..4 -> insights.add("✅ Well diversified portfolio across ${investmentsByType.size} asset types")
        in 5..6 -> insights.add("🏆 Excellent diversification with ${investmentsByType.size} different asset types")
        else -> insights.add("🌟 Outstanding diversification! You're a pro investor with ${investmentsByType.size} asset types")
    }
    
    // Concentration risk with detailed analysis
    val largestAllocation = investmentsByType.maxByOrNull { it.value.sumOf { inv -> inv.amount } }
    largestAllocation?.let { (type, investments) ->
        val percentage = (investments.sumOf { it.amount } / totalAmount * 100).toInt()
        when {
            percentage > 70 -> insights.add("⚠️ High concentration: ${percentage}% in $type. Consider rebalancing to reduce risk")
            percentage > 50 -> insights.add("📊 $type dominates ${percentage}% of portfolio. Monitor for rebalancing opportunities")
            percentage > 30 -> insights.add("👍 $type represents ${percentage}% - healthy allocation within limits")
            else -> insights.add("✨ No concentration risk detected. Excellent portfolio balance!")
        }
    }
    
    // Tax efficiency insights
    val taxSavingInvestments = investments.filter { 
        it.investmentType in listOf("PPF", "NPS", "ELSS", "Tax Saving FD")
    }
    val taxSavingAmount = taxSavingInvestments.sumOf { it.amount }
    val taxSavingPercent = (taxSavingAmount / totalAmount * 100).toInt()
    
    when {
        taxSavingPercent >= 25 -> insights.add("💰 Excellent tax planning with ${taxSavingPercent}% in tax-saving investments")
        taxSavingPercent >= 15 -> insights.add("📋 Good tax efficiency with ${taxSavingPercent}% in tax-saving options")
        taxSavingPercent >= 5 -> insights.add("🎯 Consider increasing tax-saving investments from current ${taxSavingPercent}%")
        else -> insights.add("💡 Add PPF, NPS, or ELSS for tax benefits and long-term wealth creation")
    }
    
    // Age-based recommendations (assuming based on portfolio composition)
    val retirementFocused = ppfInvestments.isNotEmpty() || npsInvestments.isNotEmpty()
    val growthFocused = equityInvestments.isNotEmpty() || mfInvestments.isNotEmpty()
    
    when {
        retirementFocused && growthFocused -> insights.add("🎯 Perfect balance of growth and retirement planning. You're on track!")
        retirementFocused && !growthFocused -> insights.add("🔄 Great retirement planning! Consider adding some growth investments for inflation protection")
        !retirementFocused && growthFocused -> insights.add("📈 Strong growth focus! Consider adding PPF/NPS for long-term retirement planning")
        else -> insights.add("🚀 Consider both growth investments and retirement planning for a complete strategy")
    }
    
    // FD specific insights with advanced analysis (using existing fdInvestments variable)
    if (fdInvestments.isNotEmpty()) {
        val avgFDRate = fdInvestments.mapNotNull { it.fdRate }.average()
        if (avgFDRate > 0) {
            when {
                avgFDRate >= 7.5 -> insights.add("🏆 Excellent FD rates averaging ${String.format("%.1f", avgFDRate)}%! You've secured great returns")
                avgFDRate >= 6.5 -> insights.add("👍 Good FD rates averaging ${String.format("%.1f", avgFDRate)}%. Consider laddering for better liquidity")
                else -> insights.add("💡 FD rates averaging ${String.format("%.1f", avgFDRate)}%. Explore high-yield options for better returns")
            }
        }
        
        // Maturity ladder analysis
        val maturingCount = fdInvestments.count { investment ->
            investment.fdMaturityDate?.let { maturityDate ->
                try {
                    val maturity = LocalDate.parse(maturityDate)
                    val daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), maturity)
                    daysToMaturity in 1..90
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
        
        when {
            maturingCount > 2 -> insights.add("⏰ ${maturingCount} FDs maturing soon! Perfect time to rebalance your portfolio")
            maturingCount > 0 -> insights.add("📅 ${maturingCount} FD(s) maturing in 90 days. Plan reinvestment strategy now")
            fdInvestments.size > 3 -> insights.add("🔄 Consider FD laddering strategy for better liquidity and rate optimization")
        }
    }
    
    // Portfolio size and milestone insights
    when {
        totalAmount >= 10000000 -> insights.add("🏆 Congratulations! ₹1 Crore+ portfolio. Consider wealth management services")
        totalAmount >= 5000000 -> insights.add("🌟 Outstanding! ₹50L+ portfolio. Focus on tax-efficient wealth preservation")
        totalAmount >= 2000000 -> insights.add("💎 Excellent progress! ₹20L+ portfolio. Optimize for long-term growth")
        totalAmount >= 1000000 -> insights.add("🎯 Great milestone! ₹10L+ portfolio. Consider diversifying into international funds")
        totalAmount >= 500000 -> insights.add("📈 Strong foundation! ₹5L+ portfolio. Time to explore advanced investment options")
        totalAmount >= 100000 -> insights.add("🚀 Good start! ₹1L+ portfolio. Consider systematic investment plans (SIPs)")
        else -> insights.add("🌱 Building wealth takes time. Increase investment frequency to reach ₹1L faster")
    }
    
    // Investment frequency and discipline insights
    val investmentCount = investments.size
    val avgInvestmentSize = totalAmount / investmentCount
    
    when {
        investmentCount >= 20 -> insights.add("🏅 Highly disciplined investor with ${investmentCount} investments! Excellent commitment")
        investmentCount >= 10 -> insights.add("👏 Good investment discipline with ${investmentCount} investments. Keep it up!")
        investmentCount >= 5 -> insights.add("📊 Decent investment frequency. Consider monthly SIPs for better compounding")
        else -> insights.add("⚡ Start investing regularly. Small, consistent investments create big wealth over time")
    }
    
    // Smart rebalancing suggestions
    val oldestInvestment = investments.minByOrNull { it.createdAt }
    oldestInvestment?.let { oldest ->
        val daysSinceOldest = ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(oldest.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now()
        )
        
        when {
            daysSinceOldest > 365 -> insights.add("🔄 Portfolio is ${daysSinceOldest/365} year(s) old. Consider annual rebalancing review")
            daysSinceOldest > 180 -> insights.add("📊 6+ months of investing! Time to review and rebalance if needed")
            else -> insights.add("🌱 New investor journey! Stay consistent and review quarterly")
        }
    }
    
    return insights.take(6) // Show more insights for richer experience
}
