package com.example.whatseye
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.profile.UpdateProfileActivity
import kotlinx.coroutines.*

class LockScreenActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate your layout (should fill the whole activity)
        setContentView(R.layout.overlay_lock_screen)
        window.addFlags(
            LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or LayoutParams.FLAG_DISMISS_KEYGUARD
                    or LayoutParams.FLAG_KEEP_SCREEN_ON
                    or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        setupPinInputsAndButton()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> true // still block home button
            KeyEvent.KEYCODE_BACK -> {
                val packageName = intent.getStringExtra("packageName") ?: ""
                val appName = applicationInfo.loadLabel(packageManager).toString()
                if(packageName==appName)  finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
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
    ): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) {
                    if (checkPinFieldsFull(allFields)) validatePin(allFields)
                    else nextField?.requestFocus()
                } else if (s.isNullOrEmpty() && before > 0) {
                    previousField?.requestFocus()
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {
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
        val scaleAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_pulse)
        field.startAnimation(scaleAnimation)
    }

    private fun validatePin(pinFields: Array<EditText>) {
        val pin = pinFields.joinToString("") { it.text.toString() }
        val passkeyManager = PasskeyManager(this)
        val packageName = intent.getStringExtra("packageName") ?: ""
        if (passkeyManager.isPasskeyValid(pin)) {
            LockManager(this).saveLockedStatus(packageName,false)
            val appName = applicationInfo.loadLabel(packageManager).toString()
            if(packageName==appName){
                val intent2 = Intent(this, UpdateProfileActivity::class.java).apply {
                    putExtra("packageName", appName)
                }
                startActivity(intent2)
            }
            finish()

        } else {
            showError(pinFields)
        }
    }

    private fun showError(pinFields: Array<EditText>) {
        val pinContainer = findViewById<LinearLayout>(R.id.pinContainer)
        pinContainer.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake))
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
    }
}