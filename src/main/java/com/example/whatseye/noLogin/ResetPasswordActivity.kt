package com.example.whatseye.noLogin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var toggleButton: MaterialButton
    private lateinit var usernameRestLayout: View
    private lateinit var phoneResetLayout: View
    private lateinit var data: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)


        toggleButton = findViewById(R.id.toggleButton)
        usernameRestLayout = findViewById(R.id.usernameRestLayout)
        phoneResetLayout = findViewById(R.id.phoneResetLayout)
        var isUsernameLogin = true
        val submitButton: Button = findViewById(R.id.buttonSubmitReset)
        toggleButton.setOnClickListener {
            if (isUsernameLogin) {
                usernameRestLayout.visibility = View.GONE
                phoneResetLayout.visibility = View.VISIBLE
                toggleButton.text = "Switch to Username Login"
            } else {
                usernameRestLayout.visibility = View.VISIBLE
                phoneResetLayout.visibility = View.GONE
                toggleButton.text = "Switch to Phone Login"
            }
            isUsernameLogin = !isUsernameLogin
        }

        // Optional: Handle submit buttons
        findViewById<MaterialButton>(R.id.buttonSubmitReset).setOnClickListener {
            if (isUsernameLogin) {
                var data = findViewById<TextInputEditText>(R.id.etUsernameReset).text.toString()
            }
            else {
                var data = findViewById<TextInputEditText>(R.id.etPhoneReset).text.toString()
            }

            println("Username: $data ")
        }

       /* submitButton.setOnClickListener {

            }
            submitButton.setOnClickListener {

                val url = intent.getStringExtra("url")

                // Implement your login logic here
                Toast.makeText(this, "Login URL: $url\n", Toast.LENGTH_LONG).show()
            }
        }*/
    }
}