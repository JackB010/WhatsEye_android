package com.example.whatseye

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.whatseye.dataType.JavaScriptCode

class LoggedInActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var contentTextView: TextView
    private lateinit var btnGetContact: Button
    private var pageLoaded: Boolean = false
    companion object {
        var text: Any = ""
    }

    val text:String = ""
    private var clicked = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logged_in_activity)

        initializeViews()
        setupWebView()
        loadWhatsAppWeb()
        btnGetContact.setOnClickListener {

            if (!clicked) {
                checkCurrentChats()
                loadCurrentChat()
//                blockUser()
            } else {
                checkForContact()
                loadContact()
                //loadCurrentChat()
            }
            clicked = !clicked
        }

    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview)
        contentTextView = findViewById(R.id.profileTextView)
        btnGetContact = findViewById(R.id.btngetContact)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }
       // webView.addJavascriptInterface(WebAppInterface(), "Androidf")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                restoreCookies()
                val jsCode = """
                    (function() {
                        var element = document.getElementById('expressions-panel-container')
                        return (element !== null);
                    })();
                """.trimIndent()

                webView.evaluateJavascript(jsCode) {pageLoaded = true}
                if(this@LoggedInActivity.pageLoaded){contentTextView.text = "hello"}

            }
        }
    }

    private fun loadWhatsAppWeb() {
        webView.loadUrl("https://web.whatsapp.com")
        ensureCookiesAccepted()
        restoreCookies()
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

    private fun checkForContact() {
        webView.evaluateJavascript(
            """(()=>{
            const a = document.querySelector("span[data-icon='new-chat-outline']");
            if (a) a.click();
        })();""".trimIndent()
        ) { webView.evaluateJavascript(JavaScriptCode.CONTACT, null) }
    }
    private fun loadContact(){
        webView.evaluateJavascript(
            """( ()=>{          const data = localStorage.getItem("CONTACT");
                        return data;
            })();""".trimIndent()
        ) { html -> contentTextView.text = html }
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