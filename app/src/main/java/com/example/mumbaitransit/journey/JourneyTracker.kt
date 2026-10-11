package com.example.mumbaitransit.journey

import com.example.mumbaitransit.engine.StationCoords
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Works out where a journey has got to.
 *
 * Deliberately free of Android types so the whole thing can be reasoned about
 * — and, if it ever matters, tested — without a device. The service feeds it
 * fixes and the clock; it hands back a [JourneyPing].
 *
 * Two rules shape everything here:
 *
 * 1. Progress only ever moves forward. A GPS fix that bounces off a building
 *    and lands the rider two stations back would otherwise make the follower's
 *    screen walk backwards, which reads as a fault in the app rather than in
 *    the fix.
 * 2. A projected position is never labelled as a sighting. Underground on Metro
 *    3 there is no fix to be had, so the leg is advanced off the timings of the
 *    service the rider locked in — and the ping says so.
 */
class JourneyTracker(private val journey: LiveJourney) {

    companion object {
        /** Within this of a station, treat the rider as being at it. */
        private const val AT_STATION_KM = 0.45

        /** Within this of the leg's last stop, the leg is done. */
        private const val LEG_DONE_KM = 0.55

        /** A fix older than this stops counting as a sighting. */
        const val STALE_FIX_MS = 90_000L

        /** Journeys don't run forever; the service gives up after this. */
        const val MAX_DURATION_MS = 6L * 60 * 60 * 1000
    }

    /** Index of the leg the rider is on. Never decreases. */
    var legIndex: Int = 0
        private set

    /** Furthest stop reached on the current leg, as an index into leg.stops. */
    private var stopIndex: Int = 0

    private var lastFixAt: Long = 0L
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0
    private var hasEverFixed = false

    private var finished = false

    /** Called when the rider re-picks the service on a leg mid-journey. */
    fun replaceJourneyLegs(updated: LiveJourney): JourneyTracker =
        JourneyTracker(updated).also {
            it.legIndex = legIndex
            it.stopIndex = stopIndex
            it.lastFixAt = lastFixAt
            it.lastLat = lastLat
            it.lastLon = lastLon
            it.hasEverFixed = hasEverFixed
            it.finished = finished
        }

    /**
     * Starts part-way through — sharing switched on while live navigation was
     * already under way on the same journey. Forward only, like everything else.
     */
    fun startAt(index: Int) {
        if (index in journey.legs.indices && index > legIndex) {
            legIndex = index
            stopIndex = 0
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /**
     * Folds a new GPS fix into the journey state.
     *
     * Walking, road and surface-rail legs all move the timeline this way. The
     * fix is matched against the current leg's own stops first, and only falls
     * through to later legs when the rider is plainly past the end of this one —
     * which is what catches a missed alighting without waiting for the clock.
     */
    fun onFix(lat: Double, lon: Double, at: Long) {
        lastLat = lat
        lastLon = lon
        lastFixAt = at
        hasEverFixed = true

        val leg = journey.legAt(legIndex) ?: return

        // Advance within this leg.
        val nearest = nearestStopIndex(leg, lat, lon)
        if (nearest != null && nearest.first > stopIndex) {
            stopIndex = nearest.first
        }

        // Leg complete? Either standing at its last stop, or already nearer to a
        // later leg's stops than to anything on this one.
        val endCoords = coordsOf(leg.stops.lastOrNull() ?: leg.toStop)
        val atEnd = endCoords != null &&
            haversineKm(lat, lon, endCoords.first, endCoords.second) <= LEG_DONE_KM

        if (atEnd) {
            advanceLeg()
            return
        }

        // Overshoot check — only look forward, never back.
        val hereKm = nearest?.second ?: Double.MAX_VALUE
        for (i in legIndex + 1 until journey.legs.size) {
            val ahead = journey.legs[i]
            val aheadNearest = nearestStopIndex(ahead, lat, lon) ?: continue
            // A clear margin, so a station shared by two legs doesn't skip one.
            if (aheadNearest.second + 0.4 < hereKm && aheadNearest.second < AT_STATION_KM) {
                legIndex = i
                stopIndex = aheadNearest.first
                return
            }
        }
    }

    /** No usable fix this round — the clock has to carry the journey. */
    fun onNoFix(now: Long) {
        val leg = journey.legAt(legIndex) ?: return
        val dep = leg.serviceDepMins
        val arr = leg.serviceArrMins

        // Only a scheduled leg with a locked service can be projected. Anything
        // else waits for a fix rather than inventing movement.
        if (!leg.legKind.isScheduled || dep == null || arr == null) return

        val fraction = scheduleFraction(dep, arr, now) ?: return
        val target = ((leg.stops.size - 1) * fraction).roundToInt()
        if (target > stopIndex) stopIndex = target
        if (fraction >= 1.0) advanceLeg()
    }

    private fun advanceLeg() {
        if (legIndex >= journey.legs.lastIndex) {
            finished = true
            stopIndex = (journey.legAt(legIndex)?.stops?.size ?: 1) - 1
        } else {
            legIndex += 1
            stopIndex = 0
        }
    }

    // ── Output ────────────────────────────────────────────────────────────────

    fun isFinished(): Boolean = finished

    /**
     * The current state, ready to publish.
     *
     * [status] is passed in rather than derived so the service can mark a
     * journey stopped by hand without this class needing to know why.
     */
    fun ping(now: Long, status: String = JourneyStatus.ACTIVE): JourneyPing {
        val leg = journey.legAt(legIndex)
        val fresh = hasEverFixed && (now - lastFixAt) <= STALE_FIX_MS

        val source = when {
            fresh -> PingSource.GPS
            leg != null && leg.legKind.isScheduled && leg.serviceDepMins != null -> PingSource.SCHEDULE
            else -> PingSource.PLANNED
        }

        val here = leg?.stops?.getOrNull(stopIndex).orEmpty()
        val next = when {
            leg == null -> ""
            stopIndex < leg.stops.lastIndex -> leg.stops[stopIndex + 1]
            else -> journey.legAt(legIndex + 1)?.toStop ?: journey.destLabel
        }

        return JourneyPing(
            at = now,
            lat = lastLat,
            lon = lastLon,
            fix = fresh,
            legIndex = legIndex,
            nearestStation = here,
            nextStop = next,
            progressPct = progressPct(),
            etaEpochMs = eta(now),
            source = source,
            status = if (finished && status == JourneyStatus.ACTIVE) JourneyStatus.COMPLETED else status
        )
    }

    /** Whole-journey progress, weighted by each leg's planned minutes. */
    fun progressPct(): Int {
        val total = journey.legs.sumOf { it.scheduledMin }.coerceAtLeast(1)
        val done = journey.legs.take(legIndex).sumOf { it.scheduledMin }
        val leg = journey.legAt(legIndex)
        val within = if (leg == null || leg.stops.size <= 1) 0.0
                     else leg.scheduledMin * (stopIndex.toDouble() / (leg.stops.size - 1))
        return (((done + within) / total) * 100).roundToInt().coerceIn(0, 100)
    }

    /**
     * Arrival estimate in epoch millis.
     *
     * Remaining time is the plan for legs not yet started plus the unfinished
     * share of this one. Where the current leg has a locked service, that
     * service's own arrival time is used instead — it is a real timetable
     * figure, and beats scaling an average.
     */
    fun eta(now: Long): Long {
        val leg = journey.legAt(legIndex) ?: return now
        val laterMin = journey.legs.drop(legIndex + 1).sumOf { it.scheduledMin }

        val remainingThisLeg: Int = run {
            val arr = leg.serviceArrMins
            if (leg.legKind.isScheduled && arr != null) {
                val minsToArrival = minutesUntilClock(arr, now)
                if (minsToArrival in 0..300) return@run minsToArrival
            }
            val fractionLeft =
                if (leg.stops.size <= 1) 1.0
                else 1.0 - (stopIndex.toDouble() / (leg.stops.size - 1))
            (leg.scheduledMin * fractionLeft).roundToInt()
        }

        return now + (remainingThisLeg + laterMin) * 60_000L
    }

    // ── Geometry and clocks ───────────────────────────────────────────────────

    /** Nearest stop on [leg] to the fix, as (index, distance km). */
    private fun nearestStopIndex(leg: JourneyLeg, lat: Double, lon: Double): Pair<Int, Double>? {
        var bestIdx = -1
        var bestKm = Double.MAX_VALUE
        leg.stops.forEachIndexed { i, stop ->
            val c = coordsOf(stop) ?: return@forEachIndexed
            val km = haversineKm(lat, lon, c.first, c.second)
            if (km < bestKm) { bestKm = km; bestIdx = i }
        }
        return if (bestIdx < 0) null else bestIdx to bestKm
    }

    /**
     * Station coordinates, tolerating the spelling drift between the graph and
     * the coordinate table (BANDRA / Bandra, Mahim Jn / Mahim Jn.).
     */
    private fun coordsOf(name: String): Pair<Double, Double>? {
        if (name.isBlank()) return null
        StationCoords.coords[name]?.let { return it }
        val key = name.trim().lowercase().trimEnd('.')
        return StationCoords.coords.entries
            .firstOrNull { it.key.trim().lowercase().trimEnd('.') == key }
            ?.value
    }

    /** How far through a scheduled leg the clock says we are, 0..1. */
    private fun scheduleFraction(depMins: Int, arrMins: Int, now: Long): Double? {
        val nowMins = clockMins(now)
        val span = ((arrMins - depMins) % 1440 + 1440) % 1440
        if (span <= 0 || span > 300) return null
        val elapsed = ((nowMins - depMins) % 1440 + 1440) % 1440
        // More than a leg-length past arrival means the clock has wrapped onto
        // something unrelated, not that the rider is hours late.
        if (elapsed > span + 120) return null
        return (elapsed.toDouble() / span).coerceIn(0.0, 1.0)
    }

    private fun minutesUntilClock(targetMins: Int, now: Long): Int {
        val diff = ((targetMins - clockMins(now)) % 1440 + 1440) % 1440
        return if (diff > 720) diff - 1440 else diff
    }

    private fun clockMins(at: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val a = sin(Math.toRadians(lat2 - lat1) / 2).pow(2) +
            cos(p1) * cos(p2) * sin(Math.toRadians(lon2 - lon1) / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
