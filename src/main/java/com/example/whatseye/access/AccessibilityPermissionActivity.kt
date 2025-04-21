package com.example.whatseye.access

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton

class AccessibilityPermissionActivity : AppCompatActivity() {
    private lateinit var buttonGrantPermission: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_permission)

        buttonGrantPermission = findViewById(R.id.buttonGrantPermission)
        updateStatus()

        buttonGrantPermission.setOnClickListener {
            // Start the accessibility service settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
        override fun onResume() {
            super.onResume()
            updateStatus()
        }

        private fun updateStatus() {
            val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val isAccessibilityEnabled = accessibilityManager.isEnabled

            if (isAccessibilityEnabled) {
                val intent = Intent(this, DeviceAdminPermissionActivity::class.java)
                startActivity(intent)
            }
        }
    }

