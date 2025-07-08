package com.example.whatseye.whatsapp

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.whatseye.MainActivity
import com.example.whatseye.R
import com.example.whatseye.api.managers.JwtTokenManager
import com.example.whatseye.utils.createNotification
import com.example.whatseye.utils.createNotificationChannel
import com.google.android.material.progressindicator.CircularProgressIndicator

class WhatsAppLink2Activity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var loadingBackground: View
    private lateinit var loadingImage: ImageView
    private lateinit var loadingText: TextView
    private val MAX_RETRIES = 120
    private val POLL_DELAY_MS = 1000L
    private var retryCount = 0
    private var checkLoginCount = 0

    private val handler = Handler(Looper.getMainLooper())
    private val checkLoginRunnable = Runnable { checkLogin() }

    override fun onCreate(savedInstanceState: Bundle
    ?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix("WhatsApp2")
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp)

        initializeViews()
        handler.removeCallbacks(checkLoginRunnable)

        setupWebView()
        if (!JwtTokenManager(this).getIsLoginWhatsApp2())
            loadWhatsAppWeb()
        else{
            val intent = Intent(this@WhatsAppLink2Activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webview_whatsapp)
        progressIndicator = findViewById(R.id.loading_spinner)
        loadingBackground = findViewById(R.id.loading_overlay)
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)
        hideLoading()
    }

    private fun showLoading() {
        loadingBackground.visibility = View.VISIBLE
        progressIndicator.visibility = View.VISIBLE
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }
        loadingImage.startAnimation(rotate)
        loadingText.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingBackground.visibility = View.GONE
        progressIndicator.visibility = View.GONE
        loadingImage.clearAnimation()
        loadingImage.visibility = View.GONE
        loadingText.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                retryCount = 0
                startPollingForElement()
            }

            private fun startPollingForElement() {
                val jsCheckElement = """
                    (function() {
                        return document.querySelector('[role="img"]') !== null;
                    })();
                """.trimIndent()

                webView.evaluateJavascript(jsCheckElement) { result ->
                    val isElementLoaded = result?.toBoolean() ?: false
                    if (isElementLoaded) {
                        handleElementLoaded()
                    } else if (retryCount < MAX_RETRIES) {
                        retryCount++
                        Handler(Looper.getMainLooper()).postDelayed({
                            startPollingForElement()
                        }, POLL_DELAY_MS)
                    } else {
                        checkLogin()
                    }
                }
            }

            private fun handleElementLoaded() {
                val clickJs = """
                    (function() {
                        const buttons = document.querySelectorAll('[role="button"]');
                        if (buttons.length > 0) {
                            buttons[buttons.length - 2].click();
                            return true;
                        }
                        return false;
                    })();
                """.trimIndent()

                webView.evaluateJavascript(clickJs) {
                    val centerJs = """
                        (function() {
                            let element = document.querySelector('#app');
                            if (element && element.firstChild && element.firstChild.lastChild) {
                                element = element.firstChild.lastChild;
                                element.style.position = 'fixed';
                                element.style.top = '50%';
                                element.style.left = '50%';
                                element.style.transform = 'translate(-50%, -50%)';
                            }
                            let item = document.querySelector('[data-icon="web-login-desktop-upsell-illustration"]')
                            item = item.parentElement.parentElement.parentElement
                            item.remove()
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(centerJs) {
                        hideLoading()
                        handleSendCode()
                    }
                }
            }

            private fun handleSendCode() {
                val jsCheckElement = """
                    (function() {
                        return document.querySelector('div[data-link-code]') !== null;
                    })();
                """.trimIndent()

                webView.evaluateJavascript(jsCheckElement) { result ->
                    val isElementLoaded = result?.toBoolean() ?: false
                    if (isElementLoaded) {
                        webView.evaluateJavascript(
                            """
                            (function() {
                                return document.querySelector('div[data-link-code]').getAttribute('data-link-code');
                            })();
                        """.trimIndent()
                        ) { data ->
                            val code = data.replace("\"", "").replace(",","").chunked(4).joinToString("-")
                            createNotificationChannel(
                                this@WhatsAppLink2Activity,
                                "Code de liaison",
                                "Code de liaison"
                            )
                            val notification = createNotification(
                                this@WhatsAppLink2Activity, "Code de liaison", "Code de liaison",
                                "Votre code de liaison WhatsApp est : $code"
                            )
                            ContextCompat.getSystemService(
                                this@WhatsAppLink2Activity,
                                NotificationManager::class.java
                            )?.notify(10001, notification)
                        }
                        getInform()
                    } else if (retryCount < MAX_RETRIES) {
                        retryCount++
                        Handler(Looper.getMainLooper()).postDelayed({
                            handleSendCode()
                        }, POLL_DELAY_MS)
                    }
                }
            }

            private fun getInform() {
                checkLogin()
                val intent = Intent(this@WhatsAppLink2Activity, WhatsAppLinkInformActivity::class.java)
                intent.putExtra("intent", "WhatsAppLink2Activity")
                startActivity(intent)
            }

            private fun handleElementNotFound() {
                hideLoading()
                Log.e("WebView", "Element not found after max retries")
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkLogin() {
        val jsCode = """
            (function() {
                var element = document.querySelector('div[data-testid="chat-list"]');
                return (element !== null);
            })();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { data ->
            if (data.toBoolean()) {
                JwtTokenManager(this@WhatsAppLink2Activity).setIsLoginWhatsApp2(true)
                handler.removeCallbacks(checkLoginRunnable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                }

                webView.loadUrl("about:blank")
                val intent = Intent(this@WhatsAppLink2Activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                if (checkLoginCount < MAX_RETRIES) {
                    checkLoginCount++
                    handler.postDelayed(checkLoginRunnable, POLL_DELAY_MS)
                } else {
                    Log.e("WebView", "Login check max retries reached")
                    JwtTokenManager(this@WhatsAppLink2Activity).setIsLoginWhatsApp2(false)
                }
            }
        }
    }

    private fun loadWhatsAppWeb() {
        webView.loadUrl("https://web.whatsapp.com")
        ensureCookiesAccepted()
    }

    private fun ensureCookiesAccepted() {
        CookieManager.getInstance().setAcceptCookie(true)
    }
}