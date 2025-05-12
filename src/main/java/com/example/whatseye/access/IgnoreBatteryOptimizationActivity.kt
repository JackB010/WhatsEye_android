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
        setContentView(R.layout.activity_ignore_battery_optimization)

        buttonEnableIgnoreBatteryOptimization =
            findViewById(R.id.buttonEnableIgnoreBatteryOptimization)

        buttonEnableIgnoreBatteryOptimization.setOnClickListener {
            if (!hasIgnoreBatteryOptimizationPermission()) {
                requestIgnoreBatteryOptimization()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasIgnoreBatteryOptimizationPermission(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun updateStatus() {
        if (hasIgnoreBatteryOptimizationPermission() ) {
            val intent = Intent(this, FileAccessPermissionActivity::class.java)
            startActivity(intent)
            finish() // Remove this activity from back stack
        }
    }
}
