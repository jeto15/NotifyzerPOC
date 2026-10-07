package com.example.notifyzerpocphase1.util

import android.content.Context
import android.content.SharedPreferences

object ApiKeyManager {
    private const val PREFS_NAME = "crucible_intelligence_user_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(context: Context): String {
        return try {
            getPrefs(context).getString(KEY_GEMINI_API_KEY, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(context: Context, apiKey: String) {
        try {
            getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun clearApiKey(context: Context) {
        try {
            getPrefs(context).edit().remove(KEY_GEMINI_API_KEY).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
