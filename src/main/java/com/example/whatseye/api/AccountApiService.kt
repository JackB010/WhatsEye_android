package com.example.whatseye.api

import com.example.whatseye.dataType.data.ChildJoinRequest
import com.example.whatseye.dataType.data.LoginData
import com.example.whatseye.dataType.data.RefreshTokenRequest
import com.example.whatseye.dataType.data.RefreshTokenResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AccountApiService {

    @POST("accounts/join/child/{fid}/{qrCode}/")
    fun joinChild(
        @Path("fid") fid: String,
        @Path("qrCode") qrCode: String,
        @Body request: ChildJoinRequest
    ): Call<ResponseBody>


    @POST("accounts/token/child/{pid}/{qrCode}/")
    fun loginUser(
        @Path("pid") pid: String,
        @Path("qrCode") qrCode: String,
        @Body data: LoginData
    ): Call<ResponseBody> // <-- Use this to access raw JSON response

    @POST("accounts/token/refresh/")
    fun refreshToken(
        @Body data: RefreshTokenRequest
    ): Call<RefreshTokenResponse>
}

