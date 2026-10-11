package com.example.mumbaitransit.journey

import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.RouteLeg
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.ui.LineStyle
import kotlin.math.roundToInt

/**
 * Turns a planned route into the leg list a live journey is tracked against.
 *
 * The route card already knows the path; what it does not carry is the flat,
 * ordered station list the tracker needs to answer "where is he now" from a
 * single GPS fix. Building that here — once, at share time — means the tracker
 * never has to re-walk the graph while the phone is in someone's pocket on a
 * moving train.
 */
object JourneyBuilder {

    /** Below this a walk isn't worth its own leg on the follower's timeline. */
    private const val MIN_WALK_MIN = 2

    /**
     * The card's legs that become journey legs, in order — every ride, and
     * every walk long enough to be worth a leg. Live navigation zips this
     * against [LiveJourney.legs] to recover stop coordinates and bus shapes,
     * so the two must never disagree.
     */
    fun keptLegs(card: RouteCard): List<RouteLeg> =
        card.legs.filter { it.mode != LegMode.WALK || it.minutes >= MIN_WALK_MIN }

    fun build(
        card: RouteCard,
        legs: List<TimetableLeg>,
        ownerUid: String,
        ownerName: String,
        code: String = ShareCode.generate()
    ): LiveJourney {
        val origin = card.originLabel.ifEmpty { card.originStation }
        val dest = card.destLabel.ifEmpty { card.destStation }

        val built = when {
            card.legs.isNotEmpty() -> routeLegs(card, legs, origin, dest)
            card.type == "transit" -> transitLegs(card, legs, origin, dest)
            else                   -> listOf(roadLeg(card, origin, dest))
        }

        return LiveJourney(
            code = code,
            ownerUid = ownerUid,
            ownerName = ownerName,
            originLabel = origin,
            destLabel = dest,
            startedAt = System.currentTimeMillis(),
            plannedTotalMin = card.totalMin.roundToInt(),
            legs = built.mapIndexed { i, leg -> leg.copy(index = i) }
        )
    }

    // ── Journeys built from legs ──────────────────────────────────────────────

    /**
     * The card's own legs, in the order they are travelled — trains, metros,
     * buses and the walks between them. Rail legs take their suggested service
     * from the timetable by order, which is how it is returned.
     */
    private fun routeLegs(
        card: RouteCard,
        timetable: List<TimetableLeg>,
        origin: String,
        dest: String
    ): List<JourneyLeg> {
        val out = mutableListOf<JourneyLeg>()
        val legs = card.legs
        var railIndex = 0
        var busIndex = 0
        legs.forEachIndexed { i, leg ->
            when {
                leg.mode == LegMode.WALK -> {
                    if (leg.minutes < MIN_WALK_MIN) return@forEachIndexed
                    val first = i == 0
                    val last = i == legs.lastIndex
                    val change = !first && !last &&
                        legs[i - 1].mode.isRail && legs.getOrNull(i + 1)?.mode?.isRail == true
                    out += walkLeg(
                        from = if (first) origin else leg.from,
                        to = if (last) dest else leg.to,
                        minutes = leg.minutes,
                        note = when {
                            last   -> "Walk to $dest"
                            change -> "Change at ${leg.to}"
                            else   -> "Walk to ${leg.to}"
                        }
                    )
                }
                leg.mode.isRail -> {
                    val kind = LegKind.forMode(leg.edges.firstOrNull()?.mode.orEmpty(), leg.line)
                    val suggested = timetable.getOrNull(railIndex++)?.trains?.firstOrNull()
                    out += JourneyLeg(
                        kind = kind.name,
                        line = leg.line,
                        lineShort = LineStyle.shorten(leg.line),
                        colorHex = LineStyle.colorHex(leg.line),
                        fromStop = leg.from,
                        toStop = leg.to,
                        stops = leg.stops.distinct(),
                        plannedMin = leg.minutes,
                        serviceLabel = suggested?.let { serviceLabel(it.depFrom, it.arrTo, it.terminus, it.trainType, it.ac) },
                        serviceDepMins = suggested?.depFrom?.let(::toMins),
                        serviceArrMins = suggested?.arrTo?.let(::toMins),
                        serviceTerminus = suggested?.terminus?.takeIf { it.isNotBlank() },
                        serviceTrainNo = suggested?.trainNo
                    )
                }
                leg.mode == LegMode.BUS -> {
                    out += JourneyLeg(
                        kind = LegKind.BUS.name,
                        line = leg.line,
                        lineShort = leg.bus?.routeRef ?: leg.line,
                        colorHex = LineStyle.busColorHex(busIndex++),
                        fromStop = leg.from,
                        toStop = leg.to,
                        stops = leg.stops.distinct(),
                        plannedMin = leg.waitMin + leg.minutes,
                        note = "${leg.line} towards ${leg.to}"
                    )
                }
            }
        }
        if (out.isEmpty()) out += roadLeg(card, origin, dest)
        return out
    }

    // ── Transit ───────────────────────────────────────────────────────────────

    private fun transitLegs(
        card: RouteCard,
        timetable: List<TimetableLeg>,
        origin: String,
        dest: String
    ): List<JourneyLeg> {
        val out = mutableListOf<JourneyLeg>()

        val inVehicle = card.path.filter { it.edgeType == "in_vehicle" }
        val transfers = card.path.filter { it.edgeType == "transfer" }
        val segments = groupByLine(inVehicle)

        // Walk to the first station.
        if (card.walkToMin >= MIN_WALK_MIN && card.originStation.isNotEmpty()) {
            out += walkLeg(
                from = origin,
                to = card.originStation,
                minutes = card.walkToMin,
                note = "Walk to ${card.originStation}"
            )
        }

        // The timetable comes back one entry per rail/metro segment, in the same
        // order, so matching on boarding station is enough to attach the trains
        // the rider will be asked to choose from.
        val ttByBoarding = timetable.associateBy { it.fromStop.uppercase() }

        var transferIdx = 0
        segments.forEachIndexed { segIdx, segment ->
            val first = segment.first()
            val last = segment.last()
            val kind = LegKind.forMode(first.mode, first.line)

            val allStops = segment.map { it.fromStop } + last.toStop
            val plannedMin = segment.sumOf { it.travelMin }.roundToInt()

            // If the timetable already found a service, pre-fill the soonest one
            // so a rider who just boards the next thing has nothing to tap.
            val suggested = ttByBoarding[first.fromStop.uppercase()]?.trains?.firstOrNull()

            out += JourneyLeg(
                kind = kind.name,
                line = first.line,
                lineShort = LineStyle.shorten(first.line),
                colorHex = LineStyle.colorHex(first.line),
                fromStop = first.fromStop,
                toStop = last.toStop,
                stops = allStops.distinct(),
                plannedMin = plannedMin,
                serviceLabel = suggested?.let { serviceLabel(it.depFrom, it.arrTo, it.terminus, it.trainType, it.ac) },
                serviceDepMins = suggested?.depFrom?.let(::toMins),
                serviceArrMins = suggested?.arrTo?.let(::toMins),
                serviceTerminus = suggested?.terminus?.takeIf { it.isNotBlank() },
                serviceTrainNo = suggested?.trainNo
            )

            // Transfer walk between two segments.
            if (segIdx < segments.lastIndex && transferIdx < transfers.size) {
                val xfer = transfers[transferIdx++]
                out += walkLeg(
                    from = xfer.fromStop,
                    to = xfer.toStop,
                    minutes = xfer.travelMin.roundToInt().coerceAtLeast(1),
                    note = "Change at ${xfer.toStop}"
                )
            }
        }

        // Walk from the last station.
        if (card.walkFromMin >= MIN_WALK_MIN && card.destStation.isNotEmpty()) {
            out += walkLeg(
                from = card.destStation,
                to = dest,
                minutes = card.walkFromMin,
                note = "Walk to $dest"
            )
        }

        // A card with no in-vehicle edges at all would otherwise share an empty
        // journey; give the follower one leg that at least names the endpoints.
        if (out.isEmpty()) out += roadLeg(card, origin, dest)
        return out
    }

    /** Consecutive in-vehicle edges on the same line are one boarding. */
    private fun groupByLine(edges: List<PathEdge>): List<List<PathEdge>> {
        val segments = mutableListOf<List<PathEdge>>()
        var current = mutableListOf<PathEdge>()
        for (edge in edges) {
            if (current.isEmpty() || current.last().line == edge.line) {
                current.add(edge)
            } else {
                segments.add(current)
                current = mutableListOf(edge)
            }
        }
        if (current.isNotEmpty()) segments.add(current)
        return segments
    }

    private fun walkLeg(from: String, to: String, minutes: Int, note: String) =
        JourneyLeg(
            kind = LegKind.WALK.name,
            fromStop = from,
            toStop = to,
            stops = listOf(from, to).distinct(),
            plannedMin = minutes,
            note = note
        )

    private fun roadLeg(card: RouteCard, origin: String, dest: String) =
        JourneyLeg(
            kind = LegKind.ROAD.name,
            line = card.scenarioLabel,
            lineShort = card.scenarioLabel,
            colorHex = "#6B6860",
            fromStop = origin,
            toStop = dest,
            stops = listOf(origin, dest).distinct(),
            plannedMin = card.totalMin.roundToInt(),
            note = card.modeStr
        )

    // ── Labels ────────────────────────────────────────────────────────────────

    /**
     * How a locked-in service reads on the follower's screen.
     *
     * Time, heading and class only. No train number anywhere — the person
     * following has no use for one, and the metro has none to give.
     */
    fun serviceLabel(
        depFrom: String,
        arrTo: String,
        terminus: String,
        trainType: String,
        ac: Boolean
    ): String = buildString {
        append("$depFrom → $arrTo")
        terminus.takeIf { it.isNotBlank() }?.let { append("  ·  towards $it") }
        trainType.takeIf { it.isNotBlank() }?.let { append("  ·  $it") }
        if (ac && !trainType.contains("AC", true)) append("  ·  AC")
    }

    /** "09:14" -> 554. Returns null on anything that isn't a clock time. */
    fun toMins(hhmm: String): Int? {
        val parts = hhmm.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].take(2).toIntOrNull() ?: return null
        if (h !in 0..47 || m !in 0..59) return null
        return (h * 60 + m) % 1440
    }
}
