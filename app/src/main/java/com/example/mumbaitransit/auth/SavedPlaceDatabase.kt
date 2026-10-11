package com.example.mumbaitransit.auth

import androidx.room.*

// ─── Entity ───────────────────────────────────────────────────────────────────
/** On-device mirror of a pinned place, for the no-Firebase fallback path. */
@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id")  val userId: Long,
    @ColumnInfo(name = "kind")     val kind: String,     // home | work | custom
    @ColumnInfo(name = "label")    val label: String,
    @ColumnInfo(name = "address")  val address: String,
    @ColumnInfo(name = "lat")      val lat: Double,
    @ColumnInfo(name = "lon")      val lon: Double,
    @ColumnInfo(name = "saved_at") val savedAt: Long = System.currentTimeMillis()
)

// ─── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlaceEntity): Long

    @Query("SELECT * FROM saved_places WHERE user_id = :userId AND kind = :kind LIMIT 1")
    suspend fun findOfKind(userId: Long, kind: String): SavedPlaceEntity?

    @Query("SELECT * FROM saved_places WHERE user_id = :userId AND kind = :kind ORDER BY saved_at ASC")
    suspend fun allOfKind(userId: Long, kind: String): List<SavedPlaceEntity>

    @Query("DELETE FROM saved_places WHERE user_id = :userId AND kind = :kind")
    suspend fun deleteKind(userId: Long, kind: String)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// ─── Mapping ──────────────────────────────────────────────────────────────────
fun SavedPlaceEntity.toModel() = SavedPlace(
    id = id.toString(), kind = kind, label = label,
    address = address, lat = lat, lon = lon, savedAt = savedAt
)
