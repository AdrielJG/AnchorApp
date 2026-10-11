package com.example.mumbaitransit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mumbaitransit.BuildConfig
import com.example.mumbaitransit.R
import com.example.mumbaitransit.auth.SessionManager
import com.example.mumbaitransit.databinding.ActivityNavigationBinding
import com.example.mumbaitransit.engine.LastMile
import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.databinding.ItemNavStepBinding
import com.example.mumbaitransit.databinding.ItemNavStopBinding
import com.example.mumbaitransit.journey.JourneyBuilder
import com.example.mumbaitransit.journey.JourneyRepository
import com.example.mumbaitransit.journey.JourneySession
import com.example.mumbaitransit.journey.LiveJourneyService
import com.example.mumbaitransit.live.LiveResult
import com.example.mumbaitransit.live.RailRadarClient
import com.example.mumbaitransit.live.TrainNumberStore
import com.example.mumbaitransit.map.DirectionsClient
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.navigation.NavAction
import com.example.mumbaitransit.navigation.NavLeg
import com.example.mumbaitransit.navigation.NavMode
import com.example.mumbaitransit.navigation.NavPhase
import com.example.mumbaitransit.navigation.NavPlanBuilder
import com.example.mumbaitransit.navigation.NavSnapshot
import com.example.mumbaitransit.navigation.NavState
import com.example.mumbaitransit.navigation.NavTrainCheck
import com.example.mumbaitransit.navigation.NavigationSession
import com.example.mumbaitransit.navigation.PositionSource
import com.example.mumbaitransit.navigation.StepStatus
import com.example.mumbaitransit.navigation.StopStatus
import com.example.mumbaitransit.navigation.formatDistance
import com.example.mumbaitransit.navigation.hhmm
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live navigation: guides the rider through the route they picked, one leg at
 * a time.
 *
 * Everything shown comes from [NavigationSession]'s snapshot, which the
 * tracking service keeps current from GPS and the clock. This screen only
 * draws it and passes on taps, so it can be closed and reopened (or reached
 * from the notification) mid-journey without losing its place.
 *
 * What is live and what isn't is kept honest on screen:
 *  - Walking: the rider's own GPS.
 *  - Train: the rider's GPS plus RailRadar live status for the chosen train.
 *  - Metro: GPS, or the timetable when underground — labelled as such.
 *  - Bus: the rider's GPS against the stop list. The bus itself is not
 *    tracked and nothing here suggests it is.
 */
class NavigationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_BEGIN = "begin"

        /** How often the live train status may be re-fetched. Each call spends quota. */
        private const val LIVE_REFRESH_MS = 120_000L

        /** Collapsed stop lists show this many upcoming stops before "…". */
        private const val UPCOMING_SHOWN = 3

        /** Starts navigating [TransitViewModel.navCard]. */
        fun begin(context: Context) {
            context.startActivity(
                Intent(context, NavigationActivity::class.java).putExtra(EXTRA_BEGIN, true)
            )
        }

        /** Reopens the navigation already running. */
        fun open(context: Context) {
            context.startActivity(
                Intent(context, NavigationActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    private lateinit var binding: ActivityNavigationBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }
    private val clock = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    /** Timetable the route was planned on — offered first in "Change train". */
    private var timetable: List<TimetableLeg> = emptyList()

    private var hasLocation = false
    private var preparing = false
    private var showAllStops = false

    // Map
    private var map: GoogleMap? = null
    private var mapLeg = -1
    private var walkPoints: List<LatLng> = emptyList()
    /** "legIndex:mode" of the path currently fetched — walking, or driving for auto/cab. */
    private var walkFetchKey: String? = null
    private var lastCameraAt: LatLng? = null

    // Live train
    private val rail by lazy { RailRadarClient(this) }
    private val trainNumbers by lazy { TrainNumberStore(this) }
    private var liveKey: String? = null
    private var liveFetchedAt = 0L
    private var liveResult: LiveResult? = null
    private var liveJob: Job? = null

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocation = granted
        if (granted) requestNotificationPermission()
        else Toast.makeText(
            this,
            "Without location, move through the journey with the buttons",
            Toast.LENGTH_LONG
        ).show()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startTracking() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Back leaves navigation running; the route card offers "Resume".
        binding.toolbar.setNavigationOnClickListener { finish() }

        hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        wireButtons()
        setupMap()

        val begin = intent.getBooleanExtra(EXTRA_BEGIN, false)
        when {
            begin -> {
                val card = vm.navCard
                if (card == null) { finish(); return }
                prepare(card)
            }
            NavigationSession.hasSession -> Unit
            else -> { finish(); return }
        }
        observe()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ── Starting ──────────────────────────────────────────────────────────────

    /**
     * Builds the leg list exactly as live sharing does (so the two can share a
     * journey), places every stop, and starts the engine.
     */
    private fun prepare(card: RouteCard) {
        preparing = true
        binding.progressPreparing.visibility = View.VISIBLE
        binding.scrollNav.visibility = View.GONE
        binding.barBottom.visibility = View.GONE

        lifecycleScope.launch {
            timetable = if (card.legs.any { it.mode.isRail }) vm.buildTimetable(card) else emptyList()
            val session = SessionManager(this@NavigationActivity)
            val journey = JourneyBuilder.build(
                card = card,
                legs = timetable,
                ownerUid = session.getUid().ifBlank { "local" },
                ownerName = session.getUsername().ifBlank { "A commuter" }
            )
            val plan = NavPlanBuilder.build(
                card, journey,
                vm.searchOriginLat, vm.searchOriginLon,
                vm.searchDestLat, vm.searchDestLon,
                if (vm.isEngineReady) vm.engine.busEngine else null,
                lastMile = { leg -> vm.lastMileOptions(leg) to vm.lastMileChoice(card, leg) }
            )
            if (plan == null) {
                Toast.makeText(this@NavigationActivity, "Navigation isn't available for this route", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Replacing a journey that was being navigated: stop its tracking first.
            if (NavigationSession.isActive) LiveJourneyService.stopNavigation(this@NavigationActivity)
            NavigationSession.begin(card, journey, plan)

            preparing = false
            binding.progressPreparing.visibility = View.GONE
            binding.scrollNav.visibility = View.VISIBLE
            binding.barBottom.visibility = View.VISIBLE

            if (hasLocation) requestNotificationPermission()
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /** The ongoing notification is how navigation stays reachable with the screen off. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        // The service is a location-type foreground service; it can only be
        // started with the permission in hand.
        if (hasLocation && NavigationSession.isActive) LiveJourneyService.startNavigation(this)
    }

    // ── Observing ─────────────────────────────────────────────────────────────

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    NavigationSession.snapshot.collect { snap ->
                        if (snap == null) {
                            if (!preparing) finish()
                            return@collect
                        }
                        render(snap)
                    }
                }
                launch { JourneySession.journey.collect { renderShareButton() } }
                // Keeps the clock-driven parts moving while the screen is open,
                // including when location was refused and the service isn't running.
                launch {
                    while (true) {
                        delay(5_000)
                        NavigationSession.onTick()
                    }
                }
            }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun render(s: NavSnapshot) {
        supportActionBar?.title = s.toolbarTitle
        renderOffRoute(s)
        renderReached(s)
        renderHero(s)
        renderLastMile(s)
        renderService(s)
        renderStops(s)
        renderActions(s)
        renderTimeline(s)
        renderMap(s)
        maybeFetchWalkRoute(s)
        renderShareButton()

        binding.btnStopNavigation.visibility = if (s.ended) View.GONE else View.VISIBLE
    }

    private fun renderOffRoute(s: NavSnapshot) {
        val msg = s.offRoute
        binding.panelOffRoute.visibility = if (msg != null && !s.ended) View.VISIBLE else View.GONE
        binding.tvOffRoute.text = msg.orEmpty()
        val leg = s.leg
        binding.btnOffRouteTrain.visibility =
            if (msg != null && leg != null && leg.mode.isRail) View.VISIBLE else View.GONE
        binding.btnOffRouteTrain.text = if (leg?.mode == NavMode.METRO) "Change metro" else "Change train"
    }

    private fun renderReached(s: NavSnapshot) {
        val text = when (s.state) {
            NavState.ARRIVED -> "You've arrived at ${s.plan.destLabel.ifBlank { "your destination" }}"
            NavState.REACHED_STOP -> "You've reached ${s.leg?.to.orEmpty()}"
            NavState.WAITING_FOR_TRAIN, NavState.WAITING_FOR_METRO, NavState.WAITING_FOR_BUS ->
                s.reached?.let { "You've reached $it" }
            else -> null
        }
        binding.panelReached.visibility = if (text != null) View.VISIBLE else View.GONE
        binding.tvReached.text = text.orEmpty()
    }

    private fun renderHero(s: NavSnapshot) {
        val leg = s.leg
        val focus: NavLeg? = when (s.state) {
            NavState.REACHED_STOP -> s.plan.legs.drop(s.legIndex + 1).firstOrNull { it.mode.isTransit } ?: s.nextLeg
            else -> leg
        }
        val mode = if (s.state == NavState.ARRIVED) null else focus?.mode

        binding.ivModeIcon.setImageResource(
            when (mode) {
                NavMode.WALK -> when {
                    focus?.byRoad != true -> R.drawable.ic_walk
                    focus?.travelBy == LastMileMode.AUTO -> R.drawable.ic_auto
                    else -> R.drawable.ic_cab
                }
                NavMode.BUS -> R.drawable.ic_bus
                NavMode.METRO -> R.drawable.ic_mode_metro
                NavMode.TRAIN -> R.drawable.ic_mode_rail
                null -> R.drawable.ic_dest_pin
            }
        )
        if (s.state == NavState.REACHED_STOP) binding.ivModeIcon.setImageResource(R.drawable.ic_transfer)

        val chip = when {
            s.state == NavState.ARRIVED -> "Arrived"
            mode == NavMode.WALK -> focus?.travelBy?.label ?: "Walk"
            focus != null -> focus.displayName
            else -> ""
        }
        val chipColour = when {
            s.state == NavState.ARRIVED -> "#0A7C42"
            mode == NavMode.WALK -> roadColour(focus?.travelBy)
            focus != null -> focus.colorHex
            else -> "#6B6860"
        }
        binding.tvModeChip.text = chip
        tint(binding.tvModeChip, chipColour, 12)

        // Say how the position is known — never call a projection live.
        val (badge, colour) = when {
            s.ended -> "" to "#6B6860"
            s.source == PositionSource.GPS -> "LIVE GPS" to "#0A7C42"
            s.source == PositionSource.SCHEDULE -> "FROM TIMETABLE" to "#B45309"
            !hasLocation -> "NO LOCATION" to "#9B1C1C"
            else -> "WAITING FOR GPS" to "#6B6860"
        }
        binding.tvSourceBadge.visibility = if (badge.isBlank()) View.GONE else View.VISIBLE
        binding.tvSourceBadge.text = badge
        tint(binding.tvSourceBadge, colour, 20)

        binding.tvEyebrow.text = s.eyebrow
        binding.tvHeadline.text = s.headline
        binding.tvDetail.text = s.detail
        binding.tvDetail.visibility = if (s.detail.isBlank()) View.GONE else View.VISIBLE

        // Walking: the next manoeuvre, then what's left.
        val walk = s.walk
        if (walk?.instruction != null) {
            binding.rowInstruction.visibility = View.VISIBLE
            binding.tvInstruction.text = walk.instruction
            binding.tvInstructionDistance.text = walk.instructionDistanceM?.let {
                formatDistance(it) + if (walk.straightLine) " in a straight line" else ""
            }.orEmpty()
            binding.ivInstruction.rotation = rotationFor(walk.maneuver)
        } else {
            binding.rowInstruction.visibility = View.GONE
        }

        val ride = s.ride
        val (pct, meta) = when {
            walk != null -> walk.progressPct to buildString {
                append("${walk.remainingMin} min remaining")
                walk.remainingM?.let { append(" · ~${formatDistance(it)}") }
                if (walk.straightLine && walk.remainingM != null) append(" (estimate)")
            }
            ride != null && ride.onBoard -> {
                val n = (leg?.stops?.size ?: 1) - 1
                val p = if (n > 0) (ride.reachedIndex * 100 / n) else 0
                p to buildString {
                    append("${ride.stopsRemaining} ${if (ride.stopsRemaining == 1) "stop" else "stops"} to ${leg?.to}")
                    if (ride.estimated) append(" · position estimated from the timetable")
                }
            }
            ride != null -> 0 to "Waiting to board"
            s.state == NavState.ARRIVED -> 100 to "Journey complete"
            else -> s.journeyPct to ""
        }
        binding.progressLeg.progress = pct
        binding.tvLegMeta.text = meta
        binding.tvLegMeta.visibility = if (meta.isBlank()) View.GONE else View.VISIBLE
    }

    /**
     * "How would you like to continue?" — Walk / Auto / Cab for a walk leg over
     * 200 m, in the order [com.example.mumbaitransit.engine.LastMile] sets.
     * Shown while on that leg (tap switches) and at a stop just before it
     * (tap picks and starts it).
     */
    private fun renderLastMile(s: NavSnapshot) {
        val (leg, startsLeg) = when {
            s.ended -> null to false
            s.state == NavState.REACHED_STOP && s.nextLeg?.hasLastMileChoice == true -> s.nextLeg to true
            s.phase == NavPhase.WALKING && s.leg?.hasLastMileChoice == true -> s.leg to false
            else -> null to false
        }
        binding.cardLastMile.visibility = if (leg != null) View.VISIBLE else View.GONE
        if (leg == null) return

        val isLast = leg.index == s.plan.legs.lastIndex
        val place = if (isLast) "Your destination" else leg.to
        val metres = s.walk?.remainingM?.takeIf { !startsLeg } ?: leg.lastMile.first().metres
        binding.tvLastMileHeader.text = "$place is ${LastMile.distanceLabel(metres)} away"

        val container = binding.containerLastMile
        container.removeAllViews()
        leg.lastMile.forEachIndexed { i, o ->
            val selected = o.mode == leg.travelBy
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(
                    if (selected) R.drawable.bg_station_option_selected else R.drawable.bg_station_option
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (i > 0) topMargin = dp(8) }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    NavigationSession.chooseTravel(leg.index, o.mode)
                    if (startsLeg) NavigationSession.perform(NavAction.CONTINUE)
                }
            }
            val icon = ImageView(this).apply {
                setImageResource(when (o.mode) {
                    LastMileMode.WALK -> R.drawable.ic_walk
                    LastMileMode.AUTO -> R.drawable.ic_auto
                    LastMileMode.CAB -> R.drawable.ic_cab
                })
                background = circle(Color.parseColor("#F1F3F7"))
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = dp(12) }
            }
            texts.addView(TextView(this).apply {
                text = o.mode.action
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            })
            texts.addView(TextView(this).apply {
                text = LastMile.metaLabel(o)
                textSize = 12.5f
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            })
            val chevron = ImageView(this).apply {
                setImageResource(R.drawable.ic_arrow_forward)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            }
            row.addView(icon)
            row.addView(texts)
            row.addView(chevron)
            container.addView(row)
        }
    }

    private fun roadColour(mode: LastMileMode?): String = when (mode) {
        LastMileMode.AUTO -> "#B45309"
        LastMileMode.CAB -> "#1F2937"
        else -> "#0A7C42"
    }

    /** The service card: the train/metro/bus of the current leg, or the next one at a change. */
    private fun renderService(s: NavSnapshot) {
        val leg: NavLeg? = when (s.phase) {
            NavPhase.WAITING, NavPhase.RIDING -> s.leg
            NavPhase.REACHED_STOP -> s.plan.legs.drop(s.legIndex + 1).firstOrNull { it.mode.isTransit }
            else -> null
        }
        if (leg == null || !leg.mode.isTransit) {
            binding.cardService.visibility = View.GONE
            return
        }
        binding.cardService.visibility = View.VISIBLE
        val upcoming = s.phase == NavPhase.REACHED_STOP || s.phase == NavPhase.WAITING

        when (leg.mode) {
            NavMode.BUS -> {
                binding.tvServiceHeader.text = if (upcoming) "NEXT BUS" else "YOUR BUS"
                binding.tvServiceTitle.text = leg.displayName
                binding.tvServiceTime.text = ""
                binding.tvServiceSub.text = "${leg.from} → ${leg.to} · ${leg.stops.lastIndex} stops"
                binding.tvLiveTrain.visibility = View.GONE
                binding.tvTrainCheck.visibility = View.GONE
                binding.btnLiveDetails.visibility = View.GONE
                binding.btnChangeTrain.visibility = View.GONE
                binding.tvBusNote.visibility = View.VISIBLE
            }
            else -> {
                val noun = if (leg.mode == NavMode.METRO) "Metro" else "Train"
                binding.tvServiceHeader.text = "${if (upcoming) "NEXT" else "YOUR"} ${noun.uppercase()} · ${leg.displayName.uppercase()}"
                binding.tvServiceTitle.text = leg.serviceTerminus?.takeIf { it.isNotBlank() }
                    ?.let { "$noun towards ${it.uppercase()}" } ?: "$noun to ${leg.to}"
                binding.tvServiceTime.text = leg.serviceDepMins?.let { hhmm(it) }.orEmpty()
                binding.tvServiceSub.text = buildString {
                    val dep = leg.serviceDepMins; val arr = leg.serviceArrMins
                    if (dep != null && arr != null) append("Scheduled ${hhmm(dep)} → ${hhmm(arr)} · ")
                    append("get off at ${leg.to}")
                    if (dep == null) append(" · pick the one you board")
                }
                binding.tvBusNote.visibility = View.GONE
                binding.btnChangeTrain.visibility = View.VISIBLE
                binding.btnChangeTrain.text = "Change ${noun.lowercase()}"
                binding.btnChangeTrain.setOnClickListener { changeService(leg) }

                // RailRadar tracks suburban rail; metro has no live feed here.
                if (leg.mode == NavMode.TRAIN) {
                    binding.btnLiveDetails.visibility = View.VISIBLE
                    binding.btnLiveDetails.setOnClickListener { openLiveDetails(leg) }
                    refreshLiveTrain(leg)
                    renderLiveTrain(s, leg)
                } else {
                    binding.btnLiveDetails.visibility = View.GONE
                    binding.tvLiveTrain.visibility = View.GONE
                    binding.tvTrainCheck.visibility = View.GONE
                }
            }
        }
    }

    private fun renderStops(s: NavSnapshot) {
        val leg = s.leg
        val ride = s.ride
        if (leg == null || ride == null) {
            binding.cardStops.visibility = View.GONE
            return
        }
        binding.cardStops.visibility = View.VISIBLE
        binding.tvStopsHeader.text = if (ride.onBoard)
            "NEXT STOP · ${ride.nextStop?.uppercase() ?: leg.to.uppercase()}"
        else "${leg.stops.lastIndex} STOPS TO ${leg.to.uppercase()}"

        val rows = ride.stops
        // Collapsed: last passed stop, the next one, a few ahead, then the alighting stop.
        val anchor = rows.indexOfFirst { it.status == StopStatus.NEXT }.coerceAtLeast(0)
        val visible: List<Int?> = if (showAllStops || rows.size <= UPCOMING_SHOWN + 4) rows.indices.toList()
        else {
            val from = (anchor - 1).coerceAtLeast(0)
            val to = (anchor + UPCOMING_SHOWN).coerceAtMost(rows.lastIndex - 1)
            buildList<Int?> {
                if (from > 0) { add(0); if (from > 1) add(null) }
                addAll(from..to)
                if (to < rows.lastIndex - 1) add(null)
                add(rows.lastIndex)
            }
        }

        binding.containerStops.removeAllViews()
        visible.forEachIndexed { pos, idx ->
            val b = ItemNavStopBinding.inflate(LayoutInflater.from(this), binding.containerStops, false)
            b.trackTop.visibility = if (pos == 0) View.INVISIBLE else View.VISIBLE
            b.trackBottom.visibility = if (pos == visible.lastIndex) View.INVISIBLE else View.VISIBLE
            if (idx == null) {
                b.tvStopName.text = "⋯"
                b.tvStopName.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                b.dot.background = null
                b.tvStopTag.visibility = View.GONE
            } else {
                val row = rows[idx]
                b.tvStopName.text = row.name
                b.dot.setBackgroundResource(
                    when (row.status) {
                        StopStatus.DONE -> R.drawable.dot_live_done
                        StopStatus.NEXT -> R.drawable.dot_live_current
                        StopStatus.UPCOMING -> R.drawable.dot_live_upcoming
                    }
                )
                b.tvStopName.setTypeface(null, if (row.status == StopStatus.NEXT || row.isAlighting) Typeface.BOLD else Typeface.NORMAL)
                b.tvStopName.setTextColor(ContextCompat.getColor(this,
                    if (row.status == StopStatus.DONE) R.color.text_muted else R.color.text_primary))
                val tag = when {
                    row.isAlighting -> "Get off here"
                    row.isBoarding && ride.onBoard -> "You boarded here"
                    row.isBoarding -> "Board here"
                    row.status == StopStatus.NEXT -> "Next stop"
                    else -> null
                }
                b.tvStopTag.visibility = if (tag != null) View.VISIBLE else View.GONE
                b.tvStopTag.text = tag.orEmpty()
                b.tvStopTag.setTextColor(ContextCompat.getColor(this,
                    if (row.isAlighting) R.color.red else R.color.accent_blue))
            }
            binding.containerStops.addView(b.root)
        }

        val collapsible = rows.size > UPCOMING_SHOWN + 4
        binding.tvToggleStops.visibility = if (collapsible) View.VISIBLE else View.GONE
        binding.tvToggleStops.text = if (showAllStops) "Show fewer stops" else "Show all ${rows.size} stops"
        binding.tvToggleStops.setOnClickListener {
            showAllStops = !showAllStops
            NavigationSession.snapshot.value?.let { renderStops(it) }
        }
    }

    private fun renderActions(s: NavSnapshot) {
        val primary = s.primary
        binding.btnPrimary.visibility = if (primary != null) View.VISIBLE else View.GONE
        binding.btnPrimary.text = s.primaryLabel.orEmpty()
        binding.btnPrimary.icon = if (s.primaryLabel == "Start Walking" || s.primaryLabel == "Take Auto" || s.primaryLabel == "Take Cab")
            ContextCompat.getDrawable(this, R.drawable.ic_navigate) else null
        binding.btnPrimary.setOnClickListener {
            if (primary == NavAction.FINISH) { finishNavigation(); return@setOnClickListener }
            if (primary != null) NavigationSession.perform(primary)
        }

        val fallback = s.fallback
        binding.btnFallback.visibility = if (fallback != null && !s.ended) View.VISIBLE else View.GONE
        binding.btnFallback.text = s.fallbackLabel.orEmpty()
        binding.btnFallback.setOnClickListener { if (fallback != null) NavigationSession.perform(fallback) }
    }

    private fun renderTimeline(s: NavSnapshot) {
        binding.progressJourney.progress = s.journeyPct
        binding.tvEta.text = if (s.state == NavState.ARRIVED) {
            s.arrivedAt?.let { "Arrived ${clock.format(Date(it))}" }.orEmpty()
        } else "Arrive ~${clock.format(Date(s.etaEpochMs))}"

        binding.containerTimeline.removeAllViews()
        s.timeline.forEach { step ->
            val b = ItemNavStepBinding.inflate(LayoutInflater.from(this), binding.containerTimeline, false)
            b.tvStepTitle.text = step.title
            b.tvStepDetail.text = step.detail
            val colour = Color.parseColor(if (step.mode == NavMode.WALK) "#0A7C42" else step.colorHex)
            when (step.status) {
                StepStatus.DONE -> {
                    b.tvStepMark.text = "✓"
                    b.tvStepMark.background = circle(Color.parseColor("#C9CDD5"))
                    b.tvStepTitle.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                    b.tvStepTitle.setTypeface(null, Typeface.NORMAL)
                    b.root.alpha = 0.75f
                }
                StepStatus.CURRENT -> {
                    b.tvStepMark.text = "●"
                    b.tvStepMark.background = circle(ContextCompat.getColor(this, R.color.accent_blue))
                    b.tvStepTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    b.tvStepTitle.setTypeface(null, Typeface.BOLD)
                    b.tvStepTitle.textSize = 14.5f
                }
                StepStatus.UPCOMING -> {
                    b.tvStepMark.text = ""
                    b.tvStepMark.background = ring(Color.parseColor("#C9CDD5"))
                    b.tvStepTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    b.tvStepTitle.setTypeface(null, Typeface.NORMAL)
                }
            }
            b.stepColor.background = GradientDrawable().apply {
                setColor(colour); cornerRadius = dp(2).toFloat()
            }
            binding.containerTimeline.addView(b.root)
        }
    }

    private fun renderShareButton() {
        val sharing = JourneySession.isSharing
        binding.btnShareJourney.text = if (sharing) "Sharing live" else "Share journey"
        binding.btnShareJourney.visibility =
            if (NavigationSession.snapshot.value?.ended == true && !sharing) View.GONE else View.VISIBLE
    }

    // ── Map (walking legs and arrival) ────────────────────────────────────────

    private fun setupMap() {
        // Without a key the map renders as a grey box; the instruction card
        // carries the walk on its own.
        if (BuildConfig.MAPS_API_KEY.isBlank()) return
        val fragment = supportFragmentManager.findFragmentById(R.id.navMap) as? SupportMapFragment ?: return
        fragment.getMapAsync { m ->
            map = m
            m.uiSettings.isMapToolbarEnabled = false
            m.uiSettings.isCompassEnabled = true
            enableMyLocation()
            NavigationSession.snapshot.value?.let { mapLeg = -1; renderMap(it) }
        }
    }

    @SuppressLint("MissingPermission")   // checked: hasLocation, and SecurityException caught
    private fun enableMyLocation() {
        val m = map ?: return
        if (!hasLocation) return
        try { m.isMyLocationEnabled = true } catch (_: SecurityException) { }
    }

    private fun renderMap(s: NavSnapshot) {
        val leg = s.leg
        val walking = leg?.mode == NavMode.WALK && s.phase == NavPhase.WALKING
        val show = BuildConfig.MAPS_API_KEY.isNotBlank() && (walking || s.state == NavState.ARRIVED)
        binding.cardMap.visibility = if (show) View.VISIBLE else View.GONE
        val m = map ?: return
        if (!show || leg == null) return
        enableMyLocation()

        val target = leg.stops.last()
        if (!target.placed) return
        val targetLatLng = LatLng(target.lat!!, target.lon!!)
        val here = if (s.lastLat != null && s.lastLon != null) LatLng(s.lastLat, s.lastLon) else null

        if (mapLeg != s.legIndex) {
            mapLeg = s.legIndex
            lastCameraAt = null
            m.clear()
            val start = leg.stops.first().takeIf { it.placed }?.let { LatLng(it.lat!!, it.lon!!) }
            val path = walkPoints.takeIf { walkFetchKey == fetchKey(s.legIndex, leg) && it.size >= 2 }
                ?: listOfNotNull(here ?: start, targetLatLng)
            if (path.size >= 2) {
                m.addPolyline(
                    PolylineOptions().addAll(path).width(10f)
                        .color(if (leg.byRoad) Color.parseColor(roadColour(leg.travelBy))
                               else ContextCompat.getColor(this, R.color.accent_blue))
                        .apply { if (!leg.byRoad) pattern(listOf(Dash(24f), Gap(14f))) }
                )
            }
            m.addMarker(
                MarkerOptions().position(targetLatLng).title(target.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Re-frame only when the rider has moved a fair bit — a camera that
        // jumps on every fix is impossible to read.
        val moved = here != null && (lastCameraAt == null ||
            com.example.mumbaitransit.navigation.distM(here.latitude, here.longitude,
                lastCameraAt!!.latitude, lastCameraAt!!.longitude) > 40.0)
        if (lastCameraAt == null || moved) {
            val pad = dp(56)
            if (here != null) {
                val bounds = LatLngBounds.builder().include(here).include(targetLatLng).build()
                runCatching { m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad)) }
                    .onFailure { m.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 16f)) }
                lastCameraAt = here
            } else {
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 16f))
                lastCameraAt = targetLatLng
            }
        }
    }

    private fun fetchKey(index: Int, leg: NavLeg) = "$index:${leg.travelBy}"

    /**
     * One Directions request per walk leg and travel choice, from where the
     * rider actually is: walking directions on foot, the road path by auto/cab.
     */
    private fun maybeFetchWalkRoute(s: NavSnapshot) {
        val leg = s.leg ?: return
        if (leg.mode != NavMode.WALK || s.phase != NavPhase.WALKING) return
        val key = fetchKey(s.legIndex, leg)
        if (walkFetchKey == key) return
        val lat = s.lastLat ?: return
        val lon = s.lastLon ?: return
        val target = leg.stops.last().takeIf { it.placed } ?: return
        if (BuildConfig.MAPS_API_KEY.isBlank()) return
        walkFetchKey = key
        walkPoints = emptyList()
        mapLeg = -1   // the old path no longer applies
        val index = s.legIndex
        lifecycleScope.launch {
            if (leg.byRoad) {
                when (val r = DirectionsClient.drivingRoute(lat, lon, target.lat!!, target.lon!!)) {
                    is DirectionsClient.Result.Success -> if (walkFetchKey == key) {
                        walkPoints = r.points
                        mapLeg = -1
                        NavigationSession.snapshot.value?.let { renderMap(it) }
                    }
                    is DirectionsClient.Result.Failure -> Unit  // straight line stays
                }
            } else {
                when (val r = DirectionsClient.walkingRoute(lat, lon, target.lat!!, target.lon!!)) {
                    is DirectionsClient.WalkResult.Success -> if (walkFetchKey == key) {
                        if (r.steps.isNotEmpty()) NavigationSession.setWalkSteps(index, r.steps)
                        walkPoints = r.points
                        mapLeg = -1   // redraw with the walked path
                        NavigationSession.snapshot.value?.let { renderMap(it) }
                    }
                    is DirectionsClient.WalkResult.Failure -> Unit  // compass heading stays, labelled
                }
            }
        }
    }

    // ── Live train ────────────────────────────────────────────────────────────

    private fun trainNumberFor(leg: NavLeg): String? {
        leg.serviceTrainNo?.takeIf { it.isNotBlank() }?.let { return it }
        val dep = leg.serviceDepMins ?: return null
        return trainNumbers.lookup(trainNumbers.signature(leg.line, leg.from, leg.to, hhmm(dep)))
    }

    /** Fetches RailRadar status for the leg's train, sparingly — every call spends quota. */
    private fun refreshLiveTrain(leg: NavLeg, force: Boolean = false) {
        val number = trainNumberFor(leg)
        val key = "${leg.index}|$number"
        if (number == null) {
            liveKey = key; liveResult = null
            return
        }
        val now = System.currentTimeMillis()
        if (!force && key == liveKey && now - liveFetchedAt < LIVE_REFRESH_MS) return
        if (liveJob?.isActive == true) return
        if (key != liveKey) liveResult = null
        liveKey = key
        liveFetchedAt = now
        liveJob = lifecycleScope.launch {
            liveResult = rail.fetchLive(number)
            NavigationSession.snapshot.value?.let { renderService(it) }
        }
    }

    private fun renderLiveTrain(s: NavSnapshot, leg: NavLeg) {
        val result = liveResult?.takeIf { liveKey?.startsWith("${leg.index}|") == true }
        val tv = binding.tvLiveTrain
        val check = binding.tvTrainCheck
        when {
            trainNumberFor(leg) == null -> {
                tv.visibility = View.VISIBLE
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                tv.text = "Live status: open live details to link this train"
                check.visibility = View.GONE
            }
            result is LiveResult.Success -> {
                val st = result.status
                tv.visibility = View.VISIBLE
                val delay = st.delayMinutes
                val where = st.current?.stationName?.takeIf { it.isNotBlank() }
                val kind = if (st.isActualPosition) "Live" else "Scheduled position"
                tv.text = buildString {
                    append("● $kind")
                    when {
                        delay == null -> Unit
                        delay <= 0 -> append(" · on time")
                        else -> append(" · running $delay min late")
                    }
                    where?.let { append(" · near $it") }
                }
                tv.setTextColor(Color.parseColor(when {
                    !st.isActualPosition -> "#B45309"
                    delay != null && delay > 5 -> "#9B1C1C"
                    else -> "#0A7C42"
                }))

                // Does the rider's GPS agree with where the train says it is?
                val ride = s.ride
                val verdict = if (st.isActualPosition && s.source == PositionSource.GPS && ride?.onBoard == true)
                    NavTrainCheck.matches(leg.stops.map { it.name }, ride.reachedIndex, where) else null
                when (verdict) {
                    true -> {
                        check.visibility = View.VISIBLE
                        check.setTextColor(ContextCompat.getColor(this, R.color.green))
                        check.text = "✓ Your position matches this train"
                    }
                    false -> {
                        check.visibility = View.VISIBLE
                        check.setTextColor(ContextCompat.getColor(this, R.color.red))
                        check.text = "This train is reported near $where — you may be on a different one. Change train?"
                        check.setOnClickListener { changeService(leg) }
                    }
                    null -> check.visibility = View.GONE
                }
            }
            result is LiveResult.Error -> {
                tv.visibility = View.VISIBLE
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                tv.text = "Live status unavailable · showing the timetable"
                check.visibility = View.GONE
            }
            else -> {
                tv.visibility = View.VISIBLE
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                tv.text = "Checking live status…"
                check.visibility = View.GONE
            }
        }
    }

    private fun openLiveDetails(leg: NavLeg) {
        LiveStatusActivity.start(
            context = this,
            line = leg.line,
            fromStop = leg.from,
            toStop = leg.to,
            departure = leg.serviceDepMins?.let { hhmm(it) }.orEmpty(),
            trainNo = trainNumberFor(leg)
        )
    }

    /** Lock in / correct the train or metro on [leg] — same picker live sharing uses. */
    private fun changeService(leg: NavLeg) {
        ServicePicker.show(
            activity = this,
            vm = vm,
            line = leg.line,
            lineShort = leg.displayName,
            fromStop = leg.from,
            toStop = leg.to,
            isMetro = leg.mode == NavMode.METRO,
            timetable = timetable,
            preferLive = true
        ) { t ->
            val label = JourneyBuilder.serviceLabel(t.depFrom, t.arrTo, t.terminus, t.trainType, t.ac)
            val dep = JourneyBuilder.toMins(t.depFrom)
            val arr = JourneyBuilder.toMins(t.arrTo)
            val terminus = t.terminus.takeIf { it.isNotBlank() }
            NavigationSession.lockService(leg.index, label, dep, arr, terminus, t.trainNo)
            liveKey = null
            syncToSharing(leg.index)
            Toast.makeText(this, "Train updated", Toast.LENGTH_SHORT).show()
        }
    }

    /** If this journey is also being shared, the follower sees the correction too. */
    private fun syncToSharing(index: Int) {
        val code = NavigationSession.code ?: return
        if (JourneySession.activeCode != code) return
        val leg = NavigationSession.journey?.legs?.getOrNull(index) ?: return
        JourneySession.updateLeg(index, leg)
        val shared = JourneySession.journey.value ?: return
        lifecycleScope.launch {
            runCatching { JourneyRepository.get(this@NavigationActivity).updateLegs(shared.code, shared.legs) }
        }
        LiveJourneyService.reloadLegs(this)
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun wireButtons() {
        binding.btnStopNavigation.setOnClickListener { confirmStop() }
        binding.btnRecalculate.setOnClickListener { recalculate() }
        binding.btnOffRouteTrain.setOnClickListener {
            NavigationSession.snapshot.value?.leg?.takeIf { it.mode.isRail }?.let { changeService(it) }
        }
        binding.btnShareJourney.setOnClickListener {
            // Reuses live sharing as it is: same screen, same code/link flow.
            if (!JourneySession.isSharing) vm.journeyCard = NavigationSession.card ?: return@setOnClickListener
            LiveJourneyActivity.start(this)
        }
    }

    private fun confirmStop() {
        val sharing = JourneySession.isSharing
        AlertDialog.Builder(this)
            .setTitle("Stop navigation?")
            .setMessage(
                if (sharing) "Live sharing will keep running until you stop it separately."
                else "Anchor will stop guiding you on this route."
            )
            .setPositiveButton("Stop navigation") { _, _ -> finishNavigation() }
            .setNegativeButton("Keep going", null)
            .show()
    }

    /** Ends navigation and its GPS use. Sharing, if on, is left alone. */
    private fun finishNavigation() {
        NavigationSession.stop()
        LiveJourneyService.stopNavigation(this)
        NavigationSession.clear()
        finish()
    }

    /**
     * Off route: plan again from here to the same destination. The routing
     * engine has no mid-journey rerouting, so this runs a fresh search and
     * returns to the routes list with the rider's position as the origin.
     */
    private fun recalculate() {
        val snap = NavigationSession.snapshot.value
        val lat = snap?.lastLat
        val lon = snap?.lastLon
        if (lat == null || lon == null) {
            Toast.makeText(this, "Waiting for your location…", Toast.LENGTH_SHORT).show()
            return
        }
        if (!vm.hasDest || !vm.isEngineReady) {
            Toast.makeText(this, "Search again from the home screen", Toast.LENGTH_SHORT).show()
            return
        }
        vm.setOrigin(lat, lon, "My Location", isMyLocation = true)
        vm.searchCurrentTrip()
        finishNavigation()
        startActivity(
            Intent(this, RouteListActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    /** Google's manoeuvre names onto the one arrow drawable. */
    private fun rotationFor(maneuver: String?): Float = when {
        maneuver == null -> 0f
        maneuver.contains("uturn") -> 180f
        maneuver.contains("sharp-left") -> -135f
        maneuver.contains("sharp-right") -> 135f
        maneuver.contains("slight-left") || maneuver.contains("fork-left") || maneuver.contains("keep-left") -> -35f
        maneuver.contains("slight-right") || maneuver.contains("fork-right") || maneuver.contains("keep-right") -> 35f
        maneuver.contains("left") -> -90f
        maneuver.contains("right") -> 90f
        else -> 0f
    }

    private fun tint(view: TextView, hex: String, radiusDp: Int) {
        val colour = Color.parseColor(hex)
        view.setTextColor(colour)
        view.background = GradientDrawable().apply {
            setColor(Color.argb(28, Color.red(colour), Color.green(colour), Color.blue(colour)))
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun circle(colour: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(colour)
    }

    private fun ring(colour: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(Color.WHITE); setStroke(dp(2), colour)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
