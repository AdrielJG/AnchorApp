package com.example.mumbaitransit.engine

import com.example.mumbaitransit.model.BusLeg
import com.example.mumbaitransit.model.BusStop
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.NearestStation
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteLeg
import com.example.mumbaitransit.model.RouteResult
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Builds every valid journey for one trip, each as a list of legs.
 *
 * A journey is a chain of rides between places — the origin, the destination
 * and the rail and metro stations — with walks wherever one ride ends and the
 * next begins. Rail rides come from the rail graph ([TransitEngine]), bus rides
 * from the bus dataset ([BusEngine]). Which chains are built is data, not code:
 * the search walks each sequence in [RoutingConfig.ROUTE_PATTERNS] step by step,
 * asking for the rides of that step's mode from wherever the journey has got to.
 *
 * Nothing is ranked or cut here. A journey is dropped only when it breaks a
 * validity rule in [RoutingConfig], revisits a station, or exactly repeats
 * another. Train-only and bus-only journeys come from the same searches the app
 * always ran, so they are what they were before. Choosing which journeys the
 * commuter sees is [RouteSelector]'s job, not this one's.
 */
class JourneyPlanner(private val rail: TransitEngine, private val bus: BusEngine) {

    /** A trip to plan — what [TransitEngine.planRoutes] is given. */
    class Request(
        val origStations: List<NearestStation>,
        val destStations: List<NearestStation>,
        val oLat: Double, val oLon: Double,
        val dLat: Double, val dLon: Double,
        val pinnedOrig: String?, val pinnedDest: String?,
        val origLabel: String, val destLabel: String
    )

    /** One generated journey. */
    class Journey(
        val legs: List<RouteLeg>,
        val pattern: List<RideMode>,
        /** Rail weightings that found a train-only journey — what its label is judged on. */
        val railScenarios: Set<String>,
        /** Door to door, unrounded, including bus waits. */
        val totalMin: Double,
        val totalFare: Int,
        /** Every rail hop in journey order, for anything still reading a path. */
        val railPath: List<PathEdge>
    )

    private data class LatLon(val lat: Double, val lon: Double)

    private sealed class Place {
        object Origin : Place()
        object Destination : Place()
        data class Station(val canonical: String) : Place()
    }

    private class Walk(val from: String, val to: String, val km: Double, val minutes: Int)

    /** One pattern step as ridden. */
    private class Step(
        val from: Place,
        val to: Place,
        /** The rides, with any change of line inside a rail ride as a walk between them. */
        val legs: List<RouteLeg>,
        /** From [from] to the first boarding point, where the step owns that walk. */
        val walkIn: Walk?,
        /** From the last alighting point to [to]. */
        val walkOut: Walk?,
        val minutes: Double,
        val fare: Int,
        val railScenarios: Set<String>,
        val railPath: List<PathEdge>,
        /** Coordinates along the ride, boarding first — what the progress rules read. */
        val track: List<LatLon>,
        /** Stations this step brings into the journey, for the no-revisit rule. */
        val newStations: List<String>
    )

    private class RailRide(val result: RouteResult, val scenarios: Set<String>)

    // ── Where buses meet trains, built once on first use ─────────────────────

    /** Station -> bus stops within [RoutingConfig.MAX_WALK_TO_STATION], with the km. */
    private val stationStops: Map<String, Map<Long, Double>> by lazy {
        rail.railStations.mapNotNull { can ->
            val (lat, lon) = rail.stationCoord(can) ?: return@mapNotNull null
            val near = bus.stopsWithin(lat, lon, RoutingConfig.MAX_WALK_TO_STATION)
            if (near.isEmpty()) null else can to near.associate { it.first.stopId to it.second }
        }.toMap()
    }

    /** Bus stop -> the stations within walking reach of it, with the km. */
    private val stopStations: Map<Long, List<Pair<String, Double>>> by lazy {
        val out = HashMap<Long, MutableList<Pair<String, Double>>>()
        for ((can, stops) in stationStops) {
            for ((stopId, km) in stops) out.getOrPut(stopId) { mutableListOf() }.add(can to km)
        }
        out
    }

    fun plan(req: Request): List<Journey> {
        val trip = Trip(req)
        val out = LinkedHashMap<String, Journey>()
        for (pattern in RoutingConfig.ROUTE_PATTERNS) {
            if (pattern.isEmpty() || pattern.size > RoutingConfig.MAX_TRANSIT_LEGS) continue
            trip.extend(pattern, 0, Place.Origin, emptyList(), emptySet()) { journey ->
                out.putIfAbsent(signature(journey.legs), journey)
            }
        }
        return out.values.toList()
    }

    private inner class Trip(private val req: Request) {

        private val destination = LatLon(req.dLat, req.dLon)
        private val originName = req.origLabel.ifBlank { "Start" }
        private val destName = req.destLabel.ifBlank { "Destination" }

        // The walking rule at each end is the one train-only routes always used.
        private val walkEntries = walkEnds(req.origStations, req.pinnedOrig)
        private val walkExits = walkEnds(req.destStations, req.pinnedDest)

        private val originStops: Map<Long, Double> =
            bus.stopsWithin(req.oLat, req.oLon, RoutingConfig.MAX_WALK_TO_BUS_STOP)
                .associate { it.first.stopId to it.second }

        private val destStops: Map<Long, List<Pair<String, Double>>> =
            bus.stopsWithin(req.dLat, req.dLon, RoutingConfig.MAX_WALK_TO_BUS_STOP)
                .associate { it.first.stopId to listOf(DEST_KEY to it.second) }

        private val trees = HashMap<Pair<String, String>, TransitEngine.RailTree?>()
        private val railCache = HashMap<Triple<String, String, Boolean>, List<RailRide>>()
        private val busCache = HashMap<Triple<Place, Boolean, Boolean>, List<Step>>()
        private val railStepCache = HashMap<List<Any?>, List<Step>>()
        private val shapes = HashMap<String, List<BusStop>>()

        /** Depth-first over one pattern: every ride for this step, then the next. */
        fun extend(
            pattern: List<RideMode>,
            index: Int,
            at: Place,
            steps: List<Step>,
            visited: Set<String>,
            emit: (Journey) -> Unit
        ) {
            if (index == pattern.size) {
                if (at == Place.Destination) emit(assemble(pattern, steps))
                return
            }
            val toDest = index == pattern.lastIndex
            val single = pattern.size == 1
            val next = pattern.getOrNull(index + 1)
            val nextIsLast = index + 1 == pattern.lastIndex
            val candidates = when (pattern[index]) {
                RideMode.RAIL -> railStepCache.getOrPut(listOf(at, toDest, single, next, nextIsLast)) {
                    railSteps(at, toDest, single) { station ->
                        // Only ride to a station the next step can actually leave from.
                        next == null || canLeave(next, station, nextIsLast)
                    }
                }
                RideMode.BUS -> busSteps(at, toDest, single)
            }
            for (step in candidates) {
                if (step.newStations.any { it in visited }) continue
                if (!single && !progresses(step)) continue
                extend(pattern, index + 1, step.to, steps + step, visited + step.newStations, emit)
            }
        }

        private fun canLeave(mode: RideMode, station: String, toDest: Boolean): Boolean = when (mode) {
            RideMode.BUS -> busSteps(Place.Station(station), toDest, single = false).isNotEmpty()
            RideMode.RAIL -> true
        }

        // ── Rail ─────────────────────────────────────────────────────────────

        private fun railSteps(
            at: Place,
            toDest: Boolean,
            single: Boolean,
            targetOk: (String) -> Boolean
        ): List<Step> {
            val starts: List<Pair<String, Walk?>> = when (at) {
                Place.Origin -> walkEntries.map {
                    it.canonical to Walk(originName, it.canonical, it.distanceKm, rail.walkMinFor(it.distanceKm))
                }
                is Place.Station -> listOf(at.canonical to null)
                Place.Destination -> emptyList()
            }
            val out = mutableListOf<Step>()
            for ((start, walkIn) in starts) {
                val ends: List<Pair<String, Walk?>> = if (toDest) {
                    walkExits.map {
                        it.canonical to Walk(it.canonical, destName, it.distanceKm, rail.walkMinFor(it.distanceKm))
                    }
                } else {
                    val startD = rail.stationCoord(start)?.let { distToDest(LatLon(it.first, it.second)) }
                    rail.railStations.filter { end ->
                        if (end == start) return@filter false
                        val c = rail.stationCoord(end) ?: return@filter false
                        (startD == null || distToDest(LatLon(c.first, c.second)) <=
                            startD - RoutingConfig.MIN_PROGRESS_KM) && targetOk(end)
                    }.map { it to null }
                }
                for ((end, walkOut) in ends) {
                    if (end == start) continue
                    for (ride in railRides(start, end, exact = single)) {
                        out += Step(
                            from = at,
                            to = if (toDest) Place.Destination else Place.Station(end),
                            legs = railLegs(ride.result.path),
                            walkIn = walkIn,
                            walkOut = walkOut,
                            minutes = ride.result.totalMin,
                            fare = ride.result.totalFare,
                            railScenarios = ride.scenarios,
                            railPath = ride.result.path,
                            track = railTrack(ride.result.path),
                            newStations = if (at == Place.Origin) listOf(start, end) else listOf(end)
                        )
                    }
                }
            }
            return out
        }

        /**
         * Every distinct rail path between two stations across the four scenario
         * weightings. [exact] uses the pairwise search train-only routes always
         * used; otherwise one search tree per start station answers every end.
         */
        private fun railRides(start: String, end: String, exact: Boolean): List<RailRide> =
            railCache.getOrPut(Triple(start, end, exact)) {
                val sNids = rail.stationNodes(start) ?: return@getOrPut emptyList()
                val eNids = rail.stationNodes(end) ?: return@getOrPut emptyList()
                val bySig = LinkedHashMap<List<Pair<String, String>>, Pair<RouteResult, MutableSet<String>>>()
                for (sc in TransitEngine.SCENARIOS) {
                    val result = (if (exact) rail.dijkstraFor(sNids, eNids, sc)
                                  else tree(start, sc)?.pathTo(eNids)) ?: continue
                    val sig = result.path.filter { it.edgeType == "in_vehicle" }.map { it.fromStop to it.toStop }
                    if (sig.isEmpty()) continue
                    bySig.getOrPut(sig) { result to mutableSetOf() }.second.add(sc)
                }
                bySig.values.map { RailRide(it.first, it.second) }
            }

        private fun tree(start: String, scenario: String): TransitEngine.RailTree? =
            trees.getOrPut(start to scenario) {
                rail.stationNodes(start)?.let { rail.railTree(it, scenario) }
            }

        private fun railTrack(path: List<PathEdge>): List<LatLon> {
            val names = mutableListOf<String>()
            for (e in path) {
                if (e.edgeType != "in_vehicle") continue
                if (names.isEmpty()) names.add(e.fromStop)
                names.add(e.toStop)
            }
            return names.mapNotNull { n -> StationCoords.of(n)?.let { LatLon(it.first, it.second) } }
        }

        // ── Bus ──────────────────────────────────────────────────────────────

        private fun busSteps(at: Place, toDest: Boolean, single: Boolean): List<Step> =
            busCache.getOrPut(Triple(at, toDest, single)) {
                // Bus-only: the same search the bus card always ran, every option kept.
                if (at == Place.Origin && toDest && single) return@getOrPut busOnlySteps()

                val boardWalk: Map<Long, Double> = when (at) {
                    Place.Origin -> originStops
                    is Place.Station -> stationStops[at.canonical].orEmpty()
                    Place.Destination -> emptyMap()
                }
                if (boardWalk.isEmpty()) return@getOrPut emptyList()
                val fromName = if (at is Place.Station) at.canonical else originName

                bus.ridesToTargets(boardWalk, if (toDest) destStops else stopStations).mapNotNull { ride ->
                    val to = if (toDest) Place.Destination else Place.Station(ride.target)
                    if (to == at) return@mapNotNull null
                    val shape = shapeOf(ride.leg)
                    Step(
                        from = at,
                        to = to,
                        legs = listOf(busLeg(ride.leg, shape, bus.waitAllowanceMin)),
                        walkIn = Walk(fromName, ride.leg.boardStop, ride.walkInKm, rail.walkMinFor(ride.walkInKm)),
                        walkOut = Walk(
                            ride.leg.alightStop, if (toDest) destName else ride.target,
                            ride.walkOutKm, rail.walkMinFor(ride.walkOutKm)
                        ),
                        minutes = (ride.leg.travelMin + bus.waitAllowanceMin).toDouble(),
                        fare = ride.leg.fareInr,
                        railScenarios = emptySet(),
                        railPath = emptyList(),
                        track = shape.map { LatLon(it.lat, it.lon) },
                        newStations = if (to is Place.Station) listOf(to.canonical) else emptyList()
                    )
                }
            }

        private fun busOnlySteps(): List<Step> =
            bus.findOptions(req.oLat, req.oLon, req.dLat, req.dLon, limit = Int.MAX_VALUE).map { opt ->
                val wait = if (opt.legs.isEmpty()) 0 else opt.waitMin / opt.legs.size
                val shapes = opt.legs.map { shapeOf(it) }
                Step(
                    from = Place.Origin,
                    to = Place.Destination,
                    legs = opt.legs.mapIndexed { i, leg -> busLeg(leg, shapes[i], wait) },
                    walkIn = Walk(originName, opt.legs.first().boardStop, opt.walkToKm, opt.walkToMin),
                    walkOut = Walk(opt.legs.last().alightStop, destName, opt.walkFromKm, opt.walkFromMin),
                    minutes = (opt.rideMin + opt.waitMin).toDouble(),
                    fare = opt.totalFare,
                    railScenarios = emptySet(),
                    railPath = emptyList(),
                    track = shapes.flatten().map { LatLon(it.lat, it.lon) },
                    newStations = emptyList()
                )
            }

        private fun shapeOf(leg: BusLeg): List<BusStop> =
            shapes.getOrPut("${leg.routeId}|${leg.boardStop}|${leg.alightStop}|${leg.stopCount}") {
                bus.legShape(leg)
            }

        // ── Rules and assembly ───────────────────────────────────────────────

        private fun distToDest(p: LatLon): Double =
            rail.haversineKm(p.lat, p.lon, destination.lat, destination.lon)

        /**
         * A ride must end no farther from the destination than it began (less
         * [RoutingConfig.MIN_PROGRESS_KM]), and may not pass nearer the destination
         * than where it ends by more than [RoutingConfig.MAX_OVERSHOOT_KM].
         */
        private fun progresses(step: Step): Boolean {
            val track = step.track
            if (track.size < 2) return true
            val startD = distToDest(track.first())
            val endD = distToDest(track.last())
            if (endD > startD - RoutingConfig.MIN_PROGRESS_KM) return false
            return track.none { distToDest(it) < endD - RoutingConfig.MAX_OVERSHOOT_KM }
        }

        private fun assemble(pattern: List<RideMode>, steps: List<Step>): Journey {
            val legs = mutableListOf<RouteLeg>()
            var minutes = 0.0
            var fare = 0
            val path = mutableListOf<PathEdge>()
            for (st in steps) {
                st.walkIn?.let { legs += walkLeg(it); minutes += it.minutes }
                legs += st.legs
                minutes += st.minutes
                fare += st.fare
                path += st.railPath
                st.walkOut?.let { legs += walkLeg(it); minutes += it.minutes }
            }
            return Journey(
                legs = legs,
                pattern = pattern,
                railScenarios = if (steps.size == 1) steps[0].railScenarios else emptySet(),
                totalMin = minutes,
                totalFare = fare,
                railPath = path
            )
        }
    }

    // ── Leg builders ─────────────────────────────────────────────────────────

    /**
     * Splits a rail path into one leg per line, with the change between two lines
     * as a walk leg. A change with no ride on one side has nothing to connect, so
     * its minutes are folded into the ride next to it; the path's own total is
     * what the journey is timed on either way.
     */
    private fun railLegs(path: List<PathEdge>): List<RouteLeg> {
        val out = mutableListOf<RouteLeg>()
        var run = mutableListOf<PathEdge>()
        var carry = 0.0
        fun flush() {
            if (run.isEmpty()) return
            val first = run.first()
            val last = run.last()
            out += RouteLeg(
                mode = LegMode.forRail(first.mode, first.line),
                from = first.fromStop,
                to = last.toStop,
                minutes = (run.sumOf { it.travelMin } + carry).roundToInt(),
                fareInr = run.sumOf { it.fareInr },
                line = first.line,
                stops = listOf(first.fromStop) + run.map { it.toStop },
                edges = run.toList()
            )
            carry = 0.0
            run = mutableListOf()
        }
        for (e in path) {
            if (e.edgeType == "in_vehicle") {
                if (run.isNotEmpty() && run.last().line != e.line) flush()
                run.add(e)
                continue
            }
            flush()
            if (out.isEmpty()) { carry += e.travelMin; continue }
            val minutes = max(1, e.travelMin.roundToInt())
            val prev = out.last()
            out += if (prev.mode == LegMode.WALK) {
                out.removeAt(out.lastIndex)
                prev.copy(to = e.toStop, minutes = prev.minutes + minutes,
                          stops = listOf(prev.from, e.toStop), edges = prev.edges + e)
            } else {
                RouteLeg(LegMode.WALK, e.fromStop, e.toStop, minutes, edges = listOf(e))
            }
        }
        flush()
        while (out.isNotEmpty() && out.last().mode == LegMode.WALK) out.removeAt(out.lastIndex)
        return out
    }

    private fun busLeg(leg: BusLeg, shape: List<BusStop>, waitMin: Int) = RouteLeg(
        mode = LegMode.BUS,
        from = leg.boardStop,
        to = leg.alightStop,
        minutes = leg.travelMin,
        fareInr = leg.fareInr,
        line = "${leg.operator} ${leg.routeRef}",
        stops = if (shape.size >= 2) shape.map { it.name } else listOf(leg.boardStop, leg.alightStop),
        bus = leg,
        waitMin = waitMin
    )

    private fun walkLeg(w: Walk) =
        RouteLeg(LegMode.WALK, w.from, w.to, w.minutes, distanceKm = w.km)

    private fun walkEnds(stations: List<NearestStation>, pinned: String?): List<NearestStation> =
        if (pinned != null) {
            stations.filter { it.canonical == pinned }.takeIf { it.isNotEmpty() } ?: stations.take(1)
        } else {
            stations.take(3)
        }

    companion object {
        private const val DEST_KEY = "\u0000destination"

        /** What makes two journeys the same journey: the same rides, boarded and left at the same places. */
        fun signature(legs: List<RouteLeg>): String =
            legs.filter { it.mode.isTransit }.joinToString("|") { leg ->
                val b = leg.bus
                if (b != null) "B:${b.routeId}:${leg.from}>${leg.to}"
                else "${leg.mode}:${leg.line}:${leg.from}>${leg.to}"
            }
    }
}
