package com.example.whatseye.dataType

import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface {
    private var data: String? = null
    private var pageLoaded: Boolean = false

    @JavascriptInterface
    fun getContact(data: String) {
        this.data = data

    }
    @JavascriptInterface
    fun setLoaded(isLoaded: Boolean) {
        this.pageLoaded = isLoaded
    }

}