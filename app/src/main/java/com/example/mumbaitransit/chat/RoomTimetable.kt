package com.example.mumbaitransit.chat

import com.example.mumbaitransit.engine.TransitEngine
import java.util.Calendar

/**
 * One service a report can be pinned to.
 *
 * [label] is what gets stored on the message, so it has to stand on its own in
 * someone else's chat window hours later — hence the departure time and both
 * termini. The train number is deliberately absent from every string here: it
 * is carried in [trainNo] for lookups only, and metro services have none at all.
 *
 * [serviceId] is the picker key rather than the number, because it is the only
 * identifier every mode actually has.
 */
data class TrainOption(
    val serviceId: String,
    val trainNo: String,
    val depMins: Int,
    val depTime: String,
    val origin: String,
    val destination: String,
    val trainType: String,
    val ac: Boolean
) {
    val label: String
        get() = buildString {
            append("$depTime  $origin → $destination")
            if (trainType.isNotBlank()) append(" · $trainType")
            if (ac && !trainType.contains("AC", true)) append(" · AC")
        }

    /** What the picker shows underneath the time. */
    val subtitle: String
        get() = buildString {
            append("$origin → $destination")
            if (trainType.isNotBlank()) append("  ·  $trainType")
            if (ac && !trainType.contains("AC", true)) append("  ·  AC")
        }
}

/**
 * Services and stations for one chat room.
 *
 * Everything here is filtered by the room's own line and direction, so a
 * report posted in Central Up can only ever name a Central Up service, and a
 * report in Metro L3 Down can only ever name an Aqua line run heading for
 * Cuffe Parade.
 */
object RoomTimetable {

    /** How far ahead of now the picker opens. Earlier services stay reachable via search. */
    private const val WINDOW_MIN = 180

    fun trainsFor(engine: TransitEngine, room: ChatRoom): List<TrainOption> =
        engine.servicesFor(room.lineName, room.direction.key).mapNotNull { row ->
            val dep = row["dep_mins"]?.toIntOrNull() ?: return@mapNotNull null
            TrainOption(
                serviceId   = row["service_id"].orEmpty().ifBlank { "$dep-${row["origin"].orEmpty()}" },
                trainNo     = row["train_no"].orEmpty(),
                depMins     = dep,
                depTime     = row["departure_time"].orEmpty().ifBlank { engine.minsToHhMm(dep) },
                origin      = row["origin"].orEmpty(),
                destination = row["destination"].orEmpty(),
                trainType   = row["train_type"].orEmpty(),
                ac          = row["ac"]?.lowercase() in listOf("true", "1", "yes")
            )
        }

    /**
     * The same list rotated so the next departure sits at the top.
     *
     * A commuter reporting a delay is almost always talking about the service in
     * front of them, so the one leaving in ten minutes should not be 400 rows
     * down a list that starts at 04:35.
     */
    fun trainsFromNow(engine: TransitEngine, room: ChatRoom): List<TrainOption> {
        val all = trainsFor(engine, room)
        if (all.isEmpty()) return all
        val now = Calendar.getInstance()
        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // Anything within the last half hour is still "the one I am on".
        val from = nowMins - 30
        val (upcoming, earlier) = all.partition { it.depMins >= from }
        val soon = upcoming.filter { it.depMins <= nowMins + WINDOW_MIN }
        val rest = upcoming.filter { it.depMins > nowMins + WINDOW_MIN }
        return soon + rest + earlier
    }

    fun stationsFor(engine: TransitEngine, room: ChatRoom): List<String> =
        engine.stationsOn(room.lineName, room.direction.key)

    /**
     * Platform numbers offered for a platform-change report.
     *
     * Metro stations are two-face island builds, so offering twelve of them
     * would only invite a wrong answer.
     */
    fun platforms(room: ChatRoom): List<String> =
        if (room.mode == RoomMode.METRO) (1..2).map { it.toString() }
        else (1..12).map { it.toString() }
}
