package com.example.whatseye.noLogin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.example.whatseye.access.AccessibilityPermissionActivity
import com.example.whatseye.access.DeviceAdminPermissionActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker

class LoginActivity : AppCompatActivity() {
    private lateinit var toggleButton: MaterialButton
    private lateinit var usernameLoginLayout: View
    private lateinit var phoneLoginLayout: View

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        toggleButton = findViewById(R.id.toggleButton)
        usernameLoginLayout = findViewById(R.id.usernameLoginLayout)
        phoneLoginLayout = findViewById(R.id.phoneLoginLayout)
        var isUsernameLogin = true
        val submitButton: Button = findViewById(R.id.buttonSubmitLogin)
        toggleButton.setOnClickListener {
            if (isUsernameLogin) {
                usernameLoginLayout.visibility = View.GONE
                phoneLoginLayout.visibility = View.VISIBLE
                toggleButton.text = "Switch to Username Login"
            } else {
                usernameLoginLayout.visibility = View.VISIBLE
                phoneLoginLayout.visibility = View.GONE
                toggleButton.text = "Switch to Phone Login"
            }
            isUsernameLogin = !isUsernameLogin
        }

        // Optional: Handle submit buttons
        findViewById<MaterialButton>(R.id.buttonSubmitLogin).setOnClickListener {
            val username = findViewById<TextInputEditText>(R.id.etUsernameLogin).text.toString()
            val password = findViewById<TextInputEditText>(R.id.etPasswordLogin).text.toString()
            // Add your username login logic here
            println("Username: $username, Password: $password")
        }

        submitButton.setOnClickListener {


            val password =
                findViewById<TextInputEditText>(R.id.etPasswordLogin).text.toString()
            if (isUsernameLogin) {
                val username =
                    findViewById<TextInputEditText>(R.id.etUsernameLogin).text.toString()
                // Add your username login logic here
                println("Username: $username, Password: $password")
            } else {
                val countryCodePicker = findViewById<CountryCodePicker>(R.id.countryCodePicker)
                val phone = findViewById<TextInputEditText>(R.id.etPhoneLogin)
                countryCodePicker.registerCarrierNumberEditText(phone)
                val fullNumber = countryCodePicker.fullNumberWithPlus
                // Add your phone login logic here
                println("Phone: $fullNumber, Password: $password")
            }
            submitButton.setOnClickListener {

                val url = intent.getStringExtra("url")

                // Implement your login logic here
                Toast.makeText(this, "Login URL: $url\n", Toast.LENGTH_LONG).show()
            }
        }

        val forgotPasswordText: TextView  = findViewById(R.id.forgotPasswordText)
        forgotPasswordText.setOnClickListener{
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)

        }

    }
}