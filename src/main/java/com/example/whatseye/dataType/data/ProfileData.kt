package com.example.whatseye.dataType.data

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String,
    val last_name: String,
    val last_login: String?,
    val is_active: Boolean,
    val date_joined: String
)
data class ChildProfile(
    val id: String,
    val photo: String,
    val birthday: String,
    val phone_number: String,
    val whatsapp_name: String,
    val whatsapp2_name: String,
    val phone_locked: Boolean,
    val user: User
)
