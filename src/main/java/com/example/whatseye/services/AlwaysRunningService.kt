package com.example.whatseye.services
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientGeneral
import com.example.whatseye.api.ws.WebSocketClientNotification
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel


class AlwaysRunningService : Service() {

    private val TAG = "AlwaysRunningService"
    private val CHANNEL_ID = "AlwaysRunningServiceChannel"
    private var webSocketClient: WebSocketClientGeneral? = null
    private val webSocketManager = WebSocketClientNotification.getInstance()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this, CHANNEL_ID, "Always Running Service Channel")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(this, CHANNEL_ID, "Always Running Service", "This service is running in the background.")
        startForeground(1, notification)
        if(JwtTokenManager(this).getIsLogin()) {
            webSocketClient = WebSocketGeneralManager.getInstance(this)
            webSocketManager.initialize(this)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not binding, so return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Always Running Service destroyed")
    }

}