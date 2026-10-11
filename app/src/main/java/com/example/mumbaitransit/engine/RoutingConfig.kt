package com.example.mumbaitransit.engine

/**
 * What a pattern step is ridden on.
 *
 * RAIL covers the train and metro lines in the rail graph — one RAIL step may
 * change line inside the network, as a train-only route always could. BUS covers
 * the bus dataset.
 */
enum class RideMode { RAIL, BUS }

/**
 * The limits the journey planner works to, and the rules the route selector
 * chooses by, in one place so they can be tuned without touching either.
 */
object RoutingConfig {

    /** Walk between the origin or destination and a bus stop, km. */
    const val MAX_WALK_TO_BUS_STOP = 0.9

    /**
     * Walk between a bus stop and a rail or metro station when changing between
     * bus and train, km.
     *
     * Walking from the origin to the first station, and from the last station to
     * the destination, keeps the rule it always had: the Board at / Get off at
     * choice from Route Options, or the three nearest stations.
     */
    const val MAX_WALK_TO_STATION = 0.5

    /**
     * Most transit steps one journey may chain — the length of the longest
     * pattern searched. A train step that changes line inside the network, or a
     * bus-only trip that changes bus at a stop, counts once, exactly as those
     * routes were built before.
     */
    const val MAX_TRANSIT_LEGS = 3

    /**
     * How much nearer the destination each ride must end than it began, km,
     * measured along the ride itself — boarding point to alighting point. Zero
     * would mean "not farther away". Applies to journeys that combine modes;
     * train-only and bus-only routes are built exactly as before.
     */
    const val MIN_PROGRESS_KM = 0.5

    /**
     * How much nearer the destination a ride may pass than where it ends, km.
     * Beyond this the commuter would simply have got off earlier.
     */
    const val MAX_OVERSHOOT_KM = 0.5

    /**
     * The combinations generated. Each is a sequence of ride modes; the walks
     * before, between and after the rides are added by the planner. Adding a
     * combination is one line here — the planner has no per-pattern code.
     */
    val ROUTE_PATTERNS: List<List<RideMode>> = listOf(
        listOf(RideMode.RAIL),
        listOf(RideMode.BUS),
        listOf(RideMode.BUS, RideMode.RAIL),
        listOf(RideMode.RAIL, RideMode.BUS),
        listOf(RideMode.BUS, RideMode.RAIL, RideMode.BUS),
        listOf(RideMode.RAIL, RideMode.BUS, RideMode.RAIL)
    )

    // ── What the Routes screen shows (RouteSelector) ──────────────────────────
    //
    // The planner above generates every valid journey. The selector keeps only
    // the most convenient, meaningfully different ones for each filter.

    /** Most routes listed under one filter. Auto and cab are added on top in All. */
    const val MAX_ROUTES_SHOWN = 8

    // Convenience score — lower is better, in minutes. Door-to-door time as
    // shown on the card, plus the costs below. Never shown to the commuter.

    /** A walking minute counts as this many riding minutes. */
    const val WALK_WEIGHT = 2.0

    /** Cost of each change of vehicle, on top of the walk and wait it involves. */
    const val TRANSFER_PENALTY_MIN = 6.0

    /** Extra cost when the change is between bus and train or metro. */
    const val MODE_CHANGE_PENALTY_MIN = 2.0

    /** Minutes one rupee of fare is worth — ₹4 counts as one minute. */
    const val MINUTES_PER_RUPEE = 0.25

    /**
     * Bus times already include a flat wait at the stop; train times do not.
     * A train or metro boarding is charged half its line's headway from the
     * graph's frequency, within these bounds, so the two compare fairly.
     */
    const val RAIL_WAIT_DEFAULT_MIN = 4.0
    const val RAIL_WAIT_MAX_MIN = 10.0

    /**
     * Minutes per point of Mobility Reliability Index below 100, charged on a
     * journey's least reliable ride. Buses use the flat BEST figure from the
     * MRI table.
     */
    const val RELIABILITY_WEIGHT = 0.1
    const val BUS_RELIABILITY_PCT = 55.0

    /** Bus rides whose stop order was only partly reconstructed. */
    const val BUS_ORDER_PENALTY_MEDIUM_MIN = 3.0
    const val BUS_ORDER_PENALTY_LOW_MIN = 10.0

    /**
     * Worth listing: score within USEFUL_FACTOR x best + USEFUL_SLACK_MIN, the
     * best being the best route under that filter.
     */
    const val USEFUL_FACTOR = 1.15
    const val USEFUL_SLACK_MIN = 10.0

    /**
     * And never more than MAX_TIME_FACTOR x the fastest route under that
     * filter + MAX_TIME_SLACK_MIN — however cheap or short on walking it is.
     */
    const val MAX_TIME_FACTOR = 1.5
    const val MAX_TIME_SLACK_MIN = 10.0

    /**
     * Under All, the best train-only, bus-only and mixed route is added when
     * none is listed yet, if its score is within this of the best overall.
     * Also the cut-off below which nothing can ever be shown.
     */
    const val ALTERNATIVE_FACTOR = 1.3
    const val ALTERNATIVE_SLACK_MIN = 10.0

    /**
     * Near-identical: same kind, same rides in the same order (any bus counts
     * as the same ride; trains must be the same line), changing between rides
     * within this distance. Only the most convenient one is kept.
     */
    const val SAME_PLACE_KM = 0.5

    /**
     * A route that is fastest, cheapest, least walking or fewest transfers gets
     * its own card beside the best route only if it beats it by this much.
     */
    const val MIN_TIME_GAIN_MIN = 3
    const val MIN_FARE_SAVING_INR = 10
    const val MIN_WALK_SAVING_KM = 0.3

    /** Walking pace used to judge whether a bus hop is worth taking. */
    const val WALK_SPEED_KMH = 5.0
}
