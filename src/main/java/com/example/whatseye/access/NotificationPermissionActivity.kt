package com.example.whatseye.access

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton

class NotificationPermissionActivity : AppCompatActivity() {

    private lateinit var buttonEnableNotifications: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_permission)

        buttonEnableNotifications = findViewById(R.id.buttonEnableNotifications)

        buttonEnableNotifications.setOnClickListener {
            requestNotificationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED && isNotificationServiceEnabled()
        } else {
            isNotificationServiceEnabled()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION_PERMISSION
            )
        } else {
            openNotificationListenerSettingsIfNeeded()
        }
    }

    private fun openNotificationListenerSettingsIfNeeded() {
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            updateStatus()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openNotificationListenerSettingsIfNeeded()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus() {
        if (hasNotificationPermission()) {
            val intent = Intent(this, MicrophonePermissionActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return !TextUtils.isEmpty(enabledListeners) && enabledListeners.contains(pkgName)
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
    }
}
