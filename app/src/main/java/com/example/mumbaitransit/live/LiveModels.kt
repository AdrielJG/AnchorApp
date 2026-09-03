package com.example.mumbaitransit.live

/**
 * Parsed shape of RailRadar's `/v1/trains/{no}/live` response.
 *
 * Every field is nullable on purpose. The API returns different subsets depending
 * on whether a train is actually being tracked or is merely scheduled, so the UI
 * is built to degrade rather than crash on a missing key.
 */
data class LiveTrainStatus(
    val trainNumber: String,
    val trainName: String?,
    val sourceCode: String?,
    val sourceName: String?,
    val destCode: String?,
    val destName: String?,

    /** True only when RailRadar has a real position feed, not a schedule replay. */
    val isLive: Boolean,
    /** e.g. "real-time", "schedule". Shown verbatim so we never overstate accuracy. */
    val trackingMode: String?,
    val statusText: String?,
    /** Epoch millis, or null if the response carried no usable timestamp. */
    val lastUpdatedAt: Long?,

    val current: CurrentPosition?,
    val previousHalt: Halt?,
    val nextHalt: Halt?,
    val route: List<RouteStop>
) {
    /** Delay in minutes, preferring the live position over the route table. */
    val delayMinutes: Int?
        get() = current?.delayMinutes
            ?: route.firstOrNull { it.status == StopStatus.CURRENT }?.delayDeparture

    /**
     * Whether the position is a genuine sighting. When false the times shown are
     * projections and the screen says so instead of dressing them up as live.
     */
    val isActualPosition: Boolean
        get() = current?.isActualPosition == true

    val headerTitle: String
        get() = if (trainName.isNullOrBlank()) trainNumber else "$trainNumber · $trainName"
}

data class CurrentPosition(
    val stationCode: String?,
    val stationName: String?,
    /** Raw API status, e.g. "AT_STATION", "DEPARTED", "RUNNING". */
    val status: String?,
    val isActualPosition: Boolean?,
    val delayMinutes: Int?
)

/** A previous / next halt reference, which the API may send as an object or a bare code. */
data class Halt(
    val stationCode: String?,
    val stationName: String?,
    /** Distance to this halt from the train's current position, in km. */
    val distanceKm: Double?,
    val scheduledTime: String?
)

enum class StopStatus { DEPARTED, CURRENT, UPCOMING, SKIPPED, UNKNOWN }

data class RouteStop(
    val sequence: Int,
    val stationCode: String,
    val stationName: String,
    val status: StopStatus,
    val scheduledArrival: String?,
    val scheduledDeparture: String?,
    val actualArrival: String?,
    val actualDeparture: String?,
    val delayArrival: Int?,
    val delayDeparture: Int?,
    val platform: String?,
    val distanceKm: Double?
) {
    /** The single most relevant delay for a one-line summary. */
    val delay: Int?
        get() = when (status) {
            StopStatus.DEPARTED -> delayDeparture ?: delayArrival
            else -> delayArrival ?: delayDeparture
        }

    /** True when the actual/expected time differs from the booked time. */
    val hasActual: Boolean
        get() = !actualArrival.isNullOrBlank() || !actualDeparture.isNullOrBlank()
}

/** Outcome of a live-status request. */
sealed class LiveResult {
    data class Success(val status: LiveTrainStatus, val fromCache: Boolean = false) : LiveResult()

    /**
     * @param message  user-facing, already written in the app's voice
     * @param retryable whether a manual retry is worth offering
     */
    data class Error(val message: String, val retryable: Boolean = true) : LiveResult()
}
