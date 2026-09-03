package com.example.mumbaitransit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Accounts held in Firebase Auth, so the same login works on any device.
 *
 * The username is written twice on purpose: onto the Auth profile so it comes
 * back with the session on a fresh install, and under users/{uid} so chat can
 * look up a display name for someone else's uid without needing admin access.
 */
class CloudAuth {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    data class CloudUser(val uid: String, val username: String, val email: String)

    fun currentUser(): CloudUser? = auth.currentUser?.let {
        CloudUser(it.uid, it.displayName.orEmpty(), it.email.orEmpty())
    }

    suspend fun signUp(username: String, email: String, password: String): CloudUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Registration failed")

        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(username).build()
        ).await()

        db.child("users").child(user.uid).setValue(
            mapOf("username" to username, "email" to email)
        ).await()

        return CloudUser(user.uid, username, email)
    }

    suspend fun logIn(email: String, password: String): CloudUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("Sign in failed")

        // A profile written before displayName was set, or by another client,
        // still has its name under users/{uid}.
        val name = user.displayName?.takeIf { it.isNotBlank() }
            ?: db.child("users").child(user.uid).child("username")
                .get().await().getValue(String::class.java)
            ?: user.email?.substringBefore('@').orEmpty()

        return CloudUser(user.uid, name, user.email.orEmpty())
    }

    fun signOut() = auth.signOut()
}
