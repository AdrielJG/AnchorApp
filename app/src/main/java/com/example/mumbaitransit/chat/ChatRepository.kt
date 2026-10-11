package com.example.mumbaitransit.chat

import android.content.Context
import com.example.mumbaitransit.backend.Backend
import kotlinx.coroutines.flow.Flow

/**
 * Where chat messages live.
 *
 * The screens talk to this interface only. With Firebase configured they reach
 * every device; without it they fall back to the on-device store, so the app
 * still builds and runs before the backend exists. See FIREBASE_SETUP.md.
 *
 * Every read is scoped to the current service day (see [ChatDay]). Nothing in
 * here ever returns yesterday's messages, so a room is empty again each morning
 * without anything having to be deleted first.
 */
interface ChatRepository {

    fun observeMessages(roomId: String): Flow<List<ChatMessage>>

    suspend fun send(
        roomId: String,
        uid: String,
        username: String,
        text: String,
        report: QuickReport? = null,
        trainNo: String? = null,
        trainLabel: String? = null,
        station: String? = null,
        platform: String? = null
    )

    /** Newest message per room today, keyed by room id. Empty rooms are absent. */
    suspend fun latestPerRoom(): Map<String, ChatMessage>

    /** Messages posted in the last [windowMs], for the "active now" hint. */
    suspend fun recentCount(roomId: String, windowMs: Long): Int

    /**
     * Best-effort removal of days that have already rolled over.
     *
     * Called when the room list opens. Failures are ignored on purpose: this is
     * housekeeping, and a device that cannot delete should still be able to chat.
     */
    suspend fun pruneOldDays() {}

    companion object {
        @Volatile private var instance: ChatRepository? = null

        fun get(context: Context): ChatRepository =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): ChatRepository =
            if (Backend.isCloud(context)) FirebaseChatRepository()
            else LocalChatRepository(ChatDatabase.get(context).chatDao())
    }
}

/**
 * On-device fallback.
 *
 * Messages are real and persist across restarts, but they never leave the
 * phone. Used only when Firebase has not been configured yet.
 */
class LocalChatRepository(private val dao: ChatDao) : ChatRepository {

    override fun observeMessages(roomId: String): Flow<List<ChatMessage>> =
        dao.observeRoomSince(roomId, ChatDay.startOfDay())

    override suspend fun send(
        roomId: String,
        uid: String,
        username: String,
        text: String,
        report: QuickReport?,
        trainNo: String?,
        trainLabel: String?,
        station: String?,
        platform: String?
    ) {
        val body = text.trim()
        if (body.isEmpty() && report == null) return
        dao.insert(
            ChatMessage(
                roomId = roomId,
                uid = uid,
                username = username,
                text = body,
                reportType = report?.name,
                trainNo = trainNo,
                trainLabel = trainLabel,
                station = station,
                platform = platform
            )
        )
    }

    override suspend fun latestPerRoom(): Map<String, ChatMessage> =
        dao.latestPerRoomSince(ChatDay.startOfDay()).associateBy { it.roomId }

    override suspend fun recentCount(roomId: String, windowMs: Long): Int =
        dao.countSince(roomId, System.currentTimeMillis() - windowMs)

    override suspend fun pruneOldDays() {
        // The day window already hides these; dropping them keeps the file from
        // growing for the whole life of the install.
        runCatching { dao.deleteOlderThan(ChatDay.startOfDay()) }
    }
}
