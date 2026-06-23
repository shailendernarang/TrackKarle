package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ss.wealthtracker.R
import android.content.Intent
import android.net.Uri
import com.ss.wealthtracker.BuildConfig
import com.example.wealthtracker.util.BiometricUtils
import com.example.wealthtracker.analytics.AnalyticsManager
import com.example.wealthtracker.analytics.TrackScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    requireDeviceLock: Boolean,
    onToggleRequireDeviceLock: () -> Unit,
    useHindiNumerals: Boolean,
    onToggleHindiNumerals: () -> Unit,
    analyticsEnabled: Boolean = true,
    onToggleAnalytics: (() -> Unit)? = null,
    crashReportingEnabled: Boolean = true,
    onToggleCrashReporting: (() -> Unit)? = null,
    isPremium: Boolean = false,
    onBack: () -> Unit,
    onOpenReferral: (() -> Unit)? = null,
    onOpenPremium: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val analytics = remember { AnalyticsManager(ctx) }
    TrackScreen(screenName = "Settings", analyticsManager = analytics)
    val isDeviceLockAvailable = BiometricUtils.isDeviceLockAvailable(ctx)
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (useHindiNumerals) onToggleHindiNumerals()
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = !useHindiNumerals, onClick = {
                            if (useHindiNumerals) onToggleHindiNumerals()
                            showLanguageDialog = false
                        })
                        Text("English", style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!useHindiNumerals) onToggleHindiNumerals()
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = useHindiNumerals, onClick = {
                            if (!useHindiNumerals) onToggleHindiNumerals()
                            showLanguageDialog = false
                        })
                        Column {
                            Text("हिंदी", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Hindi numerals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(id = R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Premium ──────────────────────────────────────────────────────
            if (onOpenPremium != null) {
                Spacer(Modifier.height(12.dp))
                if (isPremium) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Premium Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Upgrade to Premium")
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // ── Preferences ──────────────────────────────────────────────────
            SectionHeader("Preferences")

            SettingToggleRow(
                title = "Dark Mode",
                checked = darkMode,
                onToggle = {
                    onToggleDarkMode()
                    analytics.logSettingChanged("dark_mode", if (darkMode) "off" else "on")
                }
            )
            HorizontalDivider()

            SettingNavRow(
                title = "Language",
                trailingLabel = if (useHindiNumerals) "हिंदी" else "English",
                onClick = { showLanguageDialog = true }
            )
            HorizontalDivider()

            if (isDeviceLockAvailable) {
                SettingToggleRow(
                    title = "Require Device Lock",
                    checked = requireDeviceLock,
                    onToggle = {
                        onToggleRequireDeviceLock()
                        analytics.logSettingChanged("require_device_lock", if (requireDeviceLock) "off" else "on")
                    }
                )
                HorizontalDivider()
            }

            // ── Privacy & Data ───────────────────────────────────────────────
            if (onToggleAnalytics != null || onToggleCrashReporting != null) {
                SectionHeader("Privacy & Data")

                if (onToggleAnalytics != null) {
                    SettingToggleRow(
                        title = "Usage Analytics",
                        subtitle = "Help us improve the app by sharing anonymous usage data",
                        checked = analyticsEnabled,
                        onToggle = {
                            onToggleAnalytics()
                            analytics.logSettingChanged("analytics_enabled", if (analyticsEnabled) "off" else "on")
                        }
                    )
                    HorizontalDivider()
                }

                if (onToggleCrashReporting != null) {
                    SettingToggleRow(
                        title = "Crash Reports",
                        subtitle = "Automatically send crash reports to help us fix bugs faster",
                        checked = crashReportingEnabled,
                        onToggle = {
                            onToggleCrashReporting()
                            analytics.logSettingChanged("crash_reporting_enabled", if (crashReportingEnabled) "off" else "on")
                        }
                    )
                    HorizontalDivider()
                }
            }

            // ── About ────────────────────────────────────────────────────────
            SectionHeader("About")

            if (onOpenReferral != null) {
                SettingNavRow(
                    title = "Invite Friends",
                    subtitle = "Share TrackKaro with others",
                    onClick = onOpenReferral
                )
                HorizontalDivider()
            }

            SettingNavRow(
                title = "Privacy Policy",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/trackkarle/home"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { ctx.startActivity(intent) }
                }
            )
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Version", style = MaterialTheme.typography.bodyLarge)
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    subtitle: String? = null,
    trailingLabel: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (trailingLabel != null) {
                Text(
                    trailingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
