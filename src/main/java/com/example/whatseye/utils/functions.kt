package com.example.whatseye.utils

import android.Manifest
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.whatseye.R
import com.example.whatseye.access.DeviceAdminReceiver
import com.example.whatseye.api.RetrofitClient
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.dataType.data.ChildProfile
import com.example.whatseye.dataType.data.RecordingData
import com.example.whatseye.dataType.db.RecordingDatabase
import com.example.whatseye.services.AccessibilityService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

fun createNotification(context: Context, id: String, title: String, text: String): Notification {
    return NotificationCompat.Builder(context, id)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(R.drawable.app_icon) // Ensure this drawable exists
        .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority for older versions
        .build()
}

fun createNotificationChannel(context: Context, id: String, name: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val serviceChannel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }
}

fun saveProfileToLocal(context: Context, profile: ChildProfile) {

    // Store profile data and image path in SharedPreferences
    val sharedPref = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
    LockManager(context).savePhoneStatus(profile.phone_locked)
    with(sharedPref.edit()) {
        putString("first_name", profile.user.first_name)
        putString("last_name", profile.user.last_name)
        putString("username", profile.user.username)
        putString("email", profile.user.email)
        putString("phone_number", profile.phone_number)
        putString("birthday", profile.birthday)
        putBoolean("phone_locked", profile.phone_locked)
        putString("photo_path", profile.photo)
        apply()
    }
}

//fun setProfileData(context: Context, json : JSONObject){
//    var webSocketClient: WebSocketClientGeneral? = null
//
//    val access = json.getString("access")
//    val refresh = json.getString("refresh")
//
//    val tokenManager = JwtTokenManager(context)
//    tokenManager.saveAccessJwt(access)
//    tokenManager.saveRefreshJwt(refresh)
//    tokenManager.setIsLogin(true)
//    val jwt = JWT(access)
//
//    val userId = jwt.getClaim("id").asString().toString()
//    val familyId = jwt.getClaim("family_id").asString().toString()
//    val username = jwt.getClaim("username").asString().toString()
//
//    tokenManager.setUsername(username)
//    tokenManager.setUserId(userId)
//    tokenManager.setFamilyId(familyId)
//    // Get bad words list
//    webSocketClient = WebSocketGeneralManager.getInstance(context)
//    webSocketClient.getBadWords()
//    webSocketClient.getBadSchedules()
//
//    Toast.makeText(context, "Connexion réussie !", Toast.LENGTH_SHORT).show()
//}
//

fun areAllPermissionsGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    val accessibilityService = ComponentName(context, AccessibilityService::class.java)
    val deviceAdminReceiver = ComponentName(context, DeviceAdminReceiver::class.java)

    val overlayGranted = Settings.canDrawOverlays(context)

    val usageStatsGranted = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    ) == AppOpsManager.MODE_ALLOWED

    val enabledNotificationListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: ""
    val notificationGranted = enabledNotificationListeners.contains(context.packageName)

    val enabledAccessibilityServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: ""
    val accessibilityGranted = enabledAccessibilityServices.split(":")
        .any { ComponentName.unflattenFromString(it)?.equals(accessibilityService) == true }

    val batteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    val deviceAdminActive = devicePolicyManager.isAdminActive(deviceAdminReceiver)

    // Runtime Permissions


    val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No background location permission before Android 10
    }

    return overlayGranted &&
            usageStatsGranted &&
            notificationGranted &&
            accessibilityGranted &&
            batteryOptimizationIgnored &&
            deviceAdminActive &&
            backgroundLocationGranted
}


fun uploadRecord(context: Context, type: String, outputPath:String, db:Boolean) {
    val childId = JwtTokenManager(context).getUserId()
    val file = File(outputPath)
    val timestamp = System.currentTimeMillis()
    RetrofitClient.initialize(context)

    // Prepare the audio file as MultipartBody.Part
    val mimeType = when {
        outputPath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
        outputPath.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
        outputPath.endsWith(".aac", ignoreCase = true) -> "audio/aac"
        else -> "application/octet-stream" // fallback
    }

    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

    // 2. Verify the part name matches Django's expected field name
    val audioPart = MultipartBody.Part.createFormData(
        name = "record_file",  // Must match Django's model FileField name
        filename = file.name,
        body = requestFile
    )
    val recordingType = type.toRequestBody("text/plain".toMediaTypeOrNull())
    val timestampRequest = timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())    // Call the API

    val call = childId?.let {
        RetrofitClient.controlApi.uploadRecording(it, recordingType , timestampRequest ,audioPart)
    }

    call?.enqueue(object : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            if (response.isSuccessful) {
                if (file.exists()) {
                    if(db){
                        val dbHelper = RecordingDatabase(context)
                        dbHelper.deleteRecording(outputPath)
                        countFailed(context, -1)
                    }
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d("uploadRecording", "File deleted successfully")
                    } else {
                        Log.e("uploadRecording", "Failed to delete the file")
                    }
                }
            } else {
                if(!db){
                    countFailed(context, 1)
                    val dbHelper = RecordingDatabase(context)
                    val newRecording = RecordingData(
                        recordingType = type,
                        recordFile = outputPath,
                        timestamp = timestamp
                    )
                    val insertRecording = dbHelper.insertRecording(newRecording)
                }
                Log.e("uploadRecording", "Upload failed: ${response.message()}")
            }
        }
        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            if(!db){
                countFailed(context, 1)
                val dbHelper = RecordingDatabase(context)
                val newRecording = RecordingData(
                    recordingType = type,
                    recordFile = outputPath,
                    timestamp = timestamp
                )
                val insertRecording = dbHelper.insertRecording(newRecording)
            }
            Log.e("uploadRecording", "Error: ${t.message}")
        }
    })
}

fun retryFailedUploads(context : Context ) {
    val sharedPrefs = context.getSharedPreferences("upload_prefs", Context.MODE_PRIVATE)
    val failedCountKey = "failed_upload_count"
    val currentFailedCount = sharedPrefs.getInt(failedCountKey, 0)
    if (currentFailedCount != 0) {
        val dbHelper = RecordingDatabase(context)
        val failedRecords = dbHelper.getAllRecordings()
        for (record in failedRecords) {
            uploadRecord(
                context,
                record.recordingType,
                record.recordFile,
                true
            ) // true indicates we are retrying
        }
    }
}
fun countFailed(context : Context, num:Short){
    val sharedPrefs = context.getSharedPreferences("upload_prefs", Context.MODE_PRIVATE)
    val failedCountKey = "failed_upload_count"
    val currentFailedCount = sharedPrefs.getInt(failedCountKey, 0)
    val newFailedCount = currentFailedCount + num
    sharedPrefs.edit().putInt(failedCountKey, newFailedCount).apply()
}

