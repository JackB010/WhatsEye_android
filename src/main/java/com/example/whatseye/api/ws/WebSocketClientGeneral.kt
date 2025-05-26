package com.example.whatseye.api.ws

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import androidx.core.content.ContextCompat
import com.example.whatseye.dataType.db.ScheduleDataBase
import com.example.whatseye.api.managers.BadWordsManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.dataType.data.NotificationData
import com.example.whatseye.dataType.data.ScheduleData
import com.example.whatseye.services.AlwaysRunningService
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class WebSocketClientGeneral(private val context: Context, private val url: String) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for long-lived connections
        .build()
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var isReconnecting = false
    private var isClosed = false // Flag to track intentional closure
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var retryCount = 0
    private val baseDelayMs = 1000L // 1 second initial delay
    private val maxDelayMs = 15000L // Cap at 15 seconds
    private val dbHelper = ScheduleDataBase(context)
    private val mainHandler = Handler(Looper.getMainLooper())


    init {
        connect()
    }

    private fun connect() {
        if (isClosed || webSocket != null) return // Avoid connecting if closed or already connected

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryCount = 0 // Reset retry count on successful connection
                isReconnecting = false
                // WebSocket is open
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                cleanupWebSocket()
                if (!isClosed) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                cleanupWebSocket()
                if (!isClosed && !isReconnecting) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (isClosed || isReconnecting) return

        isReconnecting = true
        // Exponential backoff: 1s, 2s, 4s, 8s, ..., capped at maxDelayMs
        val delay = (baseDelayMs * (1 shl retryCount.coerceAtMost(10))).coerceAtMost(maxDelayMs)

        reconnectScope.launch {
            delay(delay)
            if (!isClosed) {
                retryCount++
                connect()
                isReconnecting = false
            }
        }
    }

    private fun cleanupWebSocket() {
        webSocket?.close(1000, "Cleanup Closure")
        webSocket = null
    }

    private fun handleIncomingMessage(text: String) {
        val jsonObject = JSONObject(text)
        val type = jsonObject.getString("type")

        when (type) {
            "PIN_CHANGE" -> {
                val newPin = jsonObject.getString("new_pin")
                PasskeyManager(context).savePasskeyWS(newPin)
                sendPinConfirmation()
            }

            "CONFIRM_PIN" -> {
                showPinChangeNotification()
            }

            "GET_LOCATION" -> {
                handleLocationRequest()
            }

            "BAD_WORDS" -> {
                val badWordsArray = jsonObject.getJSONArray("bad_words")
                val badWordsList = mutableListOf<String>()
                for (i in 0 until badWordsArray.length()) {
                    badWordsList.add(badWordsArray.getString(i))
                }
                val badWordsManager = BadWordsManager(context)
                badWordsManager.saveBadWords(badWordsList)
            }

            "LOCK_PHONE" -> {
                val lockStatus = jsonObject.getBoolean("phone_locked")
                LockManager(context).savePhoneStatus(lockStatus)
                sendLockPhoneConfirmation()
            }

            "ADD_SCHEDULE" ->{
                val data = jsonObject.getJSONObject("schedule")
                val daysJsonArray = data.getJSONArray("days")

                val daysList: List<Int> = (0 until daysJsonArray.length()).map { i ->
                    daysJsonArray.getInt(i)
                }
                val scheduleObject  = ScheduleData(
                    id = data.getInt("id"),
                    name = data.getString("name"),
                    startTime = data.getString("start_time"),
                    endTime = data.getString("end_time"),
                    startDate = data.getString("start_date"),
                    endDate = data.getString("end_date"),
                    days = daysList
                    )
                dbHelper.insertSchedule(scheduleObject)
            }

            "DELETE_SCHEDULE"->{
                val id = jsonObject.getString("id")
                dbHelper.deleteSchedule(id.toInt())
            }

            "SCHEDULE" -> {
                val schedules = jsonObject.getJSONArray("schedules")
                val newScheduleIds = mutableSetOf<Int>()

                // Step 1: Insert or update new schedules and collect their IDs
                for (i in 0 until schedules.length()) {
                    val scheduleJson = schedules.getJSONObject(i)
                    val daysJsonArray = scheduleJson.getJSONArray("days")
                    val daysList: List<Int> = (0 until daysJsonArray.length()).map { j ->
                        daysJsonArray.getInt(j)
                    }
                    val scheduleObject = ScheduleData(
                        id = scheduleJson.getInt("id"),
                        name = scheduleJson.getString("name"),
                        startTime = scheduleJson.getString("start_time"),
                        endTime = scheduleJson.getString("end_time"),
                        startDate = scheduleJson.getString("start_date"),
                        endDate = scheduleJson.getString("end_date"),
                        days = daysList
                    )
                    newScheduleIds.add(scheduleObject.id)
                    dbHelper.insertSchedule(scheduleObject)
                }

                // Step 2: Get all non-deleted schedules from the database
                val existingSchedules = dbHelper.getAllSchedules()
                val existingScheduleIds = existingSchedules.map { it.id }.toSet()

                // Step 3: Find schedules to delete (not in newScheduleIds)
                val schedulesToDelete = existingScheduleIds - newScheduleIds

                // Step 4: Soft-delete schedules not in the new list
                schedulesToDelete.forEach { id ->
                    dbHelper.deleteSchedule(id)
                }
            }

            "REQUEST_CONTACT"->{
                Log.d("REQUEST_CONTACT", "REQUEST_CONTACT")
                mainHandler.post {
                    AlwaysRunningService.getInstance()?.getContact()
                }
            }
            "REQUEST_CURRENT_CHATS"->{
                Log.d("REQUEST_CURRENT_CHATS", "REQUEST_CURRENT_CHATS")
                mainHandler.post {
                    AlwaysRunningService.getInstance()?.getContactChat()
                }
            }
            "REQUEST_BLOCK_CHAT"->{
                Log.d("REQUEST_BLOCK_CHAT", "REQUEST_BLOCK_CHAT")
                val name = jsonObject.getString("name")
                val pos = jsonObject.getString("pos")
                mainHandler.post {
                    AlwaysRunningService.getInstance()?.blockChat(name, pos)
                }
            }
            "REQUEST_CHAT"->{
                val name = jsonObject.getString("name")
                mainHandler.post {
                    AlwaysRunningService.getInstance()?.getChatRoom(name)
                    //LoggedInActivity.getInstance()?.getChatRoom(name)
                }
            }
        }
    }

    fun getSchedules() {
        val message = JSONObject().apply {
            put("type", "SCHEDULE")
        }.toString()
        webSocket?.send(message)
    }
    private fun sendLockPhoneConfirmation() {
        val confirmationMessage = JSONObject().apply {
            put("type", "CONFIRM_LOCK_PHONE")
        }.toString()
        webSocket?.send(confirmationMessage)
    }

    fun getBadWords() {
        val message = JSONObject().apply {
            put("type", "BAD_WORDS")
        }.toString()
        webSocket?.send(message)
    }

    private fun handleLocationRequest() {
        if (hasLocationPermission()) {
            if (isLocationEnabled()) {
                getLastLocation()
            } else {
                sendLocationError("GPS_OFF")
            }
        } else {
            sendLocationError("NO_PERMISSION")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return finePermission || coarsePermission
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            isGpsEnabled || isNetworkEnabled
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000 // 1 second
            fastestInterval = 500
            numUpdates = 1  // We only need one good update
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        sendLocationData(location)
                    } else {
                        sendLocationError("Location not available")
                    }

                    // Important: remove updates to prevent memory leaks
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        sendLocationError("Location not available")
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun sendLocationData(location: Location) {
        val locationData = JSONObject().apply {
            put("lat", location.latitude)
            put("lng", location.longitude)
            put("accuracy", location.accuracy)
            put("timestamp", System.currentTimeMillis())
        }

        val locationMessage = JSONObject().apply {
            put("type", "LOCATION")
            put("location", locationData)
        }.toString()

        webSocket?.send(locationMessage)
    }

    private fun sendLocationError(errorMessage: String) {
        val errorJson = JSONObject().apply {
            put("type", "LOCATION_ERROR")
            put("error", errorMessage)
        }.toString()

        webSocket?.send(errorJson)
    }

    private fun showPinChangeNotification() {
        createNotificationChannel(context, "CONFIRM_PIN", "Always Running Service Channel")
        val notification = createNotification(
            context,
            "CONFIRM_PIN",
            "PIN Changed",
            "The PIN change has been changed successfully."
        )
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(10001, notification)
    }

    private fun sendPinConfirmation() {
        val confirmationMessage = JSONObject().apply {
            put("type", "CONFIRM_PIN")
        }.toString()

        webSocket?.send(confirmationMessage)
    }

    fun sendNotification(notificationData: NotificationData) {
        val message = JSONObject().apply {
            put("type", "NOTIFICATION")
            put("notification", JSONObject(gson.toJson(notificationData)))
        }.toString()
        webSocket?.send(message)
    }

    fun sendContact(contacts: String) {
        val message = JSONObject().apply {
            put("type", "RESPONSE_CONTACT")
            put("contacts", contacts)
        }.toString()
        webSocket?.send(message)
    }


    fun sendChat(chats: String) {
        val message = JSONObject().apply {
            put("type", "RESPONSE_CHAT")
            put("chats", chats)
        }.toString()
        webSocket?.send(message)
    }



    fun sendContactChat(contacts: String) {
        val message = JSONObject().apply {
            put("type", "RESPONSE_CURRENT_CHATS")
            put("contacts", contacts)
        }.toString()
        webSocket?.send(message)
    }

    fun sendBlockedChat() {
        val message = JSONObject().apply {
            put("type", "RESPONSE_BLOCK_CHAT")
        }.toString()
        webSocket?.send(message)
    }


    fun close() {
        isClosed = true
        reconnectScope.cancel() // Cancel any pending reconnect attempts
        cleanupWebSocket()
        client.dispatcher.executorService.shutdown() // Clean up OkHttp resources
    }
}