package com.example.mumbaitransit.chat

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Live chat over Firebase Realtime Database.
 *
 * Layout: rooms/{roomId}/days/{yyyy-MM-dd}/messages/{pushId}. Push keys are
 * time-ordered, so reading them back in key order gives the conversation in the
 * order it happened without needing a sort.
 *
 * The day segment is what makes rooms reset: at the turnover hour every client
 * starts writing and reading under a new key, so the room is empty again for
 * everyone at once without a delete having to land first. [pruneOldDays] then
 * clears the previous days so the database does not grow forever.
 */
class FirebaseChatRepository : ChatRepository {

    companion object {
        private const val TAG = "AnchorChat"

        /** Messages kept in view. A single day rarely exceeds this. */
        private const val HISTORY = 300

        /** Days back that [pruneOldDays] tries to clear on each room-list open. */
        private const val PRUNE_DEPTH = 4
    }

    private val root = FirebaseDatabase.getInstance().reference

    private fun day(roomId: String, dayKey: String = ChatDay.key()) =
        root.child("rooms").child(roomId).child("days").child(dayKey)

    private fun room(roomId: String) = day(roomId).child("messages")

    /**
     * Emits the room's messages for today and re-emits on every change.
     *
     * A whole-list listener rather than per-child: the list is capped at
     * [HISTORY] and the UI diffs it anyway, so the simpler contract is worth
     * more than the saved bytes.
     */
    override fun observeMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = room(roomId).orderByKey().limitToLast(HISTORY)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(ChatMessage::class.java)?.copy(
                        // Push keys are unique strings; the UI only needs a stable
                        // identity for diffing, so hash it into the local id slot.
                        id = child.key?.hashCode()?.toLong() ?: 0L,
                        roomId = roomId
                    )
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                // Permission denied is nearly always the database rules, so say
                // so loudly in the log and close the flow — the screen catches
                // it and tells the user rather than sitting blank.
                Log.w(TAG, "Read refused on room $roomId: ${error.message}")
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

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

        // sentAt goes in as the server clock: a phone with a wrong time would
        // otherwise drop its messages into the wrong place in everyone's list.
        val payload = hashMapOf(
            "roomId" to roomId,
            "uid" to uid,
            "username" to username,
            "text" to body,
            "reportType" to report?.name,
            "trainNo" to trainNo,
            "trainLabel" to trainLabel,
            "station" to station,
            "platform" to platform,
            "sentAt" to ServerValue.TIMESTAMP
        )
        room(roomId).push().setValue(payload).await()
    }

    /**
     * One read per room in parallel, each allowed to fail on its own.
     *
     * Sequential awaits meant one slow or refused room held up all of them; a
     * thrown one killed the whole call and, with it, the room list that was
     * waiting on it. Previews are cosmetic, so a room that can't be read just
     * comes back absent.
     */
    override suspend fun latestPerRoom(): Map<String, ChatMessage> = coroutineScope {
        ChatRooms.all.map { r ->
            async {
                try {
                    val snap = room(r.id).orderByKey().limitToLast(1).get().await()
                    snap.children.firstOrNull()
                        ?.getValue(ChatMessage::class.java)
                        ?.let { r.id to it.copy(roomId = r.id) }
                } catch (e: Exception) {
                    Log.w(TAG, "No preview for ${r.id}: ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    override suspend fun recentCount(roomId: String, windowMs: Long): Int {
        val since = System.currentTimeMillis() - windowMs
        val snap = room(roomId).orderByKey().limitToLast(HISTORY).get().await()
        return snap.children.count {
            (it.child("sentAt").getValue(Long::class.java) ?: 0L) > since
        }
    }

    /**
     * Clears the last few rolled-over days.
     *
     * Deletes are idempotent, so several clients doing this at once is harmless
     * — the second one removes nothing. Anything that fails, including a rules
     * refusal, is swallowed: this must never be the reason a room won't open.
     */
    override suspend fun pruneOldDays() {
        coroutineScope {
            ChatRooms.all.flatMap { r ->
                (1..PRUNE_DEPTH).map { back ->
                    async {
                        runCatching { day(r.id, ChatDay.keyDaysAgo(back)).removeValue().await() }
                    }
                }
            }.awaitAll()
        }
    }
}
