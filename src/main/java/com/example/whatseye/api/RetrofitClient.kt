package com.example.whatseye.api

import android.annotation.SuppressLint
import android.content.Context
import com.example.whatseye.api.managers.JwtTokenManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@SuppressLint("StaticFieldLeak")
object RetrofitClient {
    private const val BASE_URL = "https://192.168.204.116:443/api/"
    private lateinit var jwtTokenManager: JwtTokenManager
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context
        jwtTokenManager = JwtTokenManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        SSLUtils.getOkHttpClient(context)
            .newBuilder()
            .addInterceptor(AuthInterceptor { jwtTokenManager.getAccessJwt() })
            .build()
    }

    private val noAuthRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(SSLUtils.getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    val controlApi: ControlApiService by lazy { authRetrofit.create(ControlApiService::class.java) }
    val profileApi: ProfileApiService by lazy { authRetrofit.create(ProfileApiService::class.java) }
    val accountApi: AccountApiService by lazy { noAuthRetrofit.create(AccountApiService::class.java) }
}