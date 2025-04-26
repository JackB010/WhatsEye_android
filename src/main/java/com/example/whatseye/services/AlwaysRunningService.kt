package com.example.whatseye.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientPin
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel


class AlwaysRunningService : Service() {

    private val TAG = "AlwaysRunningService"
    private val CHANNEL_ID = "AlwaysRunningServiceChannel"
    private var webSocketClient: WebSocketClientPin? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this, CHANNEL_ID, "Always Running Service Channel")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(this, CHANNEL_ID, "Always Running Service", "This service is running in the background.")
        startForeground(1, notification)
        val tokenManager = JwtTokenManager(this)

        if(tokenManager.getIsLogin()) {
            if (webSocketClient == null) {
                webSocketClient = WebSocketClientPin(
                    this,
                    "ws://192.168.243.116:8000/ws/general/${JwtTokenManager(this).getUserId()}/?token=${
                        JwtTokenManager(
                            this
                        ).getAccessJwt()
                    }"
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not binding, so return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Always Running Service destroyed")
        // Optionally restart the service if it gets killed
    }

}