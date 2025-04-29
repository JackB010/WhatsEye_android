package com.example.whatseye.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.whatseye.api.ws.WebSocketClientNotification
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.dataType.data.NotificationData

class NotificationListener : NotificationListenerService() {

    private val webSocketManager = WebSocketClientNotification()
    private var lastNotificationHash: Int? = null
    private var lastNotificationTime: Long = 0

    override fun onCreate() {
        super.onCreate()

        val jwtManager = JwtTokenManager(this)
        val userId = jwtManager.getUserId()
        val token = jwtManager.getAccessJwt()

        val wsUrl = "ws://192.168.128.116:8000/ws/notifications/$userId/?token=$token"
        webSocketManager.initWebSocket(wsUrl)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return

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
            timestamp = timestamp
        )

        Log.d("NotificationListener", "Sending notification: $notificationData")
        webSocketManager.sendNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional cleanup
    }
}
