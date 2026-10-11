package com.example.mumbaitransit.navigation

import com.example.mumbaitransit.engine.BusEngine
import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.engine.LastMileOption
import com.example.mumbaitransit.engine.StationCoords
import com.example.mumbaitransit.journey.JourneyBuilder
import com.example.mumbaitransit.journey.JourneyLeg
import com.example.mumbaitransit.journey.LegKind
import com.example.mumbaitransit.journey.LiveJourney
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.RouteLeg

/**
 * Turns the selected route into something GPS can be matched against.
 *
 * Starts from the [LiveJourney] that [JourneyBuilder] makes for live sharing —
 * the same legs, the same indices, the same pre-filled trains — and adds the
 * one thing navigation needs that sharing never did: a coordinate for every
 * stop, including bus stops and the rider's own two end points.
 *
 *  - Stations: [StationCoords], the table the map and sharing already use.
 *  - Bus stops: [BusEngine.legShape], the reconstructed stop order of that route.
 *  - Origin / destination: the points the search was run from and to.
 *  - A walk joins whatever the rides either side of it start and end at.
 *
 * A stop that can't be placed keeps a null coordinate. The engine then leaves
 * that part to the rider's taps rather than guessing.
 */
object NavPlanBuilder {

    fun build(
        card: RouteCard,
        journey: LiveJourney,
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double,
        busEngine: BusEngine?,
        /** Walk / Auto / Cab options for a walk leg, and the one picked on the Routes screen. */
        lastMile: ((RouteLeg) -> Pair<List<LastMileOption>, LastMileMode>)? = null
    ): NavPlan? {
        if (journey.legs.isEmpty()) return null
        if (journey.legs.any { it.legKind == LegKind.ROAD }) return null

        // Leg N of the journey came from kept leg N of the card, when the two line up.
        val kept = JourneyBuilder.keptLegs(card)
        val sources: List<RouteLeg?> =
            if (kept.size == journey.legs.size && kept.zip(journey.legs).all { (r, j) -> sameMode(r, j) }) kept
            else List(journey.legs.size) { null }

        val origin = placed(originLat, originLon)
        val dest = placed(destLat, destLon)

        // Rides first, so each walk knows the points it joins.
        val rides = arrayOfNulls<List<NavStop>>(journey.legs.size)
        journey.legs.forEachIndexed { i, leg ->
            rides[i] = when (leg.legKind) {
                LegKind.RAIL, LegKind.METRO -> leg.stops.map { station(it) }
                LegKind.BUS -> busStops(leg, sources[i], busEngine)
                else -> null
            }
        }

        val legs = journey.legs.mapIndexed { i, leg ->
            when (leg.legKind) {
                LegKind.WALK -> {
                    val from = if (i == 0) NavStop(journey.originLabel.ifBlank { leg.fromStop }, origin?.first, origin?.second)
                               else rides[i - 1]?.lastOrNull() ?: station(leg.fromStop)
                    val toName = if (i == journey.legs.lastIndex) journey.destLabel.ifBlank { leg.toStop } else leg.toStop
                    val to = if (i == journey.legs.lastIndex) NavStop(toName, dest?.first, dest?.second)
                             else rides[i + 1]?.firstOrNull()?.copy(name = leg.toStop) ?: station(leg.toStop)
                    val source = sources[i] ?: RouteLeg(LegMode.WALK, leg.fromStop, leg.toStop, leg.plannedMin)
                    val (options, picked) = lastMile?.invoke(source) ?: (emptyList<LastMileOption>() to LastMileMode.WALK)
                    val chosen = options.firstOrNull { it.mode == picked }
                    NavLeg(
                        index = i,
                        mode = NavMode.WALK,
                        stops = listOf(from.copy(name = if (i == 0) from.name else leg.fromStop), to),
                        plannedMin = chosen?.minutes ?: leg.plannedMin,
                        distanceKm = sources[i]?.distanceKm ?: 0.0,
                        lastMile = options,
                        travelBy = chosen?.mode ?: LastMileMode.WALK
                    )
                }
                else -> NavLeg(
                    index = i,
                    mode = when (leg.legKind) {
                        LegKind.METRO -> NavMode.METRO
                        LegKind.BUS -> NavMode.BUS
                        else -> NavMode.TRAIN
                    },
                    line = leg.line,
                    // A bus is named the way it is posted with its operator — "TMT 63",
                    // not a bare "63" that could be any of four undertakings.
                    lineShort = if (leg.legKind == LegKind.BUS) leg.line.ifBlank { leg.lineShort } else leg.lineShort,
                    colorHex = leg.colorHex,
                    stops = rides[i].orEmpty().ifEmpty { leg.stops.map { NavStop(it) } },
                    plannedMin = leg.plannedMin,
                    serviceLabel = leg.serviceLabel,
                    serviceDepMins = leg.serviceDepMins,
                    serviceArrMins = leg.serviceArrMins,
                    serviceTerminus = leg.serviceTerminus,
                    serviceTrainNo = leg.serviceTrainNo
                )
            }
        }

        return NavPlan(
            code = journey.code,
            routeKey = card.scenario,
            originLabel = journey.originLabel,
            destLabel = journey.destLabel,
            legs = legs
        )
    }

    private fun sameMode(r: RouteLeg, j: JourneyLeg): Boolean = when (j.legKind) {
        LegKind.WALK -> r.mode == LegMode.WALK
        LegKind.BUS -> r.mode == LegMode.BUS
        LegKind.RAIL, LegKind.METRO -> r.mode.isRail
        LegKind.ROAD -> false
    }

    private fun placed(lat: Double, lon: Double): Pair<Double, Double>? =
        if (lat != 0.0 || lon != 0.0) lat to lon else null

    private fun station(name: String): NavStop {
        val c = StationCoords.of(name)
        return NavStop(name, c?.first, c?.second)
    }

    /**
     * Bus stops in riding order. The route's own shape gives every stop a real
     * position; without it (engine not loaded, a stop the reconstruction can't
     * match) the names are kept and only the two ends are tried.
     */
    private fun busStops(leg: JourneyLeg, source: RouteLeg?, busEngine: BusEngine?): List<NavStop> {
        val shape = source?.bus?.let { busEngine?.legShape(it) }.orEmpty()
        if (shape.size >= 2) return shape.map { NavStop(it.name, it.lat, it.lon) }
        return leg.stops.map { NavStop(it) }
    }
}
