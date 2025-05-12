package com.example.whatseye.dataType.data

data class PhoneNumber(
    val phone_number: String
)

data class ChildJoinRequest(
    val username: String,
    val phone_number: PhoneNumber,
    val password: String,
    val password1: String,
    val gender: Char?,
    val birthday: String
)
