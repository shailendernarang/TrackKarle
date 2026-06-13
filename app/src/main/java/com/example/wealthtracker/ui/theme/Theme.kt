package com.example.wealthtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import com.example.wealthtracker.util.findActivity

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFA5D6A7),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF1E1F1E),
    onSurfaceVariant = Color(0xFFB9C2BA)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFF81C784),
    background = Color(0xFFF5F6F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7EFE8),
    onSurfaceVariant = Color(0xFF4D6356)
)

@Composable
fun WealthTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    if (!view.isInEditMode) {
        SideEffect {
            val window = activity?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val cfg = LocalConfiguration.current
    val isCompact = cfg.screenWidthDp < 600
    val typeScale = if (isCompact) CompactTypography else Typography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typeScale,
        content = content
    )
}
