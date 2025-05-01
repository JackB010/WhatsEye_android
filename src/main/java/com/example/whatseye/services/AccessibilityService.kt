package com.example.whatseye.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.whatseye.api.managers.BadWordsManager
import com.example.whatseye.dataType.data.NotificationData
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientNotification

class AccessibilityService : AccessibilityService() {
    private lateinit var badWords: List<String> // List of inappropriate words
    private var lastText: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val TYPING_DELAY_MS = 5000L // 5 seconds delay to detect typing pause
    private val checkBadWordsRunnable = Runnable {
        checkBadWords(lastText)
    }
    // WebSocket manager instance
    private val webSocketManager = WebSocketClientNotification.getInstance()
    override fun onCreate() {
        super.onCreate()
        badWords = BadWordsManager(this).getBadWords()
        // Initialize WebSocket connection
        val jwtManager = JwtTokenManager(this)
        val userId = jwtManager.getUserId()
        val token = jwtManager.getAccessJwt()

        val wsUrl = "ws://192.168.181.116:8000/ws/notifications/$userId/?token=$token"
        webSocketManager.initWebSocket(wsUrl)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.packageName == "com.whatsapp") {
                when (it.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        val text = it.text.toString()
                        lastText = text
                        // Remove pending checks and schedule a new one
                        handler.removeCallbacks(checkBadWordsRunnable)
                        handler.postDelayed(checkBadWordsRunnable, TYPING_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun checkBadWords(text: String) {
        for (badWord in badWords) {
            if (text.contains(badWord, ignoreCase = true)) {
                // Optional: send notification via WebSocket
                sendWebSocketNotification(text)
            }
        }
    }

    private fun sendWebSocketNotification(text: String) {
         //Prepare NotificationData object
        val notificationData = NotificationData(
            "BAD WORD",
            text,
            System.currentTimeMillis(),
            "alert"
        )
         //Send via WebSocket
       webSocketManager.sendNotification(notificationData)
    }

    override fun onInterrupt() {
        // Handle service interruption if needed
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler to prevent memory leaks
        handler.removeCallbacks(checkBadWordsRunnable)
        // Close WebSocket connection
        webSocketManager.close()
    }
}