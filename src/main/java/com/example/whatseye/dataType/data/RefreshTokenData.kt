package com.example.whatseye.dataType.data

data class RefreshTokenRequest(
    val refresh: String
)

data class RefreshTokenResponse(
    val access: String,
    val refresh: String? // Adjust fields based on your API response
)