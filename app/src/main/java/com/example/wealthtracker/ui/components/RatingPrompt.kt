package com.example.wealthtracker.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.ss.wealthtracker.BuildConfig

object RatingPromptManager {
    private const val TAG = "RatingPrompt"
    private const val PREFS_NAME = "rating_prefs"
    private const val KEY_APP_OPEN_COUNT = "app_open_count"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_LAST_PROMPT_SESSION = "last_prompt_session"
    private const val MIN_OPENS = 2

    fun shouldShowPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_APP_OPEN_COUNT, 0)
        Log.d(TAG, "App open count: $count, threshold: $MIN_OPENS")
        if (count < MIN_OPENS) return false
        val sessionId = getCurrentSessionId(context)
        val lastShown = prefs.getLong(KEY_LAST_PROMPT_SESSION, 0L)
        Log.d(TAG, "Session check — current: $sessionId, lastShown: $lastShown")
        return lastShown != sessionId
    }

    fun markPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_PROMPT_SESSION, getCurrentSessionId(context)).apply()
    }

    fun incrementAppOpenCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = prefs.getInt(KEY_APP_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, newCount).apply()
        Log.d(TAG, "App open count incremented to $newCount")
    }

    fun startNewSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_SESSION_ID, System.currentTimeMillis()).apply()
    }

    private fun getCurrentSessionId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getLong(KEY_SESSION_ID, 0L)
        if (id == 0L) {
            id = System.currentTimeMillis()
            prefs.edit().putLong(KEY_SESSION_ID, id).apply()
        }
        return id
    }

    suspend fun launchInAppReview(activity: Activity) {
        try {
            val manager = if (BuildConfig.DEBUG) FakeReviewManager(activity)
                          else ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
            Log.d(TAG, "In-app review flow launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "In-app review failed: ${e.message}", e)
        }
    }
}
