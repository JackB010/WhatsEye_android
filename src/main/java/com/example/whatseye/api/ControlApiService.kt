package com.example.whatseye.api

import com.example.whatseye.dataType.data.UsageData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ControlApiService {

    @Multipart
    @POST("control/records/{child_id}/")
    fun uploadRecording(
        @Path("child_id") childId: String,
        @Part("recording_type") type: RequestBody,
        @Part audioFile: MultipartBody.Part // ← Correct: No name in annotation
    ): Call<ResponseBody>

    @POST("control/user-usage/")
    fun sendUsageData(
        @Body data: UsageData
    ): Call<Void>
}
