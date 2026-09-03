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
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewAnimator
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
import com.example.mumbaitransit.share.RouteSharer
import com.example.mumbaitransit.share.ShareContext
import androidx.lifecycle.lifecycleScope
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

    /**
     * One pager per route card. Holds the ViewAnimator of every rail leg in that
     * route so Prev/Next steps them all to the same option index together.
     */
    private class TrainPager {
        val animators = mutableListOf<ViewAnimator>()
        var index = 0
        /** Legs can return different numbers of trains — page to the longest. */
        val optionCount: Int get() = animators.maxOfOrNull { it.childCount } ?: 0
    }

    private val trainPagers = mutableMapOf<ItemRouteDetailCardBinding, TrainPager>()

    private lateinit var savedRouteRepo: SavedRouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutesResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        savedRouteRepo = SavedRouteRepository(this)

        setupCollapseFab()
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

        // Train markers: fast = red, slow = green, AC = cyan
        private const val COLOR_FAST = "#DC2626"
        private const val COLOR_SLOW = "#0A7C42"
        private const val COLOR_AC   = "#0891B2"
        private const val COLOR_MEDIUM = "#B45309"
    }

    private fun observeTimetable() {
        vm.timetable.observe(this) { legs ->
            val cardBinding = currentTimetableCard ?: return@observe
            cardBinding.progressTimings.visibility = View.GONE
            // The card may have been collapsed or rebuilt before the data arrived.
            if (expandedCards[cardBinding] == null) return@observe
            if (cardBinding.llPath.childCount > 0) {
                addTimingsToJourney(cardBinding, legs)
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
                    binding.tvRouteHeader.text = buildString {
                        append("${state.origLabel} → ${state.destLabel}")
                        vm.departWindowLabel()?.let { append("\n$it") }
                    }
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
        // Old bindings are detached now — drop the state that pointed at them.
        expandedCards.clear()
        trainPagers.clear()
        currentTimetableCard = null
        binding.fabCollapseRoute.visibility = View.GONE
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

        val accentColor = LineStyle.modeColor(card.type)
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

        // ── Share button ──────────────────────────────────────────────────────
        b.btnShareRoute.setOnClickListener { shareRoute(card) }
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

        val accentColor = LineStyle.scenarioColor(card.scenario)
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

        cardBinding.layoutRouteHeader.setOnClickListener {
            // Read the state from expandedCards rather than a local flag, so the
            // floating collapse button and the header arrow can't drift apart.
            setCardExpanded(cardBinding, card, !expandedCards.containsKey(cardBinding))
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

        // ── Share buttons — one for each state, same handler ──────────────────
        cardBinding.btnShareRoute.setOnClickListener { shareRoute(card) }
        cardBinding.btnShareRouteCollapsed.setOnClickListener { shareRoute(card) }
    }

    /**
     * Renders this route to a card image and hands it to the share sheet.
     *
     * The timetable is fetched per-card rather than read off vm.timetable: the
     * user can share a collapsed card, which has never loaded timings, and the
     * expanded card's timings belong to whichever route is open on screen.
     */
    private fun shareRoute(card: RouteCard) {
        lifecycleScope.launch {
            val legs = if (card.type == "transit") vm.buildTimetable(card) else emptyList()
            RouteSharer.share(
                this@RoutesResultActivity,
                card,
                ShareContext(
                    legs = legs,
                    departWindow = vm.departWindowLabel(),
                    autoFareTo = vm.calculateAutoFare(card.walkToMin / 60.0 * 5.0),
                    autoFareFrom = vm.calculateAutoFare(card.walkFromMin / 60.0 * 5.0)
                )
            )
        }
    }

    /**
     * Single entry point for expanding/collapsing a route card, so the header
     * arrow and the floating collapse button always agree.
     */
    private fun setCardExpanded(
        cardBinding: ItemRouteDetailCardBinding,
        card: RouteCard,
        expanded: Boolean
    ) {
        cardBinding.layoutJourneyDetails.visibility = if (expanded) View.VISIBLE else View.GONE
        // Expanded, Share belongs above the reliability index; collapsed, the
        // details section is gone so the twin below the card takes over.
        cardBinding.btnShareRouteCollapsed.visibility = if (expanded) View.GONE else View.VISIBLE
        cardBinding.dividerShareCollapsed.visibility = if (expanded) View.GONE else View.VISIBLE
        cardBinding.ivExpandCollapse.setImageResource(
            if (expanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )

        if (expanded) {
            expandedCards[cardBinding] = card
            if (cardBinding.llPath.childCount == 0) {
                renderJourney(cardBinding, card)
            }
        } else {
            expandedCards.remove(cardBinding)
        }
        binding.scrollRoutes.post { updateCollapseFab() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Floating collapse button — keeps the arrow reachable after scrolling
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupCollapseFab() {
        // Park it just below the toolbar.
        binding.appBar.post {
            val lp = binding.fabCollapseRoute.layoutParams
                    as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            lp.topMargin = binding.appBar.height + dpToPx(12)
            binding.fabCollapseRoute.layoutParams = lp
        }

        binding.fabCollapseRoute.setOnClickListener {
            val (cardBinding, card) = currentScrolledCard() ?: return@setOnClickListener
            setCardExpanded(cardBinding, card, false)
            // Bring the collapsed card's header back into view.
            binding.scrollRoutes.post {
                binding.scrollRoutes.smoothScrollTo(0, scrollYFor(cardBinding.root))
            }
        }

        // NestedScrollView has two setOnScrollChangeListener overloads — name the
        // interface so Kotlin doesn't have to guess which one the lambda is for.
        binding.scrollRoutes.setOnScrollChangeListener(
            androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
                updateCollapseFab()
            }
        )
    }

    /** The expanded card we're currently scrolled inside of, past its header. */
    private fun currentScrolledCard(): Pair<ItemRouteDetailCardBinding, RouteCard>? {
        val scrollTop = viewTopOnScreen(binding.scrollRoutes)
        for ((cardBinding, card) in expandedCards) {
            val header = cardBinding.layoutRouteHeader
            val headerHidden = viewTopOnScreen(header) + header.height < scrollTop
            val cardStillOnScreen = viewTopOnScreen(cardBinding.root) + cardBinding.root.height > scrollTop
            if (headerHidden && cardStillOnScreen) return cardBinding to card
        }
        return null
    }

    private fun updateCollapseFab() {
        binding.fabCollapseRoute.visibility =
            if (currentScrolledCard() != null) View.VISIBLE else View.GONE
    }

    private fun viewTopOnScreen(view: View): Int {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return loc[1]
    }

    /** Scroll offset that puts [view] at the top of the scroll container. */
    private fun scrollYFor(view: View): Int =
        (binding.scrollRoutes.scrollY + viewTopOnScreen(view) - viewTopOnScreen(binding.scrollRoutes))
            .coerceAtLeast(0)

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

    private fun addTimingsToJourney(
        cardBinding: ItemRouteDetailCardBinding,
        timetableLegs: List<TimetableLeg>
    ) {
        val llPath = cardBinding.llPath
        val ttMap = timetableLegs.associateBy { it.fromStop.uppercase() }
        val pager = TrainPager()

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
            buildTimingsTable(timingsContainer, ttLeg)?.let { pager.animators.add(it) }
        }

        trainPagers[cardBinding] = pager
        setupTrainPager(cardBinding, pager)
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Train option pager — shows one train per leg, Prev/Next steps them all
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupTrainPager(cardBinding: ItemRouteDetailCardBinding, pager: TrainPager) {
        if (pager.optionCount <= 1) {
            cardBinding.layoutTrainPager.visibility = View.GONE
            return
        }
        cardBinding.layoutTrainPager.visibility = View.VISIBLE
        cardBinding.btnPrevTrain.setOnClickListener { stepTrainPager(cardBinding, -1) }
        cardBinding.btnNextTrain.setOnClickListener { stepTrainPager(cardBinding, +1) }
        updateTrainPager(cardBinding, pager, animate = false, forward = true)
    }

    private fun stepTrainPager(cardBinding: ItemRouteDetailCardBinding, delta: Int) {
        val pager = trainPagers[cardBinding] ?: return
        val target = (pager.index + delta).coerceIn(0, pager.optionCount - 1)
        if (target == pager.index) return
        pager.index = target
        updateTrainPager(cardBinding, pager, animate = true, forward = delta > 0)
    }

    private fun updateTrainPager(
        cardBinding: ItemRouteDetailCardBinding,
        pager: TrainPager,
        animate: Boolean,
        forward: Boolean
    ) {
        val total = pager.optionCount

        for (animator in pager.animators) {
            if (animate) {
                animator.inAnimation = AnimationUtils.loadAnimation(
                    this, if (forward) R.anim.slide_in_right else R.anim.slide_in_left
                )
                animator.outAnimation = AnimationUtils.loadAnimation(
                    this, if (forward) R.anim.slide_out_left else R.anim.slide_out_right
                )
            } else {
                animator.inAnimation = null
                animator.outAnimation = null
            }
            // A leg with fewer trains just holds on its last one.
            val target = pager.index.coerceAtMost(animator.childCount - 1)
            if (animator.displayedChild != target) animator.displayedChild = target
        }

        cardBinding.tvTrainPagerIndicator.text = "${pager.index + 1} of $total"
        setPagerButtonEnabled(cardBinding.btnPrevTrain, pager.index > 0)
        setPagerButtonEnabled(cardBinding.btnNextTrain, pager.index < total - 1)
    }

    private fun setPagerButtonEnabled(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.35f
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
        view.findViewById<TextView>(R.id.tvStationName).text = "→ Change train at $walkTo"
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

    /**
     * Builds the timings block for one rail leg. Each of the leg's trains becomes a
     * card inside a [ViewAnimator], so only one is on screen at a time.
     *
     * @return the animator to register with the card's pager, or null if this leg
     *         has no upcoming trains.
     */
    private fun buildTimingsTable(container: LinearLayout, leg: TimetableLeg): ViewAnimator? {
        // The train card carries its own border, so drop the container's panel frame.
        container.background = null
        container.setPadding(0, 0, 0, 0)

        if (leg.trains.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No upcoming trains from ${leg.fromStop}"
                textSize = 11f
                setTextColor(Color.parseColor("#9E9B93"))
                setPadding(dpToPx(2), dpToPx(6), dpToPx(2), dpToPx(6))
            })
            return null
        }

        val animator = ViewAnimator(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Keep the height fixed to the tallest card so the slide doesn't jolt.
            measureAllChildren = true
        }

        for (train in leg.trains) {
            animator.addView(buildTrainCard(animator, train, leg))
        }

        container.addView(animator)
        return animator
    }

    /** One train's name, departure, arrival and wait — [R.layout.item_train_timing_card]. */
    private fun buildTrainCard(parent: ViewGroup, train: TrainTiming, leg: TimetableLeg): View {
        val view = layoutInflater.inflate(R.layout.item_train_timing_card, parent, false)

        // The train number is what's on the indicator board, so lead with it.
        val terminus = train.terminus.ifEmpty { "—" }.uppercase()
        view.findViewById<TextView>(R.id.tvTrainName).text =
            train.trainNo?.let { "$it · $terminus" } ?: terminus
        view.findViewById<TextView>(R.id.tvDepTime).text = train.depFrom
        view.findViewById<TextView>(R.id.tvArrTime).text = train.arrTo
        view.findViewById<TextView>(R.id.tvWaitTime).text = "${train.waitMin}m"

        // F / S markers. Only Central Railway Main records Fast vs Slow in the
        // timetable data, so other lines simply show no speed marker.
        val speedBadge = view.findViewById<TextView>(R.id.tvSpeedBadge)
        when {
            train.trainType.contains("Fast", ignoreCase = true) ->
                applyTrainBadge(speedBadge, "F", COLOR_FAST)
            train.trainType.contains("Slow", ignoreCase = true) ->
                applyTrainBadge(speedBadge, "S", COLOR_SLOW)
            train.trainType.contains("Medium", ignoreCase = true) ->
                applyTrainBadge(speedBadge, "M", COLOR_MEDIUM)
            else -> speedBadge.visibility = View.GONE
        }

        val acBadge = view.findViewById<TextView>(R.id.tvAcBadge)
        if (train.ac || train.trainType.contains("AC", ignoreCase = true)) {
            applyTrainBadge(acBadge, "AC", COLOR_AC)
        } else {
            acBadge.visibility = View.GONE
        }

        val liveButton = view.findViewById<View>(R.id.btnLiveStatus)
        // RailRadar tracks suburban rail only, and metro carries no train number,
        // so the button is offered exactly where it can actually deliver.
        liveButton.visibility = if (train.trainNo == null) View.GONE else View.VISIBLE
        liveButton.setOnClickListener {
            // The leg identifies the service; LiveStatusActivity resolves it to an
            // IR train number, asking once and remembering the answer.
            LiveStatusActivity.start(
                context = this,
                line = leg.line,
                fromStop = leg.fromStop,
                toStop = leg.toStop,
                departure = train.depFrom,
                trainNo = train.trainNo
            )
        }

        return view
    }

    /** Colours the F / S / AC chip and tints its pill background to match. */
    private fun applyTrainBadge(badge: TextView, label: String, colorHex: String) {
        val color = Color.parseColor(colorHex)
        badge.visibility = View.VISIBLE
        badge.text = label
        badge.setTextColor(color)
        badge.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)))
            setStroke(dpToPx(1), Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)))
            cornerRadius = dpToPx(6).toFloat()
        }
    }

    private fun setDot(view: View, colorHex: String, isOrigin: Boolean) {
        val dot = view.findViewById<View>(R.id.viewDot)
        val color = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            ContextCompat.getColor(this, R.color.accent_blue)
        }
        // setBackgroundColor() would swap the oval drawable for a flat square,
        // so tint an oval drawable instead.
        dot.background = makeOvalDrawable(color)
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

    // Line styling lives in LineStyle so the shared route card paints the same
    // colours and short names the route list is showing.
    private fun lineColor(line: String) = LineStyle.colorHex(line)

    private fun shortenLine(line: String) = LineStyle.shorten(line)

    private fun mriColor(pct: Double) = LineStyle.mriColor(pct)

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
