package com.example.whatseye.api.managers

import android.content.Context
import android.content.SharedPreferences

class LockManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("lock_prefs", Context.MODE_PRIVATE)

    // Key used to store the passkey in SharedPreferences
    private val PHONE_STATUS = "phone-status"


    fun saveLockedStatus(packageName: String,status: Boolean) {
        sharedPreferences.edit().putBoolean(packageName, status).apply()
    }

    fun getLockedStatus(packageName: String): Boolean {
        return sharedPreferences.getBoolean(packageName, true)
    }

    fun savePhoneStatus(status: Boolean) {
        sharedPreferences.edit().putBoolean(PHONE_STATUS, status).apply()
    }

    fun getPhoneStatus(): Boolean {
        return sharedPreferences.getBoolean(PHONE_STATUS, true)
    }
    fun clearAllLocks() {
        sharedPreferences.edit().clear().apply()
    }
}