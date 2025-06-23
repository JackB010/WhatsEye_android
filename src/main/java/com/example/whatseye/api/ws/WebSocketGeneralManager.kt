package com.example.whatseye.api.ws

import android.annotation.SuppressLint
import android.content.Context
import com.example.whatseye.api.SSLUtils
import com.example.whatseye.api.managers.JwtTokenManager

@SuppressLint("StaticFieldLeak")
object WebSocketGeneralManager {
    private var webSocketClient: WebSocketClientGeneral? = null
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context
    }

    fun getInstance(context: Context): WebSocketClientGeneral {
        if (webSocketClient == null) {
            val userId = JwtTokenManager(context).getUserId()
            val token = JwtTokenManager(context).getAccessJwt()
            val url = "wss://192.168.204.116:443/ws/general/$userId/?token=$token"
            val okHttpClient = SSLUtils.getOkHttpClient(context)
            webSocketClient = WebSocketClientGeneral(context, url, okHttpClient)
        }
        return webSocketClient!!
    }

    fun closeConnection() {
        webSocketClient?.close()
        webSocketClient = null
    }
}