package com.example.whatseye.access

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton
import com.judemanutd.autostarter.AutoStartPermissionHelper


class AutoStartAgreementActivity : AppCompatActivity() {

    private lateinit var buttonEnableBackgroundAutoStart: MaterialButton
    private var launchedSettings = false  // Track whether we opened the settings screen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_start_agreement)
        buttonEnableBackgroundAutoStart = findViewById(R.id.buttonEnableBackgroundAutoStart)

        buttonEnableBackgroundAutoStart.setOnClickListener {
            requestAutoStartPermission()
            launchedSettings = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (launchedSettings) {
            // User came back from settings, go to MainActivity
            val intent = Intent(this, IgnoreBatteryOptimizationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun requestAutoStartPermission() {
        val helper = AutoStartPermissionHelper.getInstance()
        val success = helper.getAutoStartPermission(this, open = true, newTask = false)

        if (!success) {
            openAutoStartSettingsManually()
        }
    }

    private fun openAutoStartSettingsManually() {
        try {
            val intent = when (Build.MANUFACTURER.lowercase()) {
                "xiaomi" -> Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                "oppo" -> Intent().apply {
                    component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                }
                "vivo" -> Intent().apply {
                    component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
                "huawei" -> Intent().apply {
                    component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                }
                else -> null
            }
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Auto-start settings not available on this device.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open auto-start settings.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

}
