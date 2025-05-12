package com.example.whatseye.access

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton

class BackgroundLocationPermissionActivity : AppCompatActivity() {

    private lateinit var buttonEnableBackgroundLocation: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_location_permission)

        buttonEnableBackgroundLocation = findViewById(R.id.buttonEnableBackgroundLocation)

        updateButtonState()

        buttonEnableBackgroundLocation.setOnClickListener {
            when {
                !hasForegroundLocationPermission() -> requestForegroundLocationPermission()
                !hasBackgroundLocationPermission() -> requestBackgroundLocationPermission()
                else -> proceedToNextScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
        if (hasForegroundLocationPermission() && hasBackgroundLocationPermission()) {
            proceedToNextScreen()
        }
    }

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Background location permission is not required below Android Q
            true
        }
    }

    private fun requestForegroundLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showPermissionRationaleDialog(
                title = "Permission de localisation requise",
                message = "Cette application a besoin d'accéder à votre localisation pour fonctionner correctement.",
                requestCode = REQUEST_CODE_FOREGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FOREGROUND_LOCATION
            )
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                showPermissionRationaleDialog(
                    title = "Permission de localisation en arrière-plan requise",
                    message = "Cette application a besoin d'accéder à votre localisation en arrière-plan pour fournir des mises à jour continues.",
                    requestCode = REQUEST_CODE_BACKGROUND_LOCATION
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_CODE_BACKGROUND_LOCATION
                )
            }
        } else {
            proceedToNextScreen()
        }
    }

    private fun showPermissionRationaleDialog(title: String, message: String, requestCode: Int) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Autoriser") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        if (requestCode == REQUEST_CODE_FOREGROUND_LOCATION)
                            Manifest.permission.ACCESS_FINE_LOCATION
                        else
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    requestCode
                )
            }
            .setNegativeButton("Annuler") { _, _ ->
                updateButtonState()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_CODE_FOREGROUND_LOCATION -> {
                    if (hasForegroundLocationPermission() && !hasBackgroundLocationPermission()) {
                        requestBackgroundLocationPermission()
                    }
                }
                REQUEST_CODE_BACKGROUND_LOCATION -> {
                    if (hasBackgroundLocationPermission()) {
                        proceedToNextScreen()
                    }
                }
            }
        } else {
            // Permission denied
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permissions[0]
                )
            ) {
                // User selected "Don't ask again"
                AlertDialog.Builder(this)
                    .setTitle("Permission refusée")
                    .setMessage("Cette fonctionnalité nécessite la permission de localisation. Veuillez l'activer dans les paramètres de l'application.")
                    .setPositiveButton("Aller aux paramètres") { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
        updateButtonState()
    }

    private fun updateButtonState() {
        buttonEnableBackgroundLocation.isEnabled = !hasBackgroundLocationPermission()
        buttonEnableBackgroundLocation.text =
            if (hasBackgroundLocationPermission()) "Permission accordée" else "Activer la localisation en arrière-plan"
    }

    private fun proceedToNextScreen() {
        val intent = Intent(this, NotificationPermissionActivity::class.java) // Replace with next activity
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_FOREGROUND_LOCATION = 2001
        private const val REQUEST_CODE_BACKGROUND_LOCATION = 2002
    }
}