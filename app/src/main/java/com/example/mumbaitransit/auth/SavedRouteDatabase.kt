package com.example.mumbaitransit.auth

import android.content.Context
import androidx.room.*

// ─── Entity ───────────────────────────────────────────────────────────────────
@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id")        val userId: Long,
    @ColumnInfo(name = "origin_label")   val originLabel: String,
    @ColumnInfo(name = "dest_label")     val destLabel: String,
    @ColumnInfo(name = "scenario")       val scenario: String,
    @ColumnInfo(name = "scenario_label") val scenarioLabel: String,
    @ColumnInfo(name = "mode_str")       val modeStr: String,
    @ColumnInfo(name = "total_min")      val totalMin: Double,
    @ColumnInfo(name = "total_fare")     val totalFare: Int,
    @ColumnInfo(name = "transfers")      val transfers: Int,
    @ColumnInfo(name = "lines_used")     val linesUsed: String,   // comma-joined
    @ColumnInfo(name = "route_type")     val routeType: String,   // transit | bus | auto | cab
    @ColumnInfo(name = "saved_at")       val savedAt: Long = System.currentTimeMillis()
)

// ─── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface SavedRouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: SavedRouteEntity): Long

    @Delete
    suspend fun delete(route: SavedRouteEntity)

    @Query("SELECT * FROM saved_routes WHERE user_id = :userId ORDER BY saved_at DESC")
    suspend fun getForUser(userId: Long): List<SavedRouteEntity>

    @Query("""
        SELECT * FROM saved_routes
        WHERE user_id = :userId
          AND origin_label = :origin
          AND dest_label   = :dest
          AND scenario     = :scenario
        LIMIT 1
    """)
    suspend fun findExact(userId: Long, origin: String, dest: String, scenario: String): SavedRouteEntity?

    @Query("DELETE FROM saved_routes WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: Long)
}
