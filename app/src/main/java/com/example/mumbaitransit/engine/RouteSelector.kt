package com.example.mumbaitransit.engine

import com.example.mumbaitransit.engine.RoutingConfig.ALTERNATIVE_FACTOR
import com.example.mumbaitransit.engine.RoutingConfig.ALTERNATIVE_SLACK_MIN
import com.example.mumbaitransit.engine.RoutingConfig.MAX_ROUTES_SHOWN
import com.example.mumbaitransit.engine.RoutingConfig.MAX_TIME_FACTOR
import com.example.mumbaitransit.engine.RoutingConfig.MAX_TIME_SLACK_MIN
import com.example.mumbaitransit.engine.RoutingConfig.USEFUL_FACTOR
import com.example.mumbaitransit.engine.RoutingConfig.USEFUL_SLACK_MIN
import com.example.mumbaitransit.model.BusLeg
import com.example.mumbaitransit.model.BusStop
import com.example.mumbaitransit.model.JourneyKind
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.RouteLeg
import com.example.mumbaitransit.model.RouteSelection
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Chooses the few routes worth showing from everything the planner generated.
 *
 * [JourneyPlanner] keeps every valid combination — thousands on a long trip.
 * This turns them into a short list per Routes filter:
 *
 *  1. Validate — drop a journey with a ride that goes nowhere, or a bus hop
 *     that is quicker to walk than to wait for and ride.
 *  2. Deduplicate — one journey per set of rides.
 *  3. Classify — by the rides alone ([JourneyKind.of]); walks never count, and
 *     which search built the journey does not matter.
 *  4. Collapse near-identical journeys — same kind, same rides in the same
 *     order, changing within [RoutingConfig.SAME_PLACE_KM] of each other. Only
 *     the most convenient is kept: bus 151 and bus 351 between the same stops
 *     are one route, not two.
 *  5. Drop dominated journeys — ones another journey beats on time, fare,
 *     walking and transfers all at once.
 *  6. Rank by convenience score (see [score]) — time is one factor, not the
 *     only one.
 *  7. Pick, from routes within the score and time limits in [RoutingConfig]:
 *     the best route; the fastest, cheapest, least-walking and
 *     fewest-transfer routes where they beat it by a margin that matters;
 *     under All, the best train-only, bus-only and mixed route when none is
 *     listed yet; then the next best that ride different lines, up to
 *     [RoutingConfig.MAX_ROUTES_SHOWN]. Nothing is added to fill a quota.
 *
 * One instance per search: it caches bus stop positions for that search only.
 */
class RouteSelector(private val bus: BusEngine) {

    private data class Pt(val lat: Double, val lon: Double)

    /** One candidate, with everything the selection compares worked out once. */
    private class Scored(
        val card: RouteCard,
        val kind: JourneyKind,
        val rides: List<RouteLeg>,
        /** Door to door as the card shows it. */
        val minutes: Int,
        val fare: Int,
        val transfers: Int,
        val walkMin: Int,
        val walkKm: Double,
        val score: Double
    ) {
        /** What is ridden, in order — a rail line's name, or BUS for any bus. */
        val lines: List<String> = rides.map { if (it.mode == LegMode.BUS) BUS else it.line }

        /** Board and alight positions per ride, filled in on first use. */
        var ends: List<Pair<Pt?, Pt?>>? = null
    }

    private val shapes = HashMap<String, List<BusStop>>()

    fun select(cards: List<RouteCard>): RouteSelection {
        val candidates = cards.asSequence()
            .filter { it.legs.isNotEmpty() && isValid(it) }
            .distinctBy { it.scenario }            // kind + hash of the rides
            .map { score(it) }
            .toList()

        val kept: Map<JourneyKind, List<Scored>> = candidates
            .groupBy { it.kind }
            .filterKeys { it != JourneyKind.ROAD }
            .mapValues { (_, list) -> distinct(list) }

        fun view(kind: JourneyKind) = choose(kept[kind].orEmpty(), kept, alternatives = false)

        return RouteSelection(
            all   = choose(kept.values.flatten(), kept, alternatives = true),
            train = view(JourneyKind.TRAIN),
            bus   = view(JourneyKind.BUS),
            mixed = view(JourneyKind.MIXED),
            generated = cards.count { it.legs.isNotEmpty() }
        )
    }

    // ── 1. Validate ───────────────────────────────────────────────────────────

    private fun isValid(card: RouteCard): Boolean {
        val rides = card.transitLegs
        if (rides.isEmpty()) return false
        for (ride in rides) {
            if (ride.from.isBlank() || ride.from.equals(ride.to, ignoreCase = true)) return false
            if (ride.minutes <= 0) return false
            // A bus hop is pointless when walking its length is no slower than
            // waiting for the bus and riding it.
            val km = ride.bus?.km ?: continue
            if (km / RoutingConfig.WALK_SPEED_KMH * 60 <= ride.minutes + ride.waitMin) return false
        }
        return true
    }

    // ── 6. Convenience score ──────────────────────────────────────────────────

    /**
     * Door-to-door minutes as shown, plus: walking counted at
     * [RoutingConfig.WALK_WEIGHT]; the expected wait for each train or metro
     * (bus waits are already in the time); a cost per transfer and per change
     * between bus and train; the fare in minutes; the least reliable ride's
     * MRI; and any bus whose stop order is not fully trusted. Lower is better.
     */
    private fun score(card: RouteCard): Scored {
        val walks = card.legs.filter { it.mode == LegMode.WALK }
        val walkMin = walks.sumOf { it.minutes }
        // In-station changes carry minutes but no distance; count them at walking pace.
        val walkKm = walks.sumOf {
            if (it.distanceKm > 0) it.distanceKm else it.minutes * RoutingConfig.WALK_SPEED_KMH / 60.0
        }
        val rides = card.transitLegs
        val transfers = max(0, rides.size - 1)
        val modeChanges = rides.zipWithNext().count { (a, b) -> (a.mode == LegMode.BUS) != (b.mode == LegMode.BUS) }

        val railWait = rides.filter { it.mode.isRail }.sumOf { ride ->
            val perHour = ride.edges.firstOrNull { it.edgeType == "in_vehicle" }?.freq ?: 0.0
            if (perHour > 0) min(RoutingConfig.RAIL_WAIT_MAX_MIN, 30.0 / perHour)
            else RoutingConfig.RAIL_WAIT_DEFAULT_MIN
        }
        val worstMri = rides.minOf { ride ->
            if (ride.mode == LegMode.BUS) RoutingConfig.BUS_RELIABILITY_PCT
            else card.mriScores[ride.line]?.mriPct ?: RoutingConfig.BUS_RELIABILITY_PCT
        }
        val busOrder = rides.sumOf { ride ->
            when (ride.bus?.seqConf) {
                null, "high" -> 0.0
                "medium"     -> RoutingConfig.BUS_ORDER_PENALTY_MEDIUM_MIN
                else         -> RoutingConfig.BUS_ORDER_PENALTY_LOW_MIN
            }
        }

        val score = card.totalMin +
            (RoutingConfig.WALK_WEIGHT - 1) * walkMin +
            railWait +
            RoutingConfig.TRANSFER_PENALTY_MIN * transfers +
            RoutingConfig.MODE_CHANGE_PENALTY_MIN * modeChanges +
            RoutingConfig.MINUTES_PER_RUPEE * card.totalFare +
            RoutingConfig.RELIABILITY_WEIGHT * (100 - worstMri) +
            busOrder

        return Scored(
            card = card,
            kind = card.kind,
            rides = rides,
            minutes = card.totalMin.roundToInt(),
            fare = card.totalFare,
            transfers = transfers,
            walkMin = walkMin,
            walkKm = walkKm,
            score = score
        )
    }

    // ── 4 + 5. Collapse near-identical, drop dominated ────────────────────────

    /**
     * One kind's candidates, best first, with near-identical journeys folded
     * into the most convenient of them and dominated ones removed.
     */
    private fun distinct(list: List<Scored>): List<Scored> {
        if (list.isEmpty()) return list
        val ranked = list.sortedBy { it.score }
        // Nothing past this can be shown under any filter, so it is never compared.
        val limit = ranked[0].score * ALTERNATIVE_FACTOR + ALTERNATIVE_SLACK_MIN
        val kept = ArrayList<Scored>()
        for (s in ranked) {
            if (s.score > limit) break
            if (kept.any { sameShape(it, s) || dominates(it, s) }) continue
            kept += s
        }
        return kept.filter { k -> kept.none { it !== k && dominates(it, k) } }
    }

    /**
     * Near-identical: the same rides in the same order, and every change
     * between rides at the same place. The first boarding and last alighting
     * point are not compared — both are near the commuter's own ends, and the
     * walk there is already judged by the score.
     */
    private fun sameShape(a: Scored, b: Scored): Boolean {
        if (a.kind != b.kind || a.lines != b.lines) return false
        val ea = endsOf(a)
        val eb = endsOf(b)
        val last = a.rides.lastIndex
        for (i in a.rides.indices) {
            if (i > 0 && !samePlace(ea[i].first, a.rides[i].from, eb[i].first, b.rides[i].from)) return false
            if (i < last && !samePlace(ea[i].second, a.rides[i].to, eb[i].second, b.rides[i].to)) return false
        }
        return true
    }

    /** At least as good on time, fare, transfers and walking, and better on one. */
    private fun dominates(a: Scored, b: Scored): Boolean =
        a.minutes <= b.minutes && a.fare <= b.fare &&
            a.transfers <= b.transfers && a.walkMin <= b.walkMin &&
            (a.minutes < b.minutes || a.fare < b.fare ||
                a.transfers < b.transfers || a.walkMin < b.walkMin)

    private fun samePlace(a: Pt?, aName: String, b: Pt?, bName: String): Boolean =
        if (a != null && b != null) bus.haversineKm(a.lat, a.lon, b.lat, b.lon) <= RoutingConfig.SAME_PLACE_KM
        else aName.equals(bName, ignoreCase = true)

    private fun endsOf(s: Scored): List<Pair<Pt?, Pt?>> = s.ends ?: s.rides.map { ride ->
        val leg = ride.bus
        if (leg != null) {
            val shape = shapeOf(leg)
            shape.firstOrNull()?.let { Pt(it.lat, it.lon) } to shape.lastOrNull()?.let { Pt(it.lat, it.lon) }
        } else {
            StationCoords.of(ride.from)?.let { Pt(it.first, it.second) } to
                StationCoords.of(ride.to)?.let { Pt(it.first, it.second) }
        }
    }.also { s.ends = it }

    private fun shapeOf(leg: BusLeg): List<BusStop> =
        shapes.getOrPut("${leg.routeId}|${leg.boardStop}|${leg.alightStop}|${leg.stopCount}") {
            bus.legShape(leg)
        }

    // ── 7. Pick ──────────────────────────────────────────────────────────────

    private fun choose(
        pool: List<Scored>,
        kept: Map<JourneyKind, List<Scored>>,
        alternatives: Boolean
    ): List<RouteCard> {
        val ranked = pool.sortedBy { it.score }
        val best = ranked.firstOrNull() ?: return emptyList()
        val timeCap = ranked.minOf { it.minutes } * MAX_TIME_FACTOR + MAX_TIME_SLACK_MIN
        val useful = ranked.filter {
            it.score <= best.score * USEFUL_FACTOR + USEFUL_SLACK_MIN && it.minutes <= timeCap
        }

        val picks = mutableListOf(best)
        val alternativeKinds = HashSet<Scored>()
        fun pick(s: Scored) { if (picks.none { it === s }) picks += s }

        // Category winners, each only when it beats the best route by a margin that matters.
        useful.minWithOrNull(compareBy<Scored>({ it.minutes }, { it.score }))
            ?.takeIf { best.minutes - it.minutes >= RoutingConfig.MIN_TIME_GAIN_MIN }?.let(::pick)
        useful.minWithOrNull(compareBy<Scored>({ it.fare }, { it.score }))
            ?.takeIf { best.fare - it.fare >= RoutingConfig.MIN_FARE_SAVING_INR }?.let(::pick)
        useful.minWithOrNull(compareBy<Scored>({ it.walkMin }, { it.score }))
            ?.takeIf { best.walkKm - it.walkKm >= RoutingConfig.MIN_WALK_SAVING_KM }?.let(::pick)
        useful.minWithOrNull(compareBy<Scored>({ it.transfers }, { it.score }))
            ?.takeIf { it.transfers < best.transfers }?.let(::pick)

        // Under All, a genuinely useful route of each kind not yet listed — never
        // one that a listed route beats on every count.
        if (alternatives) {
            for (kind in listOf(JourneyKind.TRAIN, JourneyKind.MIXED, JourneyKind.BUS)) {
                if (picks.any { it.kind == kind }) continue
                val alt = kept[kind]?.firstOrNull { s ->
                    s.score <= best.score * ALTERNATIVE_FACTOR + ALTERNATIVE_SLACK_MIN &&
                        s.minutes <= timeCap && picks.none { dominates(it, s) }
                } ?: continue
                pick(alt)
                alternativeKinds += alt
            }
        }

        // Then the next most convenient, if it rides different lines from every
        // route listed and nothing listed beats it outright.
        for (s in useful) {
            if (picks.size >= MAX_ROUTES_SHOWN) break
            if (picks.any { it === s || (it.kind == s.kind && it.lines == s.lines) || dominates(it, s) }) continue
            pick(s)
        }

        return label(picks.sortedBy { it.score }, alternativeKinds)
    }

    /**
     * Badges go to the one listed route that is strictly best on each count;
     * a tie earns none. A kind alternative says what it is, and the top route
     * with no badge is marked as recommended. Everything else keeps its title.
     */
    private fun label(picks: List<Scored>, alternativeKinds: Set<Scored>): List<RouteCard> {
        val badges = HashMap<Scored, MutableList<String>>()
        if (picks.size >= 2) {
            fun award(badge: String, value: (Scored) -> Int) {
                val least = picks.minOf(value)
                val winners = picks.filter { value(it) == least }
                if (winners.size == 1) badges.getOrPut(winners[0]) { mutableListOf() } += badge
            }
            award("⚡ Fastest") { it.minutes }
            award("💰 Cheapest") { it.fare }
            award("🚶 Least walking") { it.walkMin }
            award("🔄 Fewest transfers") { it.transfers }
        }
        return picks.mapIndexed { i, s ->
            val title = badges[s]?.joinToString(" · ")
                ?: (if (s in alternativeKinds) alternativeTitle(s) else null)
                ?: (if (i == 0 && picks.size >= 2) "⭐ Recommended" else null)
            if (title == null) s.card else s.card.copy(scenarioLabel = title)
        }
    }

    private fun alternativeTitle(s: Scored): String? = when (s.kind) {
        JourneyKind.TRAIN -> if (s.rides.all { it.mode == LegMode.METRO }) "🚇 Metro only" else "🚆 Train only"
        JourneyKind.BUS   -> "🚌 Bus only"
        else              -> null     // a mixed card's own title already names its rides in order
    }

    private companion object {
        const val BUS = "BUS"
    }
}
