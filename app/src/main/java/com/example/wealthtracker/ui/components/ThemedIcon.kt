package com.example.wealthtracker.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ThemedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val iconTint = tint ?: if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = iconTint
    )
}

@Composable
fun ThemedPrimaryIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = iconTint
    )
}

@Composable
fun ThemedSecondaryIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.secondary
    }
    
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = iconTint
    )
}

@Composable
fun ThemedSurfaceIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = iconTint
    )
}

object ThemedColors {
    @Composable
    fun getSuccessColor(): Color {
        return if (isSystemInDarkTheme()) {
            Color(0xFF81C784) // Lighter green for dark mode
        } else {
            Color(0xFF4CAF50) // Standard green for light mode
        }
    }
    
    @Composable
    fun getErrorColor(): Color {
        return if (isSystemInDarkTheme()) {
            Color(0xFFE57373) // Lighter red for dark mode
        } else {
            Color(0xFFF44336) // Standard red for light mode
        }
    }
    
    @Composable
    fun getWarningColor(): Color {
        return if (isSystemInDarkTheme()) {
            Color(0xFFFFB74D) // Lighter orange for dark mode
        } else {
            Color(0xFFFF9800) // Standard orange for light mode
        }
    }
    
    @Composable
    fun getInfoColor(): Color {
        return if (isSystemInDarkTheme()) {
            Color(0xFF64B5F6) // Lighter blue for dark mode
        } else {
            Color(0xFF2196F3) // Standard blue for light mode
        }
    }
    
    @Composable
    fun getChartColors(): List<Color> {
        return if (isSystemInDarkTheme()) {
            // Dark mode colors - more vibrant and visible on dark backgrounds
            listOf(
                Color(0xFFBB86FC), // Purple
                Color(0xFF03DAC6), // Teal
                Color(0xFFCF6679), // Pink
                Color(0xFFFFB74D), // Orange
                Color(0xFF81C784), // Green
                Color(0xFF64B5F6), // Blue
                Color(0xFFF06292), // Pink
                Color(0xFFA1C181)  // Light Green
            )
        } else {
            // Light mode colors - standard Material colors
            listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFFFF9800), // Orange
                Color(0xFF9C27B0), // Purple
                Color(0xFFF44336), // Red
                Color(0xFF00BCD4), // Cyan
                Color(0xFF795548), // Brown
                Color(0xFF607D8B)  // Blue Grey
            )
        }
    }
}
