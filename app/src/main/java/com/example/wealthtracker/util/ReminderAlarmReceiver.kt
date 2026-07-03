package com.example.wealthtracker.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wealthtracker.MainActivity
import com.example.wealthtracker.WealthTrackerApp
import com.ss.wealthtracker.R
import com.example.wealthtracker.data.ReminderManager
import com.example.wealthtracker.data.ReminderStatus

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextShowTime = prefs.getLong(KEY_NEXT_SHOW_TIME, 0L)
        Log.d(TAG, "nextShowTime=$nextShowTime, now=${System.currentTimeMillis()}")

        if (System.currentTimeMillis() < nextShowTime) {
            Log.d(TAG, "Suppressed — within snooze window")
            return
        }

        val reminderManager = ReminderManager.getInstance(context)
        val active = reminderManager.activeReminders.value
            .filter { it.status == ReminderStatus.ACTIVE }
        Log.d(TAG, "Active reminders: ${active.size}")

        if (active.isEmpty()) return

        val title = "Investment Reminder"
        val body = buildNotificationText(active.map { it.investmentType to it.investmentName })

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_REMINDERS, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowIntent = snoozeIntent(context, SNOOZE_1_DAY)
        val threeDaysIntent = snoozeIntent(context, SNOOZE_3_DAYS)
        val gotItIntent = snoozeIntent(context, SNOOZE_FOREVER)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_app)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Tomorrow", tomorrowIntent)
            .addAction(0, "3 Days", threeDaysIntent)
            .addAction(0, "Got it", gotItIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun snoozeIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, ReminderSnoozeReceiver::class.java).apply {
            this.action = action
        }
        val code = when (action) {
            SNOOZE_1_DAY -> 101
            SNOOZE_3_DAYS -> 102
            else -> 103
        }
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotificationText(items: List<Pair<String, String>>): String {
        if (items.size == 1) {
            val (type, name) = items[0]
            return "$name ($type) is due soon. Tap to review."
        }

        val grouped = items.groupBy { it.first }
        val parts = grouped.map { (type, list) ->
            val label = when (type) {
                "FD" -> if (list.size == 1) "1 FD" else "${list.size} FDs"
                "Health Insurance" -> if (list.size == 1) "1 insurance" else "${list.size} insurances"
                else -> "${list.size} ${type}s"
            }
            label
        }
        return "Your ${parts.joinToString(" and ")} ${if (items.size == 1) "is" else "are"} due soon. Tap to review."
    }

    companion object {
        private const val TAG = "ReminderAlarm"
        const val CHANNEL_ID = "reminders_channel"
        const val NOTIFICATION_ID = 2001
        const val PREFS_NAME = "reminder_notif_prefs"
        const val KEY_NEXT_SHOW_TIME = "next_show_time"
        const val SNOOZE_1_DAY = "SNOOZE_1_DAY"
        const val SNOOZE_3_DAYS = "SNOOZE_3_DAYS"
        const val SNOOZE_FOREVER = "SNOOZE_FOREVER"
    }
}
