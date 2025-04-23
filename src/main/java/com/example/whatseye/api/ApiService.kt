package com.example.whatseye.api

import com.example.whatseye.dataType.UsageData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/control/user-usage/{userId}/")
    fun sendUsageData(
        @Path("userId") userId: String,
        @Body data: UsageData
    ): Call<Void>
}
