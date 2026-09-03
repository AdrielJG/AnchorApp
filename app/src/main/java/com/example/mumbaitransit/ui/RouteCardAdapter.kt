package com.example.mumbaitransit.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mumbaitransit.databinding.ItemRouteCardBinding
import com.example.mumbaitransit.model.RouteCard
import kotlin.math.roundToInt

class RouteCardAdapter(
    private val onClick: (RouteCard) -> Unit
) : ListAdapter<RouteCard, RouteCardAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemRouteCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemRouteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = getItem(position)
        val b = holder.binding

        b.tvScenarioLabel.text = card.scenarioLabel
        b.tvModeStr.text = card.modeStr
        b.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        b.tvFare.text = "₹${card.totalFare}"

        // Color-code the card left border by type
        val accentColor = when (card.type) {
            "transit" -> when (card.scenario) {
                "fastest"  -> Color.parseColor("#1a56db")
                "cheapest" -> Color.parseColor("#0a7c42")
                "reliable" -> Color.parseColor("#6d28d9")
                else       -> Color.parseColor("#b45309")
            }
            "bus"  -> Color.parseColor("#d97706")
            "auto" -> Color.parseColor("#b45309")
            "cab"  -> Color.parseColor("#1f2937")
            else   -> Color.GRAY
        }
        b.viewAccent.setBackgroundColor(accentColor)

        // Transit-specific details
        if (card.type == "transit") {
            b.tvTransfers.visibility = View.VISIBLE
            b.tvTransfers.text = when (card.transfers) {
                0    -> "Direct"
                1    -> "1 transfer"
                else -> "${card.transfers} transfers"
            }
            b.tvLines.visibility = View.VISIBLE
            b.tvLines.text = card.linesUsed.joinToString(" → ") { shortenLine(it) }

            b.tvWalk.visibility = View.VISIBLE
            b.tvWalk.text = "🚶 ${card.walkToMin}min + 🚌 ${card.transitMin.roundToInt()}min + 🚶 ${card.walkFromMin}min"

            // MRI badge — show worst MRI score
            val worstMri = card.mriScores.values.minByOrNull { it.mriPct }
            if (worstMri != null) {
                b.tvMri.visibility = View.VISIBLE
                b.tvMri.text = "MRI ${worstMri.mriPct.toInt()}%"
                b.tvMri.setTextColor(mriColor(worstMri.mriPct))
            } else {
                b.tvMri.visibility = View.GONE
            }
            b.tvNote.visibility = View.GONE
        } else {
            b.tvTransfers.visibility = View.GONE
            b.tvLines.visibility = View.GONE
            b.tvWalk.visibility = View.GONE
            b.tvMri.visibility = View.GONE
            if (card.note.isNotEmpty()) {
                b.tvNote.visibility = View.VISIBLE
                b.tvNote.text = card.note
            } else {
                b.tvNote.visibility = View.GONE
            }
        }

        b.root.setOnClickListener { if (card.type == "transit") onClick(card) }
        b.root.isClickable = card.type == "transit"
    }

    private fun shortenLine(line: String) = when {
        line.contains("Central Railway Main") -> "CR"
        line.contains("Western Railway")      -> "WR"
        line.contains("Harbour Line")         -> "HL"
        line.contains("Trans-Harbour")        -> "TH"
        line.contains("Metro Line 1")         -> "M1"
        line.contains("Metro Line 2A")        -> "M2A"
        line.contains("Metro Line 3")         -> "M3"
        line.contains("Metro Line 7")         -> "M7"
        else -> line.take(6)
    }

    private fun mriColor(pct: Double) = when {
        pct >= 70 -> Color.parseColor("#0a7c42")
        pct >= 50 -> Color.parseColor("#b45309")
        else      -> Color.parseColor("#9b1c1c")
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RouteCard>() {
            override fun areItemsTheSame(a: RouteCard, b: RouteCard) = a.scenario == b.scenario
            override fun areContentsTheSame(a: RouteCard, b: RouteCard) = a == b
        }
    }
}
