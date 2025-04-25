package com.example.whatseye.noLogin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.jwt.JWT
import com.example.whatseye.R
import com.example.whatseye.api.JwtTokenManager
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.dataType.data.LoginData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var toggleButton: MaterialButton
    private lateinit var usernameLoginLayout: View
    private lateinit var phoneLoginLayout: View
    private var isUsernameLogin = true
    private lateinit var data: LoginData

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        toggleButton = findViewById(R.id.toggleButton)
        usernameLoginLayout = findViewById(R.id.usernameLoginLayout)
        phoneLoginLayout = findViewById(R.id.phoneLoginLayout)
        val submitButton: Button = findViewById(R.id.buttonSubmitLogin)

        val tokenManager = JwtTokenManager(this)

        data = LoginData("", "")

        toggleButton.setOnClickListener {
            isUsernameLogin = !isUsernameLogin
            if (isUsernameLogin) {
                usernameLoginLayout.visibility = View.VISIBLE
                phoneLoginLayout.visibility = View.GONE
                toggleButton.text = "Passer à la connexion par téléphone"
            } else {
                usernameLoginLayout.visibility = View.GONE
                phoneLoginLayout.visibility = View.VISIBLE
                toggleButton.text = "Passer au nom d'utilisateur Login"
            }
        }

        submitButton.setOnClickListener {
            val passwordInput = findViewById<TextInputEditText>(R.id.etPasswordLogin)
            val usernameInput = findViewById<TextInputEditText>(R.id.etUsernameLogin)
            val phoneInput = findViewById<TextInputEditText>(R.id.etPhoneLogin)
            val countryCodePicker = findViewById<CountryCodePicker>(R.id.countryCodePicker)

            // Clear previous errors
            passwordInput.error = null
            usernameInput.error = null
            phoneInput.error = null

            val password = passwordInput.text.toString().trim()
            if (password.isEmpty()) {
                passwordInput.setError("Password required",null)
                return@setOnClickListener
            }
            data.password = password

            if (isUsernameLogin) {
                val username = usernameInput.text.toString().trim()
                if (username.isEmpty()) {
                    usernameInput.error = "Username required"
                    return@setOnClickListener
                }
                data.username = username
            } else {
                countryCodePicker.registerCarrierNumberEditText(phoneInput)
                if (!countryCodePicker.isValidFullNumber) {
                    phoneInput.error = "Enter a valid phone number"
                    return@setOnClickListener
                }
                data.username = countryCodePicker.fullNumberWithPlus
            }

            val pid = intent.getStringExtra("id") ?: ""
            val qrCode = intent.getStringExtra("qrCode") ?: ""

            val call = RetrofitClient.accountApi.loginUser(pid, qrCode, data)

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val jsonString = response.body()!!.string()
                            val json = JSONObject(jsonString)

                            val access = json.getString("access")
                            val refresh = json.getString("refresh")

                            tokenManager.saveAccessJwt(access)
                            tokenManager.saveRefreshJwt(refresh)
                            tokenManager.setIsLogin(true)
                            val jwt = JWT(access)

                            val userId = jwt.getClaim("id").asString().toString()
                            val familyId = jwt.getClaim("family_id").asString().toString()
                            val username = jwt.getClaim("username").asString().toString()

                            tokenManager.setUsername(username)
                            tokenManager.setUserId(userId)
                            tokenManager.setFamilyId(familyId)

                            Toast.makeText(this@LoginActivity, "Connexion réussie !", Toast.LENGTH_SHORT).show()

                            // Navigate to SignupActivity or MainActivity
                            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                            startActivity(intent)
                            finish()

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@LoginActivity,
                                "Le code QR n'est pas valide.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        try {
                            val errorString = response.errorBody()?.string()
                            val errorJson = JSONObject(errorString ?: "{}")

                            val errorArray = errorJson.optJSONArray("non_field_errors")
                            val errorMessage = errorArray?.optString(0) ?: "Erreur inconnue."

                            if (errorMessage.contains("password", ignoreCase = true)) {
                                passwordInput.setError("Mot de passe incorrect.", null)
                            } else {
                                if (isUsernameLogin) {
                                    usernameInput.error = "L'utilisateur ayant cet identifiant n'existe pas."
                                } else {
                                    phoneInput.error = "L'utilisateur ayant ce numéro n'existe pas."
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@LoginActivity, "Erreur serveur inattendue.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Erreur réseau : ${t.message}", Toast.LENGTH_LONG).show()
                }
            })

        }

        val forgotPasswordText: TextView = findViewById(R.id.forgotPasswordText)
        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
