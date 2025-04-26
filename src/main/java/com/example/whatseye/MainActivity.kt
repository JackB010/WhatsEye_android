package com.example.whatseye

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.noLogin.LoginOrSingup
import com.example.whatseye.services.AlwaysRunningService
import com.example.whatseye.services.AppMonitorService
import com.example.whatseye.worker.UsageWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, AlwaysRunningService::class.java)
        startForegroundService(serviceIntent) // Use startService(serviceIntent) for pre-Oreo

        val tokenManager = JwtTokenManager(this)

        if(tokenManager.getIsLogin()) {


            startService(Intent(this, AppMonitorService::class.java))

            val workRequest = PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "UsageDataSyncWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "AlwaysRunningServiceChannel", "This service is running in the background.",
                    NotificationManager.IMPORTANCE_HIGH
                )
                val notificationManger =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManger.createNotificationChannel(channel)
            }
        }

        if(!tokenManager.getIsLogin()){
            val intent = Intent(this, LoginOrSingup::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
            startActivity(intent)
            finish() // Optionally finish current activity
        }
    }
}