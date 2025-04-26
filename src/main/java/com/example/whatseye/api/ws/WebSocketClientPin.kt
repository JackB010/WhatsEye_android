package com.example.whatseye.api.ws

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import okhttp3.*
import org.json.JSONObject

class WebSocketClientPin(private val context: Context, url: String) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    init {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // WebSocket is open, you can send messages or perform other actions
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Received a message from the server
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Handle failure (log the error and notify)
                t.printStackTrace()
                // You can also show a notification about the error
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        // Assume the incoming message is in JSON format and contains a new pin
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
        }
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