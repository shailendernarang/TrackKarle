package com.example.wealthtracker.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.KEY_NEXT_SHOW_TIME
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.NOTIFICATION_ID
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.PREFS_NAME
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.SNOOZE_1_DAY
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.SNOOZE_3_DAYS
import com.example.wealthtracker.util.ReminderAlarmReceiver.Companion.SNOOZE_FOREVER

class ReminderSnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == RESET_FOR_TESTING) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_NEXT_SHOW_TIME, 0L).apply()
            return
        }
        val delayMs = when (intent.action) {
            SNOOZE_1_DAY -> DAY_MS
            SNOOZE_3_DAYS -> 3 * DAY_MS
            SNOOZE_FOREVER -> Long.MAX_VALUE - System.currentTimeMillis()
            else -> DAY_MS
        }

        val nextShowTime = System.currentTimeMillis() + delayMs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_NEXT_SHOW_TIME, nextShowTime).apply()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        const val RESET_FOR_TESTING = "RESET_FOR_TESTING"
    }
}
