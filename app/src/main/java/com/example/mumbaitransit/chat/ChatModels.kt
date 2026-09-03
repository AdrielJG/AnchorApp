package com.example.mumbaitransit.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Which way the train is heading.
 *
 * Mumbai suburban convention: Up runs towards the city terminus, Down runs away
 * from it. Commuters on opposite platforms have almost nothing useful to tell
 * each other, which is why every line gets two rooms rather than one.
 *
 * [key] must match the `direction` column in phase2_unified_enriched.csv — it is
 * what filters the train picker down to this room's services.
 */
enum class Direction(val label: String, val key: String) {
    UP("Up", "Up"),
    DOWN("Down", "Down")
}

/** What a report can carry alongside its label. */
enum class Attachment { TRAIN, STATION, PLATFORM }

/**
 * One chat room: a line in one direction.
 *
 * [lineName] is spelled exactly as the timetable and graph spell it, because it
 * is used as a lookup key, not as display text — [shortName] is what the user
 * sees. [id] is stable and is what the backend keys on, so it must not be
 * derived from display text that might get reworded.
 */
data class ChatRoom(
    val id: String,
    val lineName: String,
    val shortName: String,
    val direction: Direction,
    val towards: String
)

/**
 * The nine one-tap reports.
 *
 * These exist because the moment worth reporting — a train that just stopped
 * dead outside Kurla — is the moment a commuter least wants to type. Each one
 * declares what it must be pinned to: "delay" with no train attached is noise,
 * and a safety alert only means something if you know which station.
 */
enum class QuickReport(
    val label: String,
    val emoji: String,
    val tint: String,
    val requires: Set<Attachment>,
    val optional: Set<Attachment> = emptySet()
) {
    DELAY("Report Delay", "⏱", "#B45309",
        requires = setOf(Attachment.TRAIN), optional = setOf(Attachment.STATION)),

    CANCELLED("Train Cancelled", "🚫", "#9B1C1C",
        requires = setOf(Attachment.TRAIN)),

    EARLY("Train Running Early", "⏩", "#0A7C42",
        requires = setOf(Attachment.TRAIN), optional = setOf(Attachment.STATION)),

    SLOW("Train Running Slow", "🐢", "#B45309",
        requires = setOf(Attachment.TRAIN), optional = setOf(Attachment.STATION)),

    PLATFORM_CHANGE("Platform Change", "🔀", "#1A56DB",
        requires = setOf(Attachment.STATION, Attachment.TRAIN, Attachment.PLATFORM)),

    CROWDING("Crowding Report", "👥", "#6D28D9",
        requires = emptySet(), optional = setOf(Attachment.TRAIN, Attachment.STATION)),

    STATION_ISSUE("Station Issue", "🏗", "#6B6860",
        requires = setOf(Attachment.STATION)),

    DISRUPTION("Service Disruption", "⚠️", "#9B1C1C",
        requires = emptySet(), optional = setOf(Attachment.STATION, Attachment.TRAIN)),

    SAFETY("Safety Alert", "🛡", "#9B1C1C",
        requires = setOf(Attachment.STATION), optional = setOf(Attachment.TRAIN));

    val uses: Set<Attachment> get() = requires + optional

    companion object {
        fun from(name: String?): QuickReport? =
            name?.let { n -> values().firstOrNull { it.name == n } }
    }
}

/**
 * A posted message.
 *
 * [reportType] holds a [QuickReport] name for a one-tap report, or null for
 * something the user typed. The attachment fields are flat rather than a nested
 * object so the same shape serializes straight into Realtime Database and into
 * a Room column each.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["roomId", "sentAt"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val reportType: String? = null,
    /** Indian Railways number of the attached service, e.g. "95001". */
    val trainNo: String? = null,
    /** Human-readable form of the attached train, e.g. "09:14 CSMT → Kalyan · Fast". */
    val trainLabel: String? = null,
    val station: String? = null,
    val platform: String? = null,
    val sentAt: Long = System.currentTimeMillis()
) {
    /** Realtime Database needs a no-arg constructor to deserialize into this. */
    constructor() : this(0)

    val report: QuickReport? get() = QuickReport.from(reportType)

    /** One line summarising what the report is pinned to, or null if nothing. */
    fun attachmentLine(): String? {
        val bits = mutableListOf<String>()
        trainLabel?.takeIf { it.isNotBlank() }?.let { bits += "🚆 $it" }
        station?.takeIf { it.isNotBlank() }?.let {
            bits += if (platform.isNullOrBlank()) "📍 $it" else "📍 $it · Platform $platform"
        }
        if (station.isNullOrBlank() && !platform.isNullOrBlank()) bits += "🔀 Platform $platform"
        return bits.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

/**
 * The eight rooms.
 *
 * Held as data rather than eight screens so adding the Metro lines later is one
 * entry each, and so the room list, the chat screen and the train picker can
 * never disagree about what a room is.
 */
object ChatRooms {

    val all: List<ChatRoom> = listOf(
        ChatRoom("central-up", "Central Railway Main", "CR Main",
            Direction.UP, "Towards CSMT"),
        ChatRoom("central-down", "Central Railway Main", "CR Main",
            Direction.DOWN, "Towards Kalyan · Kasara · Karjat"),

        ChatRoom("western-up", "Western Railway", "Western Rly",
            Direction.UP, "Towards Churchgate"),
        ChatRoom("western-down", "Western Railway", "Western Rly",
            Direction.DOWN, "Towards Borivali · Virar · Dahanu"),

        ChatRoom("harbour-up", "Harbour Line CSMT", "Harbour Line",
            Direction.UP, "Towards CSMT"),
        ChatRoom("harbour-down", "Harbour Line CSMT", "Harbour Line",
            Direction.DOWN, "Towards Panvel · Goregaon"),

        ChatRoom("transharbour-up", "Trans-Harbour Line", "Trans-Harbour",
            Direction.UP, "Towards Thane"),
        ChatRoom("transharbour-down", "Trans-Harbour Line", "Trans-Harbour",
            Direction.DOWN, "Towards Panvel · Nerul")
    )

    fun byId(id: String): ChatRoom? = all.firstOrNull { it.id == id }
}
