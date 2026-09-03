package com.example.mumbaitransit.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.R
import com.example.mumbaitransit.databinding.ActivityLiveStatusBinding
import com.example.mumbaitransit.databinding.ItemLiveStopBinding
import com.example.mumbaitransit.live.LiveResult
import com.example.mumbaitransit.live.LiveStatusCache
import com.example.mumbaitransit.live.LiveTrainStatus
import com.example.mumbaitransit.live.RailRadarClient
import com.example.mumbaitransit.live.RouteStop
import com.example.mumbaitransit.live.StopStatus
import com.example.mumbaitransit.live.TrainNumberStore
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Live running status for one train, opened from "View live status" on a train
 * timing card in [RoutesResultActivity].
 *
 * The screen is built around one honesty rule: RailRadar will happily return a
 * schedule replay labelled as status, so whenever the position is not an actual
 * sighting the screen says the times are projected instead of presenting them
 * as observed.
 *
 * Requests are metered. Each key allows 1,000 a month, responses are cached for
 * 30 seconds, and auto-refresh pauses itself after [MAX_AUTO_REFRESHES] rounds
 * so a screen left open overnight cannot drain the pool.
 */
class LiveStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveStatusBinding
    private lateinit var client: RailRadarClient
    private lateinit var numbers: TrainNumberStore

    private var trainNumber: String? = null
    private var legSignature: String = ""
    private var status: LiveTrainStatus? = null

    private var showAllStops = false
    private var autoRefresh = true
    private var autoRefreshJob: Job? = null
    private var autoRefreshCount = 0
    private val expandedStops = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        client = RailRadarClient(this)
        numbers = TrainNumberStore(this)

        legSignature = numbers.signature(
            line = intent.getStringExtra(EXTRA_LINE).orEmpty(),
            from = intent.getStringExtra(EXTRA_FROM).orEmpty(),
            to = intent.getStringExtra(EXTRA_TO).orEmpty(),
            departure = intent.getStringExtra(EXTRA_DEPARTURE).orEmpty()
        )

        wireControls()

        // Prefer a number passed in, then one remembered for this exact leg.
        trainNumber = intent.getStringExtra(EXTRA_TRAIN_NO)?.takeIf { it.isNotBlank() }
            ?: numbers.lookup(legSignature)

        val number = trainNumber
        if (number == null) showAskState() else load(number, force = false)
    }

    private fun wireControls() {
        binding.segNearby.setOnClickListener { setStopFilter(showAll = false) }
        binding.segAll.setOnClickListener { setStopFilter(showAll = true) }
        binding.btnRetry.setOnClickListener { trainNumber?.let { load(it, force = true) } }
        binding.btnChangeNumber.setOnClickListener { showAskState() }
        binding.btnChangeTrain.setOnClickListener { showAskState() }
        binding.btnTrack.setOnClickListener { submitNumber() }

        binding.etTrainNumber.setOnEditorActionListener { _, _, _ -> submitNumber(); true }
        binding.etTrainNumber.doAfterTextChanged {
            binding.btnTrack.alpha = if ((it?.length ?: 0) >= 4) 1f else 0.45f
        }
    }

    // ── States ────────────────────────────────────────────────────────────────

    private fun show(state: View) {
        listOf(
            binding.stateContent, binding.stateAsk,
            binding.stateLoading, binding.stateError
        ).forEach { it.visibility = if (it === state) View.VISIBLE else View.GONE }
    }

    /** Fallback when the timetable has no number for this service, or the user corrects one. */
    private fun showAskState() {
        stopAutoRefresh()
        show(binding.stateAsk)
        supportActionBar?.title = "Live train status"

        val from = intent.getStringExtra(EXTRA_FROM).orEmpty()
        val to = intent.getStringExtra(EXTRA_TO).orEmpty()
        val dep = intent.getStringExtra(EXTRA_DEPARTURE).orEmpty()
        binding.tvAskContext.text = buildString {
            append("Enter the number shown on the indicator board")
            if (dep.isNotBlank() && from.isNotBlank()) {
                append(" for the $dep from $from")
                if (to.isNotBlank()) append(" to $to")
            }
            append(". It's saved against this service, so you'll only type it once.")
        }

        binding.etTrainNumber.setText(trainNumber.orEmpty())
        binding.btnTrack.alpha = if ((trainNumber?.length ?: 0) >= 4) 1f else 0.45f

        val recents = numbers.recents()
        binding.tvRecentsLabel.visibility = if (recents.isEmpty()) View.GONE else View.VISIBLE
        binding.chipsRecent.visibility = if (recents.isEmpty()) View.GONE else View.VISIBLE
        binding.chipsRecent.removeAllViews()
        recents.forEach { number ->
            binding.chipsRecent.addView(Chip(this).apply {
                text = number
                isCheckable = false
                setOnClickListener {
                    binding.etTrainNumber.setText(number)
                    submitNumber()
                }
            })
        }
    }

    private fun submitNumber() {
        val entered = binding.etTrainNumber.text.toString().trim()
        if (entered.length < 4) {
            binding.etTrainNumber.error = "Train numbers are 4 or 5 digits"
            return
        }
        hideKeyboard()
        trainNumber = entered
        if (legSignature.isNotBlank()) numbers.remember(legSignature, entered)
        autoRefreshCount = 0
        load(entered, force = true)
    }

    private fun load(number: String, force: Boolean) {
        // A cached copy renders instantly and costs nothing.
        if (!force) {
            LiveStatusCache.get(number)?.let {
                render(it)
                startAutoRefresh()
                return
            }
        }

        show(binding.stateLoading)
        binding.tvLoading.text = "Asking RailRadar where $number is"

        lifecycleScope.launch {
            when (val result = client.fetchLive(number, forceRefresh = force)) {
                is LiveResult.Success -> {
                    render(result.status)
                    startAutoRefresh()
                }
                is LiveResult.Error -> showError(result.message, result.retryable)
            }
        }
    }

    /** Refresh triggered while content is already on screen — keeps the old view visible. */
    private fun refreshInPlace() {
        val number = trainNumber ?: return
        lifecycleScope.launch {
            when (val result = client.fetchLive(number, forceRefresh = true)) {
                is LiveResult.Success -> render(result.status)
                // A blip mid-session shouldn't wipe a working screen.
                is LiveResult.Error ->
                    if (status == null) showError(result.message, result.retryable)
                    else binding.tvFooter.text = result.message
            }
        }
    }

    private fun showError(message: String, retryable: Boolean) {
        stopAutoRefresh()
        show(binding.stateError)
        binding.tvError.text = message
        binding.btnRetry.visibility = if (retryable) View.VISIBLE else View.GONE
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun render(s: LiveTrainStatus) {
        status = s
        show(binding.stateContent)
        supportActionBar?.title = "Train ${s.trainNumber}"

        binding.tvTrainNumber.text = s.trainNumber
        binding.tvTrainName.text = s.trainName ?: "Train ${s.trainNumber}"
        binding.tvTrainName.visibility = if (s.trainName.isNullOrBlank()) View.GONE else View.VISIBLE

        val origin = stationLabel(s.sourceName, s.sourceCode)
        val terminus = stationLabel(s.destName, s.destCode)
        binding.tvTrainRoute.text =
            if (origin.isBlank() || terminus.isBlank()) "" else "$origin  →  $terminus"
        binding.tvTrainRoute.visibility =
            if (binding.tvTrainRoute.text.isBlank()) View.GONE else View.VISIBLE

        renderLiveBadge(s)
        renderStatusLine(s)
        renderPosition(s)
        renderStops(s)
        renderFooter()
    }

    private fun renderLiveBadge(s: LiveTrainStatus) {
        // "LIVE" is reserved for a real position feed. Anything else says so.
        val live = s.isLive && s.isActualPosition
        applyPill(
            binding.tvLiveBadge,
            text = if (live) "LIVE" else "SCHEDULED",
            fg = if (live) R.color.live_green else R.color.text_muted,
            bg = if (live) R.color.live_green_bg else R.color.live_grey_bg
        )
    }

    private fun renderStatusLine(s: LiveTrainStatus) {
        val parts = mutableListOf<String>()
        s.statusText?.let { parts += it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase) }
        s.trackingMode?.let { parts += it.replace('_', ' ').lowercase() }
        s.lastUpdatedAt?.let { parts += "updated ${timeAgo(it)}" }
        binding.tvStatusLine.text = parts.joinToString(" · ").ifBlank { "Status unavailable" }

        val projected = !s.isLive || !s.isActualPosition
        binding.tvEstimateNotice.visibility = if (projected) View.VISIBLE else View.GONE
        if (projected) {
            binding.tvEstimateNotice.text =
                "RailRadar has no live position for this train right now. Times below are " +
                    "projected from the timetable, not observed."
        }
    }

    private fun renderPosition(s: LiveTrainStatus) {
        val current = s.current
        val currentStop = s.route.firstOrNull { it.status == StopStatus.CURRENT }

        val stationName = current?.stationName
            ?: currentStop?.stationName
            ?: current?.stationCode
        if (stationName == null) {
            binding.cardPosition.visibility = View.GONE
            return
        }
        binding.cardPosition.visibility = View.VISIBLE
        binding.tvPositionStation.text = titleCase(stationName)
        binding.tvPositionLabel.text = positionLabel(current?.status)

        renderDelayChip(s.delayMinutes)

        val prev = s.previousHalt
        val next = s.nextHalt
        if (prev == null && next == null) {
            binding.rowProgress.visibility = View.GONE
            binding.rowHalts.visibility = View.GONE
            return
        }
        binding.rowProgress.visibility = View.VISIBLE
        binding.rowHalts.visibility = View.VISIBLE

        binding.tvPrevHalt.text = titleCase(
            prev?.stationName ?: prev?.stationCode ?: "—"
        )
        binding.tvNextHalt.text = titleCase(
            next?.stationName ?: next?.stationCode ?: "—"
        )
        binding.tvPrevDist.text = prev?.distanceKm?.let { "%.1f km back".format(it) }.orEmpty()
        binding.tvNextDist.text = next?.distanceKm?.let { "%.1f km ahead".format(it) }.orEmpty()
        binding.tvPrevDist.visibility =
            if (binding.tvPrevDist.text.isBlank()) View.GONE else View.VISIBLE
        binding.tvNextDist.visibility =
            if (binding.tvNextDist.text.isBlank()) View.GONE else View.VISIBLE

        // Split the bar by how far the train sits between the two halts.
        val back = prev?.distanceKm ?: 0.0
        val ahead = next?.distanceKm ?: 0.0
        val fraction = if (back + ahead > 0) (back / (back + ahead)).toFloat() else 0.5f
        (binding.trackDone.layoutParams as LinearLayout.LayoutParams).weight =
            fraction.coerceIn(0.04f, 0.96f)
        (binding.trackRemaining.layoutParams as LinearLayout.LayoutParams).weight =
            (1f - fraction).coerceIn(0.04f, 0.96f)
        binding.trackDone.requestLayout()
        binding.trackRemaining.requestLayout()
    }

    private fun renderDelayChip(delay: Int?) {
        if (delay == null) {
            binding.tvDelayChip.visibility = View.GONE
            return
        }
        val (text, fg, bg) = when {
            delay <= 0 && delay > -2 -> Triple("On time", R.color.live_green, R.color.live_green_bg)
            delay < 0 -> Triple("${abs(delay)} min early", R.color.live_green, R.color.live_green_bg)
            delay <= 5 -> Triple("$delay min late", R.color.amber, R.color.live_amber_bg)
            else -> Triple("$delay min late", R.color.red, R.color.live_red_bg)
        }
        applyPill(binding.tvDelayChip, text, fg, bg)
    }

    // ── Route list ────────────────────────────────────────────────────────────

    private fun setStopFilter(showAll: Boolean) {
        showAllStops = showAll
        status?.let { renderStops(it) }
    }

    private fun renderStops(s: LiveTrainStatus) {
        binding.segNearby.background =
            if (showAllStops) null else getDrawable(R.drawable.bg_segment_active)
        binding.segAll.background =
            if (showAllStops) getDrawable(R.drawable.bg_segment_active) else null
        binding.segNearby.setTextColor(segColor(!showAllStops))
        binding.segAll.setTextColor(segColor(showAllStops))

        binding.llStops.removeAllViews()
        if (s.route.isEmpty()) {
            binding.llStops.addView(TextView(this).apply {
                text = "RailRadar didn't return a stop list for this train."
                textSize = 12.5f
                setTextColor(getColor(R.color.text_muted))
                setPadding(dp(14), dp(16), dp(14), dp(16))
            })
            return
        }

        val visible = if (showAllStops) s.route else nearbyWindow(s.route)
        visible.forEachIndexed { index, stop ->
            binding.llStops.addView(
                buildStopRow(stop, isFirst = index == 0, isLast = index == visible.lastIndex)
            )
        }

        // A trimmed list gets one line explaining what's hidden.
        if (!showAllStops && visible.size < s.route.size) {
            binding.llStops.addView(TextView(this).apply {
                text = "${s.route.size - visible.size} more stops · tap All stops"
                textSize = 11.5f
                setTextColor(getColor(R.color.text_hint))
                setPadding(dp(48), dp(2), dp(14), dp(12))
            })
        }
    }

    /**
     * Trims the route to what a passenger actually looks at: the last stop passed,
     * the current one, the next few, and the destination.
     */
    private fun nearbyWindow(route: List<RouteStop>): List<RouteStop> {
        val currentIdx = route.indexOfFirst { it.status == StopStatus.CURRENT }
            .takeIf { it >= 0 }
            ?: route.indexOfLast { it.status == StopStatus.DEPARTED }.takeIf { it >= 0 }
            ?: return route.take(WINDOW_AHEAD + 1)

        val start = (currentIdx - WINDOW_BEHIND).coerceAtLeast(0)
        val end = (currentIdx + WINDOW_AHEAD).coerceAtMost(route.lastIndex)
        val window = route.subList(start, end + 1).toMutableList()

        // Always keep the destination in view.
        if (end < route.lastIndex) window.add(route.last())
        return window
    }

    private fun buildStopRow(stop: RouteStop, isFirst: Boolean, isLast: Boolean): View {
        val row = ItemLiveStopBinding.inflate(layoutInflater, binding.llStops, false)

        row.tvStation.text = titleCase(stop.stationName)
        row.dot.background = getDrawable(
            when (stop.status) {
                StopStatus.DEPARTED -> R.drawable.dot_live_done
                StopStatus.CURRENT -> R.drawable.dot_live_current
                StopStatus.SKIPPED -> R.drawable.dot_live_skipped
                else -> R.drawable.dot_live_upcoming
            }
        )
        row.lineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
        row.lineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

        if (stop.status == StopStatus.CURRENT) {
            row.rowRoot.setBackgroundColor(getColor(R.color.accent_blue_bg))
            row.tvStation.setTypeface(row.tvStation.typeface, android.graphics.Typeface.BOLD)
        }

        // Headline time: what actually happened if known, otherwise what's booked.
        val headline = when (stop.status) {
            StopStatus.DEPARTED -> stop.actualDeparture ?: stop.scheduledDeparture
            else -> stop.actualArrival ?: stop.scheduledArrival
        }
        row.tvTime.text = headline ?: "—"

        row.tvSubtitle.text = buildSubtitle(stop)
        row.tvSubtitle.visibility =
            if (row.tvSubtitle.text.isBlank()) View.GONE else View.VISIBLE

        val delay = stop.delay
        if (delay == null || stop.status == StopStatus.SKIPPED) {
            row.tvDelay.visibility = View.GONE
        } else {
            val (text, fg, bg) = when {
                delay <= 0 -> Triple("on time", R.color.live_green, R.color.live_green_bg)
                delay <= 5 -> Triple("+${delay}m", R.color.amber, R.color.live_amber_bg)
                else -> Triple("+${delay}m", R.color.red, R.color.live_red_bg)
            }
            applyPill(row.tvDelay, text, fg, bg)
        }

        if (stop.status == StopStatus.UPCOMING || stop.status == StopStatus.UNKNOWN) {
            row.tvStation.setTextColor(getColor(R.color.text_muted))
        }

        // Full scheduled-vs-actual detail stays folded away until asked for.
        val key = stopKey(stop)
        val expanded = expandedStops.contains(key)
        row.llDetail.visibility = if (expanded) View.VISIBLE else View.GONE
        row.tvDetailArrival.text = pairTimes(stop.scheduledArrival, stop.actualArrival)
        row.tvDetailDeparture.text = pairTimes(stop.scheduledDeparture, stop.actualDeparture)
        row.rowPlatform.visibility = if (stop.platform.isNullOrBlank()) View.GONE else View.VISIBLE
        row.tvDetailPlatform.text = stop.platform.orEmpty()

        row.rowRoot.setOnClickListener {
            if (!expandedStops.add(key)) expandedStops.remove(key)
            status?.let { renderStops(it) }
        }
        return row.root
    }

    /** Stable per-stop key; sequence alone is unreliable when the API omits it. */
    private fun stopKey(stop: RouteStop): String = "${stop.sequence}:${stop.stationCode}"

    private fun buildSubtitle(stop: RouteStop): String {
        val bits = mutableListOf<String>()
        bits += stop.stationCode.uppercase()
        when (stop.status) {
            StopStatus.DEPARTED -> stop.scheduledDeparture?.let { bits += "booked $it" }
            StopStatus.CURRENT -> bits += "train is here"
            StopStatus.SKIPPED -> bits += "does not stop"
            else -> stop.scheduledArrival?.let { bits += "booked $it" }
        }
        stop.platform?.takeIf { it.isNotBlank() }?.let { bits += "PF $it" }
        return bits.joinToString(" · ")
    }

    private fun pairTimes(scheduled: String?, actual: String?): String = when {
        scheduled != null && actual != null && scheduled != actual -> "$scheduled → $actual"
        actual != null -> actual
        scheduled != null -> scheduled
        else -> "—"
    }

    private fun renderFooter() {
        val remaining = client.quotaTotal - client.quotaUsed
        binding.tvFooter.text = buildString {
            append(if (autoRefresh) "Refreshing every 60s" else "Auto-refresh off")
            append(" · ")
            append("$remaining of ${client.quotaTotal} monthly checks left")
            if (client.keysRemaining < 1) append(" · pool empty")
        }
    }

    // ── Auto-refresh ──────────────────────────────────────────────────────────

    private fun startAutoRefresh() {
        stopAutoRefresh()
        if (!autoRefresh) return
        autoRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                // A screen left open shouldn't quietly eat the month's quota.
                if (autoRefreshCount >= MAX_AUTO_REFRESHES) {
                    autoRefresh = false
                    invalidateOptionsMenu()
                    binding.tvFooter.text =
                        "Auto-refresh paused to save quota · tap refresh for a fresh position"
                    break
                }
                autoRefreshCount++
                refreshInPlace()
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }

    override fun onResume() {
        super.onResume()
        if (status == null) return
        // Returning to a stale screen should refresh now rather than after a full interval.
        val stale = trainNumber?.let { LiveStatusCache.ageOf(it) == null } ?: false
        if (stale && autoRefresh) refreshInPlace()
        startAutoRefresh()
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_live_status, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_auto)?.isChecked = autoRefresh
        val tracking = status != null
        menu.findItem(R.id.action_refresh)?.isVisible = tracking
        menu.findItem(R.id.action_auto)?.isVisible = tracking
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> {
            autoRefreshCount = 0
            refreshInPlace()
            startAutoRefresh()
            true
        }
        R.id.action_auto -> {
            autoRefresh = !autoRefresh
            item.isChecked = autoRefresh
            autoRefreshCount = 0
            if (autoRefresh) startAutoRefresh() else stopAutoRefresh()
            renderFooter()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // ── Small helpers ─────────────────────────────────────────────────────────

    private fun applyPill(view: TextView, text: String, fg: Int, bg: Int) {
        view.visibility = View.VISIBLE
        view.text = text
        view.setTextColor(getColor(fg))
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(getColor(bg))
            cornerRadius = dp(20).toFloat()
        }
    }

    private fun segColor(active: Boolean): Int =
        getColor(if (active) R.color.text_primary else R.color.text_muted)

    private fun stationLabel(name: String?, code: String?): String = when {
        !name.isNullOrBlank() && !code.isNullOrBlank() -> "${titleCase(name)} ($code)"
        !name.isNullOrBlank() -> titleCase(name)
        !code.isNullOrBlank() -> code
        else -> ""
    }

    private fun positionLabel(raw: String?): String {
        val s = raw?.lowercase()?.replace('_', ' ').orEmpty()
        return when {
            "at station" in s || "halt" in s -> "AT STATION"
            "depart" in s -> "DEPARTED"
            "run" in s || "between" in s -> "BETWEEN STATIONS"
            "arriv" in s -> "ARRIVING"
            else -> "LAST SEEN AT"
        }
    }

    /** RailRadar sends ALL-CAPS station names; this makes them readable. */
    private fun titleCase(raw: String): String = raw
        .trim()
        .lowercase(Locale.US)
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    private fun timeAgo(millis: Long): String {
        val seconds = ((System.currentTimeMillis() - millis) / 1000).coerceAtLeast(0)
        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86_400}d ago"
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etTrainNumber.windowToken, 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val EXTRA_TRAIN_NO = "train_no"
        private const val EXTRA_LINE = "line"
        private const val EXTRA_FROM = "from_stop"
        private const val EXTRA_TO = "to_stop"
        private const val EXTRA_DEPARTURE = "departure"

        private const val REFRESH_INTERVAL_MS = 60_000L
        private const val MAX_AUTO_REFRESHES = 20
        private const val WINDOW_BEHIND = 2
        private const val WINDOW_AHEAD = 5

        /**
         * Opens live tracking for one leg. [trainNo] may be null — the screen then
         * asks for it once and remembers it against this leg.
         */
        fun start(
            context: Context,
            line: String,
            fromStop: String,
            toStop: String,
            departure: String,
            trainNo: String? = null
        ) {
            context.startActivity(
                Intent(context, LiveStatusActivity::class.java)
                    .putExtra(EXTRA_LINE, line)
                    .putExtra(EXTRA_FROM, fromStop)
                    .putExtra(EXTRA_TO, toStop)
                    .putExtra(EXTRA_DEPARTURE, departure)
                    .putExtra(EXTRA_TRAIN_NO, trainNo)
            )
        }
    }
}
