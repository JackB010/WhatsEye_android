package com.example.whatseye.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import com.example.whatseye.R
import com.example.whatseye.api.managers.PasskeyManager
import kotlinx.coroutines.*

class LockOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_lock_screen, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            stopSelf()
            return
        }

        setupPinInputsAndButton()
    }

    private fun setupPinInputsAndButton() {
        val pinFields = arrayOf(
            overlayView.findViewById<EditText>(R.id.pin1),
            overlayView.findViewById<EditText>(R.id.pin2),
            overlayView.findViewById<EditText>(R.id.pin3),
            overlayView.findViewById<EditText>(R.id.pin4),
            overlayView.findViewById<EditText>(R.id.pin5)
        )

        pinFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(
                createPinTextWatcher(editText, pinFields.getOrNull(index + 1), pinFields.getOrNull(index - 1), pinFields)
            )
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    startFieldAnimation(editText)
                    editText.setBackgroundResource(R.drawable.pin_input_background)
                } else {
                    editText.setBackgroundResource(R.drawable.pin_input_background_default)
                }
            }
            editText.setOnClickListener { editText.requestFocus() }
            // Handle backspace/delete key
            editText.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL && editText.text.isEmpty()) {
                    pinFields.getOrNull(index - 1)?.let {
                        it.text.clear()
                        it.requestFocus()
                    }
                    true
                } else {
                    false
                }
            }
        }

        pinFields[0].requestFocus()
    }

    private fun createPinTextWatcher(
        currentField: EditText,
        nextField: EditText?,
        previousField: EditText?,
        allFields: Array<EditText>
    ): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Handle input
                if (s?.length == 1) {
                    if (checkPinFieldsFull(allFields)) validatePin(allFields)
                    else nextField?.requestFocus()
                }
                // Handle deletion (backspace when field is empty is handled by KeyListener)
                else if (s.isNullOrEmpty() && before > 0) {
                    previousField?.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Ensure only one character per field
                if (s != null && s.length > 1) {
                    currentField.setText(s.subSequence(s.length - 1, s.length))
                    currentField.setSelection(1)
                }
            }
        }
    }

    private fun checkPinFieldsFull(allFields: Array<EditText>): Boolean {
        return allFields.all { it.text.toString().isNotEmpty() }
    }

    private fun startFieldAnimation(field: EditText) {
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_pulse)
        field.startAnimation(scaleAnimation)
    }

    private fun validatePin(pinFields: Array<EditText>) {
        val pin = pinFields.joinToString("") { it.text.toString() }
        var passkeyManager: PasskeyManager =   PasskeyManager(this)
        if (passkeyManager.isPasskeyValid(pin)) { // Replace with actual validation logic
            stopSelf()
        } else {
            showError(pinFields)
        }
    }

    private fun showError(pinFields: Array<EditText>) {
        val pinContainer = overlayView.findViewById<LinearLayout>(R.id.pinContainer)
        pinContainer.startAnimation(AnimationUtils.loadAnimation(this@LockOverlayService, R.anim.shake))
        pinFields.forEach {
            it.setBackgroundResource(R.drawable.pin_input_background_error)
        }
        coroutineScope.launch {
           delay(500)
            pinFields.forEach {
                it.setBackgroundResource(R.drawable.pin_input_background_default)
                it.text.clear()
            }
            pinFields[0].requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            // Handle view removal failure gracefully
        }
    }
}