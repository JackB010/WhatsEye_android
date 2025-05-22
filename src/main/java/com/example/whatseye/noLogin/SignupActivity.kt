package com.example.whatseye.noLogin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.jwt.JWT
import com.example.whatseye.R
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientGeneral
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.dataType.data.ChildJoinRequest
import com.example.whatseye.dataType.data.PhoneNumber
import com.example.whatseye.profile.ProfileActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hbb20.CountryCodePicker
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class SignupActivity : AppCompatActivity() {

    private lateinit var birthdayField: TextInputEditText
    private lateinit var birthdayLayout: TextInputLayout
    private var webSocketClient: WebSocketClientGeneral? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val genderField = findViewById<AutoCompleteTextView>(R.id.etGenderSignup)
        val genderOptions = resources.getStringArray(R.array.gender_options)
        val adapter = ArrayAdapter(this, R.layout.item_dropdown, genderOptions)
        genderField.setAdapter(adapter)

        birthdayField = findViewById(R.id.etBirthdaySignup)
        birthdayLayout = findViewById(R.id.birthdayInputLayout)
        birthdayField.setOnClickListener {
            showDatePickerDialog()
        }

        val submitButton = findViewById<MaterialButton>(R.id.buttonSignupSubmit)

        val usernameLayout = findViewById<TextInputLayout>(R.id.signupUsernameInputLayout)
        val phoneLayout = findViewById<TextInputLayout>(R.id.phoneInputLayout) // Changed to TextInputLayout
        val phoneEditText = findViewById<TextInputEditText>(R.id.etPhoneSignup)
        val genderLayout = findViewById<TextInputLayout>(R.id.genderInputLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.signupPasswordInputLayout)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.signupPasswordConfirmInputLayout)

        submitButton.setOnClickListener {
            // Clear all previous errors
            clearErrors(usernameLayout, phoneLayout, genderLayout, birthdayLayout, passwordLayout, confirmPasswordLayout)

            val username = findViewById<TextInputEditText>(R.id.etUsernameSignup).text.toString().trim()
            val countryCodePicker = findViewById<CountryCodePicker>(R.id.countryCodePicker)
            countryCodePicker.registerCarrierNumberEditText(phoneEditText)
            val phoneNumber = countryCodePicker.fullNumberWithPlus.trim()
            val password = findViewById<TextInputEditText>(R.id.etPasswordSignup).text.toString().trim()
            val confirmPassword = findViewById<TextInputEditText>(R.id.etPasswordConfirmSignup).text.toString().trim()
            val genderText = genderField.text.toString().trim()
            val gender = if (genderText.isNotEmpty()) genderText[0] else null
            val birthday = birthdayField.text.toString().trim()

            var valid = true

            if (username.isEmpty()) {
                setErrorWithTimeout(usernameLayout, "Ce champ ne peut pas être vide")
                valid = false
            }

            if (phoneNumber.isEmpty()) {
                setErrorWithTimeout(phoneLayout, "Ce champ ne peut pas être vide")
                valid = false
            }

            if (password.isEmpty()) {
                setErrorWithTimeout(passwordLayout, "Ce champ ne peut pas être vide")
                valid = false
            } else if (password.length < 8) {
                setErrorWithTimeout(passwordLayout, "Le mot de passe doit contenir au moins 8 caractères")
                valid = false
            }

            if (confirmPassword.isEmpty()) {
                setErrorWithTimeout(confirmPasswordLayout, "Ce champ ne peut pas être vide")
                valid = false
            } else if (password != confirmPassword) {
                setErrorWithTimeout(confirmPasswordLayout, "Les mots de passe ne correspondent pas")
                valid = false
            }

            if (gender == null) {
                setErrorWithTimeout(genderLayout, "Veuillez sélectionner un genre")
                valid = false
            }

            if (birthday.isEmpty()) {
                setErrorWithTimeout(birthdayLayout, "Veuillez sélectionner une date de naissance")
                valid = false
            }

            if (!valid) return@setOnClickListener

            val pid = intent.getStringExtra("id") ?: ""
            val qrCode = intent.getStringExtra("qrCode") ?: ""

            val data = ChildJoinRequest(
                username,
                PhoneNumber(phoneNumber),
                password,
                confirmPassword,
                gender,
                birthday
            )

            val call = RetrofitClient.accountApi.joinChild(pid, qrCode, data)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val jsonString = response.body()!!.string()
                            val json = JSONObject(jsonString)

                            val access = json.getString("access")
                            val refresh = json.getString("refresh")

                            val tokenManager = JwtTokenManager(this@SignupActivity)
                            tokenManager.saveAccessJwt(access)
                            tokenManager.saveRefreshJwt(refresh)
                            tokenManager.setIsLogin(true)
                            val jwt = JWT(access)

                            val userId = jwt.getClaim("id").asString().toString()
                            val familyId = jwt.getClaim("family_id").asString().toString()
                            val userName = jwt.getClaim("username").asString().toString()

                            tokenManager.setUsername(userName)
                            tokenManager.setUserId(userId)
                            tokenManager.setFamilyId(familyId)
                            // Get bad words list
                            webSocketClient = WebSocketGeneralManager.getInstance(this@SignupActivity)
                            webSocketClient!!.getBadWords()
                            webSocketClient!!.getSchedules()

                            Toast.makeText(this@SignupActivity, "Connexion réussie !", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@SignupActivity, ProfileActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@SignupActivity,
                                "Le code QR n'est pas valide.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            try {
                                val json = JSONObject(errorBody ?: "")
                                json.optJSONArray("username")?.let {
                                    setErrorWithTimeout(usernameLayout, it.getString(0))
                                }
                                json.optJSONObject("phone_number")?.optJSONArray("phone_number")?.let {
                                    setErrorWithTimeout(phoneLayout, it.getString(0))
                                }
                                json.optJSONArray("password")?.let {
                                    setErrorWithTimeout(passwordLayout, it.getString(0))
                                }
                                json.optJSONArray("password1")?.let {
                                    setErrorWithTimeout(confirmPasswordLayout, it.getString(0))
                                }
                                json.optJSONArray("gender")?.let {
                                    setErrorWithTimeout(genderLayout, it.getString(0))
                                }
                                json.optJSONArray("birthday")?.let {
                                    setErrorWithTimeout(birthdayLayout, it.getString(0))
                                }
                                json.optJSONArray("non_field_errors")?.let {
                                    setErrorWithTimeout(confirmPasswordLayout, it.getString(0))
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@SignupActivity, "Erreur inattendue", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@SignupActivity, "Erreur réseau : ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, day)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
                birthdayField.setText(dateFormat.format(selectedCal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun setErrorWithTimeout(layout: Any, error: String) {
        when (layout) {
            is TextInputLayout -> {
                layout.error = error
                layout.isErrorEnabled = true
                handler.postDelayed({
                    layout.error = null
                    layout.isErrorEnabled = false
                }, 3000)
            }
            is TextInputEditText -> {
                layout.error = error
                handler.postDelayed({
                    layout.error = null
                }, 3000)
            }
        }
    }

    private fun clearErrors(vararg layouts: Any) {
        layouts.forEach { layout ->
            when (layout) {
                is TextInputLayout -> {
                    layout.error = null
                    layout.isErrorEnabled = false
                }
                is TextInputEditText -> layout.error = null
            }
        }
    }
}