package com.example.wealthtracker.util

import com.example.wealthtracker.data.local.InvestmentEntity
import kotlin.math.sqrt

object PortfolioAnalyzer {
    
    data class PortfolioInsight(
        val title: String,
        val message: String,
        val type: InsightType,
        val score: Double // 0.0 to 1.0
    )
    
    enum class InsightType {
        EXCELLENT, GOOD, WARNING, RISK
    }
    
    data class PortfolioAnalysis(
        val overallRating: String, // Smart, Rich, Balanced, Needs Improvement
        val diversificationScore: Double,
        val riskLevel: String,
        val insights: List<PortfolioInsight>,
        val recommendations: List<String>
    )
    
    fun analyzePortfolio(investments: List<InvestmentEntity>): PortfolioAnalysis {
        if (investments.isEmpty()) {
            return PortfolioAnalysis(
                overallRating = "Start Your Journey",
                diversificationScore = 0.0,
                riskLevel = "N/A",
                insights = emptyList(),
                recommendations = listOf("Add your first investment to get personalized insights")
            )
        }
        
        val totalAmount = investments.sumOf { it.amount }
        val insights = mutableListOf<PortfolioInsight>()
        val recommendations = mutableListOf<String>()
        
        // 1. Diversification Analysis
        val typeDistribution = investments.groupBy { it.investmentType }
            .mapValues { it.value.sumOf { inv -> inv.amount } }
        
        val uniqueTypes = typeDistribution.size
        val diversificationScore = calculateDiversificationScore(typeDistribution, totalAmount)
        
        if (uniqueTypes == 1) {
            insights.add(PortfolioInsight(
                "Low Diversification",
                "All investments in ${typeDistribution.keys.first()}. Consider diversifying.",
                InsightType.WARNING,
                0.3
            ))
            recommendations.add("🎯 Add different types: Mutual Funds, FDs, or Gold for better balance")
        } else if (uniqueTypes >= 5) {
            insights.add(PortfolioInsight(
                "Well Diversified",
                "Invested across $uniqueTypes categories. Excellent!",
                InsightType.EXCELLENT,
                0.9
            ))
        }
        
        // 2. Equity vs Debt Balance
        val equityTypes = setOf("Stocks", "Equity", "Mutual Fund", "NPS")
        val debtTypes = setOf("FD", "PPF", "EPF")
        
        val equityAmount = investments.filter { it.investmentType in equityTypes }
            .sumOf { it.amount }
        val debtAmount = investments.filter { it.investmentType in debtTypes }
            .sumOf { it.amount }
        
        val equityPercent = if (totalAmount > 0) (equityAmount / totalAmount) * 100 else 0.0
        val debtPercent = if (totalAmount > 0) (debtAmount / totalAmount) * 100 else 0.0
        
        val riskLevel = when {
            equityPercent > 70 -> "High Risk"
            equityPercent > 40 -> "Moderate Risk"
            equityPercent > 15 -> "Low-Moderate Risk"
            else -> "Conservative"
        }
        
        when {
            equityPercent > 80 -> {
                insights.add(PortfolioInsight(
                    "High Equity Exposure",
                    "%.1f%% in equity. Market volatility may impact your portfolio significantly.".format(equityPercent),
                    InsightType.RISK,
                    0.4
                ))
                recommendations.add("⚖️ Consider adding FDs or PPF to balance risk")
            }
            equityPercent < 10 && totalAmount > 50000 -> {
                insights.add(PortfolioInsight(
                    "Low Growth Potential",
                    "Only %.1f%% in equity. Consider growth-oriented investments.".format(equityPercent),
                    InsightType.WARNING,
                    0.5
                ))
                recommendations.add("📈 Add Mutual Funds or Stocks for long-term wealth creation")
            }
            equityPercent in 40.0..70.0 -> {
                insights.add(PortfolioInsight(
                    "Balanced Portfolio",
                    "%.1f%% equity, %.1f%% debt. Good balance!".format(equityPercent, debtPercent),
                    InsightType.EXCELLENT,
                    0.85
                ))
            }
        }
        
        // 3. Insurance Coverage
        val hasHealthInsurance = investments.any { it.investmentType == "Health Insurance" }
        val hasTermInsurance = investments.any { it.investmentType == "Term Insurance" }
        
        if (!hasHealthInsurance && totalAmount > 100000) {
            insights.add(PortfolioInsight(
                "Missing Health Insurance",
                "No health insurance detected. Medical emergencies can drain savings.",
                InsightType.RISK,
                0.3
            ))
            recommendations.add("🏥 Add Health Insurance to protect your wealth")
        }
        
        if (!hasTermInsurance && totalAmount > 200000) {
            insights.add(PortfolioInsight(
                "No Life Cover",
                "Consider term insurance for family financial security.",
                InsightType.WARNING,
                0.4
            ))
            recommendations.add("🛡️ Get Term Insurance for comprehensive protection")
        }
        
        if (hasHealthInsurance && hasTermInsurance) {
            insights.add(PortfolioInsight(
                "Well Protected",
                "Health and term insurance in place. Smart planning!",
                InsightType.EXCELLENT,
                0.95
            ))
        }
        
        // 4. Gold Allocation
        val goldAmount = investments.filter { it.investmentType == "Gold" }
            .sumOf { it.amount }
        val goldPercent = if (totalAmount > 0) (goldAmount / totalAmount) * 100 else 0.0
        
        if (goldPercent > 15) {
            insights.add(PortfolioInsight(
                "High Gold Allocation",
                "%.1f%% in gold. Consider reallocating to growth assets.".format(goldPercent),
                InsightType.WARNING,
                0.5
            ))
        } else if (goldPercent in 5.0..15.0) {
            insights.add(PortfolioInsight(
                "Optimal Gold Allocation",
                "%.1f%% in gold provides good hedge against inflation.".format(goldPercent),
                InsightType.GOOD,
                0.75
            ))
        }
        
        // 5. Emergency Fund Check (via FD/Liquid funds)
        val liquidAmount = investments.filter { it.investmentType in setOf("FD", "EPF", "PPF") }
            .sumOf { it.amount }
        val monthlyExpenseEstimate = totalAmount * 0.05 // Rough estimate
        val emergencyFundMonths = if (monthlyExpenseEstimate > 0) 
            liquidAmount / monthlyExpenseEstimate else 0.0
        
        when {
            emergencyFundMonths < 3 -> {
                insights.add(PortfolioInsight(
                    "Low Emergency Fund",
                    "Build 6 months emergency fund in FD/liquid instruments.",
                    InsightType.WARNING,
                    0.4
                ))
                recommendations.add("💰 Build emergency fund: 6 months of expenses in FD")
            }
            emergencyFundMonths >= 6 -> {
                insights.add(PortfolioInsight(
                    "Strong Emergency Fund",
                    "Excellent safety net with %.0f months coverage.".format(emergencyFundMonths),
                    InsightType.EXCELLENT,
                    0.9
                ))
            }
        }
        
        // Overall Rating
        val avgScore = insights.map { it.score }.average()
        val overallRating = when {
            avgScore >= 0.8 -> "Rich 💎" // Wealthy portfolio
            avgScore >= 0.65 -> "Smart 🎯" // Smart investor
            avgScore >= 0.5 -> "Balanced ⚖️" // Balanced approach
            else -> "Growing 📚" // Room to grow
        }
        
        // General recommendations
        if (recommendations.isEmpty()) {
            recommendations.add("✨ Keep monitoring and rebalancing your portfolio quarterly")
            recommendations.add("📊 Review asset allocation based on your financial goals")
        }
        
        return PortfolioAnalysis(
            overallRating = overallRating,
            diversificationScore = diversificationScore,
            riskLevel = riskLevel,
            insights = insights.sortedByDescending { it.score },
            recommendations = recommendations
        )
    }
    
    private fun calculateDiversificationScore(
        distribution: Map<String, Double>,
        total: Double
    ): Double {
        if (distribution.isEmpty() || total == 0.0) return 0.0
        
        // Shannon Diversity Index adapted for finance
        val proportions = distribution.values.map { it / total }
        val entropy = -proportions.sumOf { p ->
            if (p > 0) p * kotlin.math.ln(p) else 0.0
        }
        
        val maxEntropy = kotlin.math.ln(distribution.size.toDouble())
        return if (maxEntropy > 0) entropy / maxEntropy else 0.0
    }
}
