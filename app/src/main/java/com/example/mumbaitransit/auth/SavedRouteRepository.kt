package com.example.mumbaitransit.auth

import android.content.Context
import com.example.mumbaitransit.backend.Backend
import com.example.mumbaitransit.model.RouteCard

/**
 * Saved routes, as many per account as the user stars.
 *
 * Kept under users/{uid}/routes in Firebase so a route saved on one phone is
 * there on the next, and in the local saved_routes table when Firebase is not
 * configured. Callers pass a [RouteCard] and get back [SavedRoute] either way.
 */
class SavedRouteRepository(context: Context) {

    private val dao = UserDatabase.get(context).savedRouteDao()
    private val cloud: CloudProfile? =
        if (Backend.isCloud(context)) CloudProfile() else null

    val session = SessionManager(context)

    private fun remote(): CloudProfile? = cloud?.takeIf { it.signedIn() }

    /**
     * Every route card on a results screen asks isSaved() as it binds, so a
     * straight read per card would be a dozen round trips to draw one list.
     * The answers all come from the same small branch, so it is fetched once and
     * held for a few seconds, and dropped the moment a route is added or
     * removed.
     */
    private suspend fun cloudRoutes(c: CloudProfile): List<SavedRoute> {
        val uid = session.getUid()
        cached(uid)?.let { return it }
        val fresh = c.routes()
        put(uid, fresh)
        return fresh
    }

    // ─── Save a route ─────────────────────────────────────────────────────────
    suspend fun saveRoute(card: RouteCard): Boolean {
        if (!session.isLoggedIn()) return false

        remote()?.let { c ->
            c.addRoute(card.toSavedRoute())
            invalidate()
            return true
        }

        val userId = session.getUserId()
        if (userId < 0) return false
        dao.insert(
            SavedRouteEntity(
                userId        = userId,
                originLabel   = card.originLabel,
                destLabel     = card.destLabel,
                scenario      = card.scenario,
                scenarioLabel = card.scenarioLabel,
                modeStr       = card.modeStr,
                totalMin      = card.totalMin,
                totalFare     = card.totalFare,
                transfers     = card.transfers,
                linesUsed     = card.linesUsed.joinToString(","),
                routeType     = card.type
            )
        )
        return true
    }

    // ─── Remove a route ───────────────────────────────────────────────────────
    suspend fun removeRoute(card: RouteCard): Boolean {
        if (!session.isLoggedIn()) return false

        remote()?.let { c ->
            val hit = cloudRoutes(c).firstOrNull { it.matches(card) } ?: return false
            c.deleteRoute(hit.id)
            invalidate()
            return true
        }

        val userId = session.getUserId()
        if (userId < 0) return false
        val existing = dao.findExact(userId, card.originLabel, card.destLabel, card.scenario)
            ?: return false
        dao.delete(existing)
        return true
    }

    /** Delete a route already in hand, from the Saved Routes screen. */
    suspend fun delete(route: SavedRoute): Boolean {
        remote()?.let { c -> c.deleteRoute(route.id); invalidate(); return true }
        val id = route.id.toLongOrNull() ?: return false
        dao.deleteById(id)
        return true
    }

    // ─── Check if already saved ───────────────────────────────────────────────
    suspend fun isSaved(card: RouteCard): Boolean {
        if (!session.isLoggedIn()) return false
        remote()?.let { c -> return cloudRoutes(c).any { it.matches(card) } }
        val userId = session.getUserId()
        if (userId < 0) return false
        return dao.findExact(userId, card.originLabel, card.destLabel, card.scenario) != null
    }

    // ─── Every saved route for the current account ────────────────────────────
    suspend fun getSavedRoutes(): List<SavedRoute> {
        if (!session.isLoggedIn()) return emptyList()
        remote()?.let { c ->
            val fresh = c.routes()
            put(session.getUid(), fresh)
            return fresh
        }
        val userId = session.getUserId()
        if (userId < 0) return emptyList()
        return dao.getForUser(userId).map { it.toModel() }
    }

    private companion object {
        private const val TTL_MS = 15_000L
        private val lock = Any()
        private var cacheUid = ""
        private var cacheAt = 0L
        private var cache: List<SavedRoute>? = null

        fun cached(uid: String): List<SavedRoute>? = synchronized(lock) {
            val c = cache
            if (c != null && uid == cacheUid && System.currentTimeMillis() - cacheAt < TTL_MS) c
            else null
        }

        fun put(uid: String, routes: List<SavedRoute>) = synchronized(lock) {
            cacheUid = uid
            cache = routes
            cacheAt = System.currentTimeMillis()
        }

        fun invalidate() = synchronized(lock) { cache = null }
    }
}

// ─── Mapping ──────────────────────────────────────────────────────────────────

private fun RouteCard.toSavedRoute() = SavedRoute(
    originLabel   = originLabel,
    destLabel     = destLabel,
    scenario      = scenario,
    scenarioLabel = scenarioLabel,
    modeStr       = modeStr,
    totalMin      = totalMin,
    totalFare     = totalFare,
    transfers     = transfers,
    linesUsed     = linesUsed.joinToString(","),
    routeType     = type,
    savedAt       = System.currentTimeMillis()
)

/** Origin, destination and scenario are what make a saved route the same route. */
private fun SavedRoute.matches(card: RouteCard): Boolean =
    originLabel == card.originLabel &&
    destLabel   == card.destLabel &&
    scenario    == card.scenario
