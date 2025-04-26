package com.example.whatseye.api.ws

import com.example.whatseye.dataType.data.NotificationData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket

class WebSocketClientNotification {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    fun initWebSocket(url: String) {
        val request = Request.Builder().url(url).build()
        val listener = WebSocketListener()
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendNotification(notificationData: NotificationData) {
        val json = gson.toJson(notificationData)
        webSocket?.send(json)
    }

    inner class WebSocketListener : okhttp3.WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // Handle connection opened
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // Handle failure, implement reconnection logic here
        }
    }
}

