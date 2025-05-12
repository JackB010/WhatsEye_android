package com.example.whatseye.access

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton

class OverlayPermissionActivity : AppCompatActivity() {

    private lateinit var buttonEnableOverlay: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay_permission)

        buttonEnableOverlay = findViewById(R.id.buttonEnableOverlay)

        updateStatus()

        buttonEnableOverlay.setOnClickListener {
            if (!hasOverlayPermission()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            updateStatus()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun updateStatus() {
        if (hasOverlayPermission()) {
            val intent = Intent(this, UsageAccessPermissionActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1002
    }
}