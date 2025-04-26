package com.example.whatseye.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.whatseye.R

fun createNotification(context: Context, id: String, title: String, text: String): Notification {
    return NotificationCompat.Builder(context, id)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(R.drawable.app_icon) // Ensure this drawable exists
        .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority for older versions
        .build()
}

fun createNotificationChannel(context: Context, id: String, name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val serviceChannel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }
}