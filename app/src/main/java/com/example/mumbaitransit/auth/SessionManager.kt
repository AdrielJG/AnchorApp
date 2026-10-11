package com.example.mumbaitransit.auth

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("anchor_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID   = "user_id"
        private const val KEY_UID       = "uid"
        private const val KEY_NAME      = "name"
        private const val KEY_USERNAME  = "username"
        private const val KEY_PHONE     = "phone"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val NO_USER = -1L
    }

    /**
     * [uid] is the account's real identity — a Firebase uid when the backend is
     * configured. [userId] stays a Long because the local fallback tables key on
     * it; for a cloud account it is a stable hash of the uid, so those tables
     * keep working without a schema migration.
     */
    fun saveSession(
        userId: Long,
        name: String,
        username: String,
        phone: String,
        uid: String = ""
    ) {
        prefs.edit()
            .putString(KEY_UID, uid.ifBlank { userId.toString() })
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PHONE, phone)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    /**
     * The name is the one field not on the Firebase Auth record, so a session
     * restored after a reinstall comes back without it. The profile screen calls
     * this once it has read the profile.
     */
    fun updateName(name: String) {
        if (name.isBlank()) return
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): Long     = prefs.getLong(KEY_USER_ID, NO_USER)
    fun getUid(): String      = prefs.getString(KEY_UID, "") ?: ""
    fun getName(): String     = prefs.getString(KEY_NAME, "") ?: ""
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getPhone(): String    = prefs.getString(KEY_PHONE, "") ?: ""
}
