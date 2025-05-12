package com.example.whatseye.api

import android.content.Context
import com.example.whatseye.api.managers.JwtTokenManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.89.116:8000/api/"  // your Django server IP

    private lateinit var jwtTokenManager: JwtTokenManager

    fun initialize(context: Context) {
        jwtTokenManager = JwtTokenManager(context)
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor { jwtTokenManager.getAccessJwt() })
        .build()

    private val noAuthRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private val AuthRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    val controlApi: ControlApiService = AuthRetrofit.create(ControlApiService::class.java)
    val profileApi: ProfileApiService = AuthRetrofit.create(ProfileApiService::class.java)
    val accountApi: AccountApiService = noAuthRetrofit.create(AccountApiService::class.java)
}