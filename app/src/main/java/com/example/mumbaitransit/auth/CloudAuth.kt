package com.example.mumbaitransit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await

/** Thrown when the name is already claimed, so the caller can say which field failed. */
class UsernameTakenException : Exception("Username already taken")

/**
 * Accounts held in Firebase Auth, so the same login works on any device.
 *
 * Sign-up takes exactly the four fields the form asks for. The username is
 * written in three places, each for a different reader: onto the Auth profile so
 * it comes back with the session on a fresh install, under users/{uid} as part
 * of the profile, and into usernames/{lowercase} as a claim so nobody else can
 * take it.
 */
class CloudAuth {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    data class CloudUser(
        val uid: String,
        val name: String,
        val username: String,
        val phone: String
    )

    fun signedIn(): Boolean = auth.currentUser != null

    /**
     * The session the SDK is still holding, without a round trip.
     *
     * [CloudUser.name] comes back blank here — it lives in the database, not on
     * the Auth record, and this is called on the main thread. The profile screen
     * fills it in on its first read.
     */
    fun currentUser(): CloudUser? = auth.currentUser?.let {
        CloudUser(
            uid = it.uid,
            name = "",
            username = it.displayName.orEmpty(),
            phone = Phone.fromAuthEmail(it.email.orEmpty())
        )
    }

    /** True when the name is free. Cheap pre-check; the claim below is the real one. */
    suspend fun isUsernameFree(username: String): Boolean =
        !db.child("usernames").child(username.lowercase()).get().await().exists()

    suspend fun signUp(
        name: String,
        username: String,
        phoneE164: String,
        password: String
    ): CloudUser {
        val lower = username.lowercase()

        // Answer quickly in the common case, before an account exists to clean up.
        if (!isUsernameFree(lower)) throw UsernameTakenException()

        val result = auth
            .createUserWithEmailAndPassword(Phone.toAuthEmail(phoneE164), password)
            .await()
        val user = result.user ?: throw IllegalStateException("Registration failed")

        // Two people can clear the pre-check at the same instant. Only one write
        // gets past the database rule, and the loser is rolled back rather than
        // left holding an account with no name against it.
        try {
            db.child("usernames").child(lower).setValue(user.uid).await()
        } catch (e: Exception) {
            runCatching { user.delete().await() }
            throw UsernameTakenException()
        }

        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(username).build()
            ).await()
        }

        // Only the four sign-up fields. home, work, addresses and routes are
        // absent until the user adds them — an empty branch is not written.
        db.child("users").child(user.uid).updateChildren(
            mapOf<String, Any>(
                "name"          to name,
                "username"      to username,
                "usernameLower" to lower,
                "phone"         to phoneE164,
                "createdAt"     to ServerValue.TIMESTAMP
            )
        ).await()

        return CloudUser(user.uid, name, username, phoneE164)
    }

    suspend fun logIn(phoneE164: String, password: String): CloudUser {
        val result = auth
            .signInWithEmailAndPassword(Phone.toAuthEmail(phoneE164), password)
            .await()
        val user = result.user ?: throw IllegalStateException("Sign in failed")

        val snap = db.child("users").child(user.uid).get().await()
        val username = snap.child("username").getValue(String::class.java)
            ?.takeIf { it.isNotBlank() }
            ?: user.displayName.orEmpty()
        val name = snap.child("name").getValue(String::class.java).orEmpty()

        return CloudUser(user.uid, name, username, phoneE164)
    }

    fun signOut() = auth.signOut()
}
