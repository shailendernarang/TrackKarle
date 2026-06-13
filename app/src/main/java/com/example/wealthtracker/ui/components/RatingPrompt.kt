package com.example.wealthtracker.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RatingPromptDialog(
    onDismiss: () -> Unit,
    onRated: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Enjoying TrackKaro?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Your feedback helps us improve and reach more users like you!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Would you mind rating us on the Play Store? 🌟",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openPlayStore(context)
                    onRated()
                }
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rate Us")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

private fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        // Try to open in Play Store app
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

object RatingPromptManager {
    private const val PREFS_NAME = "rating_prefs"
    private const val KEY_HAS_RATED = "has_rated"
    private const val KEY_LAST_PROMPT_SESSION = "last_prompt_session"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_APP_OPEN_COUNT = "app_open_count"
    
    fun shouldShowPrompt(context: Context, randomProvider: () -> Double = { Math.random() }): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Don't show if user has already rated
        if (prefs.getBoolean(KEY_HAS_RATED, false)) {
            return false
        }
        
        // Check if we've shown in this session already
        val currentSessionId = getCurrentSessionId(context)
        val lastPromptSession = prefs.getLong(KEY_LAST_PROMPT_SESSION, 0L)
        
        if (lastPromptSession == currentSessionId) {
            return false
        }
        
        // Check app open count (show after at least 5 opens)
        val openCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        if (openCount < 5) {
            return false
        }
        
        // Show randomly (30% chance)
        return randomProvider() < 0.3
    }
    
    fun markPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSessionId = getCurrentSessionId(context)
        prefs.edit().putLong(KEY_LAST_PROMPT_SESSION, currentSessionId).apply()
    }
    
    fun markUserRated(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
    }
    
    fun incrementAppOpenCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, count + 1).apply()
    }
    
    private fun getCurrentSessionId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sessionId = prefs.getLong(KEY_SESSION_ID, 0L)
        
        if (sessionId == 0L) {
            sessionId = System.currentTimeMillis()
            prefs.edit().putLong(KEY_SESSION_ID, sessionId).apply()
        }
        
        return sessionId
    }
    
    fun startNewSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newSessionId = System.currentTimeMillis()
        prefs.edit().putLong(KEY_SESSION_ID, newSessionId).apply()
    }
    
    // For testing - reset all rating data
    fun resetRatingData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
