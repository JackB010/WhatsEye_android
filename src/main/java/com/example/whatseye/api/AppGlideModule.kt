package com.example.whatseye

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.example.whatseye.api.SSLUtils
import java.io.InputStream

@GlideModule
class AppGlideModule : AppGlideModule() {
    private val TAG = "AppGlideModule"

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        Log.d(TAG, "Registering OkHttpUrlLoader with custom SSL configuration")
        val okHttpClient = SSLUtils.getOkHttpClient(context)
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false // Optimize by disabling manifest parsing
    }
}