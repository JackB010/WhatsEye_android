package com.example.whatseye.dataType

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val username: String,
    val profilePicture: String,
    val accountType: String
)
