package com.example.wealthtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.ui.charts.SafePieChart

/**
 * Enhanced Dashboard Integration Example
 * 
 * This shows how to integrate all the new components:
 * - Enhanced Insights with smart analytics
 * - Modern Charts with Vico library
 * - Themed Icons with proper dark/light mode support
 * - Rich widget with mini charts
 */

@Composable
fun EnhancedDashboardExample(
    investments: List<InvestmentEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Enhanced Insights Section
            EnhancedInsights(investments = investments)
        }
        
        item {
            // Modern Chart Section
            if (investments.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Portfolio Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        val chartData = investments
                            .groupBy { it.investmentType }
                            .map { (type, invList) ->
                                type to invList.sumOf { it.amount }
                            }
                        
                        // Define colors for different investment types
                        val chartColors = listOf(
                            Color(0xFF6750A4), // Purple
                            Color(0xFF7D5260), // Pink
                            Color(0xFF625B71), // Gray
                            Color(0xFF0B57D0), // Blue
                            Color(0xFF006A6A), // Teal
                            Color(0xFF984061)  // Rose
                        )
                        
                        SafePieChart(
                            data = chartData,
                            colors = chartColors,
                            modifier = Modifier.height(250.dp),
                            showLegend = true,
                            animationEnabled = true,
                            centerText = "Portfolio\nDistribution"
                        )
                    }
                }
            }
        }
        
        item {
            // Performance Trends (if we had historical data)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Investment Trends",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Placeholder for performance trends
                    // In a real app, you would display historical performance here
                    Text(
                        text = "Performance trend chart would go here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Usage Instructions for Integration:
 * 
 * 1. Widget Enhancement:
 *    - The enhanced widget now shows mini bar charts for top 3 investment types
 *    - Displays diversification insights
 *    - Shows last updated time
 *    - Provides actionable portfolio advice
 * 
 * 2. Dashboard Insights:
 *    - Smart analytics cards showing key metrics
 *    - Diversification scoring
 *    - Maturity alerts for FDs
 *    - Recent activity tracking
 *    - Detailed portfolio analysis with actionable insights
 * 
 * 3. Modern Charts:
 *    - Vico library integration for better Compose compatibility
 *    - Enhanced pie charts with legends and animations
 *    - Column charts with rounded corners and gradients
 *    - Line charts with area fills and point markers
 *    - Automatic dark/light mode color adaptation
 * 
 * 4. Themed Icons:
 *    - ThemedIcon components that adapt to dark/light mode
 *    - Proper color contrast for accessibility
 *    - Consistent theming across all UI elements
 * 
 * To integrate into existing DashboardScreen:
 * 
 * ```kotlin
 * // In DashboardScreen.kt, replace the existing insights section with:
 * EnhancedInsights(investments = investments)
 * 
 * // Replace chart sections with:
 * SafePieChart(
 *     data = chartData,
 *     colors = chartColors,
 *     showLegend = true,
 *     animationEnabled = true,
 *     centerText = "Portfolio"
 * )
 * 
 * // Use themed icons throughout:
 * ThemedPrimaryIcon(
 *     imageVector = Icons.Default.TrendingUp,
 *     contentDescription = "Growth"
 * )
 * ```
 */
