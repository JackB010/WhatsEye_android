package com.example.whatseye.api.managers


import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PasskeyManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("keys", Context.MODE_PRIVATE)

    // Key used to store the passkey in SharedPreferences
    private val PASSKEY_KEY = "passkey"
    private val PASSKEY_KEY_STATUS = "passkeystatus"


    private fun saveStatus(status: Boolean){
        sharedPreferences.edit().putBoolean(PASSKEY_KEY_STATUS, status).apply()
    }

    fun getStatus() :Boolean{
       return sharedPreferences.getBoolean(PASSKEY_KEY_STATUS, false)
    }
    /**
     * Save the passkey in SHA-256 format.
     */
    fun savePasskey(passkey: String) {
        val hashedPasskey = hashWithSHA512(passkey)
        sharedPreferences.edit().putString(PASSKEY_KEY, hashedPasskey).apply()
        saveStatus(true)
    }

    fun savePasskeyWS(hashedPasskey: String) {
        sharedPreferences.edit().putString(PASSKEY_KEY, hashedPasskey).apply()
    }
    /**
     * Get the stored passkey in SHA-256 format.
     */
    private fun getPasskey(): String {
        return sharedPreferences.getString(PASSKEY_KEY, "").toString()
    }

    /**
     * Hash a string using SHA-256.
     */
    private fun hashWithSHA512(input: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashedBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a provided passkey matches the stored passkey.
     */
    fun isPasskeyValid(passkey: String): Boolean {
        return hashWithSHA512(passkey) == getPasskey()
    }
    fun clearPassKey() {
        sharedPreferences.edit().clear().apply()
    }
}