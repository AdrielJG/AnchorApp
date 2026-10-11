package com.example.mumbaitransit.map

import com.example.mumbaitransit.engine.BusEngine
import com.example.mumbaitransit.engine.StationCoords
import com.example.mumbaitransit.model.BusFeeder
import com.example.mumbaitransit.model.BusLeg
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.ui.LineStyle
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

/**
 * Turns a [RouteCard] into the coloured segments the map draws.
 *
 * The route list already knows the exact stops a journey passes through — the
 * transit path is one [PathEdge] per station hop, and a bus leg can name every
 * stop it calls at. This builds the polyline from those stops, so the line on
 * the map follows the corridor the commuter will actually travel rather than
 * the straight hop from origin to destination the old preview drew.
 *
 * Nothing here fetches a road geometry. Between two adjacent stations the line
 * is drawn straight, which on a rail or metro corridor sits close to the track;
 * walking legs are drawn dashed for exactly that reason — they are an
 * indication of the walk, not a turn-by-turn path.
 */
object RouteShape {

    /** One drawn run: a walk, a ride on one line, or an interchange. */
    data class Segment(
        val kind: Kind,
        /** e.g. "Western Railway", "BEST 302", "Walk to Vashi". */
        val label: String,
        /** Board → alight, for the legend row. */
        val detail: String,
        val colorHex: String,
        val points: List<LatLng>,
        /** Named stops on this segment, for the station markers. */
        val stops: List<Stop>,
        val minutes: Int
    ) {
        val dashed: Boolean get() = kind != Kind.RIDE
    }

    enum class Kind { WALK, RIDE, TRANSFER }

    data class Stop(val name: String, val position: LatLng)

    data class Plan(
        val segments: List<Segment>,
        val origin: LatLng?,
        val destination: LatLng?,
        val originLabel: String,
        val destLabel: String,
        /**
         * True when the only thing drawn is a door-to-door road hop whose real
         * geometry has to come from the Directions API — auto and cab. The map
         * screen fetches it and replaces the segment, so nothing but a
         * genuine road path is ever shown as one.
         */
        val needsRoadPath: Boolean = false
    ) {
        val isEmpty: Boolean get() = segments.none { it.points.size >= 2 }

        /** Every point the camera has to fit. */
        fun allPoints(): List<LatLng> =
            segments.flatMap { it.points } + listOfNotNull(origin, destination)
    }

    private const val WALK_COLOR = "#6B6860"

    /**
     * Colours to fall back on when two rides in one journey would otherwise be
     * painted the same — Metro Line 7 and Central Railway Main share a red, and
     * two bus legs share the bus orange.
     */
    private val fallbackPalette = listOf(
        "#0F766E", "#9333EA", "#C2410C", "#0369A1", "#65A30D", "#BE123C"
    )

    fun build(
        card: RouteCard,
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double,
        busEngine: BusEngine?
    ): Plan {
        val origin = if (originLat != 0.0 || originLon != 0.0) LatLng(originLat, originLon) else null
        val dest = if (destLat != 0.0 || destLon != 0.0) LatLng(destLat, destLon) else null

        val segments = when {
            card.legs.isNotEmpty() -> legSegments(card, origin, dest, busEngine)
            card.type == "transit" -> transitSegments(card, origin, dest, busEngine)
            card.busLegs.isNotEmpty() -> busSegments(card, origin, dest, busEngine)
            else -> roadSegments(card, origin, dest)
        }

        val road = card.type == "auto" || card.type == "cab"

        return Plan(
            segments = deduplicateColors(segments),
            origin = origin,
            destination = dest,
            originLabel = card.originLabel.ifBlank { "Start" },
            destLabel = card.destLabel.ifBlank { "Destination" },
            needsRoadPath = road && segments.isNotEmpty()
        )
    }

    // ── Auto / cab ────────────────────────────────────────────────────────────

    /**
     * The placeholder a road card starts with: origin to destination, dashed,
     * marked as an estimate. [RouteMapActivity] swaps it for the Directions
     * geometry as soon as that arrives, so this shape is only ever on screen
     * while the request is in flight or after it has failed.
     */
    private fun roadSegments(card: RouteCard, origin: LatLng?, dest: LatLng?): List<Segment> {
        if (origin == null || dest == null) return emptyList()
        return listOf(
            Segment(
                kind = Kind.WALK,
                label = if (card.type == "cab") "Cab" else "Auto",
                detail = "Finding road route…",
                colorHex = roadColorHex(card.type),
                points = listOf(origin, dest),
                stops = emptyList(),
                minutes = card.totalMin.roundToInt()
            )
        )
    }

    /** The drawn road path, once Directions has returned it. */
    fun roadSegment(
        card: RouteCard,
        points: List<LatLng>,
        distanceKm: Double,
        minutes: Int,
        summary: String
    ) = Segment(
        kind = Kind.RIDE,
        label = if (card.type == "cab") "Cab" else "Auto",
        detail = buildString {
            append("%.1f km by road".format(distanceKm))
            if (summary.isNotBlank()) append("  ·  via $summary")
        },
        colorHex = roadColorHex(card.type),
        points = points,
        stops = emptyList(),
        minutes = if (minutes > 0) minutes else card.totalMin.roundToInt()
    )

    fun roadColorHex(type: String) = if (type == "cab") "#1F2937" else "#B45309"

    // ── Journeys built from legs ──────────────────────────────────────────────

    /**
     * Draws a journey leg by leg — each ride stop by stop in its own colour, each
     * walk dashed from where the previous ride ended to where the next begins.
     * The same for a train-only, bus-only or mixed journey.
     */
    private fun legSegments(
        card: RouteCard,
        origin: LatLng?,
        dest: LatLng?,
        busEngine: BusEngine?
    ): List<Segment> {
        val legs = card.legs
        // Rides first, so each walk knows the points it joins.
        val rides = arrayOfNulls<Segment>(legs.size)
        var busIndex = 0
        legs.forEachIndexed { i, leg ->
            rides[i] = when {
                leg.mode.isRail -> rideSegment(leg.edges.filter { it.edgeType == "in_vehicle" })
                leg.mode == LegMode.BUS -> leg.bus?.let { busRideSegment(it, busEngine, busIndex++) }
                else -> null
            }
        }

        val out = mutableListOf<Segment>()
        legs.forEachIndexed { i, leg ->
            rides[i]?.let { out += it; return@forEachIndexed }
            if (leg.mode != LegMode.WALK) return@forEachIndexed   // a ride whose stops can't be placed
            val from = if (i == 0) origin
                       else rides[i - 1]?.points?.lastOrNull() ?: StationCoords.of(leg.from)?.toLatLng()
            val to = if (i == legs.lastIndex) dest
                     else rides[i + 1]?.points?.firstOrNull() ?: StationCoords.of(leg.to)?.toLatLng()
            if (from == null || to == null || from == to) return@forEachIndexed
            val between = i > 0 && i < legs.lastIndex
            out += Segment(
                kind = if (between) Kind.TRANSFER else Kind.WALK,
                label = when {
                    i == legs.lastIndex    -> "Walk to ${card.destLabel.ifBlank { "destination" }}"
                    leg.edges.isNotEmpty() -> "Change at ${leg.from}"
                    else                   -> "Walk to ${leg.to}"
                },
                detail = "${leg.minutes} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(from, to),
                stops = emptyList(),
                minutes = leg.minutes
            )
        }
        return out
    }

    private fun busRideSegment(leg: BusLeg, busEngine: BusEngine?, index: Int): Segment? {
        val stops = busEngine?.legShape(leg).orEmpty().map { Stop(it.name, LatLng(it.lat, it.lon)) }
        if (stops.size < 2) return null
        return Segment(
            kind = Kind.RIDE,
            label = "${leg.operator} ${leg.routeRef}",
            detail = "${leg.boardStop} → ${leg.alightStop}  ·  ${leg.stopCount} stops",
            colorHex = LineStyle.busColorHex(index),
            points = stops.map { it.position },
            stops = stops,
            minutes = leg.travelMin
        )
    }

    // ── Transit ───────────────────────────────────────────────────────────────

    private fun transitSegments(
        card: RouteCard,
        origin: LatLng?,
        dest: LatLng?,
        busEngine: BusEngine?
    ): List<Segment> {
        val out = mutableListOf<Segment>()

        // Walk from the origin point to the first station.
        val boardAt = card.path.firstOrNull { it.edgeType == "in_vehicle" }?.fromStop
            ?: card.originStation
        val boardPos = StationCoords.of(boardAt)?.toLatLng()
        val access = card.accessBus
        if (access != null) {
            // The feeder is the whole access leg, so drawing a walk straight to
            // the platform would put a dashed line across the corridor the bus
            // actually runs down.
            out += feederSegments(
                feeder = access,
                from = origin,
                to = boardPos,
                busEngine = busEngine,
                colorIndex = 0,
                walkInLabel = "Walk to ${access.leg.boardStop}",
                walkOutLabel = "Walk to ${access.station}"
            )
        } else if (origin != null && boardPos != null && card.walkToMin > 0) {
            out += Segment(
                kind = Kind.WALK,
                label = "Walk to $boardAt",
                detail = "${card.walkToMin} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(origin, boardPos),
                stops = emptyList(),
                minutes = card.walkToMin
            )
        }

        // One segment per run of consecutive hops on the same line.
        var run = mutableListOf<PathEdge>()
        fun flushRun() {
            if (run.isEmpty()) return
            rideSegment(run)?.let { out += it }
            run = mutableListOf()
        }

        for (edge in card.path) {
            when (edge.edgeType) {
                "in_vehicle" -> {
                    if (run.isNotEmpty() && run.last().line != edge.line) flushRun()
                    run.add(edge)
                }
                else -> {
                    flushRun()
                    transferSegment(edge)?.let { out += it }
                }
            }
        }
        flushRun()

        // Walk from the last station to the destination point.
        val alightAt = card.path.lastOrNull { it.edgeType == "in_vehicle" }?.toStop
            ?: card.destStation
        val alightPos = StationCoords.of(alightAt)?.toLatLng()
        val egress = card.egressBus
        if (egress != null) {
            out += feederSegments(
                feeder = egress,
                from = alightPos,
                to = dest,
                busEngine = busEngine,
                colorIndex = 1,
                walkInLabel = "Walk to ${egress.leg.boardStop}",
                walkOutLabel = "Walk to ${card.destLabel.ifBlank { "destination" }}"
            )
        } else if (dest != null && alightPos != null && card.walkFromMin > 0) {
            out += Segment(
                kind = Kind.WALK,
                label = "Walk to ${card.destLabel.ifBlank { "destination" }}",
                detail = "${card.walkFromMin} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(alightPos, dest),
                stops = emptyList(),
                minutes = card.walkFromMin
            )
        }

        return out
    }

    /**
     * Walk, ride and walk again — one feeder bus drawn between a point and a
     * station, at whichever end of the journey it sits.
     *
     * Falls back to a single walk line when the bus stops cannot be placed, so
     * a feeder never leaves a gap in the drawn route.
     */
    private fun feederSegments(
        feeder: BusFeeder,
        from: LatLng?,
        to: LatLng?,
        busEngine: BusEngine?,
        colorIndex: Int,
        walkInLabel: String,
        walkOutLabel: String
    ): List<Segment> {
        val shape = busEngine?.legShape(feeder.leg).orEmpty()
        val stops = shape.map { Stop(it.name, LatLng(it.lat, it.lon)) }

        if (stops.size < 2) {
            if (from == null || to == null) return emptyList()
            return listOf(
                Segment(
                    kind = Kind.WALK,
                    label = "${feeder.leg.operator} ${feeder.leg.routeRef}",
                    detail = "${feeder.leg.boardStop} → ${feeder.leg.alightStop}",
                    colorHex = WALK_COLOR,
                    points = listOf(from, to),
                    stops = emptyList(),
                    minutes = feeder.totalMin
                )
            )
        }

        val out = mutableListOf<Segment>()
        if (from != null && feeder.walkToStopMin > 0) {
            out += Segment(
                kind = Kind.WALK,
                label = walkInLabel,
                detail = "${feeder.walkToStopMin} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(from, stops.first().position),
                stops = emptyList(),
                minutes = feeder.walkToStopMin
            )
        }

        out += Segment(
            kind = Kind.RIDE,
            label = "${feeder.leg.operator} ${feeder.leg.routeRef}",
            detail = "${feeder.leg.boardStop} → ${feeder.leg.alightStop}  ·  ${feeder.leg.stopCount} stops",
            colorHex = LineStyle.busColorHex(colorIndex),
            points = stops.map { it.position },
            stops = stops,
            minutes = feeder.leg.travelMin
        )

        if (to != null && feeder.walkFromStopMin > 0) {
            out += Segment(
                kind = Kind.WALK,
                label = walkOutLabel,
                detail = "${feeder.walkFromStopMin} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(stops.last().position, to),
                stops = emptyList(),
                minutes = feeder.walkFromStopMin
            )
        }
        return out
    }

    private fun rideSegment(run: List<PathEdge>): Segment? {
        val names = mutableListOf(run.first().fromStop)
        run.forEach { names.add(it.toStop) }

        val stops = names.mapNotNull { name ->
            StationCoords.of(name)?.let { Stop(name, it.toLatLng()) }
        }
        if (stops.size < 2) return null

        val line = run.first().line
        val minutes = run.sumOf { it.travelMin }.roundToInt()
        val fast = run.any { it.isFast }

        return Segment(
            kind = Kind.RIDE,
            label = LineStyle.shorten(line) + if (fast) " (fast)" else "",
            detail = "${names.first()} → ${names.last()}  ·  ${run.size} stop" +
                if (run.size == 1) "" else "s",
            colorHex = LineStyle.colorHex(line),
            points = stops.map { it.position },
            stops = stops,
            minutes = minutes
        )
    }

    private fun transferSegment(edge: PathEdge): Segment? {
        val from = StationCoords.of(edge.fromStop)?.toLatLng() ?: return null
        val to = StationCoords.of(edge.toStop)?.toLatLng() ?: return null
        // A same-station interchange has nothing to draw.
        if (from == to) return null
        return Segment(
            kind = Kind.TRANSFER,
            label = "Change at ${edge.fromStop}",
            detail = "${edge.fromStop} → ${edge.toStop}",
            colorHex = WALK_COLOR,
            points = listOf(from, to),
            stops = emptyList(),
            minutes = edge.travelMin.roundToInt()
        )
    }

    // ── Bus ───────────────────────────────────────────────────────────────────

    private fun busSegments(
        card: RouteCard,
        origin: LatLng?,
        dest: LatLng?,
        busEngine: BusEngine?
    ): List<Segment> {
        val out = mutableListOf<Segment>()
        var previousEnd: LatLng? = null

        card.busLegs.forEachIndexed { index, leg ->
            val shape = busEngine?.legShape(leg).orEmpty()
            val stops = shape.map { Stop(it.name, LatLng(it.lat, it.lon)) }
            if (stops.size < 2) return@forEachIndexed

            // Walk in from the origin, or across from the previous alighting.
            val boardPos = stops.first().position
            if (index == 0) {
                if (origin != null && card.walkToMin > 0) {
                    out += Segment(
                        kind = Kind.WALK,
                        label = "Walk to ${leg.boardStop}",
                        detail = "${card.walkToMin} min on foot",
                        colorHex = WALK_COLOR,
                        points = listOf(origin, boardPos),
                        stops = emptyList(),
                        minutes = card.walkToMin
                    )
                }
            } else if (previousEnd != null && previousEnd != boardPos) {
                out += Segment(
                    kind = Kind.TRANSFER,
                    label = "Change to ${leg.operator} ${leg.routeRef}",
                    detail = "at ${leg.boardStop}",
                    colorHex = WALK_COLOR,
                    points = listOf(previousEnd!!, boardPos),
                    stops = emptyList(),
                    minutes = 0
                )
            }

            out += Segment(
                kind = Kind.RIDE,
                label = "${leg.operator} ${leg.routeRef}",
                detail = "${leg.boardStop} → ${leg.alightStop}  ·  ${leg.stopCount} stops",
                colorHex = LineStyle.busColorHex(index),
                points = stops.map { it.position },
                stops = stops,
                minutes = leg.travelMin
            )
            previousEnd = stops.last().position
        }

        if (dest != null && previousEnd != null && card.walkFromMin > 0) {
            out += Segment(
                kind = Kind.WALK,
                label = "Walk to ${card.destLabel.ifBlank { "destination" }}",
                detail = "${card.walkFromMin} min on foot",
                colorHex = WALK_COLOR,
                points = listOf(previousEnd!!, dest),
                stops = emptyList(),
                minutes = card.walkFromMin
            )
        }
        return out
    }

    // ── Colour ────────────────────────────────────────────────────────────────

    /**
     * Two rides in one journey must never share a colour, or the legend stops
     * telling the reader which line is which.
     */
    private fun deduplicateColors(segments: List<Segment>): List<Segment> {
        val used = mutableSetOf<String>()
        var next = 0
        return segments.map { seg ->
            if (seg.kind != Kind.RIDE) return@map seg
            if (used.add(seg.colorHex)) return@map seg
            var replacement = seg.colorHex
            while (next < fallbackPalette.size) {
                val candidate = fallbackPalette[next++]
                if (used.add(candidate)) { replacement = candidate; break }
            }
            seg.copy(colorHex = replacement)
        }
    }

    private fun Pair<Double, Double>.toLatLng() = LatLng(first, second)
}
