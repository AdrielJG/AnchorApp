package com.example.mumbaitransit.auth

import android.content.Context
import com.example.mumbaitransit.backend.Backend

/**
 * Everything on the account that is not the sign-up form.
 *
 * Home, college/work and custom addresses are optional by design: the account is
 * created with four fields and these branches simply do not exist until the user
 * fills one in. Writes go to Firebase when there is a signed-in cloud account,
 * and to the local saved_places table otherwise.
 */
class ProfileRepository(context: Context) {

    private val db = UserDatabase.get(context)
    private val places = db.savedPlaceDao()
    private val users = db.userDao()

    private val cloud: CloudProfile? =
        if (Backend.isCloud(context)) CloudProfile() else null

    val session = SessionManager(context)

    /** Cloud writes only make sense while Firebase itself holds a session. */
    private fun remote(): CloudProfile? = cloud?.takeIf { it.signedIn() }

    private fun localUserId(): Long = session.getUserId()

    // ─── Whole profile ───────────────────────────────────────────────────────

    suspend fun load(): UserProfile? {
        if (!session.isLoggedIn()) return null

        remote()?.let { c ->
            val p = c.load() ?: return null
            // The session was restored from the Auth record, which has no name on it.
            session.updateName(p.name)
            // An account created before the profile branch existed reads back
            // blank. The session still knows who is signed in, so show that
            // rather than an empty screen.
            return p.copy(
                name     = p.name.ifBlank { session.getName() },
                username = p.username.ifBlank { session.getUsername() },
                phone    = p.phone.ifBlank { session.getPhone() }
            )
        }

        val id = localUserId()
        if (id < 0) return null
        val u = users.findById(id)
        return UserProfile(
            uid       = session.getUid(),
            name      = u?.name ?: session.getName(),
            username  = u?.username ?: session.getUsername(),
            phone     = u?.phone ?: session.getPhone(),
            createdAt = u?.createdAt ?: 0L,
            home      = places.findOfKind(id, SavedPlace.KIND_HOME)?.toModel(),
            work      = places.findOfKind(id, SavedPlace.KIND_WORK)?.toModel(),
            addresses = places.allOfKind(id, SavedPlace.KIND_CUSTOM).map { it.toModel() },
            routes    = emptyList()   // held by SavedRouteRepository
        )
    }

    // ─── Home ────────────────────────────────────────────────────────────────

    suspend fun setHome(place: SavedPlace) = setSingleton(place, SavedPlace.KIND_HOME)

    suspend fun clearHome() {
        remote()?.let { it.clearHome(); return }
        val id = localUserId(); if (id < 0) return
        places.deleteKind(id, SavedPlace.KIND_HOME)
    }

    // ─── College / work ──────────────────────────────────────────────────────

    suspend fun setWork(place: SavedPlace) = setSingleton(place, SavedPlace.KIND_WORK)

    suspend fun clearWork() {
        remote()?.let { it.clearWork(); return }
        val id = localUserId(); if (id < 0) return
        places.deleteKind(id, SavedPlace.KIND_WORK)
    }

    /** Home and work hold one entry each — a new one replaces the old. */
    private suspend fun setSingleton(place: SavedPlace, kind: String) {
        val p = place.copy(kind = kind, savedAt = System.currentTimeMillis())
        remote()?.let { c ->
            if (kind == SavedPlace.KIND_HOME) c.setHome(p) else c.setWork(p)
            return
        }
        val id = localUserId(); if (id < 0) return
        places.deleteKind(id, kind)
        places.insert(
            SavedPlaceEntity(
                userId = id, kind = kind, label = p.label, address = p.address,
                lat = p.lat, lon = p.lon, savedAt = p.savedAt
            )
        )
    }

    // ─── Custom addresses — as many as the user wants ────────────────────────

    suspend fun addresses(): List<SavedPlace> {
        remote()?.let { return it.load()?.addresses ?: emptyList() }
        val id = localUserId(); if (id < 0) return emptyList()
        return places.allOfKind(id, SavedPlace.KIND_CUSTOM).map { it.toModel() }
    }

    suspend fun addAddress(place: SavedPlace): Boolean {
        val p = place.copy(
            kind = SavedPlace.KIND_CUSTOM,
            savedAt = System.currentTimeMillis()
        )
        remote()?.let { it.addAddress(p); return true }
        val id = localUserId(); if (id < 0) return false
        places.insert(
            SavedPlaceEntity(
                userId = id, kind = SavedPlace.KIND_CUSTOM, label = p.label,
                address = p.address, lat = p.lat, lon = p.lon, savedAt = p.savedAt
            )
        )
        return true
    }

    suspend fun deleteAddress(place: SavedPlace) {
        remote()?.let { it.deleteAddress(place.id); return }
        place.id.toLongOrNull()?.let { places.deleteById(it) }
    }
}
