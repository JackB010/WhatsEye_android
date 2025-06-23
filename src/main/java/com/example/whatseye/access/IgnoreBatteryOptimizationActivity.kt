package com.example.whatseye.access

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.whatseye.R

class IgnoreBatteryOptimizationActivity : AppCompatActivity() {

    private lateinit var buttonEnableIgnoreBatteryOptimization: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if permission is already granted before setting content view
        if (hasIgnoreBatteryOptimizationPermission()) {
            proceedToNextActivity()
            return
        }

        setContentView(R.layout.activity_ignore_battery_optimization)

        // Initialize button
        buttonEnableIgnoreBatteryOptimization =
            findViewById(R.id.buttonEnableIgnoreBatteryOptimization)

        // Set click listener for requesting battery optimization permission
        buttonEnableIgnoreBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permission status when activity resumes
        updateStatus()
    }

    /**
     * Checks if the app is ignoring battery optimizations
     */
    private fun hasIgnoreBatteryOptimizationPermission(): Boolean {
        return try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Requests permission to ignore battery optimizations
     */
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Handle cases where the intent cannot be handled
            // You might want to show a toast or dialog to inform the user
        }
    }

    /**
     * Updates the activity state based on permission status
     */
    private fun updateStatus() {
        if (hasIgnoreBatteryOptimizationPermission()) {
            proceedToNextActivity()
        }
    }

    /**
     * Proceeds to the next activity and finishes current one
     */
    private fun proceedToNextActivity() {
        val intent = Intent(this, FileAccessPermissionActivity::class.java)
        startActivity(intent)
        finish() // Remove this activity from back stack
    }
}