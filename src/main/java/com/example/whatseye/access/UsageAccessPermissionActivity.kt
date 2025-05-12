package com.example.whatseye.access

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.whatseye.R

class UsageAccessPermissionActivity : AppCompatActivity() {

    private lateinit var buttonEnableUsageAccess: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_access_permission)

        buttonEnableUsageAccess = findViewById(R.id.buttonEnableUsageAccess)

        updateStatus()

        buttonEnableUsageAccess.setOnClickListener {
            if (!hasUsageAccessPermission()) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasUsageAccessPermission(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 3600 * 24,
            System.currentTimeMillis()
        )

        return usageStatsList != null && usageStatsList.isNotEmpty()
    }

    private fun updateStatus() {
        if (hasUsageAccessPermission()) {
            val intent = Intent(this, AutoStartAgreementActivity::class.java)
            startActivity(intent)
        }
    }
}