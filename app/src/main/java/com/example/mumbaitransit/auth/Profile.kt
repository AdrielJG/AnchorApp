package com.example.mumbaitransit.auth

/**
 * The account model.
 *
 * Four fields are collected at sign-up — name, username, phone, password — and
 * nothing else is ever required. [home], [work], [addresses] and [routes] start
 * empty and are filled in from the profile screen or by saving a route, so an
 * account is usable the second it is created.
 *
 * The password is not in here on purpose. Firebase Auth holds it, salted and
 * hashed on Google's side; storing it in the database as well would mean a
 * readable copy of every password sitting next to the data it protects.
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val createdAt: Long = 0L,
    val home: SavedPlace? = null,
    val work: SavedPlace? = null,
    val addresses: List<SavedPlace> = emptyList(),
    val routes: List<SavedRoute> = emptyList()
)

/**
 * A pinned location. [KIND_HOME] and [KIND_WORK] are singletons — setting one
 * replaces whatever was there. [KIND_CUSTOM] is a list and holds as many as the
 * user wants to add.
 */
data class SavedPlace(
    val id: String = "",
    val kind: String = KIND_CUSTOM,
    val label: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val savedAt: Long = 0L
) {
    val hasFix: Boolean get() = lat != 0.0 || lon != 0.0

    companion object {
        const val KIND_HOME   = "home"
        const val KIND_WORK   = "work"
        const val KIND_CUSTOM = "custom"
    }
}

/** A route the user starred. Many per account. */
data class SavedRoute(
    val id: String = "",
    val originLabel: String = "",
    val destLabel: String = "",
    val scenario: String = "",
    val scenarioLabel: String = "",
    val modeStr: String = "",
    val totalMin: Double = 0.0,
    val totalFare: Int = 0,
    val transfers: Int = 0,
    val linesUsed: String = "",
    val routeType: String = "",
    val savedAt: Long = 0L
)

/**
 * Phone numbers, and the one piece of glue that makes phone + password work.
 *
 * Firebase Auth has no phone-and-password provider — its phone sign-in is
 * OTP-only, which would mean an SMS every login and no password at all. So the
 * account is created against the email/password provider with a deterministic
 * address built from the number: +91 98920 27657 becomes
 * 919892027657@anchor.app. Nothing is ever sent to it; it is an identifier.
 *
 * Because the mapping is deterministic, logging in needs no lookup — the phone
 * typed into the form resolves straight to the account. It also means Firebase
 * Auth's own uniqueness check on email gives one-account-per-number for free.
 */
object Phone {

    private const val DOMAIN = "anchor.app"
    private const val DEFAULT_CC = "91"

    /**
     * Returns the number in +E.164 form, or null when it is not a number we can
     * use. A bare 10-digit number is assumed Indian; anything longer is taken to
     * already carry its country code.
     */
    fun normalize(raw: String): String? {
        if (raw.any { it.isLetter() }) return null
        val d = raw.filter { it.isDigit() }
        val digits = when {
            d.length == 10 -> DEFAULT_CC + d
            d.length == 11 && d.startsWith("0") -> DEFAULT_CC + d.drop(1)
            d.length in 11..15 -> d
            else -> return null
        }
        return "+$digits"
    }

    /** The Firebase Auth identifier for a number already in +E.164 form. */
    fun toAuthEmail(e164: String): String = e164.removePrefix("+") + "@" + DOMAIN

    /** The number back out of an auth identifier, for restoring a session. */
    fun fromAuthEmail(email: String): String {
        val local = email.substringBefore('@')
        return if (local.isNotBlank() && local.all { it.isDigit() }) "+$local" else ""
    }

    /** +919892027657 shown as +91 98920 27657. */
    fun pretty(e164: String): String {
        val d = e164.removePrefix("+")
        if (d.length != 12 || !d.startsWith(DEFAULT_CC)) return e164
        val n = d.drop(2)
        return "+91 ${n.take(5)} ${n.drop(5)}"
    }
}
