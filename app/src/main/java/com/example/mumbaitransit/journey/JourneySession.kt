package com.example.mumbaitransit.journey

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The journey this phone is sharing, if any.
 *
 * The tracking service owns the work; this is the window the control screen
 * looks through. Keeping it here rather than binding to the service means the
 * screen can be closed and reopened — or reached from the notification after
 * the app was swept out of recents — and still find the journey running.
 *
 * Only one journey can be shared at a time. Sharing two would mean two
 * notifications, two location subscriptions and a rider who cannot tell which
 * link went to whom.
 */
object JourneySession {

    private val _journey = MutableStateFlow<LiveJourney?>(null)
    val journey: StateFlow<LiveJourney?> = _journey.asStateFlow()

    private val _ping = MutableStateFlow<JourneyPing?>(null)
    val ping: StateFlow<JourneyPing?> = _ping.asStateFlow()

    val isSharing: Boolean get() = _journey.value != null

    val activeCode: String? get() = _journey.value?.code

    fun begin(journey: LiveJourney) {
        _journey.value = journey
        _ping.value = null
    }

    /** Rider changed which service they boarded on one leg. */
    fun updateLeg(index: Int, updated: JourneyLeg) {
        val current = _journey.value ?: return
        val legs = current.legs.toMutableList()
        if (index !in legs.indices) return
        legs[index] = updated.copy(index = index)
        _journey.value = current.copy(legs = legs)
    }

    fun publish(ping: JourneyPing) {
        _ping.value = ping
    }

    fun clear() {
        _journey.value = null
        _ping.value = null
    }
}
