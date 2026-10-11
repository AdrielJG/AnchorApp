package com.example.mumbaitransit.navigation

import com.example.mumbaitransit.engine.LastMileMode
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The navigation state machine.
 *
 * Driven entirely by the legs of the route the rider picked: the current leg's
 * mode decides how GPS is interpreted, and finishing a leg hands over to
 * whatever leg comes next. Nothing here knows about any particular journey
 * shape.
 *
 *   WALK leg     WALKING  ── reach the leg's end ──▶ next leg
 *   ride leg     WAITING  ── moving along the line / "I've boarded" ──▶ RIDING
 *                RIDING   ── reach the alighting stop ──▶ REACHED_STOP (or ARRIVED)
 *                REACHED_STOP ── walk away / Continue ──▶ next leg
 *
 * Rules carried over from live sharing:
 *  - Progress only moves forward. A bounced fix never walks the rider back.
 *  - A projection is never presented as a sighting. Train/metro position may be
 *    advanced on the timetable when there is no fix (underground), and the
 *    snapshot says so. Buses are never projected: there is no bus timetable or
 *    vehicle feed, so a bus leg moves only on the rider's own GPS or a tap.
 */
class NavigationEngine(plan: NavPlan, private val clock: () -> Long = System::currentTimeMillis) {

    companion object {
        /** Fixes worse than this are stored but never move the journey. */
        const val MAX_ACCURACY_M = 150f
        const val STALE_FIX_MS = 90_000L
        const val WALK_M_PER_MIN = 80.0
        /** Straight line under-reads a city walk; used only without a Directions path. */
        const val WALK_DETOUR = 1.3
        /** Auto ~20 km/h, cab ~22 km/h — the speeds the auto/cab cards are costed on. */
        const val AUTO_M_PER_MIN = 333.0
        const val CAB_M_PER_MIN = 367.0
    }

    var plan: NavPlan = plan
        private set

    var legIndex = 0; private set
    var phase = NavPhase.NOT_STARTED; private set

    private var stopIndex = -1
    private var estimated = false
    private var reached: String? = null
    private var reachedStop: NavStop? = null

    private var startedAt = 0L
    private var arrivedAt: Long? = null

    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var lastFixAt = 0L

    // Walk tracking
    private var walkStartM: Double? = null
    private var walkMinM = Double.MAX_VALUE
    private var walkLastM: Double? = null
    private var walkSteps: List<WalkStep> = emptyList()
    private var walkStepsLeg = -1
    private var walkStepIndex = 0

    // Ride tracking
    private var minDistToAlight = Double.MAX_VALUE
    private var offCount = 0
    private var wrongWayCount = 0
    private var offRoute: String? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start(now: Long = clock()) {
        if (phase != NavPhase.NOT_STARTED || plan.legs.isEmpty()) return
        startedAt = now
        enterLeg(0)
    }

    fun stop() {
        if (phase != NavPhase.ARRIVED) phase = NavPhase.STOPPED
    }

    val ended: Boolean get() = phase == NavPhase.ARRIVED || phase == NavPhase.STOPPED

    private fun enterLeg(index: Int) {
        legIndex = index
        val leg = plan.legs[index]
        stopIndex = -1
        estimated = false
        walkStartM = null
        walkMinM = Double.MAX_VALUE
        walkLastM = null
        walkStepIndex = 0
        if (walkStepsLeg != index) walkSteps = emptyList()
        minDistToAlight = Double.MAX_VALUE
        offCount = 0
        wrongWayCount = 0
        offRoute = null
        phase = if (leg.mode == NavMode.WALK) NavPhase.WALKING else NavPhase.WAITING
        if (phase == NavPhase.WALKING) reached = null
    }

    // ── Inputs ────────────────────────────────────────────────────────────────

    fun onFix(lat: Double, lon: Double, accuracyM: Float = 0f, now: Long = clock()) {
        lastLat = lat; lastLon = lon; lastFixAt = now
        if (ended || phase == NavPhase.NOT_STARTED) return
        if (accuracyM > MAX_ACCURACY_M) return

        if (jumpAhead(lat, lon, now)) return

        when (phase) {
            NavPhase.WALKING -> walkFix(lat, lon, now)
            NavPhase.WAITING -> waitFix(currentLeg(), lat, lon, now)
            NavPhase.RIDING -> rideFix(lat, lon, now)
            NavPhase.REACHED_STOP -> reachedFix(lat, lon, now)
            else -> Unit
        }
    }

    /**
     * The clock's turn. Only a train/metro leg with a known service can be
     * moved without a fix — and only while there is no fresh fix to say
     * otherwise.
     */
    fun onTick(now: Long = clock()) {
        if (ended || phase == NavPhase.NOT_STARTED) return
        if (hasFreshFix(now)) return
        val leg = currentLeg()
        if (!leg.hasLockedService) return
        val fraction = scheduleFraction(leg.serviceDepMins!!, leg.serviceArrMins!!, now) ?: return
        when (phase) {
            NavPhase.WAITING -> if (fraction > 0.0) {
                board(0, now, bySchedule = true)
                advanceOnSchedule(leg, fraction, now)
            }
            NavPhase.RIDING -> if (estimated || !hasFreshFix(now)) advanceOnSchedule(leg, fraction, now)
            else -> Unit
        }
    }

    fun perform(action: NavAction, now: Long = clock()) {
        when (action) {
            NavAction.BOARDED -> when (phase) {
                NavPhase.WAITING -> board(0, now, bySchedule = false)
                NavPhase.REACHED_STOP -> nextLeg()?.takeIf { it.mode.isTransit }?.let {
                    enterLeg(legIndex + 1); board(0, now, bySchedule = false)
                }
                else -> Unit
            }
            NavAction.MANUAL_ARRIVE -> if (phase == NavPhase.WALKING) completeWalk(now)
            NavAction.MANUAL_ALIGHT -> if (phase == NavPhase.RIDING || phase == NavPhase.WAITING) {
                stopIndex = currentLeg().stops.lastIndex
                reachAlight(now)
            }
            NavAction.CONTINUE -> when (phase) {
                NavPhase.REACHED_STOP -> if (legIndex < plan.legs.lastIndex) enterLeg(legIndex + 1)
                NavPhase.WALKING -> completeWalk(now)
                NavPhase.WAITING -> board(0, now, bySchedule = false)
                else -> Unit
            }
            NavAction.FINISH -> if (phase != NavPhase.ARRIVED) phase = NavPhase.STOPPED
        }
    }

    /** The rider picked (or corrected) the train/metro on [index]. */
    fun lockService(
        index: Int,
        label: String,
        depMins: Int?,
        arrMins: Int?,
        terminus: String?,
        trainNo: String?
    ) {
        val legs = plan.legs.toMutableList()
        val leg = legs.getOrNull(index) ?: return
        if (!leg.mode.isRail) return
        legs[index] = leg.copy(
            serviceLabel = label,
            serviceDepMins = depMins,
            serviceArrMins = arrMins,
            serviceTerminus = terminus,
            serviceTrainNo = trainNo
        )
        plan = plan.copy(legs = legs)
    }

    /**
     * The rider chose to cover walk leg [index] on foot, by auto or by cab.
     * GPS still tracks it the same way — only the copy, timing and map change.
     */
    fun chooseTravel(index: Int, mode: LastMileMode) {
        val legs = plan.legs.toMutableList()
        val leg = legs.getOrNull(index) ?: return
        if (leg.mode != NavMode.WALK) return
        val option = leg.lastMile.firstOrNull { it.mode == mode } ?: return
        if (leg.travelBy == mode) return
        legs[index] = leg.copy(travelBy = mode, plannedMin = option.minutes)
        plan = plan.copy(legs = legs)
        if (index == legIndex) {
            offCount = 0
            offRoute = null
            walkMinM = walkLastM ?: walkMinM
        }
        // Walking directions don't apply to a ride; drop them so they aren't shown.
        if (mode.byRoad && walkStepsLeg == index) { walkSteps = emptyList(); walkStepsLeg = -1 }
    }

    /** A walked path for [index] from Directions. Replaces the straight-line estimate. */
    fun setWalkSteps(index: Int, steps: List<WalkStep>) {
        walkStepsLeg = index
        walkSteps = steps
        walkStepIndex = 0
    }

    fun hasWalkSteps(index: Int): Boolean = walkStepsLeg == index && walkSteps.isNotEmpty()

    // ── Walking ───────────────────────────────────────────────────────────────

    private fun walkFix(lat: Double, lon: Double, now: Long) {
        val leg = currentLeg()
        val target = leg.stops.last()
        if (!target.placed) return
        val d = distM(lat, lon, target.lat!!, target.lon!!)

        if (walkStartM == null) walkStartM = max(d, 1.0)
        walkLastM = d
        if (d < walkMinM) walkMinM = d

        if (d <= arriveRadiusM(legIndex)) { completeWalk(now); return }

        // Directions steps: a step is done once we are close to where it ends.
        if (!leg.byRoad && walkSteps.isNotEmpty() && walkStepsLeg == legIndex) {
            for (i in walkStepIndex until walkSteps.size) {
                val s = walkSteps[i]
                if (distM(lat, lon, s.endLat, s.endLon) <= 30.0) walkStepIndex = (i + 1).coerceAtMost(walkSteps.lastIndex)
            }
        }

        // Off route: clearly further from the target than the closest we got.
        // An auto or cab takes the driver's road, so allow it a wider berth.
        val slack = if (leg.byRoad) max(500.0, 0.5 * (walkStartM ?: 0.0))
                    else max(150.0, 0.25 * (walkStartM ?: 0.0))
        if (d > walkMinM + slack) {
            offCount++
            if (offCount >= 2) offRoute = if (leg.byRoad)
                "You seem to be moving away from ${target.name} — check the way with your driver"
            else "You appear to be off route — head back towards ${target.name}"
        } else if (d <= walkMinM + 60.0) {
            offCount = 0
            offRoute = null
        }
    }

    private fun completeWalk(now: Long) {
        val leg = currentLeg()
        if (legIndex >= plan.legs.lastIndex) { arrive(now); return }
        val arrivedAt = leg.to
        enterLeg(legIndex + 1)
        reached = arrivedAt
        reachedStop = leg.stops.last()
    }

    // ── Waiting / boarding ────────────────────────────────────────────────────

    private fun waitFix(leg: NavLeg, lat: Double, lon: Double, now: Long): Boolean {
        if (!leg.mode.isTransit) return false
        val nearest = nearestStop(leg, lat, lon) ?: return false
        val (idx, m) = nearest
        if (idx >= 1 && m <= atStopM(leg.mode)) { board(idx, now, false); return true }
        val board = leg.stops.first()
        if (board.placed) {
            val away = distM(lat, lon, board.lat!!, board.lon!!)
            if (away > boardMoveM(leg.mode) && idx >= 1 && m <= corridorM(leg.mode)) {
                board(idx, now, false); return true
            }
        }
        return false
    }

    private fun board(idx: Int, now: Long, bySchedule: Boolean) {
        phase = NavPhase.RIDING
        stopIndex = max(stopIndex, idx.coerceAtLeast(0))
        estimated = bySchedule
        reached = null
        offCount = 0
        offRoute = null
    }

    // ── Riding ────────────────────────────────────────────────────────────────

    private fun rideFix(lat: Double, lon: Double, now: Long) {
        val leg = currentLeg()
        estimated = false
        val nearest = nearestStop(leg, lat, lon) ?: return
        val (idx, m) = nearest

        if (m > corridorM(leg.mode)) {
            offCount++
            if (offCount >= 2) offRoute = if (leg.mode == NavMode.BUS)
                "You don't appear to be on the ${leg.displayName} route"
            else "You appear to be off route"
        } else {
            offCount = 0
            if (offRoute?.startsWith("You may have missed") != true) offRoute = null
        }

        if (m <= atStopM(leg.mode)) {
            if (idx > stopIndex) { stopIndex = idx; wrongWayCount = 0 }
            else if (leg.mode.isRail && idx < stopIndex - 1) {
                wrongWayCount++
                if (wrongWayCount >= 2) offRoute = "You seem to be heading the wrong way"
            }
        }

        val alight = leg.stops.last()
        if (alight.placed) {
            val toEnd = distM(lat, lon, alight.lat!!, alight.lon!!)
            if (toEnd <= atStopM(leg.mode)) { reachAlight(now); return }
            if (toEnd < minDistToAlight) minDistToAlight = toEnd
            if (minDistToAlight < approachM(leg.mode) &&
                toEnd > minDistToAlight + 2 * atStopM(leg.mode) &&
                stopIndex >= leg.stops.lastIndex - 1
            ) offRoute = "You may have missed ${leg.to}"
        }
    }

    private fun advanceOnSchedule(leg: NavLeg, fraction: Double, now: Long) {
        estimated = true
        val target = ((leg.stops.size - 1) * fraction).roundToInt()
        if (target > stopIndex) stopIndex = target
        if (fraction >= 1.0) reachAlight(now)
    }

    private fun reachAlight(now: Long) {
        val leg = currentLeg()
        stopIndex = leg.stops.lastIndex
        offRoute = null
        if (legIndex >= plan.legs.lastIndex) { arrive(now); return }
        phase = NavPhase.REACHED_STOP
        reached = leg.to
        reachedStop = leg.stops.last()
    }

    private fun reachedFix(lat: Double, lon: Double, now: Long) {
        val next = nextLeg() ?: return
        if (next.mode == NavMode.WALK) {
            val at = reachedStop
            if (at != null && at.placed && distM(lat, lon, at.lat!!, at.lon!!) > 120.0) {
                enterLeg(legIndex + 1)
                walkFix(lat, lon, now)
            }
        } else if (waitFixPeek(next, lat, lon)) {
            enterLeg(legIndex + 1)
            waitFix(next, lat, lon, now)
        }
    }

    /** Would [waitFix] board on this fix? Without changing state. */
    private fun waitFixPeek(leg: NavLeg, lat: Double, lon: Double): Boolean {
        val (idx, m) = nearestStop(leg, lat, lon) ?: return false
        if (idx >= 1 && m <= atStopM(leg.mode)) return true
        val board = leg.stops.first()
        if (!board.placed) return false
        return distM(lat, lon, board.lat!!, board.lon!!) > boardMoveM(leg.mode) &&
            idx >= 1 && m <= corridorM(leg.mode)
    }

    private fun arrive(now: Long) {
        phase = NavPhase.ARRIVED
        arrivedAt = now
        offRoute = null
        reached = plan.destLabel
    }

    /**
     * Catches a rider who is plainly further along than the screen thinks —
     * didn't tap "I've boarded", GPS missed a station, took the transfer
     * without looking. Only ever moves forward, and only on a clear margin, so
     * a stop shared by two legs doesn't skip one.
     */
    private fun jumpAhead(lat: Double, lon: Double, now: Long): Boolean {
        val current = currentLeg()
        val currentEnd = current.stops.last()
        val hereToCurrentEnd = if (currentEnd.placed)
            distM(lat, lon, currentEnd.lat!!, currentEnd.lon!!) else Double.MAX_VALUE

        for (i in legIndex + 1..plan.legs.lastIndex) {
            val ahead = plan.legs[i]
            val end = ahead.stops.last()
            if (!end.placed) continue
            val d = distM(lat, lon, end.lat!!, end.lon!!)
            val radius = if (ahead.mode == NavMode.WALK) arriveRadiusM(i) else atStopM(ahead.mode)
            if (d <= radius && d + 300.0 < hereToCurrentEnd) {
                if (i == plan.legs.lastIndex) { legIndex = i; stopIndex = ahead.stops.lastIndex; arrive(now); return true }
                enterLeg(i)
                if (ahead.mode == NavMode.WALK) completeWalk(now)
                else { board(ahead.stops.lastIndex, now, false); reachAlight(now) }
                return true
            }
            // Mid-ride on a later transit leg.
            if (ahead.mode.isTransit) {
                val (idx, m) = nearestStop(ahead, lat, lon) ?: continue
                // A bus stop is a few hundred metres from the next; one is not enough to call it.
                val minIdx = if (ahead.mode == NavMode.BUS) 2 else 1
                if (idx in minIdx until ahead.stops.lastIndex && m <= atStopM(ahead.mode) &&
                    m + 300.0 < hereToCurrentEnd && !sharesStop(current, ahead.stops[idx])
                ) {
                    enterLeg(i); board(idx, now, false); return true
                }
            }
        }
        return false
    }

    private fun sharesStop(leg: NavLeg, stop: NavStop): Boolean =
        leg.stops.any { it.name.equals(stop.name, true) }

    // ── Output ────────────────────────────────────────────────────────────────

    fun snapshot(now: Long = clock()): NavSnapshot {
        val leg = plan.legs.getOrNull(legIndex)
        val next = nextLeg()
        val state = state()
        val fresh = hasFreshFix(now)
        val source = when {
            phase == NavPhase.RIDING && estimated -> PositionSource.SCHEDULE
            fresh -> PositionSource.GPS
            else -> PositionSource.NONE
        }

        val walk = if (leg != null && phase == NavPhase.WALKING) walkProgress(leg) else null
        val ride = if (leg != null && leg.mode.isTransit &&
            (phase == NavPhase.WAITING || phase == NavPhase.RIDING)) rideProgress(leg) else null

        val copy = copyFor(state, leg, next, walk, ride)

        return NavSnapshot(
            state = state,
            phase = phase,
            legIndex = legIndex,
            leg = leg,
            nextLeg = next,
            plan = plan,
            toolbarTitle = copy.toolbar,
            eyebrow = copy.eyebrow,
            headline = copy.headline,
            detail = copy.detail,
            primary = copy.primary,
            primaryLabel = copy.primaryLabel,
            fallback = copy.fallback,
            fallbackLabel = copy.fallbackLabel,
            walk = walk,
            ride = ride,
            reached = reached,
            offRoute = offRoute,
            timeline = timeline(),
            journeyPct = journeyPct(),
            etaEpochMs = eta(now),
            source = source,
            lastLat = lastLat,
            lastLon = lastLon,
            startedAt = startedAt,
            arrivedAt = arrivedAt
        )
    }

    fun state(): NavState {
        val leg = plan.legs.getOrNull(legIndex) ?: return NavState.NOT_STARTED
        return when (phase) {
            NavPhase.NOT_STARTED -> NavState.NOT_STARTED
            NavPhase.ARRIVED -> NavState.ARRIVED
            NavPhase.STOPPED -> NavState.STOPPED
            NavPhase.REACHED_STOP -> NavState.REACHED_STOP
            NavPhase.WALKING -> when {
                legIndex == plan.legs.lastIndex -> NavState.WALKING_TO_DESTINATION
                legIndex == 0 -> NavState.WALKING_TO_STOP
                else -> NavState.WALKING_TO_TRANSFER
            }
            NavPhase.WAITING -> when (leg.mode) {
                NavMode.TRAIN -> NavState.WAITING_FOR_TRAIN
                NavMode.METRO -> NavState.WAITING_FOR_METRO
                NavMode.BUS -> NavState.WAITING_FOR_BUS
                NavMode.WALK -> NavState.WALKING_TO_STOP
            }
            NavPhase.RIDING -> when (leg.mode) {
                NavMode.TRAIN -> NavState.ON_TRAIN
                NavMode.METRO -> NavState.ON_METRO
                NavMode.BUS -> NavState.ON_BUS
                NavMode.WALK -> NavState.WALKING_TO_STOP
            }
        }
    }

    private fun walkProgress(leg: NavLeg): WalkProgress {
        val target = leg.stops.last()
        val lat = lastLat; val lon = lastLon
        val straight = if (lat != null && lon != null && target.placed)
            distM(lat, lon, target.lat!!, target.lon!!) else null

        val steps = if (walkStepsLeg == legIndex && !leg.byRoad) walkSteps else emptyList()
        val remainingM: Double? = when {
            steps.isNotEmpty() && lat != null && lon != null -> {
                val s = steps[walkStepIndex]
                distM(lat, lon, s.endLat, s.endLon) + steps.drop(walkStepIndex + 1).sumOf { it.distanceM }
            }
            straight != null -> straight * WALK_DETOUR
            leg.distanceKm > 0 -> leg.distanceKm * 1000 * WALK_DETOUR
            else -> null
        }
        val pace = when (leg.travelBy) {
            LastMileMode.AUTO -> AUTO_M_PER_MIN
            LastMileMode.CAB -> CAB_M_PER_MIN
            LastMileMode.WALK -> WALK_M_PER_MIN
        }
        val remainingMin = remainingM?.let { max(1, (it / pace).roundToInt()) } ?: leg.plannedMin
        val start = walkStartM
        val pct = if (start != null && straight != null)
            (((start - straight) / start) * 100).roundToInt().coerceIn(0, 100) else 0

        val (instruction, instrDist, maneuver) = when {
            leg.byRoad -> Triple(null, null, null)   // the driver navigates
            steps.isNotEmpty() && lat != null && lon != null -> {
                val s = steps[walkStepIndex]
                Triple(s.instruction, distM(lat, lon, s.endLat, s.endLon).roundToInt(), s.maneuver)
            }
            lat != null && lon != null && target.placed ->
                Triple("Head ${compass(bearing(lat, lon, target.lat!!, target.lon!!))}", straight?.roundToInt(), "")
            else -> Triple(null, null, null)
        }
        return WalkProgress(
            targetName = target.name,
            remainingM = remainingM?.roundToInt(),
            remainingMin = remainingMin,
            progressPct = pct,
            instruction = instruction,
            instructionDistanceM = instrDist,
            maneuver = maneuver,
            straightLine = steps.isEmpty()
        )
    }

    private fun rideProgress(leg: NavLeg): RideProgress {
        val onBoard = phase == NavPhase.RIDING
        val reachedIdx = if (onBoard) stopIndex.coerceAtLeast(0) else -1
        val rows = leg.stops.mapIndexed { i, s ->
            val status = when {
                !onBoard -> if (i == 0) StopStatus.NEXT else StopStatus.UPCOMING
                i <= reachedIdx -> StopStatus.DONE
                i == reachedIdx + 1 -> StopStatus.NEXT
                else -> StopStatus.UPCOMING
            }
            StopRow(s.name, status, isBoarding = i == 0, isAlighting = i == leg.stops.lastIndex)
        }
        val nextIdx = if (onBoard) reachedIdx + 1 else 0
        val remaining = if (onBoard) leg.stops.lastIndex - reachedIdx else leg.stops.lastIndex
        val approaching = onBoard && (nextIdx == leg.stops.lastIndex ||
            (minDistToAlight < approachM(leg.mode) && remaining <= 2))
        return RideProgress(
            stops = rows,
            reachedIndex = reachedIdx,
            nextStop = leg.stops.getOrNull(nextIdx)?.name,
            stopsRemaining = remaining.coerceAtLeast(0),
            approaching = approaching,
            onBoard = onBoard,
            estimated = estimated
        )
    }

    private data class Copy(
        val toolbar: String, val eyebrow: String, val headline: String, val detail: String,
        val primary: NavAction? = null, val primaryLabel: String? = null,
        val fallback: NavAction? = null, val fallbackLabel: String? = null
    )

    private fun copyFor(
        state: NavState, leg: NavLeg?, next: NavLeg?,
        walk: WalkProgress?, ride: RideProgress?
    ): Copy {
        if (leg == null) return Copy("Navigation", "", "", "")
        return when (state) {
            NavState.NOT_STARTED -> Copy("Navigation", "READY", "Start your journey", "")

            NavState.WALKING_TO_STOP, NavState.WALKING_TO_TRANSFER, NavState.WALKING_TO_DESTINATION -> {
                val target = if (state == NavState.WALKING_TO_DESTINATION) plan.destLabel.ifBlank { leg.to } else leg.to
                val road = leg.byRoad
                val meta = buildString {
                    append("${if (road) "~" else ""}${walk?.remainingMin ?: leg.plannedMin} min")
                    walk?.remainingM?.let { append(" · ~${formatDistance(it)}") }
                    if (road) leg.chosenOption?.let { append(" · ~₹${it.fareInr}") }
                    if (state != NavState.WALKING_TO_DESTINATION && next != null && next.mode.isTransit)
                        append(" · then board ${next.displayName}")
                }
                val how = leg.travelBy.label.uppercase()
                Copy(
                    toolbar = "${leg.travelBy.label} to $target",
                    eyebrow = when (state) {
                        NavState.WALKING_TO_TRANSFER -> "CHANGE · $how"
                        NavState.WALKING_TO_DESTINATION -> if (road) "LAST STRETCH · $how" else "LAST STRETCH"
                        else -> how
                    },
                    headline = if (road) "${leg.travelBy.action} to $target" else "Walk to $target",
                    detail = meta,
                    fallback = NavAction.MANUAL_ARRIVE,
                    fallbackLabel = "I'm at $target"
                )
            }

            NavState.WAITING_FOR_TRAIN, NavState.WAITING_FOR_METRO -> {
                val detail = buildString {
                    append(leg.mode.noun.replaceFirstChar { it.uppercase() })
                    leg.serviceTerminus?.takeIf { it.isNotBlank() }?.let { append(" towards ${it.uppercase()}") }
                    leg.serviceDepMins?.let { append(" · ${hhmm(it)}") }
                    append(" · get off at ${leg.to}")
                }
                Copy(
                    toolbar = reached?.let { "You've reached $it" } ?: "Board at ${leg.from}",
                    eyebrow = "BOARD AT ${leg.from.uppercase()}",
                    headline = "Board ${leg.displayName}",
                    detail = detail,
                    primary = NavAction.BOARDED,
                    primaryLabel = "I've boarded",
                    fallback = NavAction.MANUAL_ALIGHT,
                    fallbackLabel = "I've already got off at ${leg.to}"
                )
            }

            NavState.WAITING_FOR_BUS -> Copy(
                toolbar = reached?.let { "You've reached $it" } ?: "Board at ${leg.from}",
                eyebrow = "BOARD AT ${leg.from.uppercase()}",
                headline = "Board ${leg.displayName}",
                detail = "${leg.from} → ${leg.to} · ${leg.stops.lastIndex} stops",
                primary = NavAction.BOARDED,
                primaryLabel = "I'm on the bus",
                fallback = NavAction.MANUAL_ALIGHT,
                fallbackLabel = "I've already got off at ${leg.to}"
            )

            NavState.ON_TRAIN, NavState.ON_METRO, NavState.ON_BUS -> {
                val r = ride!!
                val headline = if (r.approaching) "Get ready to get off at ${leg.to}"
                               else "Next stop: ${r.nextStop ?: leg.to}"
                val detail = when (r.stopsRemaining) {
                    0 -> "Get off here"
                    1 -> "1 stop to ${leg.to}"
                    else -> "${r.stopsRemaining} stops to ${leg.to}"
                }
                Copy(
                    toolbar = "On your journey",
                    eyebrow = leg.displayName.uppercase(),
                    headline = headline,
                    detail = detail,
                    fallback = NavAction.MANUAL_ALIGHT,
                    fallbackLabel = "I've got off at ${leg.to}"
                )
            }

            NavState.REACHED_STOP -> {
                val laterRide = plan.legs.drop(legIndex + 1).firstOrNull { it.mode.isTransit }
                val walkNext = next?.mode == NavMode.WALK
                val how = next?.travelBy ?: LastMileMode.WALK
                val headline = when {
                    next == null -> "You've arrived"
                    walkNext && legIndex + 1 == plan.legs.lastIndex ->
                        "${if (how.byRoad) how.action else "Walk"} to ${plan.destLabel.ifBlank { next.to }}"
                    laterRide != null -> "Change to ${laterRide.displayName}"
                    else -> "Continue"
                }
                val detail = when {
                    next == null -> ""
                    walkNext && legIndex + 1 == plan.legs.lastIndex ->
                        if (how.byRoad) "~${next.plannedMin} min by ${how.label.lowercase()}" else "${next.plannedMin} min on foot"
                    walkNext -> "${how.label} to ${next.to} · ${if (how.byRoad) "~" else ""}${next.plannedMin} min"
                    else -> "Board at ${next.from}"
                }
                Copy(
                    toolbar = "You've reached ${leg.to}",
                    eyebrow = "YOU'VE REACHED ${leg.to.uppercase()}",
                    headline = headline,
                    detail = detail,
                    primary = NavAction.CONTINUE,
                    primaryLabel = when {
                        walkNext && how == LastMileMode.AUTO -> "Take Auto"
                        walkNext && how == LastMileMode.CAB -> "Take Cab"
                        walkNext -> "Start Walking"
                        else -> "Continue Journey"
                    }
                )
            }

            NavState.ARRIVED -> Copy(
                toolbar = "You've arrived",
                eyebrow = "ARRIVED",
                headline = "You've arrived",
                detail = plan.destLabel,
                primary = NavAction.FINISH,
                primaryLabel = "Done"
            )

            NavState.STOPPED -> Copy("Navigation stopped", "STOPPED", "Navigation stopped", "")
        }
    }

    /** The compact whole-journey timeline. */
    fun timeline(): List<TimelineStep> {
        val steps = mutableListOf<TimelineStep>()
        plan.legs.forEachIndexed { i, leg ->
            if (leg.mode == NavMode.WALK) {
                val target = if (i == plan.legs.lastIndex) plan.destLabel.ifBlank { leg.to } else leg.to
                steps += TimelineStep(i, StepKind.WALK, leg.mode, "${leg.travelBy.label} to $target",
                    "${if (leg.byRoad) "~" else ""}${leg.plannedMin} min", leg.colorHex, StepStatus.UPCOMING)
            } else {
                steps += TimelineStep(i, StepKind.BOARD, leg.mode, "Board ${leg.displayName}",
                    "at ${leg.from}", leg.colorHex, StepStatus.UPCOMING)
                steps += TimelineStep(i, StepKind.ALIGHT, leg.mode, "Get off at ${leg.to}",
                    "${leg.stops.lastIndex} stops", leg.colorHex, StepStatus.UPCOMING)
            }
        }
        val current: Int = when (phase) {
            NavPhase.NOT_STARTED -> 0
            NavPhase.ARRIVED, NavPhase.STOPPED -> steps.size
            NavPhase.WALKING -> steps.indexOfFirst { it.legIndex == legIndex && it.kind == StepKind.WALK }
            NavPhase.WAITING -> steps.indexOfFirst { it.legIndex == legIndex && it.kind == StepKind.BOARD }
            NavPhase.RIDING -> steps.indexOfFirst { it.legIndex == legIndex && it.kind == StepKind.ALIGHT }
            NavPhase.REACHED_STOP -> steps.indexOfFirst { it.legIndex > legIndex }.let { if (it < 0) steps.size else it }
        }.let { if (it < 0) 0 else it }
        return steps.mapIndexed { i, s ->
            s.copy(status = when {
                i < current -> StepStatus.DONE
                i == current -> StepStatus.CURRENT
                else -> StepStatus.UPCOMING
            })
        }
    }

    fun journeyPct(): Int {
        val legs = plan.legs
        val total = legs.sumOf { it.plannedMin.coerceAtLeast(1) }.toDouble()
        if (phase == NavPhase.ARRIVED) return 100
        val done = legs.take(legIndex).sumOf { it.plannedMin.coerceAtLeast(1) }
        val leg = legs.getOrNull(legIndex) ?: return 0
        val within = when (phase) {
            NavPhase.WALKING -> {
                val start = walkStartM; val last = walkLastM
                if (start != null && last != null) ((start - last) / start).coerceIn(0.0, 1.0) else 0.0
            }
            NavPhase.RIDING -> if (leg.stops.size > 1) stopIndex.coerceAtLeast(0).toDouble() / (leg.stops.size - 1) else 0.0
            NavPhase.REACHED_STOP -> 1.0
            else -> 0.0
        }
        return (((done + within * leg.plannedMin.coerceAtLeast(1)) / total) * 100).roundToInt().coerceIn(0, 100)
    }

    fun eta(now: Long): Long {
        val leg = plan.legs.getOrNull(legIndex) ?: return now
        if (phase == NavPhase.ARRIVED) return arrivedAt ?: now
        val later = plan.legs.drop(legIndex + 1).sumOf { it.plannedMin }
        val thisLeg: Int = when (phase) {
            NavPhase.WALKING -> walkProgress(leg).remainingMin
            NavPhase.REACHED_STOP -> 0
            NavPhase.WAITING, NavPhase.RIDING -> {
                val arr = leg.serviceArrMins
                val fromClock = if (leg.hasLockedService && arr != null) minutesUntil(arr, now) else null
                if (fromClock != null && fromClock in 0..300) fromClock
                else {
                    val frac = if (phase == NavPhase.RIDING && leg.stops.size > 1)
                        1.0 - stopIndex.coerceAtLeast(0).toDouble() / (leg.stops.size - 1) else 1.0
                    (leg.plannedMin * frac).roundToInt()
                }
            }
            else -> leg.plannedMin
        }
        return now + (thisLeg + later) * 60_000L
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentLeg(): NavLeg = plan.legs[legIndex]
    private fun nextLeg(): NavLeg? = plan.legs.getOrNull(legIndex + 1)

    private fun hasFreshFix(now: Long) = lastFixAt > 0 && now - lastFixAt <= STALE_FIX_MS

    /** How close counts as "there" at the end of walk leg [index]. */
    private fun arriveRadiusM(index: Int): Double {
        if (index >= plan.legs.lastIndex) return 50.0
        return when (plan.legs.getOrNull(index + 1)?.mode) {
            NavMode.TRAIN -> 150.0      // station centroid; platforms are long
            NavMode.METRO -> 120.0
            NavMode.BUS -> 60.0
            else -> 60.0
        }
    }

    private fun atStopM(mode: NavMode) = when (mode) {
        NavMode.TRAIN -> 450.0; NavMode.METRO -> 350.0; NavMode.BUS -> 120.0; NavMode.WALK -> 60.0
    }
    private fun corridorM(mode: NavMode) = when (mode) {
        NavMode.TRAIN -> 1500.0; NavMode.METRO -> 1200.0; NavMode.BUS -> 600.0; NavMode.WALK -> 300.0
    }
    private fun boardMoveM(mode: NavMode) = when (mode) {
        NavMode.TRAIN -> 700.0; NavMode.METRO -> 600.0; NavMode.BUS -> 300.0; NavMode.WALK -> 100.0
    }
    private fun approachM(mode: NavMode) = when (mode) {
        NavMode.TRAIN -> 1500.0; NavMode.METRO -> 1000.0; NavMode.BUS -> 500.0; NavMode.WALK -> 100.0
    }

    private fun nearestStop(leg: NavLeg, lat: Double, lon: Double): Pair<Int, Double>? {
        var best = -1; var bestM = Double.MAX_VALUE
        leg.stops.forEachIndexed { i, s ->
            if (!s.placed) return@forEachIndexed
            val m = distM(lat, lon, s.lat!!, s.lon!!)
            if (m < bestM) { bestM = m; best = i }
        }
        return if (best < 0) null else best to bestM
    }

    private fun scheduleFraction(dep: Int, arr: Int, now: Long): Double? {
        val nowMins = clockMins(now)
        val span = ((arr - dep) % 1440 + 1440) % 1440
        if (span <= 0 || span > 300) return null
        val elapsed = ((nowMins - dep) % 1440 + 1440) % 1440
        if (elapsed > span + 120) return null
        return (elapsed.toDouble() / span).coerceIn(0.0, 1.0)
    }

    private fun minutesUntil(target: Int, now: Long): Int {
        val diff = ((target - clockMins(now)) % 1440 + 1440) % 1440
        return if (diff > 720) diff - 1440 else diff
    }

    private fun clockMins(at: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }
}

// ── Geometry & formatting, shared with the UI ────────────────────────────────

fun distM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
    val a = sin(Math.toRadians(lat2 - lat1) / 2).pow(2) +
        cos(p1) * cos(p2) * sin(Math.toRadians(lon2 - lon1) / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
    val dl = Math.toRadians(lon2 - lon1)
    val y = sin(dl) * cos(p2)
    val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

fun compass(deg: Double): String {
    val names = listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
    return names[((deg + 22.5) / 45).toInt() % 8]
}

fun formatDistance(m: Int): String = when {
    m < 1000 -> "${((m + 5) / 10) * 10} m"
    else -> "%.1f km".format(m / 1000.0)
}

fun hhmm(mins: Int): String {
    val m = ((mins % 1440) + 1440) % 1440
    return "%02d:%02d".format(m / 60, m % 60)
}
