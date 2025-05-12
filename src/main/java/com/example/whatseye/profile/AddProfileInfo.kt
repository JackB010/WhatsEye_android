package com.example.whatseye.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.dataType.data.ChildProfile
import com.example.whatseye.utils.saveProfileToLocal
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

@SuppressLint("UseSwitchCompatOrMaterialCode")
class AddProfileInfo : AppCompatActivity() {

    private lateinit var imageViewProfile: ImageView
    private lateinit var buttonChangePhoto: MaterialButton
    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var buttonSave: MaterialButton
    private lateinit var firstNameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout


    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_infos)
        val packageName = intent.getStringExtra("packageName") ?: ""
        LockManager(this).saveLockedStatus(packageName, true)
        initViews()
        loadUserData()
        setupListeners()
        RetrofitClient.initialize(this)
    }

    @SuppressLint("CutPasteId")
    private fun initViews() {
        imageViewProfile = findViewById(R.id.imageViewProfile)
        buttonChangePhoto = findViewById(R.id.buttonChangePhoto)
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextEmail = findViewById(R.id.editTextEmail)
        buttonSave = findViewById(R.id.buttonSave)
        firstNameLayout = findViewById(R.id.firstNameLayout)
        lastNameLayout = findViewById(R.id.lastNameLayout)
        emailLayout = findViewById(R.id.emailLayout)


    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("profile_data", Context.MODE_PRIVATE)

        val firstName = sharedPref.getString("first_name", "") ?: ""
        val lastName = sharedPref.getString("last_name", "") ?: ""
        val email = sharedPref.getString("email", "") ?: ""
        val photoPath = sharedPref.getString("photo_path", null)

        // Set text fields
        editTextFirstName.setText(firstName)
        editTextLastName.setText(lastName)
        editTextEmail.setText(email)

        // Load profile image with Glide
        if (!photoPath.isNullOrEmpty()) {
            Glide.with(imageViewProfile)
                .load(photoPath)
                .placeholder(R.drawable.ic_user) // fallback image
                .error(R.drawable.rounded_button)          // in case of load error
                .into(imageViewProfile)
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_user)
        }

    }

    private fun setupListeners() {
        buttonChangePhoto.setOnClickListener {
            pickImageFromGallery()
        }

        buttonSave.setOnClickListener {
            submitProfileUpdate()
        }

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            imageViewProfile.setImageURI(selectedImageUri)
        }
    }



    private fun submitProfileUpdate() {
        val childId =  JwtTokenManager(this).getUserId()
        val firstName = editTextFirstName.text.toString()
        val lastName = editTextLastName.text.toString()
        val email = editTextEmail.text.toString()

        if (firstName.isEmpty()) {
            firstNameLayout.error = "Le prénom est requis"
            editTextFirstName.requestFocus()
            firstNameLayout.isErrorEnabled = true

            firstNameLayout.postDelayed({
                firstNameLayout.error = null
                firstNameLayout.isErrorEnabled = false
            }, 3000)
            return
        }

        if (lastName.isEmpty()) {
            lastNameLayout.error = "Le nom est requis"
            editTextLastName.requestFocus()
            lastNameLayout.isErrorEnabled = true


            lastNameLayout.postDelayed({
                lastNameLayout.error = null
                lastNameLayout.isErrorEnabled = false

            }, 3000)
            return
        }

        if (email.isEmpty()) {
            emailLayout.error = "L'e-mail est requis"
            editTextEmail.requestFocus()
            lastNameLayout.isErrorEnabled = true

            emailLayout.postDelayed({
                emailLayout.error = null
                lastNameLayout.isErrorEnabled = false
            }, 3000)
            return
        }


        val firstNamePart = firstName.toRequestBody("text/plain".toMediaTypeOrNull())
        val lastNamePart = lastName.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val photoPart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            uriToFile(uri)?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", file.name, requestFile)
            }
        }
        if (childId != null) {
            val call = RetrofitClient.profileApi.addChildProfile(
                childId = childId,
                firstName = firstNamePart,
                lastName = lastNamePart,
                email = emailPart,
                photo = photoPart
            )

            call.enqueue(object : Callback<ChildProfile> {
                override fun onResponse(
                    call: Call<ChildProfile>,
                    response: Response<ChildProfile>
                ) {
                    if (response.isSuccessful) {
                        val profile = response.body()!!
                        saveProfileToLocal(this@AddProfileInfo, profile)
                        Toast.makeText(
                            this@AddProfileInfo,
                            "Profil mis à jour",
                            Toast.LENGTH_SHORT
                        ).show()
                           goProfile()

                    } else {
                        Toast.makeText(
                            this@AddProfileInfo,
                            "La mise à jour a échoué : ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ChildProfile>, t: Throwable) {
                    Toast.makeText(
                        this@AddProfileInfo,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        }
    }
    private fun goProfile(){
        val intent2 = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent2)
    }
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("profile_photo", ".jpg", cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
