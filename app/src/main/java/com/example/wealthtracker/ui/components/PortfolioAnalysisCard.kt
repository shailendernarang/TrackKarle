package com.example.wealthtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.PortfolioAnalyzer

@Composable
fun PortfolioAnalysisCard(
    investments: List<InvestmentEntity>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val analysis = remember(investments) {
        PortfolioAnalyzer.analyzePortfolio(investments)
    }
    
    if (investments.isEmpty()) {
        return // Don't show for empty portfolio
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Portfolio Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Rating Badge
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = getRatingColor(analysis.overallRating),
                    tonalElevation = 0.dp
                ) {
                    Text(
                        analysis.overallRating,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Summary metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Diversity",
                    value = "${(analysis.diversificationScore * 100).toInt()}%",
                    icon = Icons.Default.PieChart
                )
                MetricItem(
                    label = "Risk",
                    value = analysis.riskLevel.split(" ").firstOrNull() ?: "N/A",
                    icon = Icons.Default.TrendingUp
                )
                MetricItem(
                    label = "Insights",
                    value = "${analysis.insights.size}",
                    icon = Icons.Default.Lightbulb
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Expand/Collapse button
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isExpanded) "Hide Details" else "View Insights")
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Insights
                    Text(
                        "Key Insights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    analysis.insights.take(5).forEach { insight ->
                        InsightRow(insight)
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // Recommendations
                    if (analysis.recommendations.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            "Recommendations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        analysis.recommendations.forEach { recommendation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    recommendation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightRow(insight: PortfolioAnalyzer.PortfolioInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on type
        Surface(
            shape = CircleShape,
            color = getInsightColor(insight.type).copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getInsightIcon(insight.type),
                    contentDescription = null,
                    tint = getInsightColor(insight.type),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                insight.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                insight.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getRatingColor(rating: String): Color {
    return when {
        rating.contains("Rich") -> Color(0xFF4CAF50) // Green
        rating.contains("Smart") -> Color(0xFF2196F3) // Blue
        rating.contains("Balanced") -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF9E9E9E) // Grey
    }
}

@Composable
private fun getInsightColor(type: PortfolioAnalyzer.InsightType): Color {
    return when (type) {
        PortfolioAnalyzer.InsightType.EXCELLENT -> Color(0xFF4CAF50) // Green
        PortfolioAnalyzer.InsightType.GOOD -> Color(0xFF2196F3) // Blue
        PortfolioAnalyzer.InsightType.WARNING -> Color(0xFFFF9800) // Orange
        PortfolioAnalyzer.InsightType.RISK -> Color(0xFFF44336) // Red
    }
}

@Composable
private fun getInsightIcon(type: PortfolioAnalyzer.InsightType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        PortfolioAnalyzer.InsightType.EXCELLENT -> Icons.Default.CheckCircle
        PortfolioAnalyzer.InsightType.GOOD -> Icons.Default.ThumbUp
        PortfolioAnalyzer.InsightType.WARNING -> Icons.Default.Warning
        PortfolioAnalyzer.InsightType.RISK -> Icons.Default.Error
    }
}
