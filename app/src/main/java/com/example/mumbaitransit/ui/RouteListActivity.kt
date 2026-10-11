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
import com.example.mumbaitransit.engine.LastMile
import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.auth.AuthBottomSheet
import com.example.mumbaitransit.auth.SavedRouteRepository
import com.example.mumbaitransit.databinding.ActivityRouteListBinding
import com.example.mumbaitransit.databinding.ItemRouteCardBinding
import com.example.mumbaitransit.databinding.ItemRouteDetailCardBinding
import com.example.mumbaitransit.databinding.ViewRouteActionsBinding
import com.example.mumbaitransit.model.BusLeg
import com.example.mumbaitransit.model.JourneyKind
import com.example.mumbaitransit.model.LegMode
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.RouteLeg
import com.example.mumbaitransit.model.RouteSelection
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.model.TrainTiming
import com.example.mumbaitransit.share.RouteSharer
import com.example.mumbaitransit.share.ShareContext
import com.example.mumbaitransit.journey.JourneySession
import com.example.mumbaitransit.navigation.NavCopy
import com.example.mumbaitransit.navigation.NavigationSession
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The route results.
 *
 * Split out of [RoutesResultActivity], which now does nothing but set the trip
 * up: where to board, where to get off and when to leave. Choosing and reading
 * a route are two different jobs, and running them on one screen meant the
 * selectors scrolled away the moment results arrived.
 *
 * The search itself is fired by Route Options before this screen is started;
 * everything here reads off the shared [TransitViewModel], so nothing about
 * the trip has to travel in the Intent.
 */
class RouteListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteListBinding
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

    /**
     * The routes chosen for each filter by the engine's RouteSelector —
     * validated, de-duplicated, classified by their own legs and ranked. This
     * screen only shows them; it never filters or re-orders.
     */
    private var selection: RouteSelection? = null

    /** The list under the current filter, in the engine's order. */
    private var visibleRoutes: List<RouteCard> = emptyList()

    private var routeFilter = RouteFilter.ALL

    /**
     * All = most convenient of every kind. Train = train/metro rides only,
     * Bus = bus rides only, Mixed = both — judged on the whole journey.
     */
    private enum class RouteFilter { ALL, TRAIN, BUS, MIXED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        savedRouteRepo = SavedRouteRepository(this)

        renderTripStrip()
        setupRouteFilter()
        setupCollapseFab()
        observeViewModel()
        observeTimetable()
    }

    /**
     * The one-line reminder of what was asked for. Route Options owns the
     * selectors now, so this screen has to say which trip these routes are for
     * — otherwise a back-and-forth between the two loses the thread.
     */
    private fun renderTripStrip() {
        val origin = if (vm.originIsMyLocation) "My Location" else vm.originLabel.ifEmpty { "Start" }
        binding.tvTripEnds.text = "$origin  →  ${vm.destLabel.ifEmpty { "Destination" }}"

        val parts = mutableListOf<String>()
        vm.pinnedOrigin?.let { parts.add("Board at $it") }
        vm.pinnedDest?.let { parts.add("Get off at $it") }
        parts.add(vm.departWindowLabel()?.replace("Trains from", "Departing") ?: "Departing now")
        binding.tvTripDetail.text = parts.joinToString("  ·  ")
    }

    private fun showEmpty(msg: String) {
        binding.progressSearch.visibility = View.GONE
        binding.llRoutesContainer.visibility = View.GONE
        binding.rowResultsHeader.visibility = View.GONE
        binding.rowRouteFilter.visibility = View.GONE
        binding.tvFilterEmpty.visibility = View.GONE
        binding.noteEstimates.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = msg
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
        private const val LAST_MILE_ROW = "last_mile_row"

        // Train markers: fast = red, slow = green, AC = cyan
        private const val COLOR_FAST = "#DC2626"
        private const val COLOR_SLOW = "#0A7C42"
        private const val COLOR_AC   = "#0891B2"
        private const val COLOR_MEDIUM = "#B45309"

        fun start(context: android.content.Context) {
            context.startActivity(Intent(context, RouteListActivity::class.java))
        }
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
            val card = expandedCards[cardBinding]
            if (card != null && card.legs.firstOrNull { it.mode != LegMode.WALK || it.minutes >= 2 }?.mode?.isRail == true) {
                val first = legs.firstOrNull()?.trains?.firstOrNull()
                firstTrainShown[cardBinding] = first
                bindNextStep(cardBinding, card, first)
            }
        }
    }

    private fun observeViewModel() {
        vm.routeState.observe(this) { state ->
            when (state) {
                is RouteState.Idle -> {
                    showEmpty("Go back and tap “See Routes” to search")
                }
                is RouteState.Searching -> {
                    binding.progressSearch.visibility = View.VISIBLE
                    binding.llRoutesContainer.visibility = View.GONE
                    binding.rowResultsHeader.visibility = View.GONE
                    binding.rowRouteFilter.visibility = View.GONE
                    binding.tvFilterEmpty.visibility = View.GONE
                    binding.noteEstimates.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                }
                is RouteState.Results -> {
                    binding.progressSearch.visibility = View.GONE
                    if (state.selection.isEmpty) {
                        showEmpty("No routes found")
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rowResultsHeader.visibility = View.VISIBLE
                        binding.rowRouteFilter.visibility = View.VISIBLE
                        binding.llRoutesContainer.visibility = View.VISIBLE
                        selection = state.selection
                        applyFilter()
                        // One footnote for the whole list, after the last card.
                        binding.noteEstimates.visibility = View.VISIBLE
                    }
                }
                is RouteState.Error -> {
                    showEmpty("Error: ${state.msg}")
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Filter — each one shows its own list from the engine
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupRouteFilter() {
        binding.segFilterAll.setOnClickListener { setRouteFilter(RouteFilter.ALL) }
        binding.segFilterTrain.setOnClickListener { setRouteFilter(RouteFilter.TRAIN) }
        binding.segFilterBus.setOnClickListener { setRouteFilter(RouteFilter.BUS) }
        binding.segFilterMixed.setOnClickListener { setRouteFilter(RouteFilter.MIXED) }
        paintFilter()
    }

    private fun setRouteFilter(filter: RouteFilter) {
        if (filter == routeFilter) return
        routeFilter = filter
        applyFilter()
    }

    /**
     * The engine's list for the current filter. A Train + Bus journey is only
     * ever in Mixed (and All) — never under Train or Bus.
     */
    private fun routesFor(filter: RouteFilter): List<RouteCard> {
        val sel = selection ?: return emptyList()
        return when (filter) {
            RouteFilter.ALL   -> sel.all
            RouteFilter.TRAIN -> sel.train
            RouteFilter.BUS   -> sel.bus
            RouteFilter.MIXED -> sel.mixed
        }
    }

    /** Rebuilds the list for the current filter. */
    private fun applyFilter() {
        visibleRoutes = routesFor(routeFilter)
        paintFilter()
        binding.tvRouteCount.text =
            if (visibleRoutes.size == 1) "1 option" else "${visibleRoutes.size} options"

        binding.llRoutesContainer.removeAllViews()
        // Old bindings are detached now — drop the state that pointed at them.
        expandedCards.clear()
        trainPagers.clear()
        currentTimetableCard = null
        binding.fabCollapseRoute.visibility = View.GONE

        binding.tvFilterEmpty.visibility = if (visibleRoutes.isEmpty()) View.VISIBLE else View.GONE
        binding.tvFilterEmpty.text = when (routeFilter) {
            RouteFilter.TRAIN -> "No train-only routes for this trip"
            RouteFilter.BUS   -> "No bus-only routes for this trip"
            RouteFilter.MIXED -> "No routes combining train and bus for this trip"
            RouteFilter.ALL   -> "No routes found"
        }
        // A short list by design — every card goes on at once.
        for (route in visibleRoutes) addRouteCard(route)
    }

    /** Same thumb and colours as the toggle on the live-status screen. */
    private fun paintFilter() {
        val segments = listOf(
            binding.segFilterAll to RouteFilter.ALL,
            binding.segFilterTrain to RouteFilter.TRAIN,
            binding.segFilterBus to RouteFilter.BUS,
            binding.segFilterMixed to RouteFilter.MIXED
        )
        for ((view, filter) in segments) {
            val active = filter == routeFilter
            view.background =
                if (active) ContextCompat.getDrawable(this, R.drawable.bg_segment_active) else null
            view.setTextColor(ContextCompat.getColor(this,
                if (active) R.color.text_primary else R.color.text_muted))
            view.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun addRouteCard(route: RouteCard) {
        // Every train, bus and mixed journey gets the expandable card. Auto, cab
        // and the distance-only bus estimate have no legs, so keep the flat one.
        if (route.legs.isNotEmpty()) {
            val cardBinding = ItemRouteDetailCardBinding.inflate(
                LayoutInflater.from(this), binding.llRoutesContainer, false
            )
            setupJourneyCard(cardBinding, route)
            binding.llRoutesContainer.addView(cardBinding.root)
        } else {
            val cardBinding = ItemRouteCardBinding.inflate(
                LayoutInflater.from(this), binding.llRoutesContainer, false
            )
            setupSimpleRouteCard(cardBinding, route)
            binding.llRoutesContainer.addView(cardBinding.root)
        }
    }

    private fun setupSimpleRouteCard(b: ItemRouteCardBinding, card: RouteCard) {
        b.tvScenarioLabel.text = card.scenarioLabel
        b.tvModeStr.text = card.modeStr
        b.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        b.tvFare.text = "₹${card.totalFare}"

        val accentColor = LineStyle.modeColor(card.type)
        b.viewAccent.setBackgroundColor(accentColor)

        // Bus cards backed by the dataset get the same breakdown a transit card
        // gets; auto, cab and the no-coverage bus estimate stay bare.
        if (card.busLegs.isNotEmpty()) {
            b.tvTransfers.visibility = View.VISIBLE
            b.tvTransfers.text = when (card.transfers) {
                0    -> "Direct bus"
                1    -> "1 change"
                else -> "${card.transfers} changes"
            }
            b.tvLines.visibility = View.VISIBLE
            b.tvLines.text = card.busLegs.joinToString("  →  ") { "${it.operator} ${it.routeRef}" }

            b.tvWalk.visibility = View.VISIBLE
            b.tvWalk.text = "🚶 ${card.walkToMin}min + 🚌 ${card.transitMin.roundToInt()}min + 🚶 ${card.walkFromMin}min"
        } else {
            b.tvTransfers.visibility = View.GONE
            b.tvLines.visibility = View.GONE
            b.tvWalk.visibility = View.GONE
        }
        b.tvMri.visibility = View.GONE

        if (card.note.isNotEmpty()) {
            b.tvNote.visibility = View.VISIBLE
            b.tvNote.text = card.note
        } else {
            b.tvNote.visibility = View.GONE
        }

        b.root.isClickable = false

        // Bus cards run three across — Save, Share, Live Journey. Auto and cab
        // keep the map, which draws their road path from the Directions API.
        bindRouteActions(b.routeActions, card, showMap = card.type != "bus")
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Compact action bar — same four columns on every card
    // ──────────────────────────────────────────────────────────────────────────

    private fun bindRouteActions(
        actions: ViewRouteActionsBinding,
        card: RouteCard,
        showMap: Boolean
    ) {
        actions.actionViewMap.visibility = if (showMap) View.VISIBLE else View.GONE

        refreshSaveAction(actions, card)
        actions.actionSave.setOnClickListener { toggleSaveAction(actions, card) }
        actions.actionShare.setOnClickListener { shareRoute(card) }
        actions.actionLiveJourney.setOnClickListener { shareLiveJourney(card) }
        actions.actionViewMap.setOnClickListener { RouteMapActivity.start(this, card) }
    }

    private fun refreshSaveAction(actions: ViewRouteActionsBinding, card: RouteCard) {
        if (!savedRouteRepo.session.isLoggedIn()) {
            setSaveActionState(actions, false)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val saved = savedRouteRepo.isSaved(card)
            withContext(Dispatchers.Main) { setSaveActionState(actions, saved) }
        }
    }

    private fun setSaveActionState(actions: ViewRouteActionsBinding, saved: Boolean) {
        actions.ivSave.setImageResource(
            if (saved) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        actions.tvSaveLabel.text = if (saved) "Saved" else "Save"
    }

    /**
     * Saving needs an account. The column stays on the bar either way and a tap
     * while signed out opens the auth sheet, rather than the button vanishing
     * and leaving a gap where the commuter expected it.
     */
    private fun toggleSaveAction(actions: ViewRouteActionsBinding, card: RouteCard) {
        if (!savedRouteRepo.session.isLoggedIn()) {
            val sheet = AuthBottomSheet.newInstance()
            sheet.onAuthSuccess = { refreshSaveAction(actions, card) }
            sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val wasSaved = savedRouteRepo.isSaved(card)
            if (wasSaved) savedRouteRepo.removeRoute(card) else savedRouteRepo.saveRoute(card)
            withContext(Dispatchers.Main) {
                setSaveActionState(actions, !wasSaved)
                val msg = if (!wasSaved) "Route saved" else "Route removed"
                Toast.makeText(this@RouteListActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * The header of any journey card — train-only, bus-only or mixed.
     *
     * The mode line gives the whole journey ("🚶 Walk → 🚌 Bus → 🚆 Train → 🚶 Walk")
     * and the line below names every ride ("BEST 502 AS + Harbour Line"). The
     * accent bar says which kind of journey it is. Reliability exists for rail
     * lines only, so a bus-only card carries no MRI badge — as before.
     */
    private fun setupJourneyCard(cardBinding: ItemRouteDetailCardBinding, card: RouteCard) {
        cardBinding.tvScenarioLabel.text = card.scenarioLabel
        cardBinding.tvModeStr.text = card.modeStr
        cardBinding.tvTotalTime.text = "${card.totalMin.roundToInt()} min"
        cardBinding.tvFare.text = "₹${card.totalFare}"
        cardBinding.viewAccent.setBackgroundColor(LineStyle.kindColor(card))

        cardBinding.tvTransfers.text = if (card.kind == JourneyKind.BUS) {
            when (card.transfers) {
                0    -> "Direct bus"
                1    -> "1 change"
                else -> "${card.transfers} changes"
            }
        } else {
            when (card.transfers) {
                0    -> "Direct"
                1    -> "1 transfer"
                else -> "${card.transfers} transfers"
            }
        }
        cardBinding.tvLines.text = LineStyle.journeyLines(card)

        val worstMri = card.mriScores.values.minByOrNull { it.mriPct }
        if (worstMri != null) {
            cardBinding.tvMri.visibility = View.VISIBLE
            cardBinding.tvMri.text = "MRI ${worstMri.mriPct.toInt()}%"
            cardBinding.tvMri.setTextColor(mriColor(worstMri.mriPct))
        } else {
            cardBinding.tvMri.visibility = View.GONE
        }

        val firstWalk = card.legs.firstOrNull()?.takeIf { it.mode == LegMode.WALK }
        val lastWalk = card.legs.lastOrNull()?.takeIf { it.mode == LegMode.WALK && card.legs.size > 1 }
        cardBinding.tvWalkSummary.text = buildString {
            if (firstWalk != null) append("🚶 ${firstWalk.minutes} min walk to ${firstWalk.to}")
            if (firstWalk != null && lastWalk != null) append(" · ")
            if (lastWalk != null) append("🚶 ${lastWalk.minutes} min walk from ${lastWalk.from}")
        }

        cardBinding.layoutRouteHeader.setOnClickListener {
            // Read the state from expandedCards rather than a local flag, so the
            // floating collapse button and the header arrow can't drift apart.
            setCardExpanded(cardBinding, card, !expandedCards.containsKey(cardBinding))
        }

        // Same four columns as every other card, and outside the details
        // section so they stay put whether the card is open or closed.
        bindRouteActions(cardBinding.routeActions, card, showMap = true)

        bindNextStep(cardBinding, card, null)
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Next step + live navigation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * The "Next step" panel at the top of an expanded card, and the button
     * that starts live navigation. The label follows the route's first leg;
     * [firstTrain] fills in "towards X · 12:42" once the timetable is in.
     */
    private fun bindNextStep(
        cardBinding: ItemRouteDetailCardBinding,
        card: RouteCard,
        firstTrain: TrainTiming?
    ) {
        val step = NavCopy.firstStep(card, firstTrain, travel = { leg ->
            val mode = vm.lastMileChoice(card, leg)
            vm.lastMileOptions(leg).firstOrNull { it.mode == mode }
        }) { leg ->
            leg.bus?.let { "${it.operator} ${it.routeRef}" } ?: LineStyle.shorten(leg.line)
        }
        if (step == null) {
            cardBinding.panelNextStep.visibility = View.GONE
            return
        }
        cardBinding.panelNextStep.visibility = View.VISIBLE
        cardBinding.tvNextStepTitle.text = step.title
        cardBinding.tvNextStepMeta.text = step.meta
        cardBinding.ivNextStepIcon.setImageResource(
            when (step.mode) {
                LegMode.WALK -> when (step.road) {
                    LastMileMode.AUTO -> R.drawable.ic_auto
                    LastMileMode.CAB -> R.drawable.ic_cab
                    else -> R.drawable.ic_walk
                }
                LegMode.BUS -> R.drawable.ic_bus
                LegMode.METRO -> R.drawable.ic_mode_metro
                LegMode.TRAIN -> R.drawable.ic_mode_rail
            }
        )
        val resuming = NavigationSession.isActive && NavigationSession.routeKey == card.scenario
        cardBinding.btnStartNavigation.text = if (resuming) "Resume Navigation" else step.cta
        cardBinding.btnStartNavigation.setOnClickListener { startNavigation(card) }
    }

    /**
     * Opens live navigation for [card]. A different route already being
     * navigated is only replaced after asking — navigation and live sharing
     * are independent, so this never touches a journey being shared.
     */
    private fun startNavigation(card: RouteCard) {
        if (NavigationSession.isActive) {
            if (NavigationSession.routeKey == card.scenario) {
                NavigationActivity.open(this)
                return
            }
            AlertDialog.Builder(this)
                .setTitle("Navigate this route instead?")
                .setMessage("You're already being guided on another route. Starting this one stops that.")
                .setPositiveButton("Start this route") { _, _ ->
                    vm.navCard = card
                    NavigationActivity.begin(this)
                }
                .setNegativeButton("Keep current", null)
                .show()
            return
        }
        vm.navCard = card
        NavigationActivity.begin(this)
    }

    override fun onResume() {
        super.onResume()
        // "Resume Navigation" appears or clears as navigation starts and ends.
        expandedCards.forEach { (b, card) -> bindNextStep(b, card, firstTrainShown[b]) }
    }

    /** The first train each expanded card is showing, for the Next step line. */
    private val firstTrainShown = mutableMapOf<ItemRouteDetailCardBinding, TrainTiming?>()

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
                this@RouteListActivity,
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
     * Hands this route to the live-journey screen.
     *
     * Only one journey can be shared at a time, so a tap while another is
     * already running opens that one instead of quietly replacing it — losing
     * a journey somebody is actively following would be the worst possible
     * outcome of a mis-tap.
     */
    private fun shareLiveJourney(card: RouteCard) {
        if (JourneySession.isSharing) {
            Toast.makeText(
                this,
                "You're already sharing a journey — opening it",
                Toast.LENGTH_SHORT
            ).show()
            LiveJourneyActivity.start(this)
            return
        }
        vm.journeyCard = card
        LiveJourneyActivity.start(this)
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
        cardBinding.ivExpandCollapse.setImageResource(
            if (expanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )

        if (expanded) {
            expandedCards[cardBinding] = card
            if (cardBinding.llPath.childCount == 0) renderJourney(cardBinding, card)
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

    /**
     * Paints a journey into the expanded card, one leg at a time.
     *
     * Every ride keeps the node it always had: a train or metro leg lists its
     * stations and gets the next-train card, a bus leg lists its bus stops. The
     * walks between rides use the nodes the line changes and feeder buses used
     * before. A journey with no rail leg has no timetable and no reliability
     * block, exactly as a bus card never did.
     */
    private fun renderJourney(cardBinding: ItemRouteDetailCardBinding, card: RouteCard) {
        val path = cardBinding.llPath
        path.removeAllViews()
        val legs = card.legs

        // Use user-provided labels, with station names as fallback
        val originDisplay = card.originLabel.ifEmpty { card.originStation }
        val destDisplay = card.destLabel.ifEmpty { card.destStation }
        var busIndex = 0

        legs.forEachIndexed { i, leg ->
            val prev = legs.getOrNull(i - 1)
            val next = legs.getOrNull(i + 1)
            when {
                leg.mode == LegMode.WALK && i == 0 -> {
                    addWalkNode(path, "📍 $originDisplay", card, leg, "to ${leg.to}", isFirst = true)
                }
                leg.mode == LegMode.WALK && i == legs.lastIndex -> {
                    addWalkNode(path, "🎯 $destDisplay", card, leg, "from ${leg.from}", isFirst = false)
                }
                leg.mode == LegMode.WALK -> {
                    if (prev?.mode?.isRail == true && next?.mode?.isRail == true) {
                        addTransferNode(path, leg.to, leg.minutes)
                    } else {
                        val toStation = next?.mode?.isRail == true
                        val view = addFeederLinkNode(path, "→ Walk to ${leg.to}",
                            "${leg.minutes} min on foot to the ${if (toStation) "station" else "bus stop"}")
                        bindLastMile(view, card, leg, "to ${leg.to}",
                            title = { m -> "→ ${m.label} to ${leg.to}" })
                    }
                }
                leg.mode.isRail -> {
                    if (leg.edges.isEmpty()) return@forEachIndexed
                    val first = leg.edges.first()
                    val last = leg.edges.last()
                    // Stations between boarding and alighting, for the dropdown.
                    addBoardingStationWithSkipped(path, first, leg.stops.drop(1).dropLast(1),
                        last.toStop, leg.edges.sumOf { it.travelMin.toInt() })
                    addAlightingStation(path, last, isFinalDestination = i == legs.lastIndex)
                }
                leg.mode == LegMode.BUS -> {
                    val bus = leg.bus ?: return@forEachIndexed
                    // Bus stops between boarding and alighting, for the dropdown.
                    addBusBoardingStation(path, bus, leg.stops.drop(1).dropLast(1), busIndex)
                    addBusAlightingStation(path, bus, busIndex, isFinalDestination = i == legs.lastIndex)
                    // Two buses back to back change at the same stop, with no walk between.
                    if (next != null && next.mode == LegMode.BUS) {
                        next.bus?.let { addBusTransferNode(path, it) }
                    }
                    busIndex++
                }
            }
        }

        // A journey that ends on a ride still names where it ends.
        if (legs.lastOrNull()?.mode?.isTransit == true && destDisplay.isNotEmpty()) {
            addDestinationNode(path, destDisplay)
        }

        if (card.usesRail) {
            renderMri(cardBinding, card)
            // Store reference for timetable updates
            cardBinding.progressTimings.visibility = View.VISIBLE
            currentTimetableCard = cardBinding
            // Timings are hung on the rail boarding stations only.
            vm.fetchTimetable(card)
        } else {
            cardBinding.progressTimings.visibility = View.GONE
            cardBinding.layoutTrainPager.visibility = View.GONE
            cardBinding.cardMri.visibility = View.GONE
        }
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

    /**
     * A walk at either end of the journey. Over 200 m it also offers an auto
     * and a cab (see [LastMile]); the rider's pick is kept for Live Navigation.
     */
    private fun addWalkNode(
        container: LinearLayout,
        label: String,
        card: RouteCard,
        leg: RouteLeg,
        targetName: String,
        isFirst: Boolean
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).text = label
        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE
        if (isFirst) view.findViewById<View>(R.id.viewLineTop).visibility = View.INVISIBLE
        bindLastMile(view, card, leg, targetName, isOrigin = isFirst)
        container.addView(view)
    }

    /**
     * Fills a walk node's subtitle with the chosen way of covering it and,
     * when there is a choice, a row of Walk / Auto / Cab options showing time,
     * distance and fare. Tapping one re-binds the node in place.
     */
    private fun bindLastMile(
        view: View,
        card: RouteCard,
        leg: RouteLeg,
        targetName: String,
        isOrigin: Boolean = false,
        title: ((LastMileMode) -> String)? = null
    ) {
        val options = vm.lastMileOptions(leg)
        val chosenMode = vm.lastMileChoice(card, leg)
        val chosen = options.firstOrNull { it.mode == chosenMode } ?: options.first()
        val colour = lastMileColour(chosen.mode)

        title?.let { view.findViewById<TextView>(R.id.tvStationName).text = it(chosen.mode) }
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = if (chosen.mode.byRoad)
                "${chosen.mode.emoji} ${chosen.mode.label} ~₹${chosen.fareInr} · ~${chosen.minutes} min $targetName"
            else
                "Walk ~${chosen.minutes} min · ${LastMile.distanceLabel(chosen.metres)} $targetName"
            visibility = View.VISIBLE
            setTextColor(if (chosen.mode.byRoad) Color.parseColor(colour)
                         else ContextCompat.getColor(context, R.color.text_muted))
        }
        setDot(view, colour, isOrigin = isOrigin)

        // Options row sits under the subtitle; rebuilt on every bind.
        val pill = view.findViewById<LinearLayout>(R.id.llLinePill)
        val column = pill.parent as LinearLayout
        column.findViewWithTag<View>(LAST_MILE_ROW)?.let { column.removeView(it) }
        if (options.size < 2) return

        val row = LinearLayout(this).apply {
            tag = LAST_MILE_ROW
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(8) }
        }
        options.forEachIndexed { idx, o ->
            val selected = o.mode == chosen.mode
            val chip = TextView(this).apply {
                text = "${o.mode.emoji} ${o.mode.label}\n${LastMile.metaLabel(o)}"
                textSize = 11f
                gravity = Gravity.CENTER
                setLineSpacing(dpToPx(2).toFloat(), 1f)
                setTextColor(ContextCompat.getColor(context,
                    if (selected) R.color.accent_blue else R.color.text_primary))
                setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(
                    if (selected) R.drawable.bg_station_option_selected else R.drawable.bg_station_option
                )
                setPadding(dpToPx(6), dpToPx(8), dpToPx(6), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx > 0) marginStart = dpToPx(6)
                }
                setOnClickListener {
                    if (o.mode == vm.lastMileChoice(card, leg)) return@setOnClickListener
                    vm.setLastMileChoice(card, leg, o.mode)
                    bindLastMile(view, card, leg, targetName, isOrigin, title)
                    // The Next step panel names the first leg, so keep it in step.
                    expandedCards.forEach { (b, c) -> if (c.scenario == card.scenario) bindNextStep(b, c, firstTrainShown[b]) }
                }
            }
            row.addView(chip)
        }
        column.addView(row, column.indexOfChild(pill) + 1)
    }

    private fun lastMileColour(mode: LastMileMode): String = when (mode) {
        LastMileMode.WALK -> "#6b7280"
        LastMileMode.AUTO -> "#B45309"
        LastMileMode.CAB -> "#1f2937"
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

        bindStopsDropdown(
            toggle = toggle,
            list = skippedList,
            intermediateStops = skippedStops,
            headerText = "Stops between ${edge.fromStop} and $alightAt",
            alightAt = alightAt,
            colorHex = lineColor(edge.line),
            travelMin = travelMin
        )

        container.addView(view)
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Stops dropdown — one builder for rail, metro and bus
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fills the "N stops · tap to expand" toggle and the list beneath it.
     *
     * Shared between rail and bus deliberately. A bus leg that listed its stops
     * in its own layout would read as a different feature to the one the
     * commuter just used on the train leg above it, and the two would drift
     * apart the first time either was touched.
     */
    private fun bindStopsDropdown(
        toggle: TextView,
        list: LinearLayout,
        intermediateStops: List<String>,
        headerText: String,
        alightAt: String,
        colorHex: String,
        travelMin: Int,
        totalStops: Int = intermediateStops.size + 1
    ) {
        val stopCount = intermediateStops.size + 1

        if (intermediateStops.isEmpty()) {
            // Nothing to hide — the one stop that matters is named in the toggle
            // itself and the list below shows it, with no expand affordance.
            // A ride the engine costed at several stops whose names could not be
            // placed still says how many there are rather than claiming "direct".
            toggle.visibility = View.VISIBLE
            toggle.text = if (totalStops > 1) "$totalStops stops · +${travelMin}m"
                          else "Direct to $alightAt · +${travelMin}m"
            toggle.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            toggle.setOnClickListener(null)
            toggle.isClickable = false

            list.visibility = View.VISIBLE
            list.background = makeDropdownBackground()
            list.addView(makeStopRow(alightAt, colorHex, colorHex, bold = true, trailing = "+${travelMin}m"))
            return
        }

        toggle.visibility = View.VISIBLE
        toggle.setTextColor(ContextCompat.getColor(this, R.color.accent_blue))
        toggle.text = "▶ $stopCount stops · tap to expand"
        toggle.setOnClickListener {
            if (list.visibility == View.GONE) {
                list.visibility = View.VISIBLE
                toggle.text = "▼ $stopCount stops · tap to collapse"
            } else {
                list.visibility = View.GONE
                toggle.text = "▶ $stopCount stops · tap to expand"
            }
        }

        list.background = makeDropdownBackground()
        list.addView(TextView(this).apply {
            text = headerText
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(4))
        })

        for (stop in intermediateStops) {
            list.addView(makeStopRow(stop, "#C8C5BC", "#6B6860", bold = false, trailing = null))
        }

        list.addView(makeDivider())
        list.addView(makeStopRow(alightAt, colorHex, colorHex, bold = true, trailing = "+${travelMin}m"))
    }

    /** One row of the stops dropdown: dot, name and an optional running time. */
    private fun makeStopRow(
        name: String,
        dotColorHex: String,
        textColorHex: String,
        bold: Boolean,
        trailing: String?
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), if (trailing != null) dpToPx(6) else dpToPx(4))
        }
        row.addView(View(this).apply {
            val dotSize = dpToPx(6)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginEnd = dpToPx(10)
            }
            background = makeOvalDrawable(safeColor(dotColorHex))
        })
        row.addView(TextView(this).apply {
            text = name
            textSize = 11.5f
            if (bold) setTypeface(null, Typeface.BOLD)
            setTextColor(safeColor(textColorHex))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (trailing != null) {
            row.addView(TextView(this).apply {
                text = trailing
                textSize = 10f
                setTextColor(ContextCompat.getColor(this@RouteListActivity, R.color.text_hint))
            })
        }
        return row
    }

    private fun safeColor(hex: String): Int = try {
        Color.parseColor(hex)
    } catch (e: Exception) {
        ContextCompat.getColor(this, R.color.accent_blue)
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Walk and bus nodes
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * The short hop between a bus stop and a station platform.
     *
     * Drawn as its own node rather than folded into the bus or the train, so a
     * commuter reading the card knows there is a walk there before they are
     * standing at the stop wondering where the station went.
     */
    private fun addFeederLinkNode(container: LinearLayout, title: String, sub: String): View {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).apply {
            text = title
            setTypeface(null, Typeface.BOLD)
        }
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = sub
            visibility = View.VISIBLE
        }
        setDot(view, "#6b7280", isOrigin = false)

        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE

        container.addView(view)
        return view
    }

    private fun addBusBoardingStation(
        container: LinearLayout,
        leg: BusLeg,
        intermediateStops: List<String>,
        legIndex: Int
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        // No "boarding_station" tag: that tag is what the timetable observer
        // looks for, and a bus leg has no trains to hang there.
        val colorHex = LineStyle.busColorHex(legIndex)

        view.findViewById<TextView>(R.id.tvStationName).text = leg.boardStop
        setDot(view, colorHex, isOrigin = false)

        // A route whose stop order was rebuilt rather than read is said so here,
        // next to the list it affects, rather than in a footnote nobody links
        // back to the bus they are about to board.
        if (leg.seqConf == "low") {
            view.findViewById<TextView>(R.id.tvStationSub).apply {
                text = "Stop order reconstructed — check the board before boarding"
                visibility = View.VISIBLE
                setTextColor(Color.parseColor("#B45309"))
            }
        }

        val pill = view.findViewById<LinearLayout>(R.id.llLinePill)
        pill.visibility = View.VISIBLE
        val pillText = view.findViewById<TextView>(R.id.tvLinePill)
        pillText.text = "${leg.operator} ${leg.routeRef}"
        setDrawableColor(pillText, colorHex)

        // The engine's own count leads: it is what the ride was costed on, and
        // it holds even where a stop name could not be placed on the route.
        val stopCount = maxOf(leg.stopCount, intermediateStops.size + 1)
        view.findViewById<TextView>(R.id.tvLegInfo).text =
            "${leg.travelMin} min · $stopCount stop${if (stopCount > 1) "s" else ""}"

        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE

        bindStopsDropdown(
            toggle = view.findViewById(R.id.tvSkippedToggle),
            list = view.findViewById(R.id.llSkippedStops),
            intermediateStops = intermediateStops,
            headerText = "Stops between ${leg.boardStop} and ${leg.alightStop}",
            alightAt = leg.alightStop,
            colorHex = colorHex,
            travelMin = leg.travelMin,
            totalStops = stopCount
        )

        container.addView(view)
    }

    private fun addBusAlightingStation(
        container: LinearLayout,
        leg: BusLeg,
        legIndex: Int,
        isFinalDestination: Boolean
    ) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).apply {
            text = leg.alightStop
            if (isFinalDestination) textSize = 14f
        }
        setDot(view, LineStyle.busColorHex(legIndex), isOrigin = false)

        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE

        if (isFinalDestination) view.findViewById<View>(R.id.viewLineBottom).visibility = View.INVISIBLE
        container.addView(view)
    }

    private fun addBusTransferNode(container: LinearLayout, nextLeg: BusLeg) {
        val view = layoutInflater.inflate(R.layout.item_journey_station, container, false)
        view.findViewById<TextView>(R.id.tvStationName).apply {
            text = "→ Change bus at ${nextLeg.boardStop}"
            setTypeface(null, Typeface.BOLD)
        }
        view.findViewById<TextView>(R.id.tvStationSub).apply {
            text = "Take ${nextLeg.operator} ${nextLeg.routeRef} from here"
            visibility = View.VISIBLE
        }
        setDot(view, "#7c3aed", isOrigin = false)
        view.setBackgroundColor(Color.parseColor("#F5F3FF"))

        view.findViewById<LinearLayout>(R.id.llLinePill).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvLegInfo).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.llTrainTimings).visibility = View.GONE

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

        // Where it's heading is what a commuter reads off the indicator board and
        // repeats to somebody else. The number is carried on TrainTiming for
        // live-status lookups but is never shown.
        val terminus = train.terminus.ifEmpty { "—" }.uppercase()
        view.findViewById<TextView>(R.id.tvTrainName).text = terminus
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
