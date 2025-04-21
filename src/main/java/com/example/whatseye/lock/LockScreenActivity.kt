package com.example.whatseye.lock


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R

class LockScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)

        val passwordInput = findViewById<EditText>(R.id.password_input)
        val unlockButton = findViewById<Button>(R.id.unlock_button)

        unlockButton.setOnClickListener {
            // Check the password entered (replace "1234" with your desired password)
            if (passwordInput.text.toString() == "1234") {
                // If the correct password is entered, remove the overlay
                finish() // Closes this activity
            } else {
                passwordInput.error = "Incorrect Password"
            }
        }
    }
}