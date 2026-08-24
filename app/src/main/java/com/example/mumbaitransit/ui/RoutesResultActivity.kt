package com.example.mumbaitransit.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mumbaitransit.R
import com.example.mumbaitransit.auth.SavedRouteRepository
import com.example.mumbaitransit.databinding.ActivityRoutesResultBinding
import com.example.mumbaitransit.databinding.ItemRouteCardBinding
import com.example.mumbaitransit.databinding.ItemRouteDetailCardBinding
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.model.TrainTiming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class RoutesResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutesResultBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }
    private val expandedCards = mutableMapOf<ItemRouteDetailCardBinding, RouteCard>()
    private var currentTimetableCard: ItemRouteDetailCardBinding? = null

    private lateinit var savedRouteRepo: SavedRouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutesResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        savedRouteRepo = SavedRouteRepository(this)

        observeViewModel()
        observeTimetable()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (savedRouteRepo.session.isLoggedIn()) {
            menu.add(Menu.NONE, MENU_SAVED, Menu.NONE, "Saved Routes")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            MENU_SAVED -> {
                startActivity(Intent(this, SavedRoutesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_SAVED = 1001
    }

    private fun observeTimetable() {
        vm.timetable.observe(this) { legs ->
            currentTimetableCard?.let { cardBinding ->
                cardBinding.progressTimings.visibility = View.GONE
                if (cardBinding.llPath.childCount > 0) {
                    addTimingsToJourney(cardBinding.llPath, expandedCards[cardBinding]!!, legs)
                }
            }
        }
    }

    private fun observeViewModel() {
        vm.routeState.observe(this) { state ->
            when (state) {
                is RouteState.Idle -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.llRoutesContainer.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Enter origin and destination to find routes"
                    binding.tvRouteHeader.visibility = View.GONE
                }
                is RouteState.Searching -> {
                    binding.progressSearch.visibility = View.VISIBLE
                    binding.llRoutesContainer.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                }
                is RouteState.Results -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvRouteHeader.text = "${state.origLabel} → ${state.destLabel}"
                    binding.tvRouteHeader.visibility = View.VISIBLE
                    if (state.routes.isEmpty()) {
                        binding.llRoutesContainer.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "No routes found"
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.llRoutesContainer.visibility = View.VISIBLE
                        populateRoutes(state.routes)
                    }
                }
                is RouteState.Error -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Error: ${state.msg}"
                }
            }
        }
    }

    private fun populateRoutes(routes: List<RouteCard>) {
        binding.llRoutesContainer.removeAllViews()
        for (route in routes) {
            if (route.type == "transit") {
                val cardBinding = ItemRouteDetailCardBinding.inflate(
                    LayoutInflater.from(this),
                    binding.llRoutesContainer,
                    false
                )
                setupRouteCard(cardBinding, route)
                binding.llRoutesContainer.addView(cardBinding.root)
            } else {
                val cardBinding = ItemRouteCardBinding.inflate(
                    LayoutInflater.from(this),
                    binding.llRoutesContainer,
                    false
                )
                setupSimpleRouteCard(cardBinding, route)
                binding.llRoutesContainer.addView(cardBinding.root)
            }
        }
    }

    private fun setupSimpleRouteCard(b: ItemRouteCardBinding, card: RouteCard) {
        b.tvScenarioLabel.text = card.scenarioLabel
        b.tvModeStr.text = card.modeStr
        b.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        b.tvFare.text = "₹${card.totalFare}"

        val accentColor = when (card.type) {
            "bus"  -> android.graphics.Color.parseColor("#d97706")
            "auto" -> android.graphics.Color.parseColor("#b45309")
            "cab"  -> android.graphics.Color.parseColor("#1f2937")
            else   -> android.graphics.Color.GRAY
        }
        b.viewAccent.setBackgroundColor(accentColor)

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

        b.root.isClickable = false

        // ── Save button (only visible when logged in) ──────────────────────────
        if (savedRouteRepo.session.isLoggedIn()) {
            b.btnSaveRoute.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val saved = savedRouteRepo.isSaved(card)
                withContext(Dispatchers.Main) { updateSimpleSaveButton(b, saved) }
            }
            b.btnSaveRoute.setOnClickListener { toggleSaveSimple(b, card) }
        }
    }

    private fun updateSimpleSaveButton(b: ItemRouteCardBinding, saved: Boolean) {
        b.btnSaveRoute.text = if (saved) "★  Saved" else "☆  Save Route"
        b.btnSaveRoute.alpha = if (saved) 0.7f else 1.0f
    }

    private fun toggleSaveSimple(b: ItemRouteCardBinding, card: RouteCard) {
        CoroutineScope(Dispatchers.IO).launch {
            val wasSaved = savedRouteRepo.isSaved(card)
            if (wasSaved) savedRouteRepo.removeRoute(card) else savedRouteRepo.saveRoute(card)
            withContext(Dispatchers.Main) {
                updateSimpleSaveButton(b, !wasSaved)
                val msg = if (!wasSaved) "Route saved" else "Route removed"
                Toast.makeText(this@RoutesResultActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRouteCard(cardBinding: ItemRouteDetailCardBinding, card: RouteCard) {
        cardBinding.tvScenarioLabel.text = card.scenarioLabel
        cardBinding.tvModeStr.text = card.modeStr
        cardBinding.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        cardBinding.tvFare.text = "₹${card.totalFare}"

        val accentColor = when (card.scenario) {
            "fastest"  -> Color.parseColor("#1a56db")
            "cheapest" -> Color.parseColor("#0a7c42")
            "reliable" -> Color.parseColor("#6d28d9")
            else       -> Color.parseColor("#b45309")
        }
        cardBinding.viewAccent.setBackgroundColor(accentColor)

        cardBinding.tvTransfers.text = when (card.transfers) {
            0    -> "Direct"
            1    -> "1 transfer"
            else -> "${card.transfers} transfers"
        }
        cardBinding.tvLines.text = card.linesUsed.joinToString(" → ") { shortenLine(it) }

        val worstMri = card.mriScores.values.minByOrNull { it.mriPct }
        if (worstMri != null) {
            cardBinding.tvMri.visibility = View.VISIBLE
            cardBinding.tvMri.text = "MRI ${worstMri.mriPct.toInt()}%"
            cardBinding.tvMri.setTextColor(mriColor(worstMri.mriPct))
        }

        cardBinding.tvWalkSummary.text = buildString {
            if (card.walkToMin > 0) append("🚶 ${card.walkToMin} min walk to ${card.originStation}")
            if (card.walkToMin > 0 && card.walkFromMin > 0) append(" · ")
            if (card.walkFromMin > 0) append("🚶 ${card.walkFromMin} min walk from ${card.destStation}")
        }

        var isExpanded = false
        cardBinding.layoutRouteHeader.setOnClickListener {
            isExpanded = !isExpanded
            cardBinding.layoutJourneyDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            cardBinding.ivExpandCollapse.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )

            if (isExpanded) {
                expandedCards[cardBinding] = card
                if (cardBinding.llPath.childCount == 0) {
                    renderJourney(cardBinding, card)
                }
            } else {
                expandedCards.remove(cardBinding)
            }
        }

        // ── Save button (only visible when logged in) ──────────────────────────
        if (savedRouteRepo.session.isLoggedIn()) {
            cardBinding.btnSaveRoute.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val saved = savedRouteRepo.isSaved(card)
                withContext(Dispatchers.Main) { updateDetailSaveButton(cardBinding, saved) }
            }
            cardBinding.btnSaveRoute.setOnClickListener { toggleSaveDetail(cardBinding, card) }
        }
    }

    private fun updateDetailSaveButton(b: ItemRouteDetailCardBinding, saved: Boolean) {
        b.btnSaveRoute.text = if (saved) "★  Saved" else "☆  Save Route"
        b.btnSaveRoute.alpha = if (saved) 0.7f else 1.0f
    }

    private fun toggleSaveDetail(b: ItemRouteDetailCardBinding, card: RouteCard) {
        CoroutineScope(Dispatchers.IO).launch {
            val wasSaved = savedRouteRepo.isSaved(card)
            if (wasSaved) savedRouteRepo.removeRoute(card) else savedRouteRepo.saveRoute(card)
            withContext(Dispatchers.Main) {
                updateDetailSaveButton(b, !wasSaved)
                val msg = if (!wasSaved) "Route saved" else "Route removed"
                Toast.makeText(this@RoutesResultActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderJourney(cardBinding: ItemRouteDetailCardBinding, card: RouteCard) {
        cardBinding.llPath.removeAllViews()

        val ivEdges = card.path.filter { it.edgeType == "in_vehicle" }
        val xferEdges = card.path.filter { it.edgeType == "transfer" }

        // Group edges by line to properly identify segments
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

        // Use user-provided labels, with station names as fallback
        val originDisplay = card.originLabel.ifEmpty { card.originStation }
        val destDisplay = card.destLabel.ifEmpty { card.destStation }

        if (card.walkToMin > 0) {
            // Calculate distance from walk time (assuming 5 km/hr walking speed)
            val walkDistanceKm = card.walkToMin / 60.0 * 5.0
            val suggestAuto = walkDistanceKm > 1.0
            
            addWalkNode(cardBinding.llPath,
                "📍 $originDisplay",
                walkDistanceKm,
                card.walkToMin,
                card.originStation,
                isFirst = true,
                suggestAuto = suggestAuto
            )
        }

        var xferIdx = 0
        for ((segIdx, segment) in segments.withIndex()) {
            val firstEdge = segment.first()
            val lastEdge = segment.last()
            val isLastSegment = segIdx == segments.lastIndex
            val segmentTravelMin = segment.sumOf { it.travelMin.toInt() }

            // Calculate skipped stops for this segment
            val allStops = segment.map { it.fromStop } + lastEdge.toStop
            val skippedStops = if (allStops.size > 2) allStops.drop(1).dropLast(1) else emptyList<String>()

            addBoardingStationWithSkipped(cardBinding.llPath, firstEdge, skippedStops, lastEdge.toStop, segmentTravelMin)
            addAlightingStation(cardBinding.llPath, lastEdge, isLastSegment && card.walkFromMin == 0)

            // Only add transfer between different line segments (not after the last segment)
            if (!isLastSegment && xferIdx < xferEdges.size) {
                val xfer = xferEdges[xferIdx++]
                addTransferNode(cardBinding.llPath, xfer.toStop, xfer.travelMin.roundToInt())
            }
        }

        if (card.walkFromMin > 0) {
            // Calculate distance from walk time (assuming 5 km/hr walking speed)
            val walkDistanceKm = card.walkFromMin / 60.0 * 5.0
            val suggestAuto = walkDistanceKm > 1.0
            
            addWalkNode(cardBinding.llPath,
                "🎯 $destDisplay",
                walkDistanceKm,
                card.walkFromMin,
                card.destStation,
                isFirst = false,
                suggestAuto = suggestAuto
            )
        } else if (card.destStation.isNotEmpty()) {
            addDestinationNode(cardBinding.llPath, destDisplay)
        }

        renderMri(cardBinding, card)

        // Store reference for timetable updates
        cardBinding.progressTimings.visibility = View.VISIBLE
        currentTimetableCard = cardBinding
        
        // Fetch and apply timetable - will be added to boarding stations only
        vm.fetchTimetable(card)
    }

    private fun addTimingsToJourney(llPath: LinearLayout, card: RouteCard, timetableLegs: List<TimetableLeg>) {
        val ttMap = timetableLegs.associateBy { it.fromStop.uppercase() }

        // Only populate timings on views explicitly tagged as boarding stations
        // This prevents duplicate matches when a transfer station name appears twice
        // (e.g. Kurla appears as both an alighting stop AND a boarding stop after transfer)
        for (i in 0 until llPath.childCount) {
            val view = llPath.getChildAt(i)
            if (view.tag != "boarding_station") continue

            val stationName = view.findViewById<TextView>(R.id.tvStationName)?.text?.toString() ?: continue
            val timingsContainer = view.findViewById<LinearLayout>(R.id.llTrainTimings) ?: continue
            if (timingsContainer.childCount > 0) continue  // already populated

            val ttLeg = ttMap[stationName.uppercase()] ?: continue
            timingsContainer.visibility = View.VISIBLE
            buildTimingsTable(timingsContainer, ttLeg)
        }
    }

    private fun addWalkNode(
        container: LinearLayout,
        label: String,
        distanceKm: Double,
        walkMin: Int,
        stationName: String,
        isFirst: Boolean,
        suggestAuto: Boolean = false
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).text = label
        
        // Create subtitle with walk/auto suggestion
        val subtitleText = if (suggestAuto) {
            val autoFare = vm.calculateAutoFare(distanceKm)
            "🛺 Auto ~₹$autoFare to $stationName"
        } else {
            "Walk ~$walkMin min to $stationName"
        }
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = subtitleText
            visibility = View.VISIBLE
            if (suggestAuto) {
                setTextColor(Color.parseColor("#B45309"))
            }
        }
        
        setDot(view, if (suggestAuto) "#B45309" else "#6b7280", isOrigin = isFirst)
        
        // Add auto fare info as a chip if auto is suggested
        if (suggestAuto) {
            val pill = view.findViewById<LinearLayout>(R.id.llLinePill)
            pill.visibility = View.VISIBLE
            val pillText = view.findViewById<TextView>(R.id.tvLinePill)
            pillText.text = "🛺 Auto"
            setDrawableColor(pillText, "#B45309")
            
            val legInfo = view.findViewById<TextView>(R.id.tvLegInfo)
            val autoFare = vm.calculateAutoFare(distanceKm)
            val walkSavings = "₹$autoFare"
            legInfo.text = walkSavings
            legInfo.visibility = View.VISIBLE
        }
        
        // Hide timings container
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE
        
        if (isFirst) view.findViewById<View>(R.id.viewLineTop).visibility = View.INVISIBLE
        container.addView(view)
    }

    private fun addBoardingStation(container: LinearLayout, edge: PathEdge) {
        addBoardingStationWithSkipped(container, edge, edge.skippedStops, edge.toStop, edge.travelMin.roundToInt())
    }

    private fun addBoardingStationWithSkipped(
        container: LinearLayout,
        edge: PathEdge,
        skippedStops: List<String>,
        alightAt: String,
        travelMin: Int
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.tag = "boarding_station"  // ← tag so we can find ONLY boarding views later
        view.findViewById<TextView>(R.id.tvStationName).text = edge.fromStop
        setDot(view, lineColor(edge.line), isOrigin = false)

        val pill = view.findViewById<LinearLayout>(R.id.llLinePill)
        pill.visibility = View.VISIBLE
        val pillText = view.findViewById<TextView>(R.id.tvLinePill)
        pillText.text = shortenLine(edge.line)
        setDrawableColor(pillText, lineColor(edge.line))

        val stopCount = skippedStops.size + 1
        val legInfo = view.findViewById<TextView>(R.id.tvLegInfo)
        legInfo.text = "$travelMin min · $stopCount stop${if (stopCount > 1) "s" else ""}"

        // Timings container - will be populated when timetable data arrives
        val timingsContainer = view.findViewById<LinearLayout>(R.id.llTrainTimings)
        timingsContainer.visibility = View.GONE

        val toggle = view.findViewById<TextView>(R.id.tvSkippedToggle)
        val skippedList = view.findViewById<LinearLayout>(R.id.llSkippedStops)

        if (skippedStops.isNotEmpty()) {
            toggle.visibility = View.VISIBLE
            toggle.text = "▼ $stopCount stops · tap to collapse"
            toggle.setOnClickListener {
                if (skippedList.visibility == View.GONE) {
                    skippedList.visibility = View.VISIBLE
                    toggle.text = "▼ $stopCount stops · tap to collapse"
                } else {
                    skippedList.visibility = View.GONE
                    toggle.text = "▶ $stopCount stops · tap to expand"
                }
            }

            // Build dropdown-style card with all stops
            skippedList.background = makeDropdownBackground()

            // Add header
            val header = TextView(this).apply {
                text = "Stops between ${edge.fromStop} and $alightAt"
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(4))
            }
            skippedList.addView(header)

            // Add each skipped stop
            for (stop in skippedStops) {
                val stopRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
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
                    textSize = 11.5f
                    setTextColor(Color.parseColor("#6B6860"))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                stopRow.addView(stopTv)
                skippedList.addView(stopRow)
            }

            // Divider
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1))
                setBackgroundColor(Color.parseColor("#E5E7EB"))
            }
            skippedList.addView(divider)

            // Add destination row
            val alightRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(6))
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
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(lineColor(edge.line)))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            alightRow.addView(alightTv)
            val alightTimeTv = TextView(this).apply {
                text = "+${travelMin}m"
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            }
            alightRow.addView(alightTimeTv)
            skippedList.addView(alightRow)
        } else {
            // Direct segment — still show the destination station clearly in a visible stops list
            toggle.visibility = View.VISIBLE
            toggle.text = "Direct to $alightAt · +${travelMin}m"
            toggle.setTextColor(ContextCompat.getColor(this, R.color.text_muted))

            // Always build the stops list showing origin → destination
            skippedList.visibility = View.VISIBLE
            skippedList.background = makeDropdownBackground()

            val directRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            }
            val destDot = View(this).apply {
                val dotSize = dpToPx(6)
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = dpToPx(10)
                }
                background = makeOvalDrawable(Color.parseColor(lineColor(edge.line)))
            }
            directRow.addView(destDot)
            val destTv = TextView(this).apply {
                text = alightAt
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(lineColor(edge.line)))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            directRow.addView(destTv)
            val timeTv = TextView(this).apply {
                text = "+${travelMin}m"
                textSize = 10f
                setTextColor(ContextCompat.getColor(this@RoutesResultActivity, R.color.text_hint))
            }
            directRow.addView(timeTv)
            skippedList.addView(directRow)
        }

        container.addView(view)
    }

    private fun addAlightingStation(container: LinearLayout, edge: PathEdge, isFinalDestination: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).apply {
            text = edge.toStop
            if (isFinalDestination) textSize = 14f
        }
        setDot(view, lineColor(edge.line), isOrigin = false)
        
        // Hide line pill and leg info for alighting stations
        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        
        // Hide timings container (only show at boarding stations)
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE
        
        if (isFinalDestination) view.findViewById<View>(R.id.viewLineBottom).visibility = View.INVISIBLE
        container.addView(view)
    }

    private fun addTransferNode(container: LinearLayout, walkTo: String, walkMin: Int) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).text = "→ Transfer at $walkTo"
        view.findViewById<TextView>(R.id.tvStationName).setTypeface(null, Typeface.BOLD)
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = "Walk $walkMin min to connect lines"
            visibility = View.VISIBLE
        }
        setDot(view, "#7c3aed", isOrigin = false)
        
        // Style the transfer node with a subtle background
        view.setBackgroundColor(Color.parseColor("#F5F3FF"))
        
        // Hide elements not needed for transfer nodes
        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE
        
        container.addView(view)
    }

    private fun addDestinationNode(container: LinearLayout, destName: String) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).text = "🎯 $destName"
        setDot(view, "#16a34a", isOrigin = false)
        
        // Hide elements not needed for destination nodes
        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE
        
        view.findViewById<View>(R.id.viewLineBottom).visibility = View.INVISIBLE
        container.addView(view)
    }

    private fun renderMri(cardBinding: ItemRouteDetailCardBinding, card: RouteCard) {
        cardBinding.llMri.removeAllViews()
        if (card.mriScores.isEmpty()) {
            cardBinding.cardMri.visibility = View.GONE
            return
        }
        cardBinding.cardMri.visibility = View.VISIBLE

        val entries = card.mriScores.entries.toList()
        entries.forEachIndexed { idx, (line, score) ->
            val view = layoutInflater.inflate(R.layout.item_mri_row, cardBinding.llMri, false)

            // Line name
            view.findViewById<TextView>(R.id.tvMriLine).text = shortenLine(line)

            // Score % — clean integer
            val pct = score.mriPct.toInt()
            view.findViewById<TextView>(R.id.tvMriPct).text = "$pct%"

            // Colour based on score
            val pctColor = when {
                pct >= 70 -> "#0a7c42"
                pct >= 50 -> "#b45309"
                else      -> "#9b1c1c"
            }
            view.findViewById<TextView>(R.id.tvMriPct)
                .setTextColor(Color.parseColor(pctColor))

            // Progress bar
            val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressMri)
            progressBar.progress = pct
            progressBar.progressTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(pctColor))

            // Freq — round to 1 decimal
            val freqStr = "%.1f /hr".format(score.freq)
            view.findViewById<TextView>(R.id.tvMriFreq).text = freqStr

            // CV — round to 1 decimal
            val cvStr = "CV %.1f%%".format(score.cv)
            view.findViewById<TextView>(R.id.tvMriCv).text = cvStr

            // AC ratio — only show if meaningful
            val acView = view.findViewById<TextView>(R.id.tvMriAc)
            if (score.acRatio > 0.1) {
                acView.visibility = View.VISIBLE
                acView.text = "AC ${(score.acRatio * 100).toInt()}%"
            } else {
                acView.visibility = View.GONE
            }

            // Hide divider on last item
            if (idx == entries.lastIndex) {
                view.findViewById<View>(R.id.mriDivider).visibility = View.GONE
            }

            cardBinding.llMri.addView(view)
        }
    }

    private fun buildTimingsTable(container: LinearLayout, leg: TimetableLeg) {
        // Create a card-style container for the timing table
        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F7F4"))
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Header with title
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dpToPx(6))
        }
        val title = TextView(this).apply {
            text = "🚆 Next trains from ${leg.fromStop}"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A18"))
        }
        header.addView(title)
        val subtitle = TextView(this).apply {
            text = "Current time: ${leg.arriveAtStop}"
            textSize = 10f
            setTextColor(Color.parseColor("#6B6860"))
            setPadding(0, dpToPx(1), 0, 0)
        }
        header.addView(subtitle)
        cardContainer.addView(header)

        if (leg.trains.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No upcoming trains found"
                textSize = 10f
                setTextColor(Color.parseColor("#9E9B93"))
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            cardContainer.addView(tv)
        } else {
            // Use a HorizontalScrollView for better portrait mode support
            val scrollView = android.widget.HorizontalScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isHorizontalScrollBarEnabled = false
            }
            
            val contentContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            for ((idx, train) in leg.trains.withIndex()) {
                if (idx > 0) {
                    val divider = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1))
                        setBackgroundColor(Color.parseColor("#E5E7EB"))
                    }
                    contentContainer.addView(divider)
                }
                val row = buildTimingDataRowClean(train, isFirst = idx == 0)
                contentContainer.addView(row)
            }
            
            scrollView.addView(contentContainer)
            cardContainer.addView(scrollView)
        }

        container.addView(cardContainer)
    }

    private fun buildTimingDataRowClean(train: TrainTiming, isFirst: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
            if (isFirst) setBackgroundColor(Color.parseColor("#EFF4FF"))
        }

        // Departs - bold time
        val depTime = TextView(this).apply {
            text = train.depFrom
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isFirst) Color.parseColor("#1A56DB") else Color.parseColor("#1A1A18"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(depTime)
        
        if (isFirst) {
            val nextBadge = TextView(this).apply {
                text = " NEXT"
                textSize = 8f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = makeBadgeBackground(Color.parseColor("#1A56DB"))
                setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dpToPx(4) }
            }
            row.addView(nextBadge)
        }
        
        // Arrow
        row.addView(TextView(this).apply {
            text = " → "
            textSize = 11f
            setTextColor(Color.parseColor("#9E9B93"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        // Arrives time + terminus destination
        val arrCell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        arrCell.addView(TextView(this).apply {
            text = "${train.arrTo} (${train.journeyMin}m)"
            textSize = 12f
            setTextColor(Color.parseColor("#1A1A18"))
        })
        if (train.terminus.isNotEmpty()) {
            arrCell.addView(TextView(this).apply {
                text = train.terminus
                textSize = 10f
                setTextColor(Color.parseColor("#6B6860"))
                isSingleLine = false
                maxLines = 2
            })
        }
        row.addView(arrCell)

        // Type badge
        val typeStr = when {
            train.trainType.isNotEmpty() && train.ac -> "${train.trainType} AC"
            train.trainType.isNotEmpty() -> train.trainType
            train.ac -> "AC"
            else -> ""
        }
        val typeColor = when {
            typeStr.contains("Fast", ignoreCase = true) -> "#dc2626"
            typeStr.contains("AC", ignoreCase = true) -> "#0891b2"
            else -> "#6B6860"
        }
        if (typeStr.isNotEmpty()) {
            row.addView(TextView(this).apply {
                text = typeStr
                textSize = 10f
                setTextColor(Color.parseColor(typeColor))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dpToPx(6) }
            })
        }

        // Wait time pill
        val waitBg = if (isFirst) makeWaitPillBackground(Color.parseColor("#EFF4FF"), Color.parseColor("#1A56DB"))
                      else makeWaitPillBackground(Color.parseColor("#F3F4F6"), Color.parseColor("#6B7280"))
        row.addView(TextView(this).apply {
            text = "${train.waitMin}m"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (isFirst) Color.parseColor("#1A56DB") else Color.parseColor("#6B7280"))
            background = waitBg
            setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        return row
    }

    private fun makeBadgeBackground(color: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dpToPx(4).toFloat()
        }

    private fun makeWaitPillBackground(bgColor: Int, textColor: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(bgColor)
            setStroke(dpToPx(1), textColor)
            cornerRadius = dpToPx(10).toFloat()
        }

    private fun setDot(view: View, colorHex: String, isOrigin: Boolean) {
        val dot = view.findViewById<View>(R.id.viewDot)
        try {
            dot.setBackgroundColor(Color.parseColor(colorHex))
        } catch (e: Exception) {
            dot.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_blue))
        }
    }

    private fun setDrawableColor(tv: TextView, colorHex: String) {
        try {
            val bg = tv.background?.mutate()
            if (bg is android.graphics.drawable.GradientDrawable) {
                bg.setColor(Color.parseColor(colorHex))
            }
        } catch (e: Exception) { }
    }

    private fun makeDivider(): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
        v.setBackgroundColor(Color.parseColor("#E5E7EB"))
        return v
    }

    private fun makeOvalDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    private fun makeDropdownBackground(): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#FAFAF8"))
            setStroke(dpToPx(1), Color.parseColor("#E5E7EB"))
            cornerRadius = dpToPx(8).toFloat()
        }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun walkMetres(mins: Int): Int = (mins * 80).coerceAtMost(1200)

    private fun lineColor(line: String) = when {
        line.contains("Western Railway")      -> "#1a56db"
        line.contains("Central Railway Main") -> "#dc2626"
        line.contains("Harbour Line")         -> "#d97706"
        line.contains("Trans-Harbour")       -> "#7c3aed"
        line.contains("Metro Line 1")        -> "#0284c7"
        line.contains("Metro Line 2A")       -> "#ca8a04"
        line.contains("Metro Line 3")        -> "#0891b2"
        line.contains("Metro Line 7")        -> "#dc2626"
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

    private fun mriColor(pct: Double) = when {
        pct >= 70 -> Color.parseColor("#0a7c42")
        pct >= 50 -> Color.parseColor("#b45309")
        else      -> Color.parseColor("#9b1c1c")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
