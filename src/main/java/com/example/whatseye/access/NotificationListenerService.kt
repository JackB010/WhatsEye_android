package com.example.whatseye.access;


import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // Handle notification posting here
        sbn?.let {
            val packageName = it.packageName
            val notificationText = it.notification.extras.getCharSequence("android.text")
            Log.d("NotificationListener", "Package: $packageName, Text: $notificationText")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        // Handle notification removal here
        sbn?.let {
            val packageName = it.packageName
            Log.d("NotificationListener", "Removed Package: $packageName")
        }
    }
}
