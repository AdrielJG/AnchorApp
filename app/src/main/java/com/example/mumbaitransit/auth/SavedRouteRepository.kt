package com.example.mumbaitransit.auth

import android.content.Context
import com.example.mumbaitransit.model.RouteCard

class SavedRouteRepository(context: Context) {

    private val dao = UserDatabase.get(context).savedRouteDao()
    val session = SessionManager(context)

    /** Expose DAO for direct operations (e.g. delete by entity in SavedRoutesActivity) */
    fun savedRouteDao() = dao

    // ─── Save a route ─────────────────────────────────────────────────────────
    suspend fun saveRoute(card: RouteCard): Boolean {
        val userId = session.getUserId()
        if (userId < 0) return false
        val entity = SavedRouteEntity(
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
        dao.insert(entity)
        return true
    }

    // ─── Remove a route ───────────────────────────────────────────────────────
    suspend fun removeRoute(card: RouteCard): Boolean {
        val userId = session.getUserId()
        if (userId < 0) return false
        val existing = dao.findExact(userId, card.originLabel, card.destLabel, card.scenario)
            ?: return false
        dao.delete(existing)
        return true
    }

    // ─── Check if already saved ───────────────────────────────────────────────
    suspend fun isSaved(card: RouteCard): Boolean {
        val userId = session.getUserId()
        if (userId < 0) return false
        return dao.findExact(userId, card.originLabel, card.destLabel, card.scenario) != null
    }

    // ─── Get all saved routes for current user ────────────────────────────────
    suspend fun getSavedRoutes(): List<SavedRouteEntity> {
        val userId = session.getUserId()
        if (userId < 0) return emptyList()
        return dao.getForUser(userId)
    }
}
