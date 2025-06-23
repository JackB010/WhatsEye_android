package com.example.whatseye.api

import android.content.Context
import com.example.whatseye.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object SSLUtils {
    fun getOkHttpClient(context: Context): OkHttpClient {
        try {
            // Load the self-signed certificate
            val certificateInputStream = context.resources.openRawResource(R.raw.localhost)
            val certificateFactory = java.security.cert.CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(certificateInputStream)
            certificateInputStream.close()

            // Create a KeyStore containing the certificate
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("localhost", certificate)

            // Create a TrustManager that trusts the certificate
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.trustManagers
            val trustManager = trustManagers[0] as X509TrustManager

            // Create an SSLContext
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagers, null)

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()
        } catch (e: Exception) {
            throw RuntimeException("Failed to configure SSL: ${e.message}", e)
        }
    }
}