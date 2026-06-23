package com.example.wealthtracker.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wealthtracker.WealthTrackerApp
import com.example.wealthtracker.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ss.wealthtracker.R
import android.graphics.BitmapFactory


class AppFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)

    val title = message.notification?.title ?: message.data["title"]
    val body = message.notification?.body ?: message.data["body"] ?: ""

    val mgr = getSystemService(NotificationManager::class.java) ?: return

    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("from_notification", true)
    }

    val contentPendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, WealthTrackerApp.DEFAULT_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_app)   // WHITE ICON
        .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))  // RIGHT-SIDE ICON
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .build()

    mgr.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
}
}
