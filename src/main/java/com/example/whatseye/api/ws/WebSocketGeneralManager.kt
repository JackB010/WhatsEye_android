package com.example.whatseye.api.ws

import android.annotation.SuppressLint
import android.content.Context
import com.example.whatseye.api.managers.JwtTokenManager


@SuppressLint("StaticFieldLeak")
object WebSocketGeneralManager {
    private var webSocketClient: WebSocketClientGeneral? = null

    fun getInstance(context: Context): WebSocketClientGeneral {
        if (webSocketClient == null) {
            val userId = JwtTokenManager(context).getUserId()
            val token = JwtTokenManager(context).getAccessJwt()
            val url = "ws://192.168.89.116:8000/ws/general/$userId/?token=$token"
            webSocketClient = WebSocketClientGeneral(context, url)
        }
        return webSocketClient!!
    }

    fun closeConnection() {
        webSocketClient?.close() // Assuming your client has a close method
        webSocketClient = null
    }
}