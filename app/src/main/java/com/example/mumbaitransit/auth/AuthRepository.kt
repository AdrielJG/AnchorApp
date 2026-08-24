package com.example.mumbaitransit.auth

import android.content.Context

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String)    : AuthResult()
}

class AuthRepository(context: Context) {

    private val dao = UserDatabase.get(context).userDao()
    val session     = SessionManager(context)

    // ─── Sign Up ─────────────────────────────────────────────────────────────
    suspend fun signUp(username: String, email: String, password: String): AuthResult {
        val trimmedUser  = username.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedUser.length < 3)
            return AuthResult.Error("Username must be at least 3 characters")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches())
            return AuthResult.Error("Enter a valid email address")
        if (password.length < 6)
            return AuthResult.Error("Password must be at least 6 characters")

        if (dao.emailExists(trimmedEmail) > 0)
            return AuthResult.Error("An account with this email already exists")
        if (dao.usernameExists(trimmedUser) > 0)
            return AuthResult.Error("Username already taken")

        val entity = UserEntity(
            username     = trimmedUser,
            email        = trimmedEmail,
            passwordHash = hashPassword(password)
        )
        val id = dao.insert(entity)
        val saved = dao.findById(id) ?: return AuthResult.Error("Registration failed")
        session.saveSession(saved.id, saved.username, saved.email)
        return AuthResult.Success(saved)
    }

    // ─── Log In ──────────────────────────────────────────────────────────────
    suspend fun logIn(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val user = dao.findByEmail(trimmedEmail)
            ?: return AuthResult.Error("No account found with this email")

        if (user.passwordHash != hashPassword(password))
            return AuthResult.Error("Incorrect password")

        session.saveSession(user.id, user.username, user.email)
        return AuthResult.Success(user)
    }

    // ─── Log Out ─────────────────────────────────────────────────────────────
    fun logOut() = session.clearSession()
}
