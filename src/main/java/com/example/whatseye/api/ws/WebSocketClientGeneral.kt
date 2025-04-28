package com.example.whatseye.api.ws

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import org.json.JSONObject

class WebSocketClientGeneral(private val context: Context, url: String) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // WebSocket is open
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
            }
        })
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
        if (checkLocationPermission()) {
            getLastLocation()

        } else {
            sendLocationError("Location permission denied")
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastLocation() {
        if (checkLocationPermission()) {
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
    }

    // ✅ FIXED: Send location in the format Django expects
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
            put("message", errorMessage)
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

    fun close() {
        webSocket?.close(1000, null)
    }
}
