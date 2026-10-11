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
 * Sign-up asks for four things and no more — name, username, phone, password.
 * Home, work, custom addresses and saved routes are added afterwards through
 * [ProfileRepository]; none of them is a condition of having an account.
 *
 * Routes to Firebase Auth when the backend is configured, so an account works on
 * any device; falls back to the on-device Room table otherwise. Call sites see
 * the same methods either way.
 */
class AuthRepository(context: Context) {

    private val dao = UserDatabase.get(context).userDao()
    private val cloud: CloudAuth? = if (Backend.isCloud(context)) CloudAuth() else null

    val session = SessionManager(context)

    /** True when accounts and chat reach other devices. */
    val isCloud: Boolean get() = cloud != null

    // ─── Sign Up ─────────────────────────────────────────────────────────────
    suspend fun signUp(
        name: String,
        username: String,
        phone: String,
        password: String
    ): AuthResult {
        val fullName = name.trim()
        val user     = username.trim()
        val e164     = Phone.normalize(phone)

        if (fullName.length < 2)
            return AuthResult.Error("Enter your name")
        if (user.length < 3 || user.length > 20)
            return AuthResult.Error("Username must be 3–20 characters")
        if (!user.matches(Regex("^[A-Za-z0-9._]+$")))
            return AuthResult.Error("Username can use letters, numbers, dot and underscore only")
        if (e164 == null)
            return AuthResult.Error("Enter a valid phone number")
        if (password.length < 6)
            return AuthResult.Error("Password must be at least 6 characters")

        cloud?.let { c ->
            return try {
                val created = c.signUp(fullName, user, e164, password)
                session.saveSession(
                    created.uid.hashCode().toLong(),
                    created.name, created.username, created.phone, created.uid
                )
                AuthResult.Success(created.username)
            } catch (e: UsernameTakenException) {
                AuthResult.Error("Username already taken")
            } catch (e: Exception) {
                AuthResult.Error(readable(e))
            }
        }

        if (dao.phoneExists(e164) > 0)
            return AuthResult.Error("An account with this phone number already exists")
        if (dao.usernameExists(user) > 0)
            return AuthResult.Error("Username already taken")

        val id = dao.insert(
            UserEntity(
                name = fullName,
                username = user,
                phone = e164,
                passwordHash = hashPassword(password)
            )
        )
        val saved = dao.findById(id) ?: return AuthResult.Error("Registration failed")
        session.saveSession(
            saved.id, saved.name, saved.username, saved.phone, "local-${saved.id}"
        )
        return AuthResult.Success(saved.username)
    }

    // ─── Log In ──────────────────────────────────────────────────────────────
    /** The phone number is the login. It resolves without a lookup — see [Phone]. */
    suspend fun logIn(phone: String, password: String): AuthResult {
        val e164 = Phone.normalize(phone)
            ?: return AuthResult.Error("Enter a valid phone number")

        cloud?.let { c ->
            return try {
                val user = c.logIn(e164, password)
                session.saveSession(
                    user.uid.hashCode().toLong(),
                    user.name, user.username, user.phone, user.uid
                )
                AuthResult.Success(user.username)
            } catch (e: Exception) {
                AuthResult.Error(readable(e))
            }
        }

        val user = dao.findByPhone(e164)
            ?: return AuthResult.Error("No account found with this phone number")
        if (user.passwordHash != hashPassword(password))
            return AuthResult.Error("Incorrect password")

        session.saveSession(
            user.id, user.name, user.username, user.phone, "local-${user.id}"
        )
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
     * in while the app thinks you are not. The name comes back blank here and is
     * filled in by the profile screen's first read.
     */
    fun restoreSession() {
        val c = cloud ?: return
        if (session.isLoggedIn()) return
        c.currentUser()?.let {
            session.saveSession(
                it.uid.hashCode().toLong(), it.name, it.username, it.phone, it.uid
            )
        }
    }

    /** Firebase exception text is written for developers, not commuters. */
    private fun readable(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("password is invalid", true) ||
            msg.contains("credential is incorrect", true) -> "Incorrect phone number or password"
            msg.contains("no user record", true)          -> "No account found with this phone number"
            msg.contains("already in use", true)          -> "An account with this phone number already exists"
            msg.contains("network", true)                 -> "No connection — check your internet"
            msg.contains("badly formatted", true)         -> "Enter a valid phone number"
            msg.contains("blocked all requests", true)    -> "Too many attempts — try again in a few minutes"
            else -> msg.ifBlank { "Something went wrong. Try again." }
        }
    }
}
