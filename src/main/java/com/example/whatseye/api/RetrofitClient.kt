package com.example.whatseye.api

import android.content.Context
import com.example.whatseye.api.managers.JwtTokenManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private const val BASE_URL = "http://192.168.89.116:8000/api/"

    private lateinit var jwtTokenManager: JwtTokenManager

    fun initialize(context: Context) {
        jwtTokenManager = JwtTokenManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { jwtTokenManager.getAccessJwt() })
            .build()
    }

    private val noAuthRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
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

