package com.example.mumbaitransit.journey

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.security.SecureRandom

/**
 * What a leg is travelled on.
 *
 * Stored as a string on the wire because Realtime Database round-trips enums
 * inconsistently across SDK versions, and a journey that fails to deserialize
 * leaves the person following it staring at nothing.
 */
enum class LegKind {
    WALK, RAIL, METRO, BUS, ROAD;

    /** True where the rider boards a scheduled service they can name. */
    val isScheduled: Boolean get() = this == RAIL || this == METRO

    companion object {
        fun from(name: String?): LegKind =
            values().firstOrNull { it.name == name } ?: WALK

        /** Maps the graph's `mode` column onto a leg kind. */
        fun forMode(mode: String, line: String): LegKind = when {
            mode.equals("Metro", true) || line.contains("Metro", true) -> METRO
            mode.contains("Bus", true)                                 -> BUS
            else                                                       -> RAIL
        }
    }
}

/**
 * One stage of a shared journey.
 *
 * [stops] holds every station the rider passes on this leg, [fromStop] first
 * and [toStop] last, so the follower's timeline and the tracker's
 * nearest-station search work off exactly the same list — a mismatch there is
 * how a progress bar ends up disagreeing with the station name printed under it.
 *
 * The service fields are null until the rider locks one in. [serviceDepMins] and
 * [serviceArrMins] are minutes past midnight, matching the timetable, and are
 * what lets a metro leg keep advancing underground when GPS has nothing to say.
 */
@IgnoreExtraProperties
data class JourneyLeg(
    val index: Int = 0,
    val kind: String = LegKind.WALK.name,
    val line: String = "",
    val lineShort: String = "",
    val colorHex: String = "#6B6860",
    val fromStop: String = "",
    val toStop: String = "",
    val stops: List<String> = emptyList(),
    val plannedMin: Int = 0,
    val note: String = "",
    val serviceLabel: String? = null,
    val serviceDepMins: Int? = null,
    val serviceArrMins: Int? = null,
    /** Where the locked service terminates — "Train towards THANE". */
    val serviceTerminus: String? = null,
    /**
     * The locked service's IR number, for the RailRadar live lookup only.
     * Never displayed — the app names trains by where they run to.
     */
    val serviceTrainNo: String? = null
) {
    /**
     * Derived, so it must not be written to the database.
     *
     * Realtime Database serializes every getter it can see and then refuses to
     * deserialize anything it doesn't recognise — writing these back would make
     * a shared journey unreadable to the person following it.
     */
    @get:Exclude
    val legKind: LegKind get() = LegKind.from(kind)

    /** Minutes the locked service actually takes, falling back to the plan. */
    @get:Exclude
    val scheduledMin: Int
        get() {
            val dep = serviceDepMins
            val arr = serviceArrMins
            if (dep == null || arr == null) return plannedMin
            val span = ((arr - dep) % 1440 + 1440) % 1440
            return if (span in 1..300) span else plannedMin
        }

    /** "Board the 09:14 towards Kalyan" / "Choose the metro you boarded". */
    @Exclude
    fun serviceLine(): String? = serviceLabel?.takeIf { it.isNotBlank() }

    @Exclude
    fun title(): String = when (legKind) {
        LegKind.WALK -> if (note.isNotBlank()) note else "Walk to $toStop"
        LegKind.ROAD -> "$fromStop → $toStop"
        else         -> "$fromStop → $toStop"
    }
}

/**
 * A journey somebody is sharing right now.
 *
 * [code] is the whole handle: it is the database key, the thing typed into
 * "Follow a journey", and the tail of the shared link. Nothing else identifies a
 * journey, which is deliberate — a shared journey should be followable by
 * whoever holds the link and by nobody else who has to guess.
 */
@IgnoreExtraProperties
data class LiveJourney(
    val code: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val originLabel: String = "",
    val destLabel: String = "",
    val startedAt: Long = 0L,
    val plannedTotalMin: Int = 0,
    val legs: List<JourneyLeg> = emptyList()
) {
    @Exclude
    fun legAt(index: Int): JourneyLeg? = legs.getOrNull(index)

    /** Every scheduled leg, for the "lock in what you boarded" prompts. */
    @Exclude
    fun scheduledLegs(): List<JourneyLeg> = legs.filter { it.legKind.isScheduled }
}

/** How the position in a ping was arrived at. */
object PingSource {
    const val GPS = "gps"

    /** Interpolated from the locked service's own timings — metro underground. */
    const val SCHEDULE = "schedule"

    /** No fix and no locked service; the leg is being timed out on the plan. */
    const val PLANNED = "planned"
}

/** Where a shared journey stands. */
object JourneyStatus {
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val STOPPED = "stopped"
}

/**
 * The rider's latest known position and progress.
 *
 * Written far more often than [LiveJourney], which is why it lives at its own
 * key: a follower's screen re-reads one small object every few seconds rather
 * than the whole itinerary.
 */
@IgnoreExtraProperties
data class JourneyPing(
    val at: Long = 0L,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val fix: Boolean = false,
    val legIndex: Int = 0,
    val nearestStation: String = "",
    val nextStop: String = "",
    val progressPct: Int = 0,
    val etaEpochMs: Long = 0L,
    val source: String = PingSource.PLANNED,
    val status: String = JourneyStatus.ACTIVE
) {
    @get:Exclude
    val ended: Boolean get() = status != JourneyStatus.ACTIVE

    /**
     * Whether this is a real sighting.
     *
     * The follower's screen leans on this hard: calling a schedule projection
     * "live" is the one thing that would make the feature worth less than
     * nothing to the person waiting at the other end.
     */
    @get:Exclude
    val isSighting: Boolean get() = fix && source == PingSource.GPS
}

/**
 * Short, unambiguous share codes.
 *
 * No O/0, I/1 or similar, because these get read aloud and typed by hand.
 */
object ShareCode {

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 6
    private val random = SecureRandom()

    fun generate(): String = buildString {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Uppercases and strips anything that isn't in the alphabet. */
    fun normalize(raw: String): String =
        raw.uppercase().filter { it in ALPHABET }.take(LENGTH)

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }

    /** The link put in the share sheet. Also matched by the manifest's deep link. */
    fun link(code: String): String = "https://anchor.app/j/$code"
}
