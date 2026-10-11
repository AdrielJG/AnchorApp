package com.example.mumbaitransit.auth

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest

// ─── Entity ───────────────────────────────────────────────────────────────────
/**
 * The on-device copy of an account, used only when Firebase is not configured.
 *
 * It carries the same four sign-up fields as the cloud record. The password is
 * hashed here because there is no auth service on this path to hold it — with
 * Firebase present, no password ever reaches this table.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name")          val name: String,
    @ColumnInfo(name = "username")      val username: String,
    @ColumnInfo(name = "phone")         val phone: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at")    val createdAt: Long = System.currentTimeMillis()
)

// ─── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE phone = :phone")
    suspend fun phoneExists(phone: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE username = :username COLLATE NOCASE")
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

// ─── Migration 2 → 3: accounts keyed on phone, and saved places ──────────────
/**
 * The users table was keyed on email; an account is a phone number now. There is
 * no phone number to recover from an email address, so the table is rebuilt
 * empty rather than migrated into a state where nobody can log in. Only local
 * fallback accounts are affected — real accounts live in Firebase, and
 * saved_routes is left untouched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS users")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name          TEXT    NOT NULL,
                username      TEXT    NOT NULL,
                phone         TEXT    NOT NULL,
                password_hash TEXT    NOT NULL,
                created_at    INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS saved_places (
                id       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                user_id  INTEGER NOT NULL,
                kind     TEXT    NOT NULL,
                label    TEXT    NOT NULL,
                address  TEXT    NOT NULL,
                lat      REAL    NOT NULL,
                lon      REAL    NOT NULL,
                saved_at INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [UserEntity::class, SavedRouteEntity::class, SavedPlaceEntity::class],
    version = 3,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile private var INSTANCE: UserDatabase? = null

        fun get(context: Context): UserDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "anchor_users.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}

// ─── Hash helper ──────────────────────────────────────────────────────────────
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
