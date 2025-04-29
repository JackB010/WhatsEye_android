package com.example.whatseye.api.ws

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.whatseye.api.managers.PasskeyManager
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
    private var webSocket: WebSocket? = null
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var isReconnecting = false
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var retryCount = 0
    private val maxRetries = 5
    private val baseDelayMs = 1000L // 1 second initial delay

    init {
        connect()
    }

    private fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                retryCount = 0 // Reset retry count on successful connection
                // WebSocket is open
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                if (!isReconnecting) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (isReconnecting || retryCount >= maxRetries) return

        isReconnecting = true
        val delay = baseDelayMs * (1 shl retryCount) // Exponential backoff: 1s, 2s, 4s, 8s, etc.

        reconnectScope.launch {
            delay(delay)
            retryCount++
            connect()
            isReconnecting = false
        }
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
        }
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
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    sendLocationData(it)
                } ?: run {
                    sendLocationError("Location not available")
                }
            }
            .addOnFailureListener { exception ->
                sendLocationError(exception.localizedMessage ?: "Location request failed")
            }
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

}