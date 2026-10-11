package com.example.mumbaitransit.navigation

import com.example.mumbaitransit.engine.LastMile
import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.engine.LastMileOption
import com.example.mumbaitransit.journey.JourneyBuilder
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.RouteLeg
import com.example.mumbaitransit.model.TrainTiming
import kotlin.math.roundToInt

/**
 * The "Next step" panel on an expanded route card, and the words on its button.
 *
 * The button names the first thing the rider actually does — "Start Walking"
 * when the route opens with a walk, "Start Journey" when it opens by boarding —
 * worked out from the same kept legs navigation will run, so the button and
 * the first navigation screen always agree.
 */
object NavCopy {

    data class FirstStep(
        val title: String,
        val meta: String,
        val cta: String,
        val mode: LegMode,
        /** Set when the first walk will be covered by auto or cab. */
        val road: LastMileMode? = null
    )

    fun firstStep(
        card: RouteCard,
        firstTrain: TrainTiming? = null,
        /** The walk/auto/cab option picked for a walk leg, if any. */
        travel: (RouteLeg) -> LastMileOption? = { null },
        shortName: (RouteLeg) -> String = { it.bus?.let { b -> "${b.operator} ${b.routeRef}" } ?: it.line }
    ): FirstStep? {
        val legs = JourneyBuilder.keptLegs(card)
        val first = legs.firstOrNull() ?: return null
        return when (first.mode) {
            LegMode.WALK -> {
                val ride = travel(first)?.takeIf { it.mode.byRoad }
                if (ride != null) return FirstStep(
                    title = "${ride.mode.action} to ${first.to}",
                    meta = LastMile.metaLabel(ride),
                    cta = "Start Journey",
                    mode = LegMode.WALK,
                    road = ride.mode
                )
                val metres = if (first.distanceKm > 0) (first.distanceKm * 1000 * NavigationEngine.WALK_DETOUR).roundToInt()
                             else (first.minutes * NavigationEngine.WALK_M_PER_MIN).roundToInt()
                FirstStep(
                    title = "Walk to ${first.to}",
                    meta = "${first.minutes} min · ~${formatDistance(metres)}",
                    cta = "Start Walking",
                    mode = LegMode.WALK
                )
            }
            LegMode.TRAIN, LegMode.METRO -> FirstStep(
                title = "Board ${shortName(first)} at ${first.from}",
                meta = buildString {
                    val noun = if (first.mode == LegMode.METRO) "Metro" else "Train"
                    append(noun)
                    firstTrain?.terminus?.takeIf { it.isNotBlank() }?.let { append(" towards ${it.uppercase()}") }
                    firstTrain?.depFrom?.let { append(" · $it") }
                    append(" · ${first.stops.size - 1} stops")
                },
                cta = "Start Journey",
                mode = first.mode
            )
            LegMode.BUS -> FirstStep(
                title = "Board ${shortName(first)} at ${first.from}",
                meta = "${first.from} → ${first.to} · ${(first.stops.size - 1).coerceAtLeast(1)} stops",
                cta = "Start Journey",
                mode = LegMode.BUS
            )
        }
    }
}

/**
 * Does the rider's GPS agree with where RailRadar says their train is?
 *
 * Only asked when both sides are real sightings — a live position from the
 * feed and a fresh GPS fix — so a timetable replay can never accuse the rider
 * of being on the wrong train.
 */
object NavTrainCheck {

    /**
     * True when the train is at (or one station either side of) the rider's
     * last reached stop; false when it is clearly elsewhere on this leg or not
     * on it at all; null when there is nothing to compare.
     */
    fun matches(stops: List<String>, riderIndex: Int, trainStation: String?): Boolean? {
        if (trainStation.isNullOrBlank() || riderIndex < 0 || stops.isEmpty()) return null
        val key = norm(trainStation)
        val trainIndex = stops.indexOfFirst { norm(it) == key || norm(it).startsWith(key) || key.startsWith(norm(it)) }
        if (trainIndex < 0) return false.takeIf { riderIndex in 1 until stops.lastIndex }
        return kotlin.math.abs(trainIndex - riderIndex) <= 1
    }

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
}
