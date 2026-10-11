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
 * Suburban rail or metro.
 *
 * The two behave differently enough that the same wording does not fit both:
 * metro has no fast/slow split, no train numbers on the indicator board, and a
 * commuter says "the next metro", not "the 09:14 to Kalyan". [vehicle] and
 * [vehicles] carry that difference into every label so the screens themselves
 * stay mode-agnostic.
 */
enum class RoomMode(val vehicle: String, val vehicles: String) {
    RAIL("train", "trains"),
    METRO("metro", "metros")
}

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
    val towards: String,
    val mode: RoomMode = RoomMode.RAIL
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

    /**
     * The label as it should read inside [mode]'s rooms.
     *
     * "Train Cancelled" on the Aqua line reads as though somebody wandered into
     * the wrong room, so the three train-specific labels swap the noun. The
     * enum name is what gets stored, so this only ever affects display.
     */
    fun labelIn(mode: RoomMode): String =
        if (mode == RoomMode.METRO) label.replace("Train", "Metro") else label

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

    /**
     * One line summarising what the report is pinned to, or null if nothing.
     *
     * Only [trainLabel] is rendered — [trainNo] is carried for lookups and is
     * never shown, because the service is identified to a reader by its time
     * and where it is heading, not by a number off the indicator board.
     */
    fun attachmentLine(mode: RoomMode = RoomMode.RAIL): String? {
        val bits = mutableListOf<String>()
        val icon = if (mode == RoomMode.METRO) "🚇" else "🚆"
        trainLabel?.takeIf { it.isNotBlank() }?.let { bits += "$icon $it" }
        station?.takeIf { it.isNotBlank() }?.let {
            bits += if (platform.isNullOrBlank()) "📍 $it" else "📍 $it · Platform $platform"
        }
        if (station.isNullOrBlank() && !platform.isNullOrBlank()) bits += "🔀 Platform $platform"
        return bits.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}

/**
 * The sixteen rooms — eight suburban rail, eight metro.
 *
 * Held as data rather than sixteen screens so a new corridor is one entry, and
 * so the room list, the chat screen and the service picker can never disagree
 * about what a room is.
 *
 * The `towards` text on each metro room is taken from the direction that line
 * actually runs in phase2_unified_enriched.csv — Metro 1 Up is the Ghatkopar →
 * Versova run, not the other way round — so the picker inside a room can never
 * offer services heading the opposite way to the room's own heading.
 */
object ChatRooms {

    val rail: List<ChatRoom> = listOf(
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

    val metro: List<ChatRoom> = listOf(
        ChatRoom("metro1-up", "Metro Line 1 (Blue)", "Metro L1",
            Direction.UP, "Towards Versova", RoomMode.METRO),
        ChatRoom("metro1-down", "Metro Line 1 (Blue)", "Metro L1",
            Direction.DOWN, "Towards Ghatkopar", RoomMode.METRO),

        ChatRoom("metro2a-up", "Metro Line 2A (Yellow)", "Metro L2A",
            Direction.UP, "Towards Dahisar East", RoomMode.METRO),
        ChatRoom("metro2a-down", "Metro Line 2A (Yellow)", "Metro L2A",
            Direction.DOWN, "Towards D.N. Nagar · Andheri West", RoomMode.METRO),

        ChatRoom("metro3-up", "Metro Line 3 (Aqua)", "Metro L3",
            Direction.UP, "Towards Aarey JVLR", RoomMode.METRO),
        ChatRoom("metro3-down", "Metro Line 3 (Aqua)", "Metro L3",
            Direction.DOWN, "Towards Cuffe Parade", RoomMode.METRO),

        ChatRoom("metro7-up", "Metro Line 7 (Red)", "Metro L7",
            Direction.UP, "Towards Dahisar East", RoomMode.METRO),
        ChatRoom("metro7-down", "Metro Line 7 (Red)", "Metro L7",
            Direction.DOWN, "Towards Gundavali · Andheri East", RoomMode.METRO)
    )

    val all: List<ChatRoom> = rail + metro

    fun byId(id: String): ChatRoom? = all.firstOrNull { it.id == id }
}
