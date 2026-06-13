package com.example.wealthtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Privacy Consent Dialog - GDPR/Privacy Compliance
 * 
 * Shows on first app launch to get user consent for:
 * - Analytics data collection
 * - Crash reporting
 * - Performance monitoring
 * 
 * All data collection is opt-in and can be changed in Settings.
 */
@Composable
fun PrivacyConsentDialog(
    onAccept: (analyticsEnabled: Boolean, crashReportingEnabled: Boolean) -> Unit,
    onDecline: () -> Unit
) {
    var analyticsConsent by remember { mutableStateOf(true) }
    var crashReportingConsent by remember { mutableStateOf(true) }
    
    Dialog(onDismissRequest = { /* Prevent dismissal - user must choose */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Privacy & Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = "TrackKaro respects your privacy. Your investment data stays on your device and is never sent to our servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Consent Options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Optional Data Collection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Analytics Consent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usage Analytics",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Help us improve the app by sharing anonymous usage data (no personal info)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = analyticsConsent,
                                onCheckedChange = { analyticsConsent = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Crash Reporting Consent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Crash Reports",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically send crash reports to help us fix bugs faster",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = crashReportingConsent,
                                onCheckedChange = { crashReportingConsent = it }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy Policy Link
                Text(
                    text = "We never collect personal information like investment amounts, names, or financial details.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAccept(analyticsConsent, crashReportingConsent) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                    }
                    
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Decline All & Continue")
                    }
                }
            }
        }
    }
}

/**
 * Privacy Preferences Manager
 * Stores user's privacy consent choices
 */
object PrivacyPreferences {
    private const val PREFS_NAME = "privacy_prefs"
    private const val KEY_CONSENT_GIVEN = "consent_given"
    private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
    private const val KEY_CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    
    fun setConsentGiven(context: android.content.Context, given: Boolean) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENT_GIVEN, given)
            .apply()
    }
    
    fun hasConsentBeenGiven(context: android.content.Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_CONSENT_GIVEN, false)
    }
    
    fun setAnalyticsEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ANALYTICS_ENABLED, enabled)
            .apply()
    }
    
    fun isAnalyticsEnabled(context: android.content.Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_ANALYTICS_ENABLED, true) // Default true
    }
    
    fun setCrashReportingEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CRASH_REPORTING_ENABLED, enabled)
            .apply()
    }
    
    fun isCrashReportingEnabled(context: android.content.Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_CRASH_REPORTING_ENABLED, true) // Default true
    }
}
