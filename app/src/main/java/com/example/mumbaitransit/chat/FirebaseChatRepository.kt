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
 * Layout: rooms/{roomId}/messages/{pushId}. Push keys are time-ordered, so
 * reading them back in key order gives the conversation in the order it
 * happened without needing a sort, and limitToLast keeps a busy room from
 * pulling months of history onto a phone.
 */
class FirebaseChatRepository : ChatRepository {

    companion object {
        private const val TAG = "AnchorChat"

        /** Messages kept in view. Older ones stay in the database, just off-screen. */
        private const val HISTORY = 300
    }

    private val root = FirebaseDatabase.getInstance().reference

    private fun room(roomId: String) = root.child("rooms").child(roomId).child("messages")

    /**
     * Emits the room's messages and re-emits on every change.
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
     * Eight reads in parallel, each allowed to fail on its own.
     *
     * Sequential awaits meant one slow or refused room held up all eight; a
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
}
