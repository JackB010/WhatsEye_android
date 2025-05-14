package com.example.whatseye.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.whatseye.LockScreenActivity
import com.example.whatseye.api.managers.BadWordsManager
import com.example.whatseye.api.managers.LockManager
import com.example.whatseye.api.managers.PasskeyManager
import com.example.whatseye.api.ws.WebSocketClientNotification
import com.example.whatseye.dataType.data.NotificationData
import com.example.whatseye.dataType.db.ScheduleDataBase
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel

class AccessibilityService : AccessibilityService() {

    private lateinit var badWords: List<String>
    private var lastText: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val TYPING_DELAY_MS = 1800L
    private val webSocketManager = WebSocketClientNotification.getInstance()
    private var lockScreenRunnable: Runnable? = null

    private val checkBadWordsRunnable = Runnable {
        checkBadWords(lastText)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val lockManager = LockManager(this@AccessibilityService)
                lockManager.saveLockedStatus("com.whatsapp", true)
                lockManager.saveLockedStatus("com.miui.appmanager.ApplicationsDetailsActivity", true)
                lockManager.saveLockedStatus("com.android.settings", true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        badWords = BadWordsManager(this).getBadWords()
        webSocketManager.initialize(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        configureService()
    }

    private fun configureService() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChange(it)
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> if (PasskeyManager(this).getStatus()) handleWindowChange(it)
            }
        }
    }

    private fun handleTextChange(event: AccessibilityEvent) {
        if (event.packageName == "com.whatsapp") {
            val currentText = event.text.joinToString(" ")
            if (currentText.isNotBlank()) {
                lastText = currentText
                handler.removeCallbacks(checkBadWordsRunnable)
                handler.postDelayed(checkBadWordsRunnable, TYPING_DELAY_MS)
            }
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val lockManager = LockManager(this)
        Log.d("AccessibilityEvent", "Package: ${event.packageName}, Class: ${event.className}, Text: ${event.text}")

        if (packageName == "com.android.settings" && lockManager.getLockedStatus(packageName)) {
            //performGlobalAction(GLOBAL_ACTION_HOME)

            handler.postDelayed({
                showLockScreen(packageName)
            }, 300)
            return
        }

        if (packageName == "com.whatsapp") {
            val dbHelper = ScheduleDataBase(this)
            val activeSchedules = dbHelper.getActiveSchedules()
            val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)

            if (activeSchedules.isNotEmpty()) {
                activeSchedules.forEach { schedule ->
                    createNotificationChannel(this, schedule.name, schedule.name)

                    val notification = createNotification(
                        this,
                        schedule.name,
                        "Le verrouillage du planning « ${schedule.name} » est en cours",
                        "Ce planning se terminera à ${schedule.endTime}"
                    )

                    notificationManager?.notify(schedule.id.hashCode(), notification)

                }
            }

            if ((lockManager.getPhoneStatus() || activeSchedules.isNotEmpty()) && lockManager.getLockedStatus(packageName)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                lockScreenRunnable?.let { handler.removeCallbacks(it) }
                lockScreenRunnable = Runnable {
                    showLockScreen(packageName)
                }
                handler.postDelayed(lockScreenRunnable!!, 100)
                return
            }
        }
    }

    private fun showLockScreen(packageName: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            putExtra("packageName", packageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        startActivity(intent)
    }

    private fun checkBadWords(text: String) {
        badWords.firstOrNull { text.contains(it, ignoreCase = true) }?.let {
            sendWebSocketNotification(text.replace("[\\[\\]]".toRegex(), ""))
        }
    }

    private fun sendWebSocketNotification(text: String) {
        webSocketManager.sendNotification(
            NotificationData(
                title = "BAD WORD",
                content = text,
                timestamp = System.currentTimeMillis(),
                type = "alert"
            )
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        lockScreenRunnable?.let { handler.removeCallbacks(it) }

        handler.removeCallbacks(checkBadWordsRunnable)
        unregisterReceiver(screenReceiver)
    }
}
