package com.example.whatseye.access

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
                AlertDialog.Builder(this)
                    .setTitle("Autorisation d'affichage requise")
                    .setMessage("Cette application doit afficher des pop-ups pour [votre fonctionnalité]. Veuillez activer 'Afficher par-dessus les autres applications' dans l'écran suivant.")
                    .setPositiveButton("Aller aux paramètres") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
                    }
                    .setNegativeButton("Annuler") { dialog, _ -> dialog.dismiss() }
                    .setCancelable(false)
                    .show()
            }
        }

        // Vérifier l'état de l'optimisation de la batterie
        if (!isIgnoringBatteryOptimizations()) {
            requestBatteryOptimizationExemption()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION || requestCode == REQUEST_CODE_XIAOMI_PERMISSION || requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            updateStatus()
            if (hasOverlayPermission() && "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
                requestBackgroundPopupPermission()
            }
            if (!isIgnoringBatteryOptimizations()) {
                requestBatteryOptimizationExemption()
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    /**
     * Vérifie si l'application est exemptée des optimisations de batterie, lui permettant de fonctionner en arrière-plan.
     * @return true si l'application ignore les optimisations de batterie, false sinon ou sur les versions d'API non prises en charge.
     */
    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true // L'optimisation de batterie n'est pas applicable avant l'API 23
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
    }

    private fun updateStatus() {
        if (hasOverlayPermission()) {
            val intent = Intent(this, UsageAccessPermissionActivity::class.java)
            startActivity(intent)
            finish() // Terminer cette activité pour éviter de revenir en arrière
        }
    }

    private fun requestBackgroundPopupPermission() {
        if ("xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
            try {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                intent.putExtra("extra_pkgname", packageName)
                startActivityForResult(intent, REQUEST_CODE_XIAOMI_PERMISSION)
            } catch (e: Exception) {
                Toast.makeText(this, "Veuillez activer l'autorisation de pop-up en arrière-plan dans les paramètres", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(this)
                .setTitle("Optimisation de la batterie")
                .setMessage("Pour garantir un fonctionnement fluide en arrière-plan, veuillez désactiver l'optimisation de la batterie pour cette application.")
                .setPositiveButton("Aller aux paramètres") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
                }
                .setNegativeButton("Annuler") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }
    }

    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1002
        private const val REQUEST_CODE_XIAOMI_PERMISSION = 1003
        private const val REQUEST_CODE_BATTERY_OPTIMIZATION = 1004
    }
}