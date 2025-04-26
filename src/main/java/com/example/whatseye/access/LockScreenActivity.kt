package com.example.whatseye.access

import android.os.Bundle
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.R
import com.example.whatseye.api.managers.PasskeyManager
import kotlinx.coroutines.*

class LockScreenActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var vibrator: Vibrator
    private var prevPassKey: String? = null
    private var isConfirmMode = false
    private var tempPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_lock_screen)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        prevPassKey = intent.getStringExtra("prevPassKey")
        isConfirmMode = prevPassKey != null

        setupPinInputsAndButton()
        updatePromptText()
    }

    private fun setupPinInputsAndButton() {
        val pinFields = arrayOf(
            findViewById<EditText>(R.id.pin1),
            findViewById(R.id.pin2),
            findViewById(R.id.pin3),
            findViewById(R.id.pin4),
            findViewById(R.id.pin5)
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
                if (s?.length == 1) {
                    if (checkPinFieldsFull(allFields)) validatePin(allFields)
                    else nextField?.requestFocus()
                } else if (s.isNullOrEmpty() && before > 0) {
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
        if (pin.length != 5) {
            showError(pinFields, "PIN must be 5 digits")
            return
        }

        if (!isConfirmMode) {
            // First PIN entry: store temporarily and switch to confirm mode
            tempPin = pin
            isConfirmMode = true
            updatePromptText()
            resetFields(pinFields)
        } else {
            // Confirmation mode: check against stored PIN or prevPassKey
            val targetPin = prevPassKey ?: tempPin
            if (pin == targetPin) {
                try {
                    val passkeyManager = PasskeyManager(this)
                    passkeyManager.savePasskey(pin)
                    finish()
                } catch (e: Exception) {
                    showError(pinFields, "Failed to save PIN. Please try again.")
                }
            } else {
                showError(pinFields, "PINs do not match")
                isConfirmMode = false
                tempPin = null
                updatePromptText()
            }
        }
    }

    private fun showError(pinFields: Array<EditText>, errorMessage: String = "Incorrect PIN") {
        val pinContainer = findViewById<LinearLayout>(R.id.pinContainer)
        pinContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
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

    private fun resetFields(pinFields: Array<EditText>) {
        pinFields.forEach {
            it.text.clear()
            it.setBackgroundResource(R.drawable.pin_input_background_default)
        }
        pinFields[0].requestFocus()
    }

    private fun updatePromptText() {
        findViewById<TextView>(R.id.prompt_text)?.let {
            it.text = if (isConfirmMode) "Confirm your PIN" else "Enter new PIN"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}