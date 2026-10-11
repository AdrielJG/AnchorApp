package com.example.mumbaitransit.engine

import kotlin.math.max
import kotlin.math.roundToInt

/** How a walk leg (to/from a station, stop or the destination) is travelled. */
enum class LastMileMode(val label: String, val action: String, val emoji: String) {
    WALK("Walk", "Walk", "🚶"),
    AUTO("Auto", "Take an auto", "🛺"),
    CAB("Cab", "Take a cab", "🚕");

    val byRoad: Boolean get() = this != WALK
}

/** One way of covering a walk leg, with its time, distance and cost. */
data class LastMileOption(
    val mode: LastMileMode,
    val minutes: Int,
    val metres: Int,
    /** 0 for walking. */
    val fareInr: Int
)

/**
 * The single rule for walk/auto/cab choices, shared by the Routes screen and
 * Live Navigation so both always offer the same options in the same order.
 *
 *  - up to 200 m      → Walk only
 *  - 200 m – 600 m    → Walk, then auto/cab (cheaper first)
 *  - over 600 m       → auto/cab (cheaper first), then Walk
 *
 * The first option is the default choice.
 */
object LastMile {
    const val WALK_ONLY_MAX_M = 200
    const val ROAD_FIRST_ABOVE_M = 600

    /** Same detour factor and walking pace the planner and navigation use. */
    private const val DETOUR = 1.3
    private const val WALK_M_PER_MIN = 80.0
    private const val AUTO_KMH = 20.0
    private const val CAB_KMH = 22.0

    /** Walking distance in metres for a walk leg. */
    fun metres(straightKm: Double, walkMin: Int): Int =
        if (straightKm > 0) (straightKm * 1000 * DETOUR).roundToInt()
        else (walkMin * WALK_M_PER_MIN).roundToInt()

    fun options(
        straightKm: Double,
        walkMin: Int,
        autoFare: (Double) -> Int,
        cabFare: (Double) -> Int
    ): List<LastMileOption> {
        val m = metres(straightKm, walkMin)
        val walk = LastMileOption(LastMileMode.WALK, max(1, walkMin), m, 0)
        if (m <= WALK_ONLY_MAX_M) return listOf(walk)

        val km = m / 1000.0
        val auto = LastMileOption(LastMileMode.AUTO, roadMin(km, AUTO_KMH), m, autoFare(km))
        val cab = LastMileOption(LastMileMode.CAB, roadMin(km, CAB_KMH), m, cabFare(km))
        // Cheaper first; auto wins a tie.
        val road = if (cab.fareInr < auto.fareInr) listOf(cab, auto) else listOf(auto, cab)

        return if (m > ROAD_FIRST_ABOVE_M) road + walk else listOf(walk) + road
    }

    private fun roadMin(km: Double, kmh: Double): Int = max(1, (km / kmh * 60).roundToInt())

    /** "450 m", "1.2 km". */
    fun distanceLabel(metres: Int): String =
        if (metres < 1000) "${(metres / 10) * 10} m" else "%.1f km".format(metres / 1000.0)

    /** "6 min · 450 m" for a walk, "~2 min · 450 m · ₹26" for a ride. */
    fun metaLabel(o: LastMileOption): String =
        if (o.mode.byRoad) "~${o.minutes} min · ${distanceLabel(o.metres)} · ₹${o.fareInr}"
        else "${o.minutes} min · ${distanceLabel(o.metres)}"
}
