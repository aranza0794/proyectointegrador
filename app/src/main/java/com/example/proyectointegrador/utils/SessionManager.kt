package com.example.proyectointegrador.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("PatitasEnRuta", Context.MODE_PRIVATE)

    fun saveSession(userId: String, userType: String, userName: String) {
        prefs.edit()
            .putString("userId",   userId)
            .putString("userType", userType)
            .putString("userName", userName)
            .putBoolean("isLoggedIn", true)
            .apply()
    }

    fun getUserId():   String  = prefs.getString("userId",   "") ?: ""
    fun getUserType(): String  = prefs.getString("userType", "") ?: ""
    fun getUserName(): String  = prefs.getString("userName", "") ?: ""
    fun isLoggedIn():  Boolean = prefs.getBoolean("isLoggedIn", false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}