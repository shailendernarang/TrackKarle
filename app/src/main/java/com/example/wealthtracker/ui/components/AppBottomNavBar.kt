package com.example.wealthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val NavBlue = Color(0xFF155DFC)
private val GradStart = Color(0xFF2563EB)
private val GradEnd = Color(0xFF7C3AED)
private val PillShape = RoundedCornerShape(percent = 50)

// FAB sits this many dp above the nav bar surface
private val FabOverhang = 20.dp
private val FabSize = 54.dp
private val BarHeight = 68.dp
// Total box height = bar + overhang so FAB never leaves the layout bounds
private val BoxHeight = BarHeight + FabOverhang

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(PillShape)
                .background(if (selected) indicatorColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) selectedColor else unselectedColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) selectedColor else unselectedColor,
            maxLines = 1
        )
    }
}

@Composable
fun AppBottomNavBar(
    currentRoute: String,
    onGoHome: () -> Unit,
    onGoPortfolio: () -> Unit,
    onGoCalculators: () -> Unit,
    onGoSettings: () -> Unit,
    onAddClick: () -> Unit = {},
    onGoStocks: (() -> Unit)? = null   // if set, replaces Settings tab with Stock Analysis
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val navGray = MaterialTheme.colorScheme.onSurfaceVariant
    val indicatorColor = NavBlue.copy(alpha = if (isDark) 0.22f else 0.12f)

    Column(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.fillMaxWidth().height(BoxHeight)) {

            // Nav bar surface — sits at the bottom of the Box (BarHeight dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .align(Alignment.BottomCenter),
                color = surfaceColor,
                tonalElevation = 3.dp,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        selected = currentRoute == "dashboard",
                        onClick = onGoHome,
                        selectedColor = NavBlue,
                        unselectedColor = navGray,
                        indicatorColor = indicatorColor,
                        modifier = Modifier.weight(1f)
                    )
                    NavItem(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Portfolio",
                        selected = currentRoute == "portfolio",
                        onClick = onGoPortfolio,
                        selectedColor = NavBlue,
                        unselectedColor = navGray,
                        indicatorColor = indicatorColor,
                        modifier = Modifier.weight(1f)
                    )
                    // Gap for the FAB
                    Spacer(Modifier.weight(1f))
                    NavItem(
                        icon = Icons.Default.Calculate,
                        label = "Calculate",
                        selected = currentRoute.startsWith("calculators"),
                        onClick = onGoCalculators,
                        selectedColor = NavBlue,
                        unselectedColor = navGray,
                        indicatorColor = indicatorColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (onGoStocks != null) {
                        NavItem(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = "Stocks",
                            selected = currentRoute == "stocks",
                            onClick = onGoStocks,
                            selectedColor = NavBlue,
                            unselectedColor = navGray,
                            indicatorColor = indicatorColor,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        NavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            selected = currentRoute == "settings",
                            onClick = onGoSettings,
                            selectedColor = NavBlue,
                            unselectedColor = navGray,
                            indicatorColor = indicatorColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // FAB — anchored at top-center of the Box; protrudes FabOverhang dp above the bar
            Box(
                modifier = Modifier
                    .size(FabSize)
                    .align(Alignment.TopCenter)
                    .shadow(elevation = 16.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GradStart, GradEnd)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Investment",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // System navigation bar inset — same background as the bar so there's no visible gap
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(surfaceColor)
        )
    }
}
