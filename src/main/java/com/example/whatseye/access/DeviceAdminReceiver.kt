package com.example.whatseye.access

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
    }

    override fun onDisabled(context: Context, intent: Intent) {
    }
}