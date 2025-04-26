package com.example.whatseye.services

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.*
import android.provider.Settings

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val topApp = getForegroundAppPackageName()
            if (topApp == "com.android.settings"  && Settings.canDrawOverlays(this@AppMonitorService)) {
                startService(Intent(this@AppMonitorService, LockOverlayService::class.java))
            }
            handler.postDelayed(this, 3000)
        }
    }


    private fun getForegroundAppPackageName(): String? {
        val time = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(time - 10000, time)
        val event = UsageEvents.Event()
        var lastForeground: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForeground = event.packageName
            }
        }
        return lastForeground
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }
}
