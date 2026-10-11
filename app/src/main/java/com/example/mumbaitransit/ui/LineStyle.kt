package com.example.mumbaitransit.ui

import android.graphics.Color
import com.example.mumbaitransit.model.JourneyKind
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.RouteCard

/**
 * Line colours and short names, in one place.
 *
 * RouteListActivity and the share card both paint the same lines, so keeping
 * the mapping here stops a shared card from showing Harbour Line in a different
 * colour than the route list the sender was looking at.
 */
object LineStyle {

    fun colorHex(line: String): String = when {
        line.contains("Western Railway")      -> "#1a56db"
        line.contains("Central Railway Main") -> "#dc2626"
        line.contains("Trans-Harbour")        -> "#7c3aed"
        line.contains("Harbour Line")         -> "#d97706"
        line.contains("Metro Line 1")         -> "#0284c7"
        line.contains("Metro Line 2A")        -> "#ca8a04"
        line.contains("Metro Line 3")         -> "#0891b2"
        line.contains("Metro Line 7")         -> "#dc2626"
        else                                  -> "#6b7280"
    }

    fun color(line: String): Int = Color.parseColor(colorHex(line))

    fun shorten(line: String): String = when {
        line.contains("Central Railway Main") -> "CR Main"
        line.contains("Western Railway")      -> "Western Rly"
        line.contains("Trans-Harbour")        -> "Trans-Harbour"
        line.contains("Harbour Line")         -> "Harbour Line"
        line.contains("Metro Line 1")         -> "Metro L1"
        line.contains("Metro Line 2A")        -> "Metro L2A"
        line.contains("Metro Line 3")         -> "Metro L3"
        line.contains("Metro Line 7")         -> "Metro L7"
        else                                  -> line
    }

    /** Accent colour for a route card, by scenario. */
    fun scenarioColor(scenario: String): Int = when (scenario) {
        "fastest"  -> Color.parseColor("#1a56db")
        "cheapest" -> Color.parseColor("#0a7c42")
        "reliable" -> Color.parseColor("#6d28d9")
        else       -> Color.parseColor("#b45309")
    }

    /**
     * Colour for the n-th bus ride in a journey.
     *
     * Bus routes carry no line identity the way rail does, so the colour has to
     * come from position instead — otherwise a two-bus journey draws both legs
     * in the same orange and the map stops being readable.
     */
    fun busColorHex(index: Int): String =
        busPalette[index % busPalette.size]

    private val busPalette = listOf("#D97706", "#EA580C", "#B45309", "#92400E")

    /** Accent colour for the non-transit cards (bus / auto / cab). */
    fun modeColor(type: String): Int = when (type) {
        "bus"  -> Color.parseColor("#d97706")
        "auto" -> Color.parseColor("#b45309")
        "cab"  -> Color.parseColor("#1f2937")
        else   -> Color.GRAY
    }

    /**
     * Accent colour for a route card by what it rides, so train-only, bus-only
     * and mixed journeys can be told apart at a glance: the train blue, the bus
     * orange, and teal where a journey combines the two. Auto and cab keep theirs.
     */
    fun kindColorHex(kind: JourneyKind, type: String): String = when (kind) {
        JourneyKind.TRAIN -> "#1a56db"
        JourneyKind.BUS   -> "#d97706"
        JourneyKind.MIXED -> "#0f766e"
        JourneyKind.ROAD  -> if (type == "cab") "#1f2937" else "#b45309"
    }

    fun kindColor(card: RouteCard): Int = Color.parseColor(kindColorHex(card.kind, card.type))

    /**
     * Every ride's line in order — "BEST 502 AS + Harbour Line". A ride that
     * continues on the same line as the one before is named once.
     */
    fun journeyLines(card: RouteCard): String {
        val names = mutableListOf<String>()
        for (leg in card.transitLegs) {
            val name = if (leg.mode == LegMode.BUS) leg.line else shorten(leg.line)
            if (names.lastOrNull() != name) names.add(name)
        }
        return names.joinToString(" + ")
    }

    fun mriColor(pct: Double): Int = when {
        pct >= 70 -> Color.parseColor("#0a7c42")
        pct >= 50 -> Color.parseColor("#b45309")
        else      -> Color.parseColor("#9b1c1c")
    }
}
