package com.example.whatseye.api

import com.example.whatseye.dataType.data.ChildProfile
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApiService {
    @GET("accounts/profile/child/{id}/")
    fun getChildProfile(
        @Path("id") childId: String
    ): Call<ChildProfile>

    @Multipart
    @PATCH("accounts/profile/child/{id}/")
    fun patchChildProfile(
        @Path("id") childId: String,
        @Part("user.first_name") firstName: RequestBody,
        @Part("user.last_name") lastName: RequestBody,
        @Part("user.email") email: RequestBody,
        @Part("birthday") birthday: RequestBody,
        @Part("phone_number") phoneNumber: RequestBody,
        @Part("phone_locked") phoneLocked: RequestBody,
        @Part photo: MultipartBody.Part?
    ): Call<ChildProfile>

    @Multipart
    @PATCH("accounts/profile/child/{id}/")
    fun addChildProfile(
        @Path("id") childId: String,
        @Part("user.first_name") firstName: RequestBody,
        @Part("user.last_name") lastName: RequestBody,
        @Part("user.email") email: RequestBody,
        @Part photo: MultipartBody.Part?
    ): Call<ChildProfile>

}