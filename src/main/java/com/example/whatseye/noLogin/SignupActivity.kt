package com.example.whatseye.noLogin

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.example.whatseye.access.AccessibilityPermissionActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import java.util.Date
import java.util.Locale


class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)


        // Gender Dropdown
        val genderField = findViewById<AutoCompleteTextView>(R.id.etGenderSignup)
        val genderOptions = resources.getStringArray(R.array.gender_options)
        // Set up the adapter for the dropdown options

        val adapter = ArrayAdapter(this, R.layout.item_dropdown, genderOptions)
        genderField.setAdapter(adapter)



        // Optionally, you can set an item click listener for selection
        genderField.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>, view: View?, position: Int, id: Long ->
                val selectedGender = parent.getItemAtPosition(position) as String
            }

        // Birthday Date Picker
        val birthdayField = findViewById<TextInputEditText>(R.id.etBirthdaySignup)
        birthdayField.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthday")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.UK)
                birthdayField.setText(dateFormat.format(Date(selection)))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Submit Button
        val submitButton = findViewById<MaterialButton>(R.id.buttonSignupSubmit)
        submitButton.setOnClickListener {
            val intent = Intent(this, AccessibilityPermissionActivity::class.java)
            startActivity(intent)
            val username = findViewById<TextInputEditText>(R.id.etUsernameSignup).text.toString()
            val countryCodePicker = findViewById<CountryCodePicker>(R.id.countryCodePicker)
            val phoneField = findViewById<TextInputEditText>(R.id.etPhoneSignup)
            countryCodePicker.registerCarrierNumberEditText(phoneField)
            val phoneNumber = countryCodePicker.fullNumberWithPlus
            val password = findViewById<TextInputEditText>(R.id.etPasswordSignup).text.toString()
            val confirmPassword = findViewById<TextInputEditText>(R.id.etPasswordConfirmSignup).text.toString()
            val gender = genderField.text.toString()
            val birthday = birthdayField.text.toString()

            if (password == confirmPassword) {
                // Add your signup logic here (e.g., API call)
                println("Signup - Username: $username, Phone: $phoneNumber, Password: $password, Gender: $gender, Birthday: $birthday")
            } else {
                // Handle password mismatch
                println("Passwords do not match")
            }
        }
    }
}