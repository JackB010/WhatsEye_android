package com.example.whatseye

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebSettings

class WhatsAppActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp)

        webView = findViewById(R.id.webview)
        // Enable DOM storage
        webView.settings.domStorageEnabled = true
        // Setup cache mode (optional)
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        // Set the user agent to a desktop browser (this is a Chrome user agent)
        val newUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        webView.settings.userAgentString = newUserAgent

        // Set a WebViewClient to handle loading within the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Check if cookies are set after the page is loaded
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)

                // Save cookies to SharedPreferences
                saveCookies(cookies)
//                webView.evaluateJavascript("""(()=>{const a = document.querySelector("span[data-icon='new-chat-outline']");
//                                                if (a) a.click();})();""".trimIndent()){}
                // Check if the expected cookies for WhatsApp are present
                    // Start the LoggedInActivity
                 /*   val intent = Intent(this@WhatsAppActivity, LoggedInActivity::class.java)
                    intent.putExtra("cookies", cookies)
                    startActivity(intent)
                    finish()*/ // Optionally call finish() to close MainActivity

            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Load the URL in the same WebView
                view?.loadUrl(url ?: "")
                return true
            }
        }

        // Load the WhatsApp Web URL
        webView.loadUrl("https://web.whatsapp.com")

        // Ensure cookies are accepted
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // Restore cookies if available
        restoreCookies()
    }

    private fun saveCookies(cookies: String?) {
        // Save cookies to SharedPreferences
        val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("whatsapp_cookies", cookies).apply()
    }

    private fun restoreCookies() {
        // Restore cookies from SharedPreferences
        val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
        val savedCookies = sharedPreferences.getString("whatsapp_cookies", null)
        if (savedCookies != null) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie("https://web.whatsapp.com", savedCookies)
        }
    }

    override fun onBackPressed() {
        // Allow the user to navigate back within the WebView
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}