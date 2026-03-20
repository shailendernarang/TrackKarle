package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ss.wealthtracker.R
import android.content.Intent
import android.net.Uri
import com.ss.wealthtracker.BuildConfig
import com.example.wealthtracker.util.BiometricUtils
import com.example.wealthtracker.analytics.AnalyticsManager
import com.example.wealthtracker.analytics.TrackScreen
import androidx.compose.runtime.remember

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    requireDeviceLock: Boolean,
    onToggleRequireDeviceLock: () -> Unit,
    useHindiNumerals: Boolean,
    onToggleHindiNumerals: () -> Unit,
    onBack: () -> Unit,
    onOpenReferral: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(stringResource(id = R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        val ctx = LocalContext.current
        val analytics = remember { AnalyticsManager(ctx) }
        TrackScreen(screenName = "Settings", analyticsManager = analytics)
        val isDeviceLockAvailable = BiometricUtils.isDeviceLockAvailable(ctx)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow(
                title = stringResource(id = R.string.toggle_dark_mode),
                checked = darkMode,
                onToggle = {
                    onToggleDarkMode()
                    analytics.logSettingChanged("dark_mode", if (darkMode) "off" else "on")
                }
            )
            HorizontalDivider()
            
            // Only show device lock option if it's available on this device
            if (isDeviceLockAvailable) {
                SettingRow(
                    title = if (requireDeviceLock) stringResource(id = R.string.require_device_lock_on) else stringResource(id = R.string.require_device_lock_off),
                    checked = requireDeviceLock,
                    onToggle = {
                        onToggleRequireDeviceLock()
                        analytics.logSettingChanged("require_device_lock", if (requireDeviceLock) "off" else "on")
                    }
                )
                HorizontalDivider()
            }
            SettingRow(
                title = stringResource(id = R.string.change_language_hindi),
                checked = useHindiNumerals,
                onToggle = {
                    onToggleHindiNumerals()
                    analytics.logSettingChanged("use_hindi_numerals", if (useHindiNumerals) "off" else "on")
                }
            )
            HorizontalDivider()
            
            // Referral section
            if (onOpenReferral != null) {
                androidx.compose.material3.TextButton(
                    onClick = onOpenReferral,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Invite Friends",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider()
            }
            
            // App info
            val appName = try {
                val ai = ctx.packageManager.getApplicationInfo(ctx.packageName, 0)
                ctx.packageManager.getApplicationLabel(ai).toString()
            } catch (e: Exception) { "TrackKaro" }
            val version = BuildConfig.VERSION_NAME
            Text("App", style = MaterialTheme.typography.titleMedium)
            Text("$appName v$version", color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Privacy Policy link
            androidx.compose.material3.TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/trackkarle/home"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(intent) }
            }) { Text("Privacy Policy") }
        }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (checked) stringResource(id = R.string.settings_on) else stringResource(id = R.string.settings_off), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
