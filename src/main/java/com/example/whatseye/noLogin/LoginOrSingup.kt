package com.example.whatseye.noLogin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import com.example.whatseye.qrCode.CustomCaptureActivity


class LoginOrSingup : AppCompatActivity() {

    private var isLogin: Boolean = false // Track whether login or signup was clicked

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_or_singup)

        val buttonLogin: Button = findViewById(R.id.btnLoginMain)
        val buttonSignup: Button = findViewById(R.id.btnSignupMain)
        buttonLogin.setOnClickListener {
            isLogin = true // Set the action to login
            initiateQRCodeScan()
        }

        buttonSignup.setOnClickListener {
            isLogin = false // Set the action to signup
            initiateQRCodeScan()
        }
    }

    private fun initiateQRCodeScan() {
        val options = ScanOptions()
        options.setPrompt("Volume up to flash on")
        options.setBeepEnabled(false)
        options.setOrientationLocked(true)
        options.setBarcodeImageEnabled(true)
        options.setCaptureActivity(CustomCaptureActivity::class.java)
        barLaucher.launch(options)

    }

    private var barLaucher: ActivityResultLauncher<ScanOptions> =
        registerForActivityResult<ScanOptions, ScanIntentResult>(
            ScanContract()
        ) { result: ScanIntentResult ->

            if (result.contents != null) {
                val qrCodeData = result.contents.trim()

                try {
                    // Parse the JSON data
                    val jsonObject = JSONObject(qrCodeData)
                    val id = jsonObject.getString("id")
                    val qrCode = jsonObject.getString("qr_code")

                    val targetActivity = if (isLogin) {
                        LoginActivity::class.java
                    } else {
                        SignupActivity::class.java
                    }

                    val intent = Intent(this, targetActivity).apply {
                        putExtra("id", id)
                        putExtra("qrCode", qrCode)
                    }
                    startActivity(intent)

                } catch (e: Exception) {
                    // Handle JSON parsing error
                    Toast.makeText(this, "Invalid QR code data format", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                navigateToMainActivity()
            }

        }
    private fun navigateToMainActivity() {
        val intent = Intent(this, LoginOrSingup::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
        startActivity(intent)
        finish() // Optionally finish current activity
    }
}