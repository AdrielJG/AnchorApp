package com.example.mumbaitransit.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mumbaitransit.R
import com.example.mumbaitransit.databinding.ActivityRouteDetailBinding
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.model.TrainTiming
import kotlin.math.roundToInt

class RouteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteDetailBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val card = vm.selectedRoute ?: run { finish(); return }
        supportActionBar?.title = card.scenarioLabel

        renderSummary(card)
        renderMri(card)

        binding.progressTimings.visibility = View.VISIBLE
        vm.fetchTimetable(card)

        vm.timetable.observe(this) { legs ->
            binding.progressTimings.visibility = View.GONE
            renderJourney(card, legs)
        }
    }

    // ─── Summary header ───────────────────────────────────────────────────────

    private fun renderSummary(card: RouteCard) {
        binding.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        binding.tvFare.text = "₹${card.totalFare}"
        binding.tvTransfers.text = card.transfers.toString()
        binding.tvModeStr.text = card.modeStr

        val walkParts = mutableListOf<String>()
        if (card.walkToMin > 0)   walkParts += "🚶 ${card.walkToMin} min walk to ${card.originStation}"
        if (card.walkFromMin > 0) walkParts += "🚶 ${card.walkFromMin} min walk from ${card.destStation}"
        binding.tvWalkSummary.text = walkParts.joinToString("  ·  ")
        binding.tvWalkSummary.visibility = if (walkParts.isEmpty()) View.GONE else View.VISIBLE

        binding.tvLines.text = card.linesUsed.joinToString(" → ") { shortenLine(it) }
        binding.tvLines.visibility = if (card.linesUsed.isEmpty()) View.GONE else View.VISIBLE

        binding.tvScenarioIcon.text = when (card.scenario) {
            "fastest"  -> "⚡"
            "cheapest" -> "💰 Affordable"
            "reliable" -> "🛡️"
            else       -> "🚆"
        }
    }

    // ─── Full journey timeline ────────────────────────────────────────────────

    private fun renderJourney(card: RouteCard, timetableLegs: List<TimetableLeg>) {
        binding.llPath.removeAllViews()

        val ttMap = timetableLegs.associateBy { it.fromStop.uppercase() to it.line }
        val ivEdges   = card.path.filter { it.edgeType == "in_vehicle" }
        val xferEdges = card.path.filter { it.edgeType == "transfer" }

        val segments = mutableListOf<List<PathEdge>>()
        var currentSegment = mutableListOf<PathEdge>()

        for (edge in ivEdges) {
            if (currentSegment.isEmpty() || currentSegment.last().line == edge.line) {
                currentSegment.add(edge)
            } else {
                segments.add(currentSegment)
                currentSegment = mutableListOf(edge)
            }
        }
        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment)
        }

        if (card.walkToMin > 0) {
            addWalkNode(
                label   = "📍 ${card.originStation.ifEmpty { "Your location" }}",
                sub     = "Walk ~${card.walkToMin} min · ${walkMetres(card.walkToMin)} m",
                isFirst = true
            )
        }

        for ((segIdx, segment) in segments.withIndex()) {
            val firstEdge = segment.first()
            val lastEdge = segment.last()
            val segmentTravelMin = segment.sumOf { it.travelMin.toInt() }

            val allStops = segment.map { it.fromStop } + lastEdge.toStop
            val skippedStops = if (allStops.size > 2) allStops.drop(1).dropLast(1) else emptyList<String>()
            val ttLeg = ttMap[firstEdge.fromStop.uppercase() to firstEdge.line]

            addBoardingStation(firstEdge, skippedStops, lastEdge.toStop, segmentTravelMin, ttLeg)
            addAlightingStation(lastEdge, segIdx == segments.lastIndex && card.walkFromMin == 0)

            if (!isLastSegment(segIdx, segments)) {
                val nextSegmentFirstStop = segments[segIdx + 1].first().fromStop
                val xfer = xferEdges.find { it.fromStop == lastEdge.toStop }
                if (xfer != null) {
                    addTransferNode(xfer.toStop, xfer.travelMin.roundToInt())
                } else {
                    addTransferNode(nextSegmentFirstStop, 5)
                }
            }
        }

        if (card.walkFromMin > 0) {
            addWalkNode(
                label   = "🎯 ${card.destStation.ifEmpty { "Destination" }}",
                sub     = "Walk ~${card.walkFromMin} min · ${walkMetres(card.walkFromMin)} m",
                isFirst = false
            )
        } else if (card.destStation.isNotEmpty()) {
            addDestinationNode(card.destStation)
        }
    }

    private fun isLastSegment(segIdx: Int, segments: List<List<PathEdge>>) = segIdx == segments.lastIndex

    // ─── Node builders ────────────────────────────────────────────────────────

    private fun addWalkNode(label: String, sub: String, isFirst: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, binding.llPath, false)
        view.findViewById<TextView>(R.id.tvStationName).text = label
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = sub; visibility = View.VISIBLE
        }
        setDot(view, "#6b7280", isOrigin = isFirst)
        if (isFirst) view.findViewById<View>(R.id.viewLineTop).visibility = View.INVISIBLE
        binding.llPath.addView(view)
    }

    private fun addBoardingStation(
        edge: PathEdge,
        skippedStops: List<String>,
        alightAt: String,
        travelMin: Int,
        ttLeg: TimetableLeg?
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, binding.llPath, false)
        view.findViewById<TextView>(R.id.tvStationName).text = edge.fromStop
        setDot(view, lineColor(edge.line), isOrigin = false)

        val pill = view.findViewById<LinearLayout>(R.id.llLinePill)
        pill.visibility = View.VISIBLE
        val pillText = view.findViewById<TextView>(R.id.tvLinePill)
        pillText.text = shortenLine(edge.line)
        setDrawableColor(pillText, lineColor(edge.line))

        val stopCount = skippedStops.size + 1
        val legInfo = view.findViewById<TextView>(R.id.tvLegInfo)
        legInfo.text = "$travelMin min · $stopCount stop${if (stopCount > 1) "s" else ""} to $alightAt"

        val toggle = view.findViewById<TextView>(R.id.tvSkippedToggle)
        val skippedList = view.findViewById<LinearLayout>(R.id.llSkippedStops)

        if (skippedStops.isNotEmpty()) {
            toggle.visibility = View.VISIBLE
            toggle.text = "▶  ${skippedStops.size} stop${if (skippedStops.size > 1) "s" else ""} along the way"

            for ((idx, stop) in skippedStops.withIndex()) {
                val stopRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dpToPx(2), 0, dpToPx(2))
                }
                val dot = View(this).apply {
                    val dotSize = dpToPx(6)
                    layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                        marginEnd = dpToPx(10)
                    }
                    background = makeOvalDrawable(Color.parseColor("#C8C5BC"))
                }
                stopRow.addView(dot)

                val stopTv = TextView(this).apply {
                    text = stop
                    textSize = 12f
                    setTextColor(Color.parseColor("#6B6860"))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                stopRow.addView(stopTv)

                val mins = ((idx + 1) * travelMin.toDouble() / (skippedStops.size + 1)).roundToInt()
                val timeTv = TextView(this).apply {
                    text = "+${mins}m"
                    textSize = 10f
                    setTextColor(Color.parseColor("#9E9B93"))
                }
                stopRow.addView(timeTv)
                skippedList.addView(stopRow)
            }

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1))
                setBackgroundColor(Color.parseColor("#E5E7EB"))
            }
            skippedList.addView(divider)

            val alightRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(2), 0, dpToPx(2))
            }
            val alightDot = View(this).apply {
                val dotSize = dpToPx(6)
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = dpToPx(10)
                }
                background = makeOvalDrawable(Color.parseColor(lineColor(edge.line)))
            }
            alightRow.addView(alightDot)
            val alightTv = TextView(this).apply {
                text = alightAt
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(lineColor(edge.line)))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            alightRow.addView(alightTv)
            val alightTimeTv = TextView(this).apply {
                text = "+${travelMin}m"
                textSize = 10f
                setTextColor(Color.parseColor("#9E9B93"))
            }
            alightRow.addView(alightTimeTv)
            skippedList.addView(alightRow)

            toggle.setOnClickListener {
                val opening = skippedList.visibility == View.GONE
                skippedList.visibility = if (opening) View.VISIBLE else View.GONE
                toggle.text = (if (opening) "▼" else "▶") +
                        "  ${skippedStops.size} stop${if (skippedStops.size > 1) "s" else ""} along the way"
            }
        } else {
            toggle.visibility = View.VISIBLE
            toggle.text = "Direct to $alightAt · +${travelMin}m"
            toggle.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        }

        val timingsContainer = view.findViewById<LinearLayout>(R.id.llTrainTimings)
        timingsContainer.visibility = View.VISIBLE
        if (ttLeg != null) {
            buildTimingsTable(timingsContainer, ttLeg)
        } else {
            val tv = TextView(this).apply {
                text = "Loading timings…"
                textSize = 11f
                setTextColor(Color.parseColor("#9E9B93"))
            }
            timingsContainer.addView(tv)
        }

        binding.llPath.addView(view)
    }

    private fun addAlightingStation(edge: PathEdge, isFinalDestination: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, binding.llPath, false)
        view.findViewById<TextView>(R.id.tvStationName).apply {
            text = edge.toStop
            if (isFinalDestination) textSize = 14f
        }
        setDot(view, lineColor(edge.line), isOrigin = false)
        if (isFinalDestination) view.findViewById<View>(R.id.viewLineBottom).visibility = View.INVISIBLE
        binding.llPath.addView(view)
    }

    private fun addTransferNode(walkTo: String, walkMin: Int) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, binding.llPath, false)
        val tv = view.findViewById<TextView>(R.id.tvStationName)
        val italic = SpannableString("Transfer at $walkTo")
        italic.setSpan(StyleSpan(Typeface.ITALIC), 0, italic.length, 0)
        tv.text = italic
        tv.textSize = 12f
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = "Walk · $walkMin min"
            visibility = View.VISIBLE
        }
        setDot(view, "#9ca3af", isOrigin = false)
        binding.llPath.addView(view)
    }

    private fun addDestinationNode(destName: String) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, binding.llPath, false)
        view.findViewById<TextView>(R.id.tvStationName).text = "🎯 $destName"
        setDot(view, "#16a34a", isOrigin = false)
        view.findViewById<View>(R.id.viewLineBottom).visibility = View.INVISIBLE
        binding.llPath.addView(view)
    }

    // ─── Timings table builder ────────────────────────────────────────────────

    private fun buildTimingsTable(container: LinearLayout, leg: TimetableLeg) {
        // ── CHANGED: "Next trains from X · HH:MM" as styled header ──
        val headerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = makeRoundedRect("#F8F7F4", "#E2E0DA", dpToPx(8))
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(4) }
        }

        // Title: "Next trains from Sanpada · 23:25"
        val titleSb = SpannableString("Next trains from ${leg.fromStop}  ·  ${leg.arriveAtStop}")
        titleSb.setSpan(StyleSpan(Typeface.BOLD),
            "Next trains from ".length,
            "Next trains from ".length + leg.fromStop.length, 0)
        val titleTv = TextView(this).apply {
            text = titleSb
            textSize = 11.5f
            setTextColor(Color.parseColor("#6B6860"))
            setPadding(0, 0, 0, dpToPx(6))
        }
        headerBox.addView(titleTv)

        // Column header row
        val colHeaders = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf(
            Triple("DEPARTS",  68, 0f),
            Triple("",         22, 0f),   // arrow spacer
            Triple("ARRIVES",  68, 0f),
            Triple("JOURNEY",   0, 1f),
            Triple("TYPE",      0, 1f),
            Triple("WAIT",     58, 0f)
        ).forEach { (label, w, wt) ->
            colHeaders.addView(TextView(this).apply {
                text = label
                textSize = 9f
                letterSpacing = 0.06f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#9E9B93"))
                gravity = if (label == "WAIT") Gravity.END else Gravity.START
                layoutParams = if (wt > 0)
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, wt)
                else
                    LinearLayout.LayoutParams(dpToPx(w), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
        }
        headerBox.addView(colHeaders)
        container.addView(headerBox)

        if (leg.trains.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "  No upcoming trains found"
                textSize = 11f
                setPadding(0, dpToPx(4), 0, 0)
                setTextColor(Color.parseColor("#9E9B93"))
            })
        } else {
            leg.trains.forEachIndexed { idx, train ->
                container.addView(buildTimingDataRow(train, isFirst = idx == 0))
                if (idx < leg.trains.lastIndex) container.addView(makeDivider())
            }
        }
    }

    // ── CHANGED: full row with NEXT badge + colored TYPE ──
    private fun buildTimingDataRow(train: TrainTiming, isFirst: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(2), dpToPx(6), dpToPx(2), dpToPx(6))
            if (isFirst) setBackgroundColor(Color.parseColor("#F0F6FF"))
        }

        // DEPARTS + optional NEXT badge
        val depCell = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(dpToPx(68), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        depCell.addView(TextView(this).apply {
            text = train.depFrom
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isFirst) Color.parseColor("#1A56DB") else Color.parseColor("#1A1A18"))
        })
        if (isFirst) {
            depCell.addView(TextView(this).apply {
                text = " NEXT"
                textSize = 8f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = makeRoundedSolid("#1A56DB", dpToPx(8))
                setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dpToPx(4) }
            })
        }
        row.addView(depCell)

        // Arrow
        row.addView(TextView(this).apply {
            text = "→"
            textSize = 11f
            setTextColor(Color.parseColor("#9E9B93"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(22), ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        // ARRIVES
        row.addView(TextView(this).apply {
            text = train.arrTo
            textSize = 13f
            setTextColor(Color.parseColor("#1A1A18"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(68), ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        // JOURNEY
        row.addView(TextView(this).apply {
            text = "${train.journeyMin} min"
            textSize = 11f
            setTextColor(Color.parseColor("#6B6860"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        // TYPE — colored for fast/slow/AC
        val typeStr = buildString {
            if (train.trainType.isNotEmpty()) append(train.trainType)
            if (train.ac) append(" · AC")
        }.ifEmpty { "Local" }
        val typeColor = when {
            typeStr.contains("Fast", ignoreCase = true) -> "#dc2626"
            typeStr.contains("AC",   ignoreCase = true) -> "#0891b2"
            else                                        -> "#6B6860"
        }
        row.addView(TextView(this).apply {
            text = typeStr
            textSize = 11f
            setTextColor(Color.parseColor(typeColor))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        // WAIT pill
        row.addView(TextView(this).apply {
            text = "${train.waitMin} min"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A56DB"))
            background = makeRoundedRect("#EFF4FF", "#C3D4FB", dpToPx(10))
            setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(dpToPx(58), ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        return row
    }

    // ─── MRI section ──────────────────────────────────────────────────────────

    private fun renderMri(card: RouteCard) {
        binding.llMri.removeAllViews()
        if (card.mriScores.isEmpty()) { binding.cardMri.visibility = View.GONE; return }
        binding.cardMri.visibility = View.VISIBLE

        val explanation = TextView(this).apply {
            text = "Reliability Score: how often trains run on time. Higher = better."
            textSize = 10f
            setTextColor(Color.parseColor("#6B6860"))
            setPadding(0, 0, 0, dpToPx(8))
        }
        binding.llMri.addView(explanation)

        for ((line, score) in card.mriScores) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }

            // Line name
            row.addView(TextView(this).apply {
                text = shortenLine(line)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1A18"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })

            // CV + freq (explain CV = Coefficient of Variation = how consistent the timing is)
            row.addView(TextView(this).apply {
                text = "CV ${score.cv.toInt()}% · ${score.freq.toInt()}/hr"
                textSize = 10f
                setTextColor(Color.parseColor("#9E9B93"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = dpToPx(8) }
            })

            // Progress bar
            val pctColor = when {
                score.mriPct >= 70 -> Color.parseColor("#0a7c42")
                score.mriPct >= 50 -> Color.parseColor("#b45309")
                else               -> Color.parseColor("#dc2626")
            }
            row.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = score.mriPct.toInt()
                progressTintList = android.content.res.ColorStateList.valueOf(pctColor)
                layoutParams = LinearLayout.LayoutParams(dpToPx(60), dpToPx(8))
                    .apply { marginEnd = dpToPx(8) }
            })

            // Pct label
            row.addView(TextView(this).apply {
                text = "${score.mriPct.toInt()}%"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(pctColor)
                minWidth = dpToPx(40)
                gravity = Gravity.END
            })

            binding.llMri.addView(row)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun setDot(view: View, colorHex: String, isOrigin: Boolean) {
        try {
            view.findViewById<View>(R.id.viewDot)
                .setBackgroundColor(Color.parseColor(colorHex))
        } catch (e: Exception) { /* ignore */ }
    }

    private fun setDrawableColor(tv: TextView, colorHex: String) {
        try {
            val bg = tv.background?.mutate()
            if (bg is android.graphics.drawable.GradientDrawable)
                bg.setColor(Color.parseColor(colorHex))
        } catch (e: Exception) { /* ignore */ }
    }

    private fun makeOvalDrawable(color: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
        }

    private fun makeRoundedSolid(colorHex: String, radiusPx: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.parseColor(colorHex))
            cornerRadius = radiusPx.toFloat()
        }

    private fun makeRoundedRect(fillHex: String, strokeHex: String, radiusPx: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.parseColor(fillHex))
            setStroke(dpToPx(1), Color.parseColor(strokeHex))
            cornerRadius = radiusPx.toFloat()
        }

    private fun makeDivider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1))
        setBackgroundColor(Color.parseColor("#E5E7EB"))
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
    private fun walkMetres(mins: Int) = (mins * 80).coerceAtMost(1200)

    private fun lineColor(line: String) = when {
        line.contains("Western Railway")      -> "#1a56db"
        line.contains("Central Railway Main") -> "#dc2626"
        line.contains("Harbour Line")         -> "#d97706"
        line.contains("Trans-Harbour")        -> "#7c3aed"
        line.contains("Metro Line 1")         -> "#0284c7"
        line.contains("Metro Line 2A")        -> "#ca8a04"
        line.contains("Metro Line 3")         -> "#0891b2"
        line.contains("Metro Line 7")         -> "#dc2626"
        else                                  -> "#6b7280"
    }

    private fun shortenLine(line: String) = when {
        line.contains("Central Railway Main") -> "CR Main"
        line.contains("Western Railway")      -> "Western Rly"
        line.contains("Harbour Line")         -> "Harbour Line"
        line.contains("Trans-Harbour")        -> "Trans-Harbour"
        line.contains("Metro Line 1")         -> "Metro L1"
        line.contains("Metro Line 2A")        -> "Metro L2A"
        line.contains("Metro Line 3")         -> "Metro L3"
        line.contains("Metro Line 7")         -> "Metro L7"
        else                                  -> line
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}
