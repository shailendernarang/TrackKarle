package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.ui.components.AppodealNativeNewsFeed
import com.example.wealthtracker.util.FormatUtils

private val SucGradStart = Color(0xFF2563EB)
private val SucGradEnd = Color(0xFF7C3AED)

private fun emojiForSuccessType(type: String): String = when (type) {
    "FD" -> "🏦"; "Mutual Fund" -> "📈"; "PPF" -> "🏛️"; "NPS" -> "🏛️"
    "Gold", "SGB" -> "🪙"; "Health Insurance" -> "🏥"; "Term Insurance" -> "🛡️"
    "Stocks", "Equity" -> "📊"; "Real Estate" -> "🏠"; "Crypto" -> "₿"
    "EPF" -> "🏦"; "Bonds" -> "📜"; else -> "💼"
}

private fun bgForSuccessType(type: String): Color = when (type) {
    "FD" -> Color(0xFFEFF6FF); "Mutual Fund" -> Color(0xFFECFDF5)
    "PPF", "NPS", "EPF" -> Color(0xFFECFEFF); "Gold", "SGB" -> Color(0xFFFEF3C7)
    "Health Insurance", "Term Insurance" -> Color(0xFFFEF2F2)
    "Stocks", "Equity" -> Color(0xFFFFFBEB); "Real Estate" -> Color(0xFFF0FDF4)
    "Crypto" -> Color(0xFFFFF7ED); "Bonds" -> Color(0xFFF5F3FF); else -> Color(0xFFF5F3FF)
}

private fun categoryForSuccess(type: String): String = when (type) {
    "FD" -> "Banking"; "Mutual Fund" -> "Mutual Funds"
    "PPF", "NPS", "EPF" -> "Retirement"; "Gold", "SGB" -> "Gold"
    "Health Insurance", "Term Insurance" -> "Insurance"
    "Stocks", "Equity" -> "Stocks & ETFs"; "Real Estate" -> "Real Estate"
    "Crypto" -> "Crypto"; "Bonds" -> "Bonds"; else -> "Alternative"
}

private fun displayNameForType(investmentType: String, fallbackName: String): String = when (investmentType) {
    "FD" -> "Fixed Deposit"; "Mutual Fund" -> "Mutual Fund"
    "PPF" -> "Public Provident Fund"; "NPS" -> "National Pension System"
    "Gold" -> "Gold investment"; "SGB" -> "Sovereign Gold Bond"
    "Health Insurance" -> "Health Insurance"; "Term Insurance" -> "Term Insurance"
    "Stocks", "Equity" -> "Stock investment"; "Crypto" -> "Crypto investment"
    "EPF" -> "Employee Provident Fund"; "Bonds" -> "Bond investment"
    "Real Estate" -> "Real Estate investment"; else -> fallbackName
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    surfaceColor: Color,
    borderColor: Color,
    titleColor: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = titleColor, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = mutedColor, textAlign = TextAlign.Center)
    }
}

@Composable
fun InvestmentSuccessScreen(
    viewModel: InvestmentViewModel = hiltViewModel(),
    onViewPortfolio: () -> Unit,
    onAddAnother: () -> Unit
) {
    val lastSaved by viewModel.lastSaved.collectAsState()
    val entity = lastSaved

    // Theme-aware semantic colors
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surface
    val bgColor = MaterialTheme.colorScheme.background
    val titleColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    // "Add Another" button: brand-blue-tinted container, adapts to dark
    val addBtnBg = if (isDark) Color(0xFF1E2E4A) else Color(0xFFEFF6FF)
    val addBtnBorder = if (isDark) Color(0xFF3B5999) else Color(0xFFDBEAFE)

    Scaffold(containerColor = bgColor) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Gradient circle with checkmark + confetti dots
            Box(contentAlignment = Alignment.Center) {
                listOf(
                    Triple(-56.dp, -20.dp, Color(0xFFFB923C)),
                    Triple(-36.dp, -48.dp, Color(0xFF60A5FA)),
                    Triple(20.dp, -56.dp, Color(0xFF34D399)),
                    Triple(52.dp, -30.dp, Color(0xFFA78BFA)),
                    Triple(56.dp, 12.dp, Color(0xFFF472B6)),
                    Triple(-52.dp, 20.dp, Color(0xFFFBBF24)),
                    Triple(28.dp, 44.dp, Color(0xFF60A5FA))
                ).forEach { (dx, dy, color) ->
                    Box(
                        modifier = Modifier
                            .offset(x = dx, y = dy)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SucGradStart, SucGradEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Investment Added!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = titleColor
            )
            Spacer(Modifier.height(8.dp))

            val subtitleText = if (entity != null) {
                val name = displayNameForType(entity.investmentType ?: "", entity.type)
                "Your $name has been added to your portfolio successfully."
            } else {
                "Your investment has been saved successfully.\nTrack it anytime from your portfolio."
            }
            Text(subtitleText, style = MaterialTheme.typography.bodyMedium, color = subColor, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))

            // Investment summary card
            if (entity != null) {
                val invType = entity.investmentType ?: "Others"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgForSuccessType(invType)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emojiForSuccessType(invType), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entity.type, style = MaterialTheme.typography.titleSmall, color = titleColor)
                            entity.bankName?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = mutedColor)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                FormatUtils.formatCurrency(entity.amount),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = titleColor
                            )
                            val dateDisplay = entity.fdStartDate ?: entity.mfDate
                                ?: entity.goldDate ?: entity.stockDate ?: entity.ppfDate
                                ?: entity.npsDate ?: entity.hiRenewalDate
                            dateDisplay?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = mutedColor)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val invCategory = categoryForSuccess(invType)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoTile(
                        label = "Category", value = invCategory,
                        icon = Icons.Default.AccountBalance, color = SucGradStart,
                        surfaceColor = surface, borderColor = cardBorder,
                        titleColor = titleColor, mutedColor = mutedColor,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Status", value = "Active",
                        icon = Icons.Default.CheckCircle, color = Color(0xFF16A34A),
                        surfaceColor = surface, borderColor = cardBorder,
                        titleColor = titleColor, mutedColor = mutedColor,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Tracking", value = "On",
                        icon = Icons.Default.Notifications, color = Color(0xFF7C3AED),
                        surfaceColor = surface, borderColor = cardBorder,
                        titleColor = titleColor, mutedColor = mutedColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AppodealNativeNewsFeed(modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            // View Portfolio gradient button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(SucGradStart, SucGradEnd)))
                    .clickable { onViewPortfolio() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "View Portfolio",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            // Add Another Investment button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(addBtnBg)
                    .border(1.dp, addBtnBorder, RoundedCornerShape(12.dp))
                    .clickable { onAddAnother() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add Another Investment",
                    style = MaterialTheme.typography.titleSmall,
                    color = SucGradStart
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
