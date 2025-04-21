package com.example.whatseye.access

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.MainActivity
import com.google.android.material.button.MaterialButton
import com.example.whatseye.R

class IgnoreBatteryOptimizationActivity : AppCompatActivity() {

    private lateinit var buttonEnableIgnoreBatteryOptimization: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ignore_battery_optimization)

        buttonEnableIgnoreBatteryOptimization = findViewById(R.id.buttonEnableIgnoreBatteryOptimization)

        updateStatus()

        buttonEnableIgnoreBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasIgnoreBatteryOptimizationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        if (hasIgnoreBatteryOptimizationPermission()) {
            val intent = Intent(this, NotificationPermissionActivity::class.java)
            startActivity(intent)
        }
    }
}