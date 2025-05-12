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
import com.example.whatseye.access.AccessibilityPermissionActivity
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.noLogin.LoginOrSingup
import com.example.whatseye.profile.ProfileActivity
import com.example.whatseye.services.AlwaysRunningService
import com.example.whatseye.utils.areAllPermissionsGranted
import com.example.whatseye.whatsapp.WhatsAppLinkActivity
import com.example.whatseye.worker.TokenRefreshWorker
import com.example.whatseye.worker.UsageWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var intent: Intent

    @SuppressLint("NewApi", "UnsafeIntentLaunch")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenManager = JwtTokenManager(this)
        tokenManager.setIsLoginWhatsApp(true)
        if (tokenManager.getIsLogin()) {

//            if(tokenManager.getIsLoginWhatsApp()) {
//
//            }

            val serviceIntent = Intent(this, AlwaysRunningService::class.java)
            startForegroundService(serviceIntent) // Use startService(serviceIntent) for pre-Oreo

            val workUsageRequest = PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "UsageDataSyncWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                workUsageRequest
            )

            val workTokenRefreshRequest =
                PeriodicWorkRequestBuilder<TokenRefreshWorker>(48, TimeUnit.HOURS)
                    .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "TokenRefreshSyncWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                workTokenRefreshRequest
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



        intent = if (!tokenManager.getIsLogin()) {
            Intent(this, LoginOrSingup::class.java)
        } else {
            if (!areAllPermissionsGranted(this)) {
                Intent(this, AccessibilityPermissionActivity::class.java)
            } else {
                if (!tokenManager.getIsLoginWhatsApp()) {
                    Intent(this, WhatsAppLinkActivity::class.java)
                } else {
                    Intent(this, ProfileActivity::class.java)
                }
            }
        }

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
        startActivity(intent)
        finish()
    }
}