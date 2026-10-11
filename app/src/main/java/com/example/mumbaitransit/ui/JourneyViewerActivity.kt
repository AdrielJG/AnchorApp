package com.example.mumbaitransit.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.databinding.ActivityJourneyViewerBinding
import com.example.mumbaitransit.databinding.ItemJourneyLegBinding
import com.example.mumbaitransit.journey.JourneyPing
import com.example.mumbaitransit.journey.JourneyRepository
import com.example.mumbaitransit.journey.JourneyStatus
import com.example.mumbaitransit.journey.LegKind
import com.example.mumbaitransit.journey.LiveJourney
import com.example.mumbaitransit.journey.PingSource
import com.example.mumbaitransit.journey.ShareCode
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Follows somebody else's journey.
 *
 * Reached from a shared link, or by typing the code. This screen is the whole
 * point of the feature, so it is built around one question — "should I leave
 * for the station yet?" — and answers it with a position, a next stop and an
 * arrival time, in that order.
 *
 * The location is given as the nearest station plus a Maps hand-off rather than
 * an embedded map: an embedded map renders as a grey rectangle on any build
 * without a Maps key in local.properties, and a grey rectangle is a worse
 * answer than a station name.
 */
class JourneyViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODE = "journey_code"

        fun start(context: Context, code: String? = null) {
            context.startActivity(
                Intent(context, JourneyViewerActivity::class.java)
                    .putExtra(EXTRA_CODE, code)
            )
        }
    }

    private lateinit var binding: ActivityJourneyViewerBinding
    private val repo by lazy { JourneyRepository.get(this) }
    private val clock = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    private var journey: LiveJourney? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJourneyViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Live journey"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnFollow.setOnClickListener { submitCode() }
        binding.etCode.doAfterTextChanged {
            binding.tvCodeError.visibility = View.GONE
            binding.btnFollow.alpha = if ((it?.length ?: 0) >= 6) 1f else 0.45f
        }
        binding.btnFollow.alpha = 0.45f

        val code = codeFromIntent()
        if (code == null) showCodeEntry() else load(code)
    }

    /** A share link, a deep link, or an explicit extra all land here. */
    private fun codeFromIntent(): String? {
        intent.getStringExtra(EXTRA_CODE)
            ?.let { ShareCode.normalize(it) }
            ?.takeIf { ShareCode.isValid(it) }
            ?.let { return it }

        val tail = intent.data?.lastPathSegment ?: return null
        return ShareCode.normalize(tail).takeIf { ShareCode.isValid(it) }
    }

    // ── States ────────────────────────────────────────────────────────────────

    private fun show(state: View) {
        listOf(binding.stateCode, binding.stateLoading, binding.stateJourney)
            .forEach { it.visibility = if (it === state) View.VISIBLE else View.GONE }
    }

    private fun showCodeEntry(error: String? = null) {
        show(binding.stateCode)
        binding.tvCodeError.visibility = if (error == null) View.GONE else View.VISIBLE
        binding.tvCodeError.text = error.orEmpty()
    }

    private fun submitCode() {
        val code = ShareCode.normalize(binding.etCode.text.toString())
        if (!ShareCode.isValid(code)) {
            showCodeEntry("That doesn't look like a six-character code.")
            return
        }
        load(code)
    }

    private fun load(code: String) {
        show(binding.stateLoading)
        binding.tvLoading.text = "Looking up journey $code"

        lifecycleScope.launch {
            val found = repo.fetch(code)
            if (found == null || found.legs.isEmpty()) {
                showCodeEntry(
                    "No journey found for $code. It may have finished, or the code " +
                        "may have been typed wrong."
                )
                return@launch
            }
            journey = found
            renderStatic(found)
            show(binding.stateJourney)
            observe(code)
        }
    }

    private fun observe(code: String) {
        lifecycleScope.launch {
            repo.observePing(code)
                .catch {
                    // A refused read is the database rules; say so rather than
                    // leaving a frozen position on screen looking authoritative.
                    binding.tvSourceNote.visibility = View.VISIBLE
                    binding.tvSourceNote.text =
                        "Lost the connection to this journey. Check your network."
                }
                .collect { ping -> renderLive(ping) }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun renderStatic(j: LiveJourney) {
        binding.tvWho.text = "${j.ownerName} is on the way"
        binding.tvRoute.text = "${j.originLabel}  →  ${j.destLabel}"
        renderLegs(j, null)
    }

    private fun renderLive(ping: JourneyPing) {
        val j = journey ?: return
        val leg = j.legAt(ping.legIndex)

        val (badge, colour) = when {
            ping.status == JourneyStatus.COMPLETED -> "ARRIVED" to "#0A7C42"
            ping.status == JourneyStatus.STOPPED   -> "SHARING ENDED" to "#6B6860"
            ping.isSighting                        -> "LIVE GPS" to "#0A7C42"
            ping.source == PingSource.SCHEDULE     -> "ON SCHEDULE" to "#B45309"
            else                                   -> "NO SIGNAL" to "#9B1C1C"
        }
        applyBadge(binding.tvStatusBadge, badge, colour)
        binding.tvUpdated.text = "Updated ${relative(ping.at)}"

        // Position
        binding.tvWhere.text = when {
            ping.status == JourneyStatus.COMPLETED -> "Arrived at ${j.destLabel}"
            ping.status == JourneyStatus.STOPPED   -> "Sharing stopped"
            ping.nearestStation.isNotBlank()       -> "At ${ping.nearestStation}"
            else                                   -> leg?.title() ?: "On the way"
        }

        binding.tvCurrentLeg.text = when {
            ping.ended  -> ""
            leg == null -> ""
            leg.legKind == LegKind.WALK -> leg.note.ifBlank { "Walking to ${leg.toStop}" }
            else -> "${leg.lineShort} · ${leg.fromStop} → ${leg.toStop}"
        }
        binding.tvCurrentLeg.visibility =
            if (binding.tvCurrentLeg.text.isNullOrBlank()) View.GONE else View.VISIBLE

        // Which service they are on
        val service = leg?.takeIf { it.legKind.isScheduled }?.serviceLine()
        if (service != null && !ping.ended) {
            val noun = if (leg.legKind == LegKind.METRO) "metro" else "train"
            binding.tvOnService.visibility = View.VISIBLE
            binding.tvOnService.text = "On the $noun  ·  $service"
        } else {
            binding.tvOnService.visibility = View.GONE
        }

        // Next stop or transfer
        binding.tvNextStop.text = when {
            ping.ended -> ""
            leg != null && ping.nextStop.isNotBlank() &&
                ping.nextStop == leg.toStop && j.legAt(ping.legIndex + 1) != null ->
                "Next: ${ping.nextStop} — then changes"
            ping.nextStop.isNotBlank() -> "Next: ${ping.nextStop}"
            else -> ""
        }
        binding.tvNextStop.visibility =
            if (binding.tvNextStop.text.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.progressJourney.progress = ping.progressPct
        binding.tvEta.text = when {
            ping.status == JourneyStatus.COMPLETED -> "Arrived ${clock.format(Date(ping.at))}"
            ping.ended -> ""
            else -> "Arriving around ${clock.format(Date(ping.etaEpochMs))}  ·  ${ping.progressPct}% there"
        }
        binding.tvEta.visibility = if (binding.tvEta.text.isNullOrBlank()) View.GONE else View.VISIBLE

        // Honesty note. Anything not a real sighting says so in words.
        val note = when {
            ping.status == JourneyStatus.STOPPED ->
                "${j.ownerName} stopped sharing before arriving, so this is the last known position."
            ping.status == JourneyStatus.COMPLETED -> null
            ping.source == PingSource.SCHEDULE ->
                "No GPS right now — underground or in a tunnel. This position is worked out from " +
                    "the timetable of the service they boarded, not from a signal."
            ping.source == PingSource.PLANNED ->
                "No position for a few minutes. This is the last place they were seen."
            else -> null
        }
        binding.tvSourceNote.visibility = if (note == null) View.GONE else View.VISIBLE
        binding.tvSourceNote.text = note.orEmpty()

        // Maps hand-off, only where there is a real fix to hand off.
        val hasCoords = ping.lat != 0.0 || ping.lon != 0.0
        binding.btnOpenMap.visibility = if (hasCoords) View.VISIBLE else View.GONE
        binding.btnOpenMap.setOnClickListener { openInMaps(ping, j) }

        renderLegs(j, ping)
    }

    private fun renderLegs(j: LiveJourney, ping: JourneyPing?) {
        binding.containerLegs.removeAllViews()
        j.legs.forEach { leg ->
            val b = ItemJourneyLegBinding.inflate(
                LayoutInflater.from(this), binding.containerLegs, false
            )
            JourneyLegView.bind(b, leg, ping, editable = false)
            binding.containerLegs.addView(b.root)
        }
    }

    private fun openInMaps(ping: JourneyPing, j: LiveJourney) {
        val label = Uri.encode("${j.ownerName} · ${ping.nearestStation.ifBlank { "last seen" }}")
        val uri = Uri.parse("geo:${ping.lat},${ping.lon}?q=${ping.lat},${ping.lon}($label)")
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure {
                binding.tvSourceNote.visibility = View.VISIBLE
                binding.tvSourceNote.text = "No maps app available on this phone."
            }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyBadge(view: TextView, text: String, hex: String) {
        val colour = Color.parseColor(hex)
        view.text = text
        view.setTextColor(colour)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(26, Color.red(colour), Color.green(colour), Color.blue(colour)))
            cornerRadius = 20 * resources.displayMetrics.density
        }
    }

    private fun relative(at: Long): String {
        val secs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - at)
        val mins = TimeUnit.SECONDS.toMinutes(secs)
        return when {
            secs < 45  -> "just now"
            mins < 60  -> "${mins}m ago"
            else       -> clock.format(Date(at))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
