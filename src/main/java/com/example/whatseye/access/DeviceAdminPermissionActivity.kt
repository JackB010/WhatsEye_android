package com.example.whatseye.access

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.example.whatseye.R

class DeviceAdminPermissionActivity : AppCompatActivity() {

    private lateinit var buttonActivateAdmin: MaterialButton
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_admin_permission)

        buttonActivateAdmin = findViewById(R.id.buttonActivateAdmin)

        // Initialize Device Policy Manager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, DeviceAdminReceiver::class.java)

        updateStatus()

        buttonActivateAdmin.setOnClickListener {
            if (!devicePolicyManager.isAdminActive(componentName)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "This app needs device admin privileges to manage app usage."
                )
                startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            updateStatus()
        }
    }

    private fun updateStatus() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(this, OverlayPermissionActivity::class.java)
            startActivity(intent)        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }
}