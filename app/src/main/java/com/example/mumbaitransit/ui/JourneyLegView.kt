package com.example.mumbaitransit.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.example.mumbaitransit.databinding.ItemJourneyLegBinding
import com.example.mumbaitransit.journey.JourneyPing
import com.example.mumbaitransit.journey.JourneyLeg
import com.example.mumbaitransit.journey.LegKind
import com.example.mumbaitransit.journey.PingSource

/**
 * Paints one journey leg.
 *
 * Shared by the rider's control screen and the follower's view on purpose: if
 * the two rendered legs separately they would eventually disagree about which
 * train was locked in, and the person waiting at the far end would be reading
 * something the rider never said.
 */
object JourneyLegView {

    /**
     * @param editable true on the rider's screen, where a scheduled leg can be
     *        tapped to change the service. The follower gets the same row with
     *        no affordance, so it never looks like something they can act on.
     */
    fun bind(
        b: ItemJourneyLegBinding,
        leg: JourneyLeg,
        ping: JourneyPing?,
        editable: Boolean
    ) {
        val kind = leg.legKind
        val colour = runCatching { Color.parseColor(leg.colorHex) }
            .getOrDefault(Color.parseColor("#6B6860"))

        b.viewLegAccent.setBackgroundColor(colour)

        b.tvLegMode.text = when (kind) {
            LegKind.WALK  -> "WALK"
            LegKind.RAIL  -> leg.lineShort.ifBlank { "TRAIN" }
            LegKind.METRO -> leg.lineShort.ifBlank { "METRO" }
            LegKind.BUS   -> "BUS"
            LegKind.ROAD  -> "ROAD"
        }
        b.tvLegMode.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (kind == LegKind.WALK) Color.parseColor("#9E9B93") else colour)
            cornerRadius = 20 * b.root.resources.displayMetrics.density
        }

        b.tvLegTitle.text = leg.title()
        b.tvLegMinutes.text = "${leg.scheduledMin} min"

        b.tvLegSub.text = when {
            kind == LegKind.WALK -> "${leg.fromStop} → ${leg.toStop}"
            leg.stops.size > 2   -> "${leg.stops.size - 1} stops"
            else                 -> "Direct"
        }
        b.tvLegSub.visibility = if (b.tvLegSub.text.isNullOrBlank()) View.GONE else View.VISIBLE

        // ── Locked-in service ────────────────────────────────────────────────
        if (kind.isScheduled) {
            b.rowLegService.visibility = View.VISIBLE
            val noun = if (kind == LegKind.METRO) "METRO" else "TRAIN"
            val label = leg.serviceLine()
            if (label != null) {
                b.tvLegServiceLabel.text = if (editable) "$noun BOARDED · TAP TO CHANGE" else "$noun BOARDED"
                b.tvLegServiceValue.text = label
                b.tvLegServiceValue.setTextColor(Color.parseColor("#1A1A18"))
            } else {
                b.tvLegServiceLabel.text = noun
                b.tvLegServiceValue.text =
                    if (editable) "Tap to choose the ${noun.lowercase()} you boarded"
                    else "Not confirmed yet"
                b.tvLegServiceValue.setTextColor(Color.parseColor("#9E9B93"))
            }
            b.rowLegService.isClickable = editable
        } else {
            b.rowLegService.visibility = View.GONE
        }

        // ── Live position within this leg ────────────────────────────────────
        val onThisLeg = ping != null && !ping.ended && ping.legIndex == leg.index
        if (onThisLeg && ping != null) {
            b.tvLegLive.visibility = View.VISIBLE
            val where = ping.nearestStation.ifBlank { leg.fromStop }
            val next = ping.nextStop.takeIf { it.isNotBlank() }?.let { " · next $it" }.orEmpty()
            // Say plainly when the position is worked out from the timetable
            // rather than seen — underground on the metro, this is every ping.
            val prefix = when (ping.source) {
                PingSource.GPS      -> "● Here now"
                PingSource.SCHEDULE -> "◐ On schedule"
                else                -> "○ Position unconfirmed"
            }
            b.tvLegLive.text = "$prefix — $where$next"
            b.tvLegLive.setTextColor(
                when (ping.source) {
                    PingSource.GPS      -> Color.parseColor("#0A7C42")
                    PingSource.SCHEDULE -> Color.parseColor("#B45309")
                    else                -> Color.parseColor("#9B1C1C")
                }
            )
            b.root.alpha = 1f
        } else {
            b.tvLegLive.visibility = View.GONE
            // Legs already behind the rider fade back so the current one leads.
            b.root.alpha = if (ping != null && !ping.ended && leg.index < ping.legIndex) 0.55f else 1f
        }
    }
}
