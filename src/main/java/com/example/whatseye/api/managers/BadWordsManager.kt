package com.example.whatseye.api.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BadWordsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bad_words_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()
    private val key = "BAD_WORDS_LIST"

    fun saveBadWords(words: List<String>) {
        val json = gson.toJson(words)
        prefs.edit().putString(key, json).apply()
    }

    fun getBadWords(): List<String> {
        val json = prefs.getString(key, null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearBadWords() {
        prefs.edit().remove(key).apply()
    }
}
