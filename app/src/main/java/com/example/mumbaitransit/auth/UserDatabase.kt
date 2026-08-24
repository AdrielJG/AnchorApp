package com.example.mumbaitransit.auth

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest

// ─── Entity ───────────────────────────────────────────────────────────────────
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email")    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ─── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun emailExists(email: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun usernameExists(username: String): Int
}

// ─── Migration 1 → 2: add saved_routes table ─────────────────────────────────
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS saved_routes (
                id             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id        INTEGER NOT NULL,
                origin_label   TEXT    NOT NULL,
                dest_label     TEXT    NOT NULL,
                scenario       TEXT    NOT NULL,
                scenario_label TEXT    NOT NULL,
                mode_str       TEXT    NOT NULL,
                total_min      REAL    NOT NULL,
                total_fare     INTEGER NOT NULL,
                transfers      INTEGER NOT NULL,
                lines_used     TEXT    NOT NULL,
                route_type     TEXT    NOT NULL,
                saved_at       INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [UserEntity::class, SavedRouteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun savedRouteDao(): SavedRouteDao

    companion object {
        @Volatile private var INSTANCE: UserDatabase? = null

        fun get(context: Context): UserDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "anchor_users.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}

// ─── Hash helper ──────────────────────────────────────────────────────────────
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
