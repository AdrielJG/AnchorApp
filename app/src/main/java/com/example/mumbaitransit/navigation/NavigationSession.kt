package com.example.mumbaitransit.navigation

import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.journey.LiveJourney
import com.example.mumbaitransit.model.RouteCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The route this phone is navigating, if any.
 *
 * Holds the [NavigationEngine]; the tracking service feeds it fixes and clock
 * ticks, the navigation screen feeds it taps, and both read [snapshot]. Kept
 * outside the service for the same reason [com.example.mumbaitransit.journey.JourneySession]
 * is: the screen can be closed and reopened — or reached from the notification —
 * and still find the journey running.
 *
 * [journey] is the leg list navigation was built from. When the rider starts
 * sharing from the navigation screen, sharing uses this same journey (same code,
 * same locked trains), which is how the two stay in step without either one
 * owning the other.
 */
object NavigationSession {

    private var engine: NavigationEngine? = null

    private val _snapshot = MutableStateFlow<NavSnapshot?>(null)
    val snapshot: StateFlow<NavSnapshot?> = _snapshot.asStateFlow()

    var card: RouteCard? = null
        private set

    var journey: LiveJourney? = null
        private set

    /** Navigating right now — started and not yet finished or stopped. */
    val isActive: Boolean get() = engine?.ended == false

    /** Something to show: running, or just arrived and not yet dismissed. */
    val hasSession: Boolean get() = engine != null

    val routeKey: String? get() = engine?.plan?.routeKey
    val code: String? get() = engine?.plan?.code

    @Synchronized
    fun begin(card: RouteCard, journey: LiveJourney, plan: NavPlan, now: Long = System.currentTimeMillis()) {
        this.card = card
        this.journey = journey
        engine = NavigationEngine(plan).also { it.start(now) }
        publish(now)
    }

    @Synchronized
    fun onFix(lat: Double, lon: Double, accuracyM: Float, now: Long = System.currentTimeMillis()) {
        val e = engine ?: return
        if (e.ended) return
        e.onFix(lat, lon, accuracyM, now)
        publish(now)
    }

    @Synchronized
    fun onTick(now: Long = System.currentTimeMillis()) {
        val e = engine ?: return
        if (e.ended) return
        e.onTick(now)
        publish(now)
    }

    @Synchronized
    fun perform(action: NavAction, now: Long = System.currentTimeMillis()) {
        engine?.perform(action, now) ?: return
        publish(now)
    }

    /** Rider picked the train/metro they're on. Mirrors into [journey] so sharing can pick it up. */
    @Synchronized
    fun lockService(
        index: Int, label: String, depMins: Int?, arrMins: Int?,
        terminus: String?, trainNo: String?
    ) {
        val e = engine ?: return
        e.lockService(index, label, depMins, arrMins, terminus, trainNo)
        journey = journey?.let { j ->
            val legs = j.legs.toMutableList()
            legs.getOrNull(index)?.let {
                legs[index] = it.copy(
                    serviceLabel = label, serviceDepMins = depMins, serviceArrMins = arrMins,
                    serviceTerminus = terminus, serviceTrainNo = trainNo
                )
            }
            j.copy(legs = legs)
        }
        publish(System.currentTimeMillis())
    }

    /** Rider picked Walk / Auto / Cab for walk leg [index]. */
    @Synchronized
    fun chooseTravel(index: Int, mode: LastMileMode) {
        engine?.chooseTravel(index, mode) ?: return
        publish(System.currentTimeMillis())
    }

    @Synchronized
    fun setWalkSteps(index: Int, steps: List<WalkStep>) {
        engine?.setWalkSteps(index, steps) ?: return
        publish(System.currentTimeMillis())
    }

    @Synchronized
    fun hasWalkSteps(index: Int): Boolean = engine?.hasWalkSteps(index) == true

    /** Stops tracking; the last snapshot stays readable until [clear]. */
    @Synchronized
    fun stop() {
        engine?.stop() ?: return
        publish(System.currentTimeMillis())
    }

    @Synchronized
    fun clear() {
        engine = null
        card = null
        journey = null
        _snapshot.value = null
    }

    private fun publish(now: Long) {
        _snapshot.value = engine?.snapshot(now)
    }
}
