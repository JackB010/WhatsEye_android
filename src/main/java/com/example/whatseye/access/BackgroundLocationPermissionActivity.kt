package com.example.whatseye.access

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton

class BackgroundLocationPermissionActivity : AppCompatActivity() {

    private lateinit var buttonEnableBackgroundLocation: MaterialButton
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_location_permission)

        buttonEnableBackgroundLocation = findViewById(R.id.buttonEnableBackgroundLocation)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        updateButtonState()

        buttonEnableBackgroundLocation.setOnClickListener {
            when {
                !hasForegroundLocationPermission() -> requestForegroundLocationPermission()
                !hasBackgroundLocationPermission() -> requestBackgroundLocationPermission()
                !isGPSEnabled() -> requestGPSEnablement()
                else -> proceedToNextScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
        if (hasForegroundLocationPermission() && hasBackgroundLocationPermission() && isGPSEnabled()) {
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
            true // La permission de localisation en arrière-plan n'est pas requise avant Android Q
        }
    }

    private fun isGPSEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestForegroundLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showPermissionRationaleDialog(
                title = "Permission GPS requise",
                message = "Cette application nécessite l'accès à votre localisation GPS pour fournir des fonctionnalités basées sur la localisation précise.",
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
                    title = "Permission GPS en arrière-plan requise",
                    message = "Cette application a besoin d'accéder à votre localisation GPS en arrière-plan pour fournir des mises à jour continues, même lorsque l'application n'est pas utilisée.",
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
            if (isGPSEnabled()) {
                proceedToNextScreen()
            } else {
                requestGPSEnablement()
            }
        }
    }

    private fun requestGPSEnablement() {
        AlertDialog.Builder(this)
            .setTitle("GPS désactivé")
            .setMessage("Le GPS est requis pour utiliser cette fonctionnalité. Veuillez activer le GPS dans les paramètres de votre appareil.")
            .setPositiveButton("Aller aux paramètres") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_ENABLE_GPS)
            }
            .setNegativeButton("Annuler") { _, _ ->
                updateButtonState()
            }
            .setCancelable(false)
            .show()
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
                    } else if (hasForegroundLocationPermission() && !isGPSEnabled()) {
                        requestGPSEnablement()
                    }
                }
                REQUEST_CODE_BACKGROUND_LOCATION -> {
                    if (hasBackgroundLocationPermission() && !isGPSEnabled()) {
                        requestGPSEnablement()
                    } else if (hasBackgroundLocationPermission()) {
                        proceedToNextScreen()
                    }
                }
            }
        } else {
            // Permission refusée
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permissions[0]
                )
            ) {
                // L'utilisateur a sélectionné "Ne plus demander"
                AlertDialog.Builder(this)
                    .setTitle("Permission refusée")
                    .setMessage("Cette fonctionnalité nécessite la permission GPS. Veuillez l'activer dans les paramètres de l'application.")
                    .setPositiveButton("Aller aux paramètres") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
        updateButtonState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_GPS) {
            if (isGPSEnabled()) {
                if (hasForegroundLocationPermission() && hasBackgroundLocationPermission()) {
                    proceedToNextScreen()
                }
            } else {
                updateButtonState()
            }
        }
    }

    private fun updateButtonState() {
        buttonEnableBackgroundLocation.isEnabled = !hasBackgroundLocationPermission() || !isGPSEnabled()
        buttonEnableBackgroundLocation.text = when {
            !hasForegroundLocationPermission() -> "Activer la localisation GPS"
            !hasBackgroundLocationPermission() -> "Activer la localisation GPS en arrière-plan"
            !isGPSEnabled() -> "Activer le GPS"
            else -> "Permission accordée"
        }
    }

    private fun proceedToNextScreen() {
        val intent = Intent(this, NotificationPermissionActivity::class.java) // Remplacez par l'activité suivante
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_FOREGROUND_LOCATION = 2001
        private const val REQUEST_CODE_BACKGROUND_LOCATION = 2002
        private const val REQUEST_CODE_ENABLE_GPS = 2003
    }
}