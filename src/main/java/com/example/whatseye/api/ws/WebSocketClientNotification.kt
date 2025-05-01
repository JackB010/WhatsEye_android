package com.example.whatseye.api.ws

import com.example.whatseye.dataType.data.NotificationData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.json.JSONObject

class WebSocketClientNotification private constructor() {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()
    private var url: String? = null
    private var isReconnecting = false
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var retryCount = 0
    private val maxRetries = 5
    private val baseDelayMs = 1000L // 1 second initial delay

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
    fun initWebSocket(url: String) {
        if (this.url == url && webSocket != null) {
            // Already connected to the same URL
            return
        }

        disconnect() // Disconnect any existing connection
        this.url = url
        connect()
    }

    @Synchronized
    private fun connect() {
        url?.let {
            val request = Request.Builder().url(it).build()
            val listener = WebSocketListener()
            webSocket = client.newWebSocket(request, listener)
        }
    }

    fun sendNotification(notificationData: NotificationData) {
        val message = JSONObject().apply {
            put("type", "NOTIFICATION")
            put("notification", JSONObject(gson.toJson(notificationData)))
        }.toString()
        webSocket?.send(message)
    }

    @Synchronized
    fun disconnect() {
        reconnectScope.coroutineContext.cancelChildren() // Cancel reconnection attempts
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
    }

    private fun scheduleReconnect() {
        if (isReconnecting || retryCount >= maxRetries) return

        isReconnecting = true
        val delay = baseDelayMs * (1 shl retryCount)

        reconnectScope.launch {
            delay(delay)
            retryCount++
            connect()
            isReconnecting = false
        }
    }

    fun close() {
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
    }

    inner class WebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            retryCount = 0
            // Connection established
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Handle incoming messages
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!isReconnecting) {
                scheduleReconnect()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // Connection closed
        }
    }
}