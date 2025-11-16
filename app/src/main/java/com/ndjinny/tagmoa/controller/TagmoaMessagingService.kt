package com.ndjinny.tagmoa.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ndjinny.tagmoa.R

class TagmoaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        showNotification(remoteMessage)
    }

    private fun showNotification(message: RemoteMessage) {
        ensureChannel()
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.notification_remote_fallback_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: getString(R.string.notification_remote_fallback_body)

        val notification = NotificationCompat.Builder(this, REMOTE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_tasks)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(REMOTE_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            REMOTE_CHANNEL_ID,
            getString(R.string.notification_channel_remote),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_remote_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMOTE_CHANNEL_ID = "tagmoa_remote_notifications"
    }
}
