package com.example.whatseye.access


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton
import com.judemanutd.autostarter.AutoStartPermissionHelper

class AutoStartAgreementActivity : AppCompatActivity()  {

    private lateinit var buttonEnableBackgroundAutoStart: MaterialButton
    private var visited: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_start_agreement)

        buttonEnableBackgroundAutoStart = findViewById(R.id.buttonEnableBackgroundAutoStart)
        buttonEnableBackgroundAutoStart.setOnClickListener {
            visited = !visited
            AutoStartPermissionHelper.getInstance().getAutoStartPermission(this, true, false)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun hasBackgroundAutoStartPermission(): Boolean {
        return AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(this, true);
    }


    private fun updateStatus() {
        if (hasBackgroundAutoStartPermission() && visited) {
            val intent = Intent(this, IgnoreBatteryOptimizationActivity::class.java)
            startActivity(intent)

        }
    }


}