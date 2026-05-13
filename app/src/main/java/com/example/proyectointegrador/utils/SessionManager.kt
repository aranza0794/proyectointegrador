package com.example.proyectointegrador.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "dogwalk_session", Context.MODE_PRIVATE
    )

    fun saveSession(userId: String, userType: String, userName: String) {
        prefs.edit()
            .putString("user_id", userId)
            .putString("user_type", userType)
            .putString("user_name", userName)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun getUserId(): String = prefs.getString("user_id", "") ?: ""
    fun getUserType(): String = prefs.getString("user_type", "") ?: ""
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun clearSession() = prefs.edit().clear().apply()
}