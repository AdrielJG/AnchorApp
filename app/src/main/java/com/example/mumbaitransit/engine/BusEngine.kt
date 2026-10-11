package com.example.mumbaitransit.engine

import com.example.mumbaitransit.model.BusLeg
import com.example.mumbaitransit.model.BusOption
import com.example.mumbaitransit.model.BusRouteInfo
import com.example.mumbaitransit.model.BusStop
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bus routing over the bundled BEST / TMT / KDMT / VVMT dataset.
 *
 * Replaces the straight-line distance estimate the bus card used to show. Every
 * option returned here names real routes and real boarding and alighting stops.
 *
 * What the dataset does and does not carry:
 *  - Each direction is its own route_id, so a route's stop order already encodes
 *    direction and no reverse-travel check is needed beyond comparing sequences.
 *  - There are no timetables or headways. Journey time is distance-based and the
 *    wait is a flat allowance, so times are estimates and are labelled as such.
 *  - Coverage is 1,112 routes across BEST, Thane, Kalyan-Dombivali and
 *    Vasai-Virar, calling at 3,896 stops. Outside that footprint [findOptions]
 *    returns nothing and the caller falls back to its own estimate.
 */
class BusEngine(
    private val stops: List<BusStop>,
    private val routes: List<BusRouteInfo>,
    routeStops: List<Triple<Long, Long, Double>>   // routeId, stopId, cumKm — in sequence order
) {

    /** Average BEST/TMT speed in Mumbai traffic, including dwell at stops. */
    private val avgSpeedKmh = 13.0

    /** Flat allowance for waiting at the stop — the dataset carries no headways. */
    private val waitMin = 8

    /** Farthest a commuter is asked to walk to reach or leave a bus stop. */
    private val maxWalkKm = RoutingConfig.MAX_WALK_TO_BUS_STOP

    /**
     * How much a minute of walking counts against a minute of riding when
     * choosing between options.
     *
     * Without this the two are interchangeable, and a route that stops at the
     * commuter's actual destination loses by a whisker to one that stops six
     * minutes short of it. Weighting walking the way transit planners do — a
     * little under twice in-vehicle time — puts the direct option back on top.
     * Only the choice is weighted; the times shown to the commuter are real.
     */
    private val walkWeight = 1.8

    private val walkSpeedKmh = 5.0

    private val stopById = stops.associateBy { it.stopId }
    private val routeById = routes.associateBy { it.routeId }

    /** routeId -> stops in travel order. */
    private val stopsOnRoute: Map<Long, List<StopOnRoute>>

    /** stopId -> the routes calling there, with the position along each. */
    private val routesAtStop: Map<Long, List<StopOnRoute>>

    data class StopOnRoute(val routeId: Long, val stopId: Long, val seq: Int, val cumKm: Double)

    init {
        val byRoute = LinkedHashMap<Long, MutableList<StopOnRoute>>()
        for ((routeId, stopId, cumKm) in routeStops) {
            val list = byRoute.getOrPut(routeId) { mutableListOf() }
            list.add(StopOnRoute(routeId, stopId, list.size, cumKm))
        }
        stopsOnRoute = byRoute

        val byStop = HashMap<Long, MutableList<StopOnRoute>>()
        for ((_, list) in byRoute) {
            for (sor in list) byStop.getOrPut(sor.stopId) { mutableListOf() }.add(sor)
        }
        routesAtStop = byStop
    }

    // ── Stop lookup ───────────────────────────────────────────────────────────

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val a = Math.sin(Math.toRadians(lat2 - lat1) / 2).let { it * it } +
                Math.cos(p1) * Math.cos(p2) *
                Math.sin(Math.toRadians(lon2 - lon1) / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    /** Bus stops within [maxKm], nearest first. */
    fun nearestStops(lat: Double, lon: Double, k: Int = 6, maxKm: Double = maxWalkKm): List<Pair<BusStop, Double>> =
        stops.asSequence()
            .map { it to haversineKm(lat, lon, it.lat, it.lon) }
            .filter { it.second <= maxKm }
            .sortedBy { it.second }
            .take(k)
            .toList()

    /** Every stop within [maxKm] of a point, nearest first — no count limit. */
    fun stopsWithin(lat: Double, lon: Double, maxKm: Double): List<Pair<BusStop, Double>> =
        stops.asSequence()
            .map { it to haversineKm(lat, lon, it.lat, it.lon) }
            .filter { it.second <= maxKm }
            .sortedBy { it.second }
            .toList()

    /** The flat allowance for waiting at a stop, in minutes. */
    val waitAllowanceMin: Int get() = waitMin

    /**
     * A ride on one route from near a start to near a named target — a station
     * the commuter walks to afterwards, or the destination.
     */
    data class TargetRide(
        val target: String,
        val leg: BusLeg,
        val walkInKm: Double,
        val walkOutKm: Double
    )

    /**
     * One ride per route and target: every route that calls at one of
     * [boardWalk]'s stops and later at one of [targets]'s, with the boarding and
     * alighting pair that costs least for that route and target — the same cost
     * a direct ride is chosen on in [findOptions].
     *
     * @param boardWalk stop id -> km walked to reach that stop
     * @param targets stop id -> each target reachable on foot from it, with the km
     */
    fun ridesToTargets(
        boardWalk: Map<Long, Double>,
        targets: Map<Long, List<Pair<String, Double>>>
    ): List<TargetRide> {
        class Best(
            val board: StopOnRoute, val alight: StopOnRoute,
            val walkIn: Double, val walkOut: Double, val cost: Double
        )
        val best = HashMap<Pair<Long, String>, Best>()
        for ((stopId, walkIn) in boardWalk) {
            for (board in routesAtStop[stopId].orEmpty()) {
                val seq = stopsOnRoute[board.routeId] ?: continue
                for (k in board.seq + 1 until seq.size) {
                    val alight = seq[k]
                    val reachable = targets[alight.stopId] ?: continue
                    val rideHours = (alight.cumKm - board.cumKm) / avgSpeedKmh
                    for ((target, walkOut) in reachable) {
                        val cost = rideHours + walkWeight * (walkIn + walkOut) / walkSpeedKmh
                        val key = board.routeId to target
                        val cur = best[key]
                        if (cur == null || cost < cur.cost) best[key] = Best(board, alight, walkIn, walkOut, cost)
                    }
                }
            }
        }
        return best.mapNotNull { (key, b) ->
            val route = routeById[key.first] ?: return@mapNotNull null
            TargetRide(key.second, buildLeg(route, b.board, b.alight), b.walkIn.roundTo(2), b.walkOut.roundTo(2))
        }
    }

    /**
     * The stops a [BusLeg] actually calls at, board to alight, in travel order.
     *
     * The leg carries stop names rather than ids, so the board and alight are
     * matched by name along that route. A route that calls twice at the same
     * name resolves to the pairing that runs forwards, which is the ride the
     * engine costed. Returns an empty list if either end cannot be placed —
     * the map then falls back to a straight board-to-alight line rather than
     * drawing a wrong shape.
     */
    fun legShape(leg: BusLeg): List<BusStop> {
        val seq = stopsOnRoute[leg.routeId].orEmpty()
        if (seq.isEmpty()) return emptyList()

        fun matches(sor: StopOnRoute, name: String) =
            stopById[sor.stopId]?.name.equals(name, ignoreCase = true)

        val boards  = seq.filter { matches(it, leg.boardStop) }
        val alights = seq.filter { matches(it, leg.alightStop) }
        if (boards.isEmpty() || alights.isEmpty()) return emptyList()

        // Prefer the forward pairing whose stop count matches what was costed.
        var board  = boards.first()
        var alight = alights.first()
        var bestErr = Int.MAX_VALUE
        for (b in boards) for (a in alights) {
            if (a.seq <= b.seq) continue
            val err = kotlin.math.abs((a.seq - b.seq) - leg.stopCount)
            if (err < bestErr) { bestErr = err; board = b; alight = a }
        }
        if (alight.seq <= board.seq) return emptyList()

        return seq.filter { it.seq in board.seq..alight.seq }
            .mapNotNull { stopById[it.stopId] }
    }

    /** Every route calling at [stopId], for the nearby-stops list on the home screen. */
    fun routeRefsAtStop(stopId: Long): List<String> =
        routesAtStop[stopId].orEmpty()
            .mapNotNull { routeById[it.routeId]?.ref?.takeIf { r -> r.isNotBlank() } }
            .distinct()
            .sortedWith(compareBy({ it.toIntOrNull() ?: Int.MAX_VALUE }, { it }))

    // ── Routing ───────────────────────────────────────────────────────────────

    /**
     * Bus journeys from the origin point to the destination point, best first.
     *
     * Direct rides are searched first and only if none exist is a single change
     * considered, so a commuter is never offered a transfer where one bus does
     * the whole trip. Returns an empty list where the dataset has no coverage.
     */
    fun findOptions(
        oLat: Double, oLon: Double, dLat: Double, dLon: Double,
        limit: Int = 3,
        allowTransfer: Boolean = true
    ): List<BusOption> {
        val origin = nearestStops(oLat, oLon)
        val dest = nearestStops(dLat, dLon)
        if (origin.isEmpty() || dest.isEmpty()) return emptyList()

        val originWalk = origin.associate { it.first.stopId to it.second }
        val destWalk = dest.associate { it.first.stopId to it.second }

        val direct = findDirect(originWalk, destWalk)
        if (direct.isNotEmpty()) return direct.take(limit)
        if (!allowTransfer) return emptyList()

        return findOneTransfer(originWalk, destWalk).take(limit)
    }

    private fun findDirect(
        originWalk: Map<Long, Double>,
        destWalk: Map<Long, Double>
    ): List<BusOption> {
        val out = mutableListOf<BusOption>()

        for ((routeId, seqStops) in stopsOnRoute) {
            val route = routeById[routeId] ?: continue

            val boardings = seqStops.filter { it.stopId in originWalk }
            val alightings = seqStops.filter { it.stopId in destWalk }
            if (boardings.isEmpty() || alightings.isEmpty()) continue

            // Correct direction only: the alighting stop must come later along
            // the route than the boarding stop.
            var best: Pair<StopOnRoute, StopOnRoute>? = null
            var bestCost = Double.MAX_VALUE
            for (b in boardings) {
                for (a in alightings) {
                    if (a.seq <= b.seq) continue
                    val walkKm = originWalk.getValue(b.stopId) + destWalk.getValue(a.stopId)
                    val cost = (a.cumKm - b.cumKm) / avgSpeedKmh +
                               walkWeight * walkKm / walkSpeedKmh
                    if (cost < bestCost) {
                        bestCost = cost
                        best = b to a
                    }
                }
            }
            val (board, alight) = best ?: continue

            val leg = buildLeg(route, board, alight)
            out.add(
                BusOption(
                    legs = listOf(leg),
                    walkToKm = originWalk.getValue(board.stopId).roundTo(2),
                    walkFromKm = destWalk.getValue(alight.stopId).roundTo(2),
                    walkToMin = walkMin(originWalk.getValue(board.stopId)),
                    walkFromMin = walkMin(destWalk.getValue(alight.stopId)),
                    waitMin = waitMin
                )
            )
        }

        return rank(dedupe(out))
    }

    private fun findOneTransfer(
        originWalk: Map<Long, Double>,
        destWalk: Map<Long, Double>
    ): List<BusOption> {
        // Work backwards from the destination first.
        //
        // Searched forwards, every stop on every route out of the origin is a
        // candidate interchange, and each one is then tested against every route
        // calling there — which on a 1,112-route dataset in central Mumbai is
        // tens of millions of comparisons for a handful of answers. Almost none
        // of those interchanges can lead anywhere: only a few routes reach the
        // destination at all. Collecting those first, and the stops they call
        // at, turns the interchange test into a set lookup.
        val destStopsByRoute = HashMap<Long, MutableList<StopOnRoute>>()
        for ((stopId, _) in destWalk) {
            for (sor in routesAtStop[stopId].orEmpty()) {
                if (routeById[sor.routeId]?.seqConf == "low") continue
                destStopsByRoute.getOrPut(sor.routeId) { mutableListOf() }.add(sor)
            }
        }
        if (destStopsByRoute.isEmpty()) return emptyList()
        for (list in destStopsByRoute.values) list.sortBy { it.seq }

        // Every stop those routes call at — the only places worth changing at.
        val interchanges = HashSet<Long>()
        for (routeId in destStopsByRoute.keys) {
            for (sor in stopsOnRoute[routeId].orEmpty()) interchanges.add(sor.stopId)
        }

        // Routes reachable from the origin, and the stops they can drop you at.
        val firstLegs = mutableListOf<Pair<StopOnRoute, StopOnRoute>>()   // board, interchange
        for ((stopId, _) in originWalk) {
            for (board in routesAtStop[stopId].orEmpty()) {
                // Two rides compound any ordering error, so a change is only
                // offered between routes whose stop order is trusted.
                if (routeById[board.routeId]?.seqConf == "low") continue
                for (mid in stopsOnRoute[board.routeId].orEmpty()) {
                    if (mid.seq > board.seq && mid.stopId in interchanges) {
                        firstLegs.add(board to mid)
                    }
                }
            }
        }
        if (firstLegs.isEmpty()) return emptyList()

        val out = mutableListOf<BusOption>()
        for ((board, mid) in firstLegs) {
            val route1 = routeById[board.routeId] ?: continue
            // Second bus must call at the interchange stop and reach the destination.
            for (board2 in routesAtStop[mid.stopId].orEmpty()) {
                if (board2.routeId == board.routeId) continue
                val destStops = destStopsByRoute[board2.routeId] ?: continue
                val alight = destStops
                    .filter { it.seq > board2.seq }
                    .minByOrNull { destWalk.getValue(it.stopId) } ?: continue
                val route2 = routeById[board2.routeId] ?: continue

                out.add(
                    BusOption(
                        legs = listOf(buildLeg(route1, board, mid), buildLeg(route2, board2, alight)),
                        walkToKm = originWalk.getValue(board.stopId).roundTo(2),
                        walkFromKm = destWalk.getValue(alight.stopId).roundTo(2),
                        walkToMin = walkMin(originWalk.getValue(board.stopId)),
                        walkFromMin = walkMin(destWalk.getValue(alight.stopId)),
                        waitMin = waitMin * 2
                    )
                )
            }
        }

        return rank(dedupe(out))
    }

    /**
     * Order options by journey time, but let a trusted stop order break a near
     * tie. A route reconstructed without its terminals may have its stops in the
     * wrong order, which would put a commuter on a bus going the wrong way — not
     * worth saving two minutes for.
     */
    private fun rank(options: List<BusOption>): List<BusOption> =
        options.sortedWith(compareBy({ it.rankScore }, { it.totalMin }))

    /** Selection score only — never shown. Real times stay on [BusOption]. */
    private val BusOption.rankScore: Double
        get() = rideMin + waitMin +
                walkWeight * (walkToMin + walkFromMin) +
                legs.sumOf {
                    when (it.seqConf) { "high" -> 0.0; "medium" -> 3.0; else -> 10.0 }
                }

    private fun buildLeg(route: BusRouteInfo, board: StopOnRoute, alight: StopOnRoute): BusLeg {
        val km = (alight.cumKm - board.cumKm).coerceAtLeast(0.1)
        return BusLeg(
            routeId = route.routeId,
            routeRef = route.ref.ifBlank { "Bus" },
            routeName = route.name,
            operator = shortOperator(route.operator),
            boardStop = stopById[board.stopId]?.name.orEmpty(),
            alightStop = stopById[alight.stopId]?.name.orEmpty(),
            stopCount = alight.seq - board.seq,
            km = km.roundTo(2),
            travelMin = max(2, (km / avgSpeedKmh * 60).roundToInt()),
            fareInr = stageFare(km),
            seqConf = route.seqConf
        )
    }

    /** Keep one option per route combination — the shortest. */
    private fun dedupe(options: List<BusOption>): List<BusOption> {
        val best = LinkedHashMap<String, BusOption>()
        for (o in options) {
            val key = o.legs.joinToString("|") { "${it.routeId}" }
            val cur = best[key]
            if (cur == null || o.totalMin < cur.totalMin) best[key] = o
        }
        return best.values.toList()
    }

    /**
     * BEST / TMT ordinary stage fare: ₹5 covers the first stage of about 2 km
     * and each further stage adds ₹5, capped where the undertakings cap it.
     */
    private fun stageFare(km: Double): Int {
        val stages = kotlin.math.ceil(km / 2.0).toInt().coerceAtLeast(1)
        return (5 * stages).coerceAtMost(35)
    }

    private fun walkMin(km: Double) = max(1, (km / walkSpeedKmh * 60).roundToInt())

    private fun shortOperator(op: String) = when {
        op.contains("Brihanmumbai", true) || op.equals("BEST", true) -> "BEST"
        op.contains("Thane", true) -> "TMT"
        op.contains("Kalyan", true) || op.contains("Dombivali", true) -> "KDMT"
        op.contains("Vasai", true) || op.contains("Virar", true) -> "VVMT"
        op.contains("Navi Mumbai", true) || op.contains("NMMT", true) -> "NMMT"
        op.contains("Mira", true) || op.contains("Bhayander", true) -> "MBMT"
        else -> op.take(12)
    }

    private fun Double.roundTo(dp: Int): Double {
        val f = Math.pow(10.0, dp.toDouble())
        return Math.round(this * f) / f
    }
}
