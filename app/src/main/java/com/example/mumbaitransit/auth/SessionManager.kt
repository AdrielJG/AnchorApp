package com.example.mumbaitransit.auth

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("anchor_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID   = "user_id"
        private const val KEY_USERNAME  = "username"
        private const val KEY_EMAIL     = "email"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val NO_USER = -1L
    }

    fun saveSession(userId: Long, username: String, email: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): Long       = prefs.getLong(KEY_USER_ID, NO_USER)
    fun getUsername(): String   = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getEmail(): String      = prefs.getString(KEY_EMAIL, "") ?: ""
}
