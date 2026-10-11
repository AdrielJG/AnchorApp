package com.example.mumbaitransit.model

data class GraphNode(
    val nodeId: String,
    val stopName: String,
    val line: String,
    val mode: String,
    val canonical: String,
    val isInterchange: Boolean
)

data class GraphEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val edgeType: String,   // in_vehicle | transfer | egress | access
    val line: String,
    val mode: String,
    val fromStop: String,
    val toStop: String,
    val medianTravelMin: Double,
    val estFareInr: Int,
    val reliabilityProxy: Double,
    val freqPerHour: Double,
    val direction: String,
    val isFast: Boolean = false,
    val skippedStops: List<String> = emptyList()
)

data class MriRow(
    val line: String,
    val mriPct: Double,
    val cv: Double,
    val avgFreqPerHour: Double,
    val acRatio: Double,
    val mode: String
)

// ── Bus dataset (BEST / TMT / KDMT / VVMT) ──────────────────────────────────────────

data class BusStop(
    val stopId: Long,
    val name: String,
    val lat: Double,
    val lon: Double
)

data class BusRouteInfo(
    val routeId: Long,
    val ref: String,        // route number as posted on the bus, e.g. "56", "500"
    val name: String,
    val operator: String,
    val from: String,
    val to: String,
    val nStops: Int,
    val km: Double,
    /**
     * How far the reconstructed stop order can be trusted: high, medium or low.
     *
     * The source data has no sequence column, so order was rebuilt from the
     * route's declared terminals and the stop geometry. A route whose terminals
     * both resolved to real stops is "high"; one that had to be inferred is
     * "low" and should not be offered as confidently.
     */
    val seqConf: String = "low"
)

/** One bus ride within a journey — board here, stay on, get off there. */
data class BusLeg(
    val routeId: Long,
    val routeRef: String,
    val routeName: String,
    val operator: String,   // BEST | TMT | KDMT | VVMT
    val boardStop: String,
    val alightStop: String,
    val stopCount: Int,
    val km: Double,
    val travelMin: Int,
    val fareInr: Int,
    /** Carried through from [BusRouteInfo] so the card can flag a weak ordering. */
    val seqConf: String = "low"
)

/**
 * A complete bus journey: walk, one or two rides, walk.
 *
 * Times are estimates. The dataset carries no timetable, so [waitMin] is a flat
 * allowance rather than a real headway.
 */
data class BusOption(
    val legs: List<BusLeg>,
    val walkToKm: Double,
    val walkFromKm: Double,
    val walkToMin: Int,
    val walkFromMin: Int,
    val waitMin: Int
) {
    val rideMin: Int get() = legs.sumOf { it.travelMin }
    val totalMin: Double get() = (walkToMin + waitMin + rideMin + walkFromMin).toDouble()
    val totalFare: Int get() = legs.sumOf { it.fareInr }
    val transfers: Int get() = (legs.size - 1).coerceAtLeast(0)
}

/**
 * A bus ridden to or from a rail/metro station as part of a longer journey.
 *
 * Kept apart from the ride itself so a card can say where the bus sits in the
 * trip — feeding the station or leaving it — without a renderer having to work
 * that out from stop names. [station] is the rail or metro station this feeder
 * connects to.
 */
data class BusFeeder(
    val leg: BusLeg,
    val walkToStopMin: Int,
    val walkFromStopMin: Int,
    val waitMin: Int,
    val station: String
) {
    /** Door to platform, or platform to door — everything the feeder costs. */
    val totalMin: Int get() = walkToStopMin + waitMin + leg.travelMin + walkFromStopMin
}

data class NearestStation(
    val canonical: String,
    val distanceKm: Double,
    val lines: List<String>,
    val twins: List<String>,
    val modeType: String,   // rail | metro | bus
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class PathEdge(
    val edgeType: String,
    val line: String,
    val mode: String,
    val fromStop: String,
    val toStop: String,
    val travelMin: Double,
    var fareInr: Int,
    val reliability: Double,
    val freq: Double,
    val direction: String,
    val isFast: Boolean,
    val skippedStops: List<String>,
    var fareReal: Boolean = false
)

data class RouteResult(
    val path: List<PathEdge>,
    val totalMin: Double,
    val totalFare: Int,
    val transfers: Int,
    val linesUsed: List<String>,
    val modesUsed: List<String>,
    val isMultimodal: Boolean
)

data class RouteCard(
    val type: String,            // transit | bus | auto | cab
    val scenario: String,
    val scenarioLabel: String,
    val modeStr: String,
    val originLabel: String = "",      // User's origin input (e.g., "My Location")
    val destLabel: String = "",         // User's destination input (e.g., "Cat Cafe Studio")
    val originStation: String = "",     // Nearest station to origin
    val destStation: String = "",       // Nearest station to destination
    val walkToMin: Int = 0,
    val walkFromMin: Int = 0,
    val transitMin: Double = 0.0,
    val totalMin: Double,
    val totalFare: Int,
    val transfers: Int = 0,
    val linesUsed: List<String> = emptyList(),
    val modesUsed: List<String> = emptyList(),
    val isMultimodal: Boolean = false,
    val path: List<PathEdge> = emptyList(),
    val mriScores: Map<String, MriScore> = emptyMap(),
    val roadKm: Double = 0.0,
    val note: String = "",
    /**
     * Real bus rides behind a `type == "bus"` card, from the bundled BEST/TMT/KDMT/VVMT
     * dataset. Empty when the dataset does not cover the corridor, in which case
     * the card falls back to a distance-based estimate.
     */
    val busLegs: List<BusLeg> = emptyList(),
    /**
     * A bus ridden to the boarding station, on a train/metro + bus route.
     *
     * [walkToMin] holds this feeder's whole door-to-platform time, so anything
     * that only knows about access minutes — the timetable cursor, the totals,
     * the share card — stays right without knowing a bus is involved. The
     * breakdown is here for the renderers that draw it.
     */
    val accessBus: BusFeeder? = null,
    /** The same at the far end: a bus from the alighting station. */
    val egressBus: BusFeeder? = null,
    /**
     * The journey, leg by leg: walk, ride, walk, ride … walk.
     *
     * Set on every train, bus and mixed card, and the one thing the Routes
     * screen, the map, the share image and a live journey read to draw it.
     * Empty only on auto and cab — and on the distance-only bus estimate shown
     * where the bus dataset has no coverage — which have no legs to list.
     */
    val legs: List<RouteLeg> = emptyList()
) {
    /** The rides alone — every leg that is not a walk. */
    val transitLegs: List<RouteLeg> get() = legs.filter { it.mode.isTransit }

    /** Uses a train or metro anywhere. */
    val usesRail: Boolean
        get() = if (legs.isNotEmpty()) legs.any { it.mode.isRail } else type == "transit"

    /** Uses a bus anywhere. */
    val usesBus: Boolean
        get() = if (legs.isNotEmpty()) legs.any { it.mode == LegMode.BUS } else type == "bus"

    /**
     * What the journey is, from its rides — see [JourneyKind.of]. Auto and cab
     * have no legs and stay ROAD.
     */
    val kind: JourneyKind
        get() = if (legs.isNotEmpty()) JourneyKind.of(legs) else when (type) {
            "transit" -> JourneyKind.TRAIN
            "bus"     -> JourneyKind.BUS
            else      -> JourneyKind.ROAD
        }
}

// ── Journeys: a route is a list of legs ─────────────────────────────────────────

/**
 * What one leg of a journey is travelled on.
 *
 * TRAIN and METRO both come from the rail graph — the suburban lines and the
 * metro lines already in graph_nodes.csv — and BUS from the BEST / TMT / KDMT /
 * VVMT dataset. Nothing that reads legs assumes a fixed set of journey shapes, so
 * a new mode is a new value here plus a generator for it in the planner.
 */
enum class LegMode {
    WALK, TRAIN, METRO, BUS;

    val isTransit: Boolean get() = this != WALK

    /** Train or metro — the legs the timetable can answer for. */
    val isRail: Boolean get() = this == TRAIN || this == METRO

    companion object {
        /** Maps the rail graph's `mode` column onto a leg mode. */
        fun forRail(mode: String, line: String): LegMode =
            if (mode.equals("Metro", true) || line.contains("Metro", true)) METRO else TRAIN
    }
}

/**
 * One leg of a journey.
 *
 * A transit leg is one ride: board once, stay on, get off. Changing line — even
 * inside one station — ends a leg, and the walk to the next boarding point is a
 * WALK leg of its own. A change of bus at the same stop needs no walk, so two bus
 * legs can sit side by side.
 */
data class RouteLeg(
    val mode: LegMode,
    /** Where the leg starts: a station, a bus stop, or the commuter's own origin. */
    val from: String,
    val to: String,
    /** Time on the leg itself, without [waitMin]. */
    val minutes: Int,
    val fareInr: Int = 0,
    /** Rail: the graph's full line name. Bus: operator and route, e.g. "BEST 502 AS". */
    val line: String = "",
    /**
     * Every stop the leg calls at, [from] first and [to] last — stations on a
     * rail leg, bus stops on a bus leg, just the two ends on a walk.
     */
    val stops: List<String> = listOf(from, to),
    /** Rail legs: the graph hops, so fares, fast-train flags and MRI survive. */
    val edges: List<PathEdge> = emptyList(),
    /** Bus legs: the ride as the bus engine costed it. */
    val bus: BusLeg? = null,
    /** Bus legs: the flat allowance for waiting at the stop. Counted in the total. */
    val waitMin: Int = 0,
    /** Walk legs: straight-line distance walked. */
    val distanceKm: Double = 0.0
)

/** What a card is, for the Routes filter and the card's accent colour. */
enum class JourneyKind {
    TRAIN, BUS, MIXED, ROAD;

    companion object {
        /**
         * The one rule for classifying a journey: look at its rides, never at
         * which search built it. Walks do not count.
         *
         *   [WALK, TRAIN, WALK]            -> TRAIN   (metro counts as train)
         *   [WALK, BUS, WALK]              -> BUS
         *   [WALK, TRAIN, BUS, WALK]       -> MIXED
         *   [WALK, BUS, TRAIN, BUS, WALK]  -> MIXED
         */
        fun of(legs: List<RouteLeg>): JourneyKind {
            val rail = legs.any { it.mode.isRail }
            val bus = legs.any { it.mode == LegMode.BUS }
            return when {
                rail && bus -> MIXED
                rail        -> TRAIN
                bus         -> BUS
                else        -> ROAD
            }
        }
    }
}

/**
 * What the Routes screen lists under each filter — already validated,
 * de-duplicated, ranked and cut down by
 * [com.example.mumbaitransit.engine.RouteSelector].
 *
 * Each filter is its own selection, so a card can carry a different label in
 * two lists: the fastest bus-only route is "⚡ Fastest" under Bus even when a
 * train beats it under All.
 */
data class RouteSelection(
    /** The most convenient routes of every kind, then auto and cab. */
    val all: List<RouteCard>,
    /** Train or metro rides only — never a bus leg. */
    val train: List<RouteCard>,
    /** Bus rides only — never a train or metro leg. */
    val bus: List<RouteCard>,
    /** At least one train or metro ride and at least one bus ride. */
    val mixed: List<RouteCard>,
    /** Journeys the planner generated before selection. */
    val generated: Int = 0
) {
    val isEmpty: Boolean get() = all.isEmpty()
}

data class MriScore(
    val mriPct: Double,
    val cv: Double,
    val freq: Double,
    val acRatio: Double
)

data class TimetableLeg(
    val line: String,
    val fromStop: String,
    val toStop: String,
    val arriveAtStopMins: Int,
    val arriveAtStop: String,
    val trains: List<TrainTiming>
)

data class TrainTiming(
    val depFrom: String,
    val arrTo: String,
    val journeyMin: Int,
    val trainType: String,
    val ac: Boolean,
    val waitMin: Int,
    val terminus: String = "",
    /**
     * Indian Railways train number, e.g. "91045". The bundled timetable has no
     * such column, so this is null today; LiveStatusActivity asks the user once
     * and remembers it. Populate here if the CSV ever gains train numbers and
     * live tracking will open with no prompt.
     */
    val trainNo: String? = null
)
