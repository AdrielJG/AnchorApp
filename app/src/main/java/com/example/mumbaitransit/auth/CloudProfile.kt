package com.example.mumbaitransit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * The profile branch under users/{uid}, read and written a field at a time.
 *
 * Snapshots are unpacked by hand rather than through getValue(Class). Kotlin
 * data classes with val properties have no setters for Firebase's reflection to
 * call, so a mapped read would come back with every field at its default and
 * fail silently. Reading child by child is longer and cannot do that.
 */
class CloudProfile {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun signedIn(): Boolean = auth.currentUser != null

    private fun me(): DatabaseReference? =
        auth.currentUser?.uid?.let { db.child("users").child(it) }

    // ─── Whole profile ───────────────────────────────────────────────────────

    suspend fun load(): UserProfile? {
        val ref = me() ?: return null
        val s = ref.get().await()
        return UserProfile(
            uid       = auth.currentUser?.uid.orEmpty(),
            name      = s.child("name").getValue(String::class.java).orEmpty(),
            username  = s.child("username").getValue(String::class.java).orEmpty(),
            phone     = s.child("phone").getValue(String::class.java).orEmpty(),
            createdAt = s.child("createdAt").getValue(Long::class.java) ?: 0L,
            home      = place(s.child("home"), SavedPlace.KIND_HOME),
            work      = place(s.child("work"), SavedPlace.KIND_WORK),
            addresses = s.child("addresses").children
                .mapNotNull { place(it, SavedPlace.KIND_CUSTOM) }
                .sortedBy { it.savedAt },
            routes    = s.child("routes").children
                .mapNotNull { route(it) }
                .sortedByDescending { it.savedAt }
        )
    }

    suspend fun setName(name: String) {
        me()?.child("name")?.setValue(name)?.await()
    }

    // ─── Home and work ───────────────────────────────────────────────────────

    /** One home per account — setting it replaces whatever was there. */
    suspend fun setHome(p: SavedPlace) {
        me()?.child("home")?.setValue(toMap(p.copy(id = "home", kind = SavedPlace.KIND_HOME)))?.await()
    }

    suspend fun setWork(p: SavedPlace) {
        me()?.child("work")?.setValue(toMap(p.copy(id = "work", kind = SavedPlace.KIND_WORK)))?.await()
    }

    suspend fun clearHome() { me()?.child("home")?.removeValue()?.await() }
    suspend fun clearWork() { me()?.child("work")?.removeValue()?.await() }

    // ─── Custom addresses (many) ─────────────────────────────────────────────

    suspend fun addAddress(p: SavedPlace): String {
        val ref = me()?.child("addresses")?.push() ?: return ""
        val id = ref.key.orEmpty()
        ref.setValue(toMap(p.copy(id = id, kind = SavedPlace.KIND_CUSTOM))).await()
        return id
    }

    suspend fun deleteAddress(id: String) {
        if (id.isBlank()) return
        me()?.child("addresses")?.child(id)?.removeValue()?.await()
    }

    // ─── Saved routes (many) ─────────────────────────────────────────────────

    suspend fun routes(): List<SavedRoute> {
        val ref = me() ?: return emptyList()
        return ref.child("routes").get().await().children
            .mapNotNull { route(it) }
            .sortedByDescending { it.savedAt }
    }

    suspend fun addRoute(r: SavedRoute): String {
        val ref = me()?.child("routes")?.push() ?: return ""
        val id = ref.key.orEmpty()
        ref.setValue(toMap(r.copy(id = id))).await()
        return id
    }

    suspend fun deleteRoute(id: String) {
        if (id.isBlank()) return
        me()?.child("routes")?.child(id)?.removeValue()?.await()
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private fun toMap(p: SavedPlace) = mapOf(
        "id"      to p.id,
        "kind"    to p.kind,
        "label"   to p.label,
        "address" to p.address,
        "lat"     to p.lat,
        "lon"     to p.lon,
        "savedAt" to (if (p.savedAt > 0) p.savedAt else System.currentTimeMillis())
    )

    private fun toMap(r: SavedRoute) = mapOf(
        "id"            to r.id,
        "originLabel"   to r.originLabel,
        "destLabel"     to r.destLabel,
        "scenario"      to r.scenario,
        "scenarioLabel" to r.scenarioLabel,
        "modeStr"       to r.modeStr,
        "totalMin"      to r.totalMin,
        "totalFare"     to r.totalFare,
        "transfers"     to r.transfers,
        "linesUsed"     to r.linesUsed,
        "routeType"     to r.routeType,
        "savedAt"       to (if (r.savedAt > 0) r.savedAt else System.currentTimeMillis())
    )

    private fun place(s: DataSnapshot, kind: String): SavedPlace? {
        if (!s.exists()) return null
        return SavedPlace(
            id      = s.child("id").getValue(String::class.java) ?: s.key.orEmpty(),
            kind    = s.child("kind").getValue(String::class.java) ?: kind,
            label   = s.child("label").getValue(String::class.java).orEmpty(),
            address = s.child("address").getValue(String::class.java).orEmpty(),
            lat     = s.child("lat").getValue(Double::class.java) ?: 0.0,
            lon     = s.child("lon").getValue(Double::class.java) ?: 0.0,
            savedAt = s.child("savedAt").getValue(Long::class.java) ?: 0L
        )
    }

    private fun route(s: DataSnapshot): SavedRoute? {
        if (!s.exists()) return null
        return SavedRoute(
            id            = s.child("id").getValue(String::class.java) ?: s.key.orEmpty(),
            originLabel   = s.child("originLabel").getValue(String::class.java).orEmpty(),
            destLabel     = s.child("destLabel").getValue(String::class.java).orEmpty(),
            scenario      = s.child("scenario").getValue(String::class.java).orEmpty(),
            scenarioLabel = s.child("scenarioLabel").getValue(String::class.java).orEmpty(),
            modeStr       = s.child("modeStr").getValue(String::class.java).orEmpty(),
            totalMin      = s.child("totalMin").getValue(Double::class.java) ?: 0.0,
            totalFare     = s.child("totalFare").getValue(Int::class.java) ?: 0,
            transfers     = s.child("transfers").getValue(Int::class.java) ?: 0,
            linesUsed     = s.child("linesUsed").getValue(String::class.java).orEmpty(),
            routeType     = s.child("routeType").getValue(String::class.java).orEmpty(),
            savedAt       = s.child("savedAt").getValue(Long::class.java) ?: 0L
        )
    }
}
