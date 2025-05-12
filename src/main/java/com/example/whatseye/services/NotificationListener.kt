package com.example.whatseye.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.whatseye.api.ws.WebSocketClientNotification
import com.example.whatseye.dataType.data.NotificationData


class NotificationListener : NotificationListenerService() {

    private val webSocketManager = WebSocketClientNotification.getInstance()
    private var lastNotificationHash: Int? = null
    private var lastNotificationTime: Long = 0
    override fun onCreate() {
        super.onCreate()
        webSocketManager.initialize(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return

        Log.d("NotificationListener", "Sending notification: ")
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val timestamp = sbn.postTime

        val contentHash = (title + content).hashCode()
        val now = System.currentTimeMillis()

        // Skip if identical notification was sent less than 1 second ago
        if (contentHash == lastNotificationHash && (now - lastNotificationTime) < 1000) {
            Log.d("NotificationListener", "Duplicate (time-based) notification ignored")
            return
        }

        lastNotificationHash = contentHash
        lastNotificationTime = now

        val notificationData = NotificationData(
            title = title,
            content = content,
            timestamp = timestamp,
            type= "message"
        )

        Log.d("NotificationListener", "Sending notification: $notificationData")
        webSocketManager.sendNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional cleanup
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Listener connected")
    }
}
