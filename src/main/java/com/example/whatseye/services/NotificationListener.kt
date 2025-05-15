package com.example.whatseye.services

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientGeneral
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.dataType.data.NotificationData

class NotificationListener : NotificationListenerService() {

    private lateinit var webSocketManager: WebSocketClientGeneral
    private var lastNotificationHash: Int? = null
    private var lastNotificationTime: Long = 0
    private val knownOngoingCalls = listOf(
        "Ongoing voice call",
        "Appel vocal en cours",
        "مكالمة صوتية جارية"
    )
    private val knownOngoingVideoCalls = listOf(
        "Ongoing video call",
        "Appel vidéo en cours",
        "مكالمة فيديو جارية"
    )
    override fun onCreate() {
        super.onCreate()
        if(JwtTokenManager(this).getIsLogin())
            webSocketManager = WebSocketGeneralManager.getInstance(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp") return

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val timestamp = sbn.postTime




        val matchesCallText = knownOngoingCalls.any { content.contains(it, ignoreCase = true) }
        val matchesVideoCallText = knownOngoingVideoCalls.any { content.contains(it, ignoreCase = true) }

        // Check if it's an ongoing notification
        val isOngoingNotification = !sbn.isClearable ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        // Only start recording if both conditions are met
        if (isOngoingNotification) {
            when {
                matchesVideoCallText -> startRecordingService()
                matchesCallText -> startRecordingVoiceService()
            }
        }
        val contentHash = (title + content).hashCode()
        val now = System.currentTimeMillis()

        // Avoid sending duplicate notifications within 1 second
        if (contentHash == lastNotificationHash && (now - lastNotificationTime) < 1000) {
            Log.d("NotificationListener", "Duplicate notification ignored.")
            return
        }

        lastNotificationHash = contentHash
        lastNotificationTime = now

        val notificationData = NotificationData(
            title = title,
            content = content,
            timestamp = timestamp,
            type = "message"
        )

        Log.d("NotificationListener", "Sending notification: $notificationData")
        webSocketManager.sendNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName != "com.whatsapp") return

        val notification = sbn.notification
        val extras = notification.extras
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val matchesCallText = knownOngoingCalls.any { content.contains(it, ignoreCase = true) }
        val matchesVideoCallText = knownOngoingVideoCalls.any { content.contains(it, ignoreCase = true) }

        val wasOngoingCall = !sbn.isClearable ||
                (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

        if (wasOngoingCall) {
            when {
                matchesVideoCallText ->{
                    stopRecordingService()
                    stopRecordingVoiceService()}
                matchesCallText -> stopRecordingVoiceService()
            }
        }
    }

    private fun startRecordingService() {
        try {
            val intent = Intent(this, CaptureActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RecordingService", "Failed to start screen recording service: ${e.message}")
        }
    }

    private fun stopRecordingService() {
        val intent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ScreenRecorderService.ACTION_STOP
        }
        ContextCompat.startForegroundService(this, intent)
    }



    private fun startRecordingVoiceService() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopRecordingVoiceService() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        startService( intent)

    }
    // To stop recording

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Notification Listener connected")
    }
}
