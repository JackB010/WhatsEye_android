package com.example.whatseye

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WhatsAppActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pageLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp)

        initializeWebView()

        // Enable and restore cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        restoreCookies()

        // Load WhatsApp Web
        webView.loadUrl("https://web.whatsapp.com/")
    }

    private fun initializeWebView() {
        webView = findViewById(R.id.webview)
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Desktop User-Agent
        val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.5938.62 Safari/537.36"
        settings.userAgentString = desktopUserAgent

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!url.isNullOrEmpty()) {

                    waitForElementToLoad("wa-popovers-bucket") {
                        // ✅ Element is loaded — WhatsApp UI is ready
                        saveCookies(url ?: "")
                        webView.evaluateJavascript("""(()=>{
                            btn = document.querySelectorAll('[role="button"]')
                            btn[btn.length-1].click()
                            })();""".trimIndent(), null)
                    }

                    //saveCookies(url)


                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }
        }
    }
    private fun waitForElementToLoad(
        elementId: String,
        maxRetries: Int = 20,
        delayMillis: Long = 1000,
        onLoaded: () -> Unit
    ) {
        if (maxRetries <= 0) return

        val js = """
        (function() {
            return document.getElementById("$elementId") !== null;
        })();
    """.trimIndent()

        webView.evaluateJavascript(js) { result ->
            val isLoaded = result.toBooleanStrictOrNull() ?: false
            if (isLoaded) {
                onLoaded()
            } else {
                webView.postDelayed({
                    waitForElementToLoad(elementId, maxRetries - 1, delayMillis, onLoaded)
                }, delayMillis)
            }
        }
    }


    private fun saveCookies(url: String) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        if (cookies != null) {
            val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("whatsapp_cookies", cookies).apply()
        }
    }

    private fun restoreCookies() {
        val sharedPreferences = getSharedPreferences("whatsapp_prefs", MODE_PRIVATE)
        val savedCookies = sharedPreferences.getString("whatsapp_cookies", null)
        if (!savedCookies.isNullOrEmpty()) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie("https://web.whatsapp.com", savedCookies)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
