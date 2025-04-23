package com.example.whatseye.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatseye.MainActivity
import com.example.whatseye.R

class AlwaysRunningService : Service() {

    private val TAG = "AlwaysRunningService"
    private val CHANNEL_ID = "AlwaysRunningServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        // If the service gets killed, restart with a null intent
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

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Always Running Service")
            .setContentText("This service is running in the background.")
            .setSmallIcon(R.drawable.app_icon) // Use your own icon
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Always Running Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}