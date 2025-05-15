package com.example.whatseye.profile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.example.whatseye.access.LockScreenNewPINActivity
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.BadWordsManager
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.dataType.data.ChildProfile
import com.example.whatseye.services.AlwaysRunningService
import com.example.whatseye.utils.saveProfileToLocal
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.hbb20.CountryCodePicker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Calendar

@SuppressLint("UseSwitchCompatOrMaterialCode")
class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var imageViewProfile: ImageView
    private lateinit var buttonChangePhoto: MaterialButton
    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPhone: TextInputEditText
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var editTextBirthday: TextInputEditText
    private lateinit var switchPhoneLocked: SwitchCompat
    private lateinit var buttonSave: MaterialButton
    private lateinit var buttonChangePin: MaterialButton
    private lateinit var buttonDeleteAccount: MaterialButton
    private lateinit var logoutButton: MaterialButton


    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)
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
        editTextPhone = findViewById(R.id.etPhoneLogin)
        countryCodePicker = findViewById(R.id.countryCodePicker)
        editTextBirthday = findViewById(R.id.editTextBirthday)
        switchPhoneLocked = findViewById(R.id.switchPhoneLocked)
        buttonSave = findViewById(R.id.buttonSave)
        buttonChangePin = findViewById(R.id.buttonChangePin)
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount)
        logoutButton = findViewById(R.id.logoutButton)
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("profile_data", Context.MODE_PRIVATE)

        val firstName = sharedPref.getString("first_name", "") ?: ""
        val lastName = sharedPref.getString("last_name", "") ?: ""
        val email = sharedPref.getString("email", "") ?: ""
        val phoneNumber = sharedPref.getString("phone_number", "") ?: ""
        val birthday = sharedPref.getString("birthday", "") ?: ""
        val phoneLocked = LockManager(this).getPhoneStatus()
        val photoPath = sharedPref.getString("photo_path", null)

        // Set text fields
        editTextFirstName.setText(firstName)
        editTextLastName.setText(lastName)
        editTextEmail.setText(email)
        editTextBirthday.setText(birthday)
        switchPhoneLocked.isChecked = phoneLocked

        // Parse and set phone number if applicable
        getCountryCode(phoneNumber)?.let { countryCodePicker.setCountryForPhoneCode(it) }
        editTextPhone.setText(getLocalPhoneNumber(phoneNumber))

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

    private fun getLocalPhoneNumber(fullPhoneNumber: String): String? {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val number = phoneUtil.parse(fullPhoneNumber, null)
            number.nationalNumber.toString()  // Returns the number without country code
        } catch (e: NumberParseException) {
            null
        }
    }
    private fun getCountryCode(fullPhoneNumber: String): Int? {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val number = phoneUtil.parse(fullPhoneNumber, null)
            number.countryCode
        } catch (e: NumberParseException) {
            null
        }
    }
    private fun setupListeners() {
        buttonChangePhoto.setOnClickListener {
            pickImageFromGallery()
        }

        editTextBirthday.setOnClickListener {
            showDatePickerDialog()
        }

        buttonSave.setOnClickListener {
            submitProfileUpdate()
        }

        buttonDeleteAccount.setOnClickListener {
            confirmAccountDeletion()
        }
        buttonChangePin.setOnClickListener {
            changePin()
        }
        logoutButton.setOnClickListener {
            confirmAccountLogout()
        }
    }

    private fun confirmAccountLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout Account")
            .setMessage("Are you sure you want to logout your account?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout(){
        JwtTokenManager(this).clearAllTokens()
        LockManager(this).clearAllLocks()
        PasskeyManager(this).clearPassKey()
        BadWordsManager(this).clearBadWords()
        val webSocketGen = WebSocketGeneralManager
        webSocketGen.getInstance(this)
        webSocketGen.closeConnection()

        val sharedPref = getSharedPreferences("profile_data", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        val intent1 = Intent(this, AlwaysRunningService::class.java)
        stopService(intent1)

        // To cancel UsageWorker
        WorkManager.getInstance(applicationContext).cancelUniqueWork("UsageDataSyncWork")

        // To cancel TokenRefreshWorker
        WorkManager.getInstance(applicationContext).cancelUniqueWork("TokenRefreshSyncWork")

        //To cancel RetryUploadWorker
        WorkManager.getInstance(applicationContext).cancelUniqueWork("retryUploadsWork")

        deleteDatabase("schedule.db")
        deleteDatabase("recordings.db")
        deleteDatabase("usage.db")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the stack
        startActivity(intent)
        finish()

    }

    private fun changePin(){
        val intent = Intent(this, LockScreenNewPINActivity::class.java)
        startActivity(intent)
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

    @SuppressLint("DefaultLocale")
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                editTextBirthday.setText(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }


    private fun submitProfileUpdate() {
        val childId =  JwtTokenManager(this).getUserId()
        val firstName = editTextFirstName.text.toString()
        val lastName = editTextLastName.text.toString()
        val email = editTextEmail.text.toString()
        val birthday = editTextBirthday.text.toString()
        val rawPhone = editTextPhone.text?.toString()?.trim() ?: ""
        val countryCode = countryCodePicker.selectedCountryCodeWithPlus
        val phoneNumber = if (rawPhone.startsWith("+")) {
            rawPhone // assume user entered full number
        } else {
            countryCode + rawPhone
        }
        LockManager(this).savePhoneStatus(switchPhoneLocked.isChecked)
//        val sharedPref = getSharedPreferences("profile_data", Context.MODE_PRIVATE)
//        sharedPref.getBoolean("phone_locked", false)
        val phoneLocked = switchPhoneLocked.isChecked.toString()


        val firstNamePart = firstName.toRequestBody("text/plain".toMediaTypeOrNull())
        val lastNamePart = lastName.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val birthdayPart = birthday.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneNumberPart = phoneNumber.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneLockedPart = phoneLocked.toRequestBody("text/plain".toMediaTypeOrNull())

        val photoPart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            uriToFile(uri)?.let { file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", file.name, requestFile)
            }
        }
        if (childId != null) {
            val call = RetrofitClient.profileApi.patchChildProfile(
                childId = childId,
                firstName = firstNamePart,
                lastName = lastNamePart,
                email = emailPart,
                birthday = birthdayPart,
                phoneNumber = phoneNumberPart,
                phoneLocked = phoneLockedPart,
                photo = photoPart
            )

            call.enqueue(object : Callback<ChildProfile> {
                override fun onResponse(
                    call: Call<ChildProfile>,
                    response: Response<ChildProfile>
                ) {
                    if (response.isSuccessful) {
                        val profile = response.body()!!
                        saveProfileToLocal(this@UpdateProfileActivity, profile)
                        Toast.makeText(
                            this@UpdateProfileActivity,
                            "Profil mis à jour",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@UpdateProfileActivity,
                            "La mise à jour a échoué : ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ChildProfile>, t: Throwable) {
                    Toast.makeText(
                        this@UpdateProfileActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

            val intent = Intent(this, ProfileActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
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


    private fun confirmAccountDeletion() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        // TODO: Call DELETE endpoint
        Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show()
        // Navigate back or logout
    }
}
