package com.example.wealthtracker.consent

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Custom GDPR/CCPA consent dialog that creates a TCF v2 consent string
 * and passes it to Appodeal SDK.
 */
@Composable
fun ConsentDialog(
    onConsentGiven: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = { /* Cannot dismiss without choice */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Privacy & Consent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "TrackKarle uses advertising to support the app. We and our advertising partners need your consent to:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "• Store and/or access information on your device\n" +
                          "• Use precise geolocation data\n" +
                          "• Actively scan device characteristics for identification\n" +
                          "• Create personalized ads profile\n" +
                          "• Select personalized ads\n" +
                          "• Measure ad performance\n" +
                          "• Apply market research to generate audience insights",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your data will be processed by our advertising partners:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Meta, AppLovin, Unity Ads, InMobi, BidMachine, and others",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You can change your preferences at any time in the app settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Accept button
                Button(
                    onClick = {
                        ConsentPrefs.save(context, true)
                        Log.d("ConsentDialog", "User accepted personalized ads")
                        onConsentGiven(true)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accept & Continue")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reject button (non-personalized ads)
                OutlinedButton(
                    onClick = {
                        ConsentPrefs.save(context, false)
                        Log.d("ConsentDialog", "User chose non-personalized ads")
                        onConsentGiven(false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Non-Personalized Ads")
                }
            }
        }
    }
}

/**
 * SharedPreferences helper for consent storage
 * Saves user consent choice for persistence across app sessions
 */
object ConsentPrefs {
    private const val PREFS = "consent_prefs"
    private const val KEY_SET = "consent_set"
    private const val KEY_VALUE = "consent_value"
    
    fun isSet(ctx: Context): Boolean = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SET, false)
    
    fun getValue(ctx: Context): Boolean = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_VALUE, false)
    
    fun save(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SET, true)
            .putBoolean(KEY_VALUE, value)
            .apply()
        Log.d("ConsentPrefs", "Consent saved: $value")
    }
    
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        Log.d("ConsentPrefs", "Consent cleared")
    }
}

/**
 * Check if consent needs to be shown
 * 
 * Best practice from Appodeal: Show consent dialog to ALL users to avoid
 * accidentally missing an EU user. This is the recommended approach.
 * 
 * Legally required for: EEA, UK, Switzerland users
 * Recommended for: All users (to avoid missing GDPR users)
 */
fun shouldShowConsentDialog(context: Context): Boolean {
    if (ConsentPrefs.isSet(context)) {
        // Already decided — consent is passed to Appodeal.initialize()
        Log.d("ConsentDialog", "Consent already set: ${ConsentPrefs.getValue(context)}")
        return false
    }
    
    // Show dialog for ALL first-time users (Appodeal best practice)
    // This ensures we don't miss any EU/UK/Swiss users
    return true
}

/**
 * Revoke consent (for settings screen)
 * Clears saved consent so dialog will show again on next app start
 */
fun revokeConsent(context: Context) {
    ConsentPrefs.clear(context)
    Log.d("ConsentDialog", "Consent revoked - will show dialog on next app start")
}
