package com.example.mumbaitransit.ui

import android.graphics.Color

/**
 * Line colours and short names, in one place.
 *
 * RoutesResultActivity and the share card both paint the same lines, so keeping
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

    /** Accent colour for the non-transit cards (bus / auto / cab). */
    fun modeColor(type: String): Int = when (type) {
        "bus"  -> Color.parseColor("#d97706")
        "auto" -> Color.parseColor("#b45309")
        "cab"  -> Color.parseColor("#1f2937")
        else   -> Color.GRAY
    }

    fun mriColor(pct: Double): Int = when {
        pct >= 70 -> Color.parseColor("#0a7c42")
        pct >= 50 -> Color.parseColor("#b45309")
        else      -> Color.parseColor("#9b1c1c")
    }
}
