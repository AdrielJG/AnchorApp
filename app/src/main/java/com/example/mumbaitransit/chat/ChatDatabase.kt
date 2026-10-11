package com.example.mumbaitransit.chat

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    /**
     * Live message list for one room since [since], oldest first so the list
     * reads downward.
     *
     * The window is what makes the room reset daily: yesterday's rows are still
     * on disk until the purge runs, but they are never part of today's room.
     */
    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId AND sentAt >= :since ORDER BY sentAt ASC")
    fun observeRoomSince(roomId: String, since: Long): Flow<List<ChatMessage>>

    @Insert
    suspend fun insert(message: ChatMessage): Long

    /**
     * The newest message in each room today, for the room list previews.
     * MAX(id) rather than MAX(sentAt) so two messages posted in the same
     * millisecond still resolve to exactly one row per room.
     */
    @Query(
        "SELECT * FROM chat_messages WHERE id IN " +
        "(SELECT MAX(id) FROM chat_messages WHERE sentAt >= :since GROUP BY roomId)"
    )
    suspend fun latestPerRoomSince(since: Long): List<ChatMessage>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE roomId = :roomId AND sentAt > :since")
    suspend fun countSince(roomId: String, since: Long): Int

    @Query("DELETE FROM chat_messages WHERE sentAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(entities = [ChatMessage::class], version = 2, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var instance: ChatDatabase? = null

        fun get(context: Context): ChatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "anchor_chat.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
