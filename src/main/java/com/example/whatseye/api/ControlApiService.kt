package com.example.whatseye.api

import com.example.whatseye.dataType.data.UsageData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ControlApiService {

    @POST("control/user-usage/")
    fun sendUsageData(
        @Body data: UsageData
    ): Call<Void>
}
