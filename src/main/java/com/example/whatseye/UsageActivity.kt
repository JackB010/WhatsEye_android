package com.example.whatseye

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class UsageActivity : AppCompatActivity() {

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var tvUsageInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        tvUsageInfo = findViewById(R.id.tv_usage_info)

        val btnCheckUsage: Button = findViewById(R.id.btn_check_usage)
        btnCheckUsage.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } else {
                getAppUsage()
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAppUsage() {
        val usageInfo = StringBuilder()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val now = Calendar.getInstance()

        for (i in 0 until 7) {
            val startCal = now.clone() as Calendar
            startCal.add(Calendar.DAY_OF_YEAR, -i)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 1)

            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis

            val events = usageStatsManager.queryEvents(startTime, endTime)
            val dateLabel = dateFormatter.format(Date(startTime))

            var dailyUsage = 0L
            var lastForegroundTime = 0L
            var lastPackage: String? = null

            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                if (event.packageName != "com.whatsapp") continue

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastForegroundTime = event.timeStamp
                        lastPackage = event.packageName
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (lastForegroundTime > 0 && lastPackage == "com.whatsapp") {
                            dailyUsage += event.timeStamp - lastForegroundTime
                            lastForegroundTime = 0L
                            lastPackage = null
                        }
                    }
                }
            }

            val usageMinutes = dailyUsage / 1000 / 60
            usageInfo.append("Date: $dateLabel\n")
            usageInfo.append("Screen Time: $usageMinutes minutes\n\n")
        }

        tvUsageInfo.text = usageInfo.toString()
    }

    private fun getHourlyUsage() {
        val usageInfo = StringBuilder()
        val calendar = Calendar.getInstance()

        // Start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // Now
        val endTime = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val hourlyUsage = LongArray(24) // milliseconds per hour

        var lastForegroundTime = 0L
        var lastPackage: String? = null

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.packageName != "com.whatsapp") continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastForegroundTime = event.timeStamp
                    lastPackage = event.packageName
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (lastForegroundTime > 0 && lastPackage == "com.whatsapp") {
                        val duration = event.timeStamp - lastForegroundTime
                        val calendarHour = Calendar.getInstance()
                        calendarHour.timeInMillis = lastForegroundTime
                        val hour = calendarHour.get(Calendar.HOUR_OF_DAY)
                        hourlyUsage[hour] += duration

                        // Reset
                        lastForegroundTime = 0L
                        lastPackage = null
                    }
                }
            }
        }

        usageInfo.append("WhatsApp Usage for Today (Hourly):\n\n")
        for (hour in 0 until 24) {
            val minutes = hourlyUsage[hour] / 1000 / 60 // convert to minutes
            val res = (hourlyUsage[hour] / 1000) % 60 // convert to minutes
            val hourFormatted = String.format("%02d:00 - %02d:59", hour, hour)
            usageInfo.append("$hourFormatted → $minutes minutes and $res second\n")
        }

        tvUsageInfo.text = usageInfo.toString()
    }
}
