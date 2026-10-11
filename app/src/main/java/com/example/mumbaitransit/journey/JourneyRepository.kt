package com.example.mumbaitransit.journey

import android.content.Context
import android.util.Log
import com.example.mumbaitransit.backend.Backend
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.tasks.await

/**
 * Where a shared journey lives while it is running.
 *
 * The itinerary and the position are stored under separate keys because they
 * change at wildly different rates: the legs are written once (and again only
 * when the rider locks in a service), while the ping is rewritten every few
 * seconds. Splitting them keeps a follower's screen from re-downloading the
 * whole route on every heartbeat, which on a metered connection is the
 * difference between this being usable and not.
 */
interface JourneyRepository {

    suspend fun start(journey: LiveJourney)

    /** Rewrites the leg list — used when the rider changes what they boarded. */
    suspend fun updateLegs(code: String, legs: List<JourneyLeg>)

    suspend fun publish(code: String, ping: JourneyPing)

    suspend fun end(code: String, status: String)

    /** One-shot fetch of the itinerary. Null when the code is unknown. */
    suspend fun fetch(code: String): LiveJourney?

    /** Live position feed for a follower. */
    fun observePing(code: String): Flow<JourneyPing>

    companion object {
        @Volatile private var instance: JourneyRepository? = null

        fun get(context: Context): JourneyRepository =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): JourneyRepository =
            if (Backend.isCloud(context)) FirebaseJourneyRepository()
            else LocalJourneyRepository
    }
}

/**
 * Live sharing over Firebase Realtime Database.
 *
 * Layout: journeys/{code} holds the itinerary, journeys/{code}/ping holds the
 * position. See FIREBASE_SETUP.md for the rules — a journey is readable by
 * anyone holding the code, which is the whole point of sharing one, and
 * writable only by the account that started it.
 */
class FirebaseJourneyRepository : JourneyRepository {

    companion object {
        private const val TAG = "AnchorJourney"
    }

    private val root = FirebaseDatabase.getInstance().reference

    private fun node(code: String) = root.child("journeys").child(code)

    override suspend fun start(journey: LiveJourney) {
        node(journey.code).child("meta").setValue(
            mapOf(
                "code" to journey.code,
                "ownerUid" to journey.ownerUid,
                "ownerName" to journey.ownerName,
                "originLabel" to journey.originLabel,
                "destLabel" to journey.destLabel,
                "startedAt" to journey.startedAt,
                "plannedTotalMin" to journey.plannedTotalMin
            )
        ).await()
        node(journey.code).child("legs").setValue(journey.legs).await()
    }

    override suspend fun updateLegs(code: String, legs: List<JourneyLeg>) {
        node(code).child("legs").setValue(legs).await()
    }

    override suspend fun publish(code: String, ping: JourneyPing) {
        node(code).child("ping").setValue(ping).await()
    }

    override suspend fun end(code: String, status: String) {
        node(code).child("ping").child("status").setValue(status).await()
    }

    override suspend fun fetch(code: String): LiveJourney? = try {
        val snap = node(code).get().await()
        readJourney(snap)
    } catch (e: Exception) {
        Log.w(TAG, "Could not fetch journey $code: ${e.message}")
        null
    }

    override fun observePing(code: String): Flow<JourneyPing> = callbackFlow {
        val ref = node(code).child("ping")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(JourneyPing::class.java)?.let { trySend(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Ping read refused for $code: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun readJourney(snap: DataSnapshot): LiveJourney? {
        val meta = snap.child("meta")
        val code = meta.child("code").getValue(String::class.java) ?: return null
        val legs = snap.child("legs")
            .getValue(object : GenericTypeIndicator<List<JourneyLeg>>() {})
            .orEmpty()
        return LiveJourney(
            code = code,
            ownerUid = meta.child("ownerUid").getValue(String::class.java).orEmpty(),
            ownerName = meta.child("ownerName").getValue(String::class.java).orEmpty(),
            originLabel = meta.child("originLabel").getValue(String::class.java).orEmpty(),
            destLabel = meta.child("destLabel").getValue(String::class.java).orEmpty(),
            startedAt = meta.child("startedAt").getValue(Long::class.java) ?: 0L,
            plannedTotalMin = meta.child("plannedTotalMin").getValue(Int::class.java) ?: 0,
            legs = legs
        )
    }
}

/**
 * On-device fallback, used when Firebase has not been configured.
 *
 * A journey shared this way is real on the sender's phone — the tracker runs,
 * the notification updates, the timeline advances — but nothing leaves the
 * device, so nobody can follow it. The share sheet says so rather than handing
 * out a code that will never resolve.
 */
object LocalJourneyRepository : JourneyRepository {

    private val journeys = mutableMapOf<String, LiveJourney>()
    private val pings = mutableMapOf<String, MutableStateFlow<JourneyPing?>>()

    private fun flowFor(code: String) =
        pings.getOrPut(code) { MutableStateFlow(null) }

    override suspend fun start(journey: LiveJourney) {
        journeys[journey.code] = journey
        flowFor(journey.code)
    }

    override suspend fun updateLegs(code: String, legs: List<JourneyLeg>) {
        journeys[code]?.let { journeys[code] = it.copy(legs = legs) }
    }

    override suspend fun publish(code: String, ping: JourneyPing) {
        flowFor(code).value = ping
    }

    override suspend fun end(code: String, status: String) {
        flowFor(code).value = flowFor(code).value?.copy(status = status)
    }

    override suspend fun fetch(code: String): LiveJourney? = journeys[code]

    override fun observePing(code: String): Flow<JourneyPing> =
        flowFor(code).filterNotNull()
}
