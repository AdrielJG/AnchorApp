package com.example.mumbaitransit.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.auth.AuthRepository
import com.example.mumbaitransit.backend.Backend
import com.example.mumbaitransit.databinding.ActivityLiveJourneyBinding
import com.example.mumbaitransit.databinding.ItemJourneyLegBinding
import com.example.mumbaitransit.journey.JourneyBuilder
import com.example.mumbaitransit.journey.JourneyLeg
import com.example.mumbaitransit.journey.JourneyPing
import com.example.mumbaitransit.journey.JourneyRepository
import com.example.mumbaitransit.journey.JourneySession
import com.example.mumbaitransit.journey.JourneyStatus
import com.example.mumbaitransit.journey.LegKind
import com.example.mumbaitransit.journey.LiveJourney
import com.example.mumbaitransit.journey.LiveJourneyService
import com.example.mumbaitransit.journey.PingSource
import com.example.mumbaitransit.journey.ShareCode
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.navigation.NavigationSession
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Sets up a live journey, then becomes its control panel.
 *
 * One screen rather than two because the rider's job does not actually change
 * when sharing starts: they are still confirming which train they are on. Two
 * screens would mean re-finding the journey to correct a mis-picked service,
 * which is exactly the moment it matters most.
 *
 * Reached three ways — the "Share live journey" button on a route card, the
 * ongoing notification, and re-opening the app mid-journey.
 */
class LiveJourneyActivity : AppCompatActivity() {

    companion object {
        /** Opens setup for the route currently parked in the shared ViewModel. */
        fun start(context: android.content.Context) {
            context.startActivity(Intent(context, LiveJourneyActivity::class.java))
        }
    }

    private lateinit var binding: ActivityLiveJourneyBinding
    private lateinit var authRepo: AuthRepository
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }
    private val repo by lazy { JourneyRepository.get(this) }

    private val clock = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    /** The draft being set up, or the running journey once sharing has begun. */
    private var draft: LiveJourney? = null

    /** Timetable legs the pickers draw from. Empty once the journey is running. */
    private var timetable: List<TimetableLeg> = emptyList()

    private val sharing: Boolean get() = JourneySession.isSharing

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) requestNotificationPermission()
        else Toast.makeText(
            this,
            "Location permission is what makes this live — sharing not started",
            Toast.LENGTH_LONG
        ).show()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { beginSharing() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveJourneyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        authRepo = AuthRepository(this)

        val running = JourneySession.journey.value
        if (running != null) {
            draft = running
            renderRunning()
        } else {
            val card = vm.journeyCard
            if (card == null) {
                Toast.makeText(this, "Pick a route first", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            prepareDraft(card)
        }

        binding.btnStartSharing.setOnClickListener { onStartTapped() }
        binding.btnStopSharing.setOnClickListener { confirmStop() }
        binding.btnShareAgain.setOnClickListener { draft?.let { sendShareSheet(it) } }

        observeSession()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun prepareDraft(card: RouteCard) {
        supportActionBar?.title = "Share live journey"
        lifecycleScope.launch {
            timetable = if (card.type == "transit") vm.buildTimetable(card) else emptyList()
            // Sharing from live navigation shares the journey being navigated —
            // same code, same locked-in trains — so the two stay in step.
            val navigated = NavigationSession.journey
                ?.takeIf { NavigationSession.isActive && NavigationSession.routeKey == card.scenario }
            draft = navigated?.copy(
                ownerUid = authRepo.session.getUid().ifBlank { "local" },
                ownerName = authRepo.session.getUsername().ifBlank { "A commuter" }
            ) ?: JourneyBuilder.build(
                card = card,
                legs = timetable,
                ownerUid = authRepo.session.getUid().ifBlank { "local" },
                ownerName = authRepo.session.getUsername().ifBlank { "A commuter" }
            )
            renderSetup(card)
        }
    }

    private fun renderSetup(card: RouteCard) {
        val journey = draft ?: return
        binding.tvJourneyRoute.text = "${journey.originLabel}  →  ${journey.destLabel}"
        binding.tvJourneySummary.text =
            "${card.scenarioLabel} · ${card.totalMin.roundToInt()} min · ₹${card.totalFare}"

        binding.panelLive.visibility = View.GONE
        binding.cardShareCode.visibility = View.GONE
        binding.btnStopSharing.visibility = View.GONE
        binding.btnStartSharing.visibility = View.VISIBLE

        val scheduled = journey.scheduledLegs().size
        binding.tvLegsHint.text = if (scheduled == 0) {
            "Your friend will see where you are and how far along you've got, updated as you move."
        } else {
            "Tap a train or metro leg to lock in the one you actually boarded. " +
                "It's pre-filled with the next service, so you can leave it as is."
        }

        binding.tvPrivacyNote.text =
            "Your live location is shared with whoever holds the link, until you arrive or stop."

        renderLegs(journey, null)
    }

    // ── Running ───────────────────────────────────────────────────────────────

    private fun renderRunning() {
        val journey = draft ?: return
        supportActionBar?.title = "Live journey"
        binding.tvJourneyRoute.text = "${journey.originLabel}  →  ${journey.destLabel}"
        binding.tvJourneySummary.text =
            "Sharing since ${clock.format(Date(journey.startedAt))} · " +
                "${journey.plannedTotalMin} min planned"

        binding.panelLive.visibility = View.VISIBLE
        binding.cardShareCode.visibility = View.VISIBLE
        binding.tvShareCode.text = journey.code
        binding.tvShareCodeHint.text = if (Backend.isCloud(this)) {
            "Whoever has this code or the link can follow you in Anchor."
        } else {
            "This build has no database configured, so nobody can follow this code yet — " +
                "see FIREBASE_SETUP.md."
        }

        binding.btnStartSharing.visibility = View.GONE
        binding.btnStopSharing.visibility = View.VISIBLE
        binding.tvLegsHint.text =
            "Tap a train or metro leg any time to correct which service you're on."
        binding.tvPrivacyNote.text = "Sharing stops on its own when you arrive."

        renderLegs(journey, JourneySession.ping.value)
    }

    private fun observeSession() {
        lifecycleScope.launch {
            combine(JourneySession.journey, JourneySession.ping) { j, p -> j to p }
                .collect { (journey, ping) ->
                    if (journey == null) {
                        // The service finished while this screen was open.
                        if (sharingWasShown) {
                            binding.btnStopSharing.visibility = View.GONE
                            binding.tvLiveBadge.text = "ENDED"
                            binding.tvPrivacyNote.text = "Sharing has ended."
                        }
                        return@collect
                    }
                    sharingWasShown = true
                    draft = journey
                    renderLive(journey, ping)
                    renderLegs(journey, ping)
                }
        }
    }

    private var sharingWasShown = false

    private fun renderLive(journey: LiveJourney, ping: JourneyPing?) {
        if (ping == null) {
            binding.tvLiveBadge.text = "STARTING"
            applyBadge(binding.tvLiveBadge, "#6B6860")
            binding.tvLiveWhere.text = "Waiting for a first fix…"
            binding.tvLiveNext.text = ""
            binding.tvLiveEta.text = ""
            binding.progressJourney.progress = 0
            return
        }

        // Never call a projection "live". The whole value of this to the person
        // waiting is that the word means what it says.
        val (badge, colour) = when {
            ping.ended                     -> "ENDED" to "#6B6860"
            ping.isSighting                -> "LIVE GPS" to "#0A7C42"
            ping.source == PingSource.SCHEDULE -> "ON SCHEDULE" to "#B45309"
            else                           -> "NO SIGNAL" to "#9B1C1C"
        }
        binding.tvLiveBadge.text = badge
        applyBadge(binding.tvLiveBadge, colour)
        binding.tvLiveUpdated.text = "Updated ${clock.format(Date(ping.at))}"

        val leg = journey.legAt(ping.legIndex)
        binding.tvLiveWhere.text = when {
            ping.ended -> "Journey ${if (ping.status == JourneyStatus.COMPLETED) "completed" else "stopped"}"
            ping.nearestStation.isNotBlank() -> "At ${ping.nearestStation}"
            else -> leg?.title().orEmpty()
        }
        binding.tvLiveNext.text = when {
            ping.ended -> ""
            ping.nextStop.isNotBlank() -> "Next: ${ping.nextStop}"
            else -> ""
        }
        binding.progressJourney.progress = ping.progressPct
        binding.tvLiveEta.text = if (ping.ended) ""
            else "${ping.progressPct}% of the way · arriving around ${clock.format(Date(ping.etaEpochMs))}"
    }

    // ── Legs ──────────────────────────────────────────────────────────────────

    private fun renderLegs(journey: LiveJourney, ping: JourneyPing?) {
        binding.containerLegs.removeAllViews()
        journey.legs.forEach { leg ->
            val b = ItemJourneyLegBinding.inflate(
                LayoutInflater.from(this), binding.containerLegs, false
            )
            JourneyLegView.bind(b, leg, ping, editable = true)

            if (leg.legKind.isScheduled) {
                b.rowLegService.setOnClickListener { pickService(leg) }
            }
            binding.containerLegs.addView(b.root)
        }
    }

    /**
     * Lets the rider name the exact service they boarded.
     *
     * Before sharing starts the options come from the timetable built for the
     * route. Once it is running the timetable has usually gone stale, so the
     * engine is asked afresh from the current clock — a rider correcting this
     * mid-journey is almost always doing it because they let one go past.
     * Same picker live navigation uses.
     */
    private fun pickService(leg: JourneyLeg) {
        ServicePicker.show(
            activity = this,
            vm = vm,
            line = leg.line,
            lineShort = leg.lineShort,
            fromStop = leg.fromStop,
            toStop = leg.toStop,
            isMetro = leg.legKind == LegKind.METRO,
            timetable = timetable,
            preferLive = false
        ) { t -> lockService(leg, t.depFrom, t.arrTo, t.terminus, t.trainType, t.ac, t.trainNo) }
    }

    private fun lockService(
        leg: JourneyLeg,
        depFrom: String,
        arrTo: String,
        terminus: String,
        trainType: String,
        ac: Boolean,
        trainNo: String?
    ) {
        val updated = leg.copy(
            serviceLabel = JourneyBuilder.serviceLabel(depFrom, arrTo, terminus, trainType, ac),
            serviceDepMins = JourneyBuilder.toMins(depFrom),
            serviceArrMins = JourneyBuilder.toMins(arrTo),
            serviceTerminus = terminus.takeIf { it.isNotBlank() },
            serviceTrainNo = trainNo
        )

        // Navigating the same journey? Its train follows the correction too.
        if (NavigationSession.code == draft?.code) {
            NavigationSession.lockService(
                leg.index, updated.serviceLabel.orEmpty(), updated.serviceDepMins,
                updated.serviceArrMins, updated.serviceTerminus, updated.serviceTrainNo
            )
        }

        if (sharing) {
            JourneySession.updateLeg(leg.index, updated)
            val journey = JourneySession.journey.value ?: return
            draft = journey
            lifecycleScope.launch {
                runCatching { repo.updateLegs(journey.code, journey.legs) }
            }
            LiveJourneyService.reloadLegs(this)
        } else {
            val journey = draft ?: return
            val legs = journey.legs.toMutableList()
            legs[leg.index] = updated
            draft = journey.copy(legs = legs)
            renderLegs(draft!!, null)
        }
    }

    // ── Start / stop ──────────────────────────────────────────────────────────

    private fun onStartTapped() {
        if (draft == null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * The ongoing notification is how sharing stays visible and stoppable, so
     * on Android 13+ it is asked for before starting rather than after.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            beginSharing()
        }
    }

    private fun beginSharing() {
        val journey = draft?.copy(startedAt = System.currentTimeMillis()) ?: return
        draft = journey
        JourneySession.begin(journey)
        LiveJourneyService.start(this)
        renderRunning()
        sendShareSheet(journey)
    }

    private fun confirmStop() {
        AlertDialog.Builder(this)
            .setTitle("Stop sharing?")
            .setMessage("Your friend will stop seeing where you are.")
            .setPositiveButton("Stop sharing") { _, _ ->
                LiveJourneyService.stop(this)
                Toast.makeText(this, "Live sharing stopped", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Keep sharing", null)
            .show()
    }

    // ── Share sheet ───────────────────────────────────────────────────────────

    private fun sendShareSheet(journey: LiveJourney) {
        val text = buildString {
            appendLine("${journey.ownerName} is on the way to ${journey.destLabel}.")
            appendLine("Follow the journey live: ${ShareCode.link(journey.code)}")
            appendLine()
            appendLine("Or open Anchor → Follow a journey and enter code ${journey.code}.")
            appendLine()
            append("Shared from Anchor")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Live journey to ${journey.destLabel}")
        }
        runCatching { startActivity(Intent.createChooser(send, "Share live journey")) }
            .onFailure {
                Toast.makeText(this, "No app available to share with", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyBadge(view: android.widget.TextView, hex: String) {
        val colour = Color.parseColor(hex)
        view.setTextColor(colour)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(26, Color.red(colour), Color.green(colour), Color.blue(colour)))
            cornerRadius = 20 * resources.displayMetrics.density
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
