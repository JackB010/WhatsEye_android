package com.example.whatseye.access


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R

class FileAccessPermissionActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_access_permission)

        val agreeButton: Button = findViewById(R.id.buttonEnableFileAccess)

        agreeButton.setOnClickListener {
            requestStoragePermission()
        }

        checkPermission()
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermission() {
        if (isStoragePermissionGranted()) {
            permissionGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestStoragePermission() {
        if (!isStoragePermissionGranted()) {
            // Navigate to Settings to allow Manage External Storage permission
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            // Permission already granted
            permissionGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isStoragePermissionGranted(): Boolean {
        return Environment.isExternalStorageManager() // Check for MANAGE_EXTERNAL_STORAGE permission
    }

    private fun permissionGranted() {

    }
}