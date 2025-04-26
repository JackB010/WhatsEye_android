package com.example.whatseye.api.managers

import android.content.Context
import android.content.SharedPreferences

class JwtTokenManager(context: Context) {
    // 1. Get SharedPreferences through context
    //authJWT
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("OK", Context.MODE_PRIVATE)

    // 2. Remove 'override' and 'suspend' (SharedPreferences operations are synchronous)
    fun saveAccessJwt(token: String) {
        sharedPrefs.edit().putString("ACCESS", token).apply()
    }
    fun saveRefreshJwt(token: String) {
        sharedPrefs.edit().putString("REFRESH", token).apply()
    }
    fun setIsLogin(isLogin: Boolean){
        sharedPrefs.edit().putBoolean("isLogin", isLogin).apply()
    }
    fun getAccessJwt(): String? {
        return sharedPrefs.getString("ACCESS", null)
    }

    fun setUsername(username: String) {
        sharedPrefs.edit().putString("username", username).apply()
    }
    fun setUserId(userId: String) {
        sharedPrefs.edit().putString("userId", userId).apply()
    }
    fun setFamilyId(family_id: String) {
        sharedPrefs.edit().putString("family_id", family_id).apply()
    }

    fun getUsername(): String? = sharedPrefs.getString("username", null)
    fun getUserId(): String? { return sharedPrefs.getString("userId", null) }
    fun getFamilyId(): String? = sharedPrefs.getString("family_id", null)

    fun getRefreshJwt(): String? = sharedPrefs.getString("REFRESH", null)
    fun getIsLogin(): Boolean = sharedPrefs.getBoolean("isLogin", false)

    fun clearAllTokens() {
        sharedPrefs.edit().clear().apply()
    }
}