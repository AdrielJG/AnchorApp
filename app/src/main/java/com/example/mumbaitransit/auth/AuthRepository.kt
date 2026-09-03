package com.example.mumbaitransit.auth

import android.content.Context
import com.example.mumbaitransit.backend.Backend

sealed class AuthResult {
    data class Success(val username: String) : AuthResult()
    data class Error(val message: String)    : AuthResult()
}

/**
 * Sign-up and sign-in.
 *
 * Routes to Firebase Auth when the backend is configured, so an account works
 * on any device; falls back to the on-device Room table otherwise. Call sites
 * see the same three methods either way.
 */
class AuthRepository(context: Context) {

    private val dao = UserDatabase.get(context).userDao()
    private val cloud: CloudAuth? = if (Backend.isCloud(context)) CloudAuth() else null

    val session = SessionManager(context)

    /** True when accounts and chat reach other devices. */
    val isCloud: Boolean get() = cloud != null

    // ─── Sign Up ─────────────────────────────────────────────────────────────
    suspend fun signUp(username: String, email: String, password: String): AuthResult {
        val user  = username.trim()
        val mail  = email.trim().lowercase()

        if (user.length < 3)
            return AuthResult.Error("Username must be at least 3 characters")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches())
            return AuthResult.Error("Enter a valid email address")
        if (password.length < 6)
            return AuthResult.Error("Password must be at least 6 characters")

        cloud?.let { c ->
            return try {
                val created = c.signUp(user, mail, password)
                session.saveSession(
                    created.uid.hashCode().toLong(), created.username, created.email, created.uid
                )
                AuthResult.Success(created.username)
            } catch (e: Exception) {
                AuthResult.Error(readable(e))
            }
        }

        if (dao.emailExists(mail) > 0)
            return AuthResult.Error("An account with this email already exists")
        if (dao.usernameExists(user) > 0)
            return AuthResult.Error("Username already taken")

        val id = dao.insert(
            UserEntity(username = user, email = mail, passwordHash = hashPassword(password))
        )
        val saved = dao.findById(id) ?: return AuthResult.Error("Registration failed")
        session.saveSession(saved.id, saved.username, saved.email, "local-${saved.id}")
        return AuthResult.Success(saved.username)
    }

    // ─── Log In ──────────────────────────────────────────────────────────────
    suspend fun logIn(email: String, password: String): AuthResult {
        val mail = email.trim().lowercase()

        cloud?.let { c ->
            return try {
                val user = c.logIn(mail, password)
                session.saveSession(
                    user.uid.hashCode().toLong(), user.username, user.email, user.uid
                )
                AuthResult.Success(user.username)
            } catch (e: Exception) {
                AuthResult.Error(readable(e))
            }
        }

        val user = dao.findByEmail(mail)
            ?: return AuthResult.Error("No account found with this email")
        if (user.passwordHash != hashPassword(password))
            return AuthResult.Error("Incorrect password")

        session.saveSession(user.id, user.username, user.email, "local-${user.id}")
        return AuthResult.Success(user.username)
    }

    // ─── Log Out ─────────────────────────────────────────────────────────────
    fun logOut() {
        cloud?.signOut()
        session.clearSession()
    }

    /**
     * Restores a cloud session the SDK is still holding.
     *
     * Firebase keeps the user signed in across restarts, but SharedPreferences
     * is cleared on reinstall — without this the SDK would think you are signed
     * in while the app thinks you are not.
     */
    fun restoreSession() {
        val c = cloud ?: return
        if (session.isLoggedIn()) return
        c.currentUser()?.let {
            session.saveSession(it.uid.hashCode().toLong(), it.username, it.email, it.uid)
        }
    }

    /** Firebase exception text is written for developers, not commuters. */
    private fun readable(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("password is invalid", true) ||
            msg.contains("credential is incorrect", true) -> "Incorrect email or password"
            msg.contains("no user record", true)          -> "No account found with this email"
            msg.contains("already in use", true)          -> "An account with this email already exists"
            msg.contains("network", true)                 -> "No connection — check your internet"
            msg.contains("badly formatted", true)         -> "Enter a valid email address"
            else -> msg.ifBlank { "Something went wrong. Try again." }
        }
    }
}
