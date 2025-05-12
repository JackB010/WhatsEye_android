package com.example.whatseye.api.ws

import android.content.Context
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.dataType.data.NotificationData
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClientNotification private constructor() {

    private var webSocket: WebSocket? = null
    private var url: String? = null
    private var isClosed = false
    private var isReconnecting = false
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var retryCount = 0
    private val baseDelayMs = 1000L
    private val maxDelayMs = 15000L

    companion object {
        @Volatile
        private var instance: WebSocketClientNotification? = null

        fun getInstance(): WebSocketClientNotification {
            return instance ?: synchronized(this) {
                instance ?: WebSocketClientNotification().also { instance = it }
            }
        }
    }

    @Synchronized
    fun initialize(context: Context) {
        val jwtManager = JwtTokenManager(context)
        val userId = jwtManager.getUserId()
        val token = jwtManager.getAccessJwt()

        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            disconnect()
            return
        }

        val baseUrl = "ws://192.168.89.116:8000"
        val newUrl = "$baseUrl/ws/notifications/$userId/?token=$token"

        if (newUrl == url) return

       // disconnect() // Ensure previous connection is cleaned up
        url = newUrl
        isClosed = false
        connect()
    }

    @Synchronized
    fun disconnect() {
        isClosed = true
        reconnectScope.coroutineContext.cancelChildren()
        cleanupWebSocket()
        url = null
        retryCount = 0
        isReconnecting = false
    }

    private fun connect() {
        if (isClosed || webSocket != null || url == null) return

        val request = Request.Builder().url(url!!).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryCount = 0
                isReconnecting = false
                println("WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket closing: $code - $reason")
                cleanupWebSocket()
                if (!isClosed) scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                cleanupWebSocket()
                if (!isClosed && !isReconnecting) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (isClosed || isReconnecting) return

        isReconnecting = true
        val delay = (baseDelayMs * (1L shl retryCount.coerceAtMost(10))).coerceAtMost(maxDelayMs)

        reconnectScope.launch {
            delay(delay)
            if (!isClosed) {
                retryCount++
                connect()
                isReconnecting = false
            }
        }
    }

    private fun cleanupWebSocket() {
        webSocket?.close(1000, "Cleanup Closure")
        webSocket = null
    }

    private fun handleIncomingMessage(text: String) {
        println("WebSocket message: $text")
        // Parse and handle your message here
    }

    fun sendNotification(notificationData: NotificationData) {
        val message = JSONObject().apply {
            put("type", "NOTIFICATION")
            put("notification", JSONObject(gson.toJson(notificationData)))
        }.toString()
        webSocket?.send(message)
    }
}
