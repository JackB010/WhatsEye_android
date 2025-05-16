package com.example.whatseye.access

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.profile.ProfileActivity
import com.example.whatseye.utils.areAllPermissionsGranted
import com.example.whatseye.whatsapp.WhatsAppLinkActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MicrophonePermissionActivity : AppCompatActivity() {
    private val MICROPHONE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_microphone_permission)

        val grantPermissionButton = findViewById<MaterialButton>(R.id.buttonGrantPermission)
        grantPermissionButton.setOnClickListener {
            if (!isMicrophonePermissionGranted()) {
                requestMicrophonePermission()
            } else {
                proceedToNextActivity()
            }
        }

        // Vérifie au lancement
        if (isMicrophonePermissionGranted()) {
            proceedToNextActivity()
        }
    }

    private fun isMicrophonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            )

            when {
                granted -> {
                    proceedToNextActivity()
                }
                !granted && !shouldShowRationale -> {
                    // L'utilisateur a sélectionné "Ne plus demander"
                    showGoToSettingsDialog()
                }
                else -> {
                    // Permission refusée sans "Ne plus demander"
                    showPermissionRationaleDialog()
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Autorisation requise")
            .setMessage("L'application a besoin de l'autorisation d'utiliser le microphone pour fonctionner correctement.")
            .setPositiveButton("Réessayer") { _, _ ->
                requestMicrophonePermission()
            }
            .setNegativeButton("Quitter") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGoToSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Autorisation refusée")
            .setMessage("Vous avez refusé l'autorisation du microphone. Veuillez aller dans les paramètres pour l'activer manuellement.")
            .setPositiveButton("Aller aux paramètres") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Quitter") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        if (hasMicrophonePermission()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    private fun proceedToNextActivity() {
        val tokenManager = JwtTokenManager(this)
        val intent = if (!areAllPermissionsGranted(this)) {
            Intent(this, AccessibilityPermissionActivity::class.java)
        } else {
            if (!tokenManager.getIsLoginWhatsApp()) {
                Intent(this, WhatsAppLinkActivity::class.java)
            } else {
                Intent(this, ProfileActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
