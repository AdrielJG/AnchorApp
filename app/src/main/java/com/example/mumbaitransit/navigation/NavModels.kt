package com.example.mumbaitransit.navigation

import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.engine.LastMileOption

/**
 * Live navigation — the rider's own step-by-step guidance through the route
 * they picked. Not to be confused with live *sharing* (journey/), which sends
 * the rider's position to somebody else. The two run side by side off the same
 * location subscription in [com.example.mumbaitransit.journey.LiveJourneyService]
 * and neither depends on the other.
 *
 * Everything in this file is plain data with no Android types, so the state
 * machine in [NavigationEngine] can be exercised on a JVM without a device.
 */

/** What a navigation leg is travelled on. */
enum class NavMode {
    WALK, TRAIN, METRO, BUS;

    val isTransit: Boolean get() = this != WALK
    val isRail: Boolean get() = this == TRAIN || this == METRO

    /** "train", "metro", "bus" — for copy such as "I'm on the bus". */
    val noun: String get() = name.lowercase()
}

/** A stop, station or end point. Coordinates are null when the dataset can't place it. */
data class NavStop(val name: String, val lat: Double? = null, val lon: Double? = null) {
    val placed: Boolean get() = lat != null && lon != null
}

/**
 * One leg of the route being navigated.
 *
 * Built from the same leg list live sharing uses ([com.example.mumbaitransit.journey.JourneyBuilder]),
 * so leg N here is leg N there — which is what lets a train locked in on one
 * screen show up on the other.
 *
 * The service fields exist only for train and metro legs. A bus leg never has
 * them: there is no bus timetable or vehicle feed, and nothing here pretends
 * otherwise.
 */
data class NavLeg(
    val index: Int,
    val mode: NavMode,
    val line: String = "",
    val lineShort: String = "",
    val colorHex: String = "#6B6860",
    /** [from] first, [to] last. A walk has exactly two. */
    val stops: List<NavStop>,
    val plannedMin: Int,
    /** Walks only: straight-line distance, for "~500 m" before the first fix. */
    val distanceKm: Double = 0.0,
    val serviceLabel: String? = null,
    val serviceDepMins: Int? = null,
    val serviceArrMins: Int? = null,
    val serviceTerminus: String? = null,
    /** Used for the RailRadar lookup only. Never displayed. */
    val serviceTrainNo: String? = null,
    /** Walks only: Walk / Auto / Cab, in display order. One entry = walk only (≤ 200 m). */
    val lastMile: List<LastMileOption> = emptyList(),
    /** Walks only: how the rider is covering this leg. */
    val travelBy: LastMileMode = LastMileMode.WALK
) {
    /** A walk leg the rider is covering by auto or cab. */
    val byRoad: Boolean get() = mode == NavMode.WALK && travelBy.byRoad

    /** The rider has a walk/auto/cab choice on this leg. */
    val hasLastMileChoice: Boolean get() = mode == NavMode.WALK && lastMile.size > 1

    val chosenOption: LastMileOption? get() = lastMile.firstOrNull { it.mode == travelBy }

    val from: String get() = stops.firstOrNull()?.name.orEmpty()
    val to: String get() = stops.lastOrNull()?.name.orEmpty()

    /** A train/metro whose departure and arrival the rider has (or was given). */
    val hasLockedService: Boolean
        get() = mode.isRail && serviceDepMins != null && serviceArrMins != null

    /** "Trans-Harbour", "TMT 63". */
    val displayName: String get() = lineShort.ifBlank { line }.ifBlank { mode.noun }

    /** Whether GPS can follow this leg at all. */
    val trackable: Boolean get() = stops.count { it.placed } >= 2
}

data class NavPlan(
    /** Same code as the LiveJourney it was built from — links navigation to sharing. */
    val code: String,
    /** Stable route key (RouteCard.scenario) so the Routes screen can offer "Resume". */
    val routeKey: String,
    val originLabel: String,
    val destLabel: String,
    val legs: List<NavLeg>
)

/** Where the engine is within the current leg. */
enum class NavPhase { NOT_STARTED, WALKING, WAITING, RIDING, REACHED_STOP, ARRIVED, STOPPED }

/**
 * The user-facing navigation state. Derived from the current leg and phase —
 * never from a fixed journey shape — so any sequence the planner produces
 * (Walk → Bus → Walk → Train → Walk, Train → Bus, …) walks through it cleanly.
 */
enum class NavState {
    NOT_STARTED,
    WALKING_TO_STOP,
    WALKING_TO_TRANSFER,
    WALKING_TO_DESTINATION,
    WAITING_FOR_TRAIN,
    ON_TRAIN,
    WAITING_FOR_METRO,
    ON_METRO,
    WAITING_FOR_BUS,
    ON_BUS,
    /** Got off a ride; the next action (walk / board) is on screen. */
    REACHED_STOP,
    ARRIVED,
    STOPPED
}

/** Things the rider can tap. Automatic detection covers the same ground; these are the fallback. */
enum class NavAction { BOARDED, MANUAL_ARRIVE, MANUAL_ALIGHT, CONTINUE, FINISH }

/** How the current position was worked out. Shown honestly on screen. */
enum class PositionSource { GPS, SCHEDULE, NONE }

enum class StepKind { WALK, BOARD, ALIGHT }
enum class StepStatus { DONE, CURRENT, UPCOMING }

/** One line of the compact "Complete journey" timeline. */
data class TimelineStep(
    val legIndex: Int,
    val kind: StepKind,
    val mode: NavMode,
    val title: String,
    val detail: String,
    val colorHex: String,
    val status: StepStatus
)

enum class StopStatus { DONE, NEXT, UPCOMING }

data class StopRow(
    val name: String,
    val status: StopStatus,
    val isBoarding: Boolean,
    val isAlighting: Boolean
)

/** One Directions step, e.g. "Turn left onto Palm Beach Rd", ending at (endLat, endLon). */
data class WalkStep(
    val instruction: String,
    val maneuver: String,
    val distanceM: Int,
    val endLat: Double,
    val endLon: Double
)

data class WalkProgress(
    val targetName: String,
    val remainingM: Int?,
    val remainingMin: Int,
    val progressPct: Int,
    /** Next manoeuvre — from Directions when available, else a compass heading. */
    val instruction: String?,
    val instructionDistanceM: Int?,
    val maneuver: String?,
    /** True when [remainingM] is a straight-line estimate, not a walked path. */
    val straightLine: Boolean
)

data class RideProgress(
    val stops: List<StopRow>,
    /** Furthest stop reached, index into leg.stops. -1 before boarding. */
    val reachedIndex: Int,
    val nextStop: String?,
    val stopsRemaining: Int,
    val approaching: Boolean,
    val onBoard: Boolean,
    /** True when position came from the timetable rather than GPS. */
    val estimated: Boolean
)

data class NavSnapshot(
    val state: NavState,
    val phase: NavPhase,
    val legIndex: Int,
    val leg: NavLeg?,
    val nextLeg: NavLeg?,
    val plan: NavPlan,
    val toolbarTitle: String,
    val eyebrow: String,
    val headline: String,
    val detail: String,
    val primary: NavAction?,
    val primaryLabel: String?,
    val fallback: NavAction?,
    val fallbackLabel: String?,
    val walk: WalkProgress?,
    val ride: RideProgress?,
    /** Set right after reaching a station/stop, for the "You've reached X" banner. */
    val reached: String?,
    val offRoute: String?,
    val timeline: List<TimelineStep>,
    val journeyPct: Int,
    val etaEpochMs: Long,
    val source: PositionSource,
    val lastLat: Double?,
    val lastLon: Double?,
    val startedAt: Long,
    val arrivedAt: Long?
) {
    val ended: Boolean get() = phase == NavPhase.ARRIVED || phase == NavPhase.STOPPED
}
