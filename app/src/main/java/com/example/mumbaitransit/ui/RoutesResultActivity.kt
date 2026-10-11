package com.example.mumbaitransit.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mumbaitransit.R
import com.example.mumbaitransit.auth.SavedRouteRepository
import com.example.mumbaitransit.databinding.ActivityRoutesResultBinding
import com.example.mumbaitransit.engine.TransitEngine
import com.example.mumbaitransit.model.NearestStation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Route Options — where the trip is set up.
 *
 * This screen used to carry the results as well, which put the two things a
 * commuter does here in competition: the boarding and alighting selectors sat
 * above a list that grew taller than the phone, so the moment a route appeared
 * the controls for changing it were off-screen. Optimising the trip is now the
 * whole job of this screen, and [RouteListActivity] shows what comes of it.
 *
 * Nothing here runs a route search. Nearby stations come straight off the
 * engine, and the single search is fired when "See Routes" is tapped — so
 * changing your mind about a station is instant rather than a re-query.
 */
class RoutesResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutesResultBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }

    private lateinit var savedRouteRepo: SavedRouteRepository
    private lateinit var fusedLocation: FusedLocationProviderClient

    /** Which end of the trip the Places picker was opened for. */
    private var pickingOrigin = false

    /** Guards against the load-state observer resolving the origin twice. */
    private var initialLoadDone = false

    /** What is actually in range at each end — decides whether a card slides. */
    private var originHasRail = false
    private var originHasMetro = false
    private var destHasRail = false
    private var destHasMetro = false

    private val placesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val lat = place.latLng?.latitude ?: return@registerForActivityResult
            val lon = place.latLng?.longitude ?: return@registerForActivityResult
            val name = place.name ?: place.address ?: "Selected location"
            if (!inMumbaiArea(lat, lon)) {
                Toast.makeText(this, "Location must be within Mumbai area", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            if (pickingOrigin) vm.setOrigin(lat, lon, name) else vm.setDestination(lat, lon, name)
            renderJourneyCard()
            refreshStations()
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchMyLocation()
        else {
            Toast.makeText(this, "Location permission denied — pick a starting point", Toast.LENGTH_SHORT).show()
            showStatus("Tap “My Location” above to set where you're starting from")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutesResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        savedRouteRepo = SavedRouteRepository(this)

        val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized()) Places.initialize(applicationContext, apiKey)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        setupJourneyCard()
        setupSwitchers()
        renderJourneyCard()
        updateDepartTimeUi()
        renderSummary()

        binding.btnSeeRoutes.setOnClickListener { seeRoutes() }

        // The CSVs may still be loading — this screen can be reached before the
        // engine is up, so wait for it before asking for nearby stations.
        vm.loadState.observe(this) { state ->
            if (state is LoadState.Ready && !initialLoadDone) {
                initialLoadDone = true
                startTrip()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Journey card — origin, destination and departure time
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupJourneyCard() {
        binding.rowOrigin.setOnClickListener { pickingOrigin = true; launchPlaces() }
        binding.rowDest.setOnClickListener { pickingOrigin = false; launchPlaces() }
        binding.rowDepartTime.setOnClickListener { pickDepartTime() }
        binding.btnSwapOd.setOnClickListener {
            if (!vm.hasOrigin || !vm.hasDest) return@setOnClickListener
            vm.swapEnds()
            renderJourneyCard()
            refreshStations()
        }
    }

    /** Paints the two rows from whatever the ViewModel currently holds. */
    private fun renderJourneyCard() {
        if (vm.originIsMyLocation) {
            binding.tvOriginLabel.text = "My Location"
            binding.tvOriginSub.visibility = View.VISIBLE
            binding.tvOriginSub.text = if (vm.hasOrigin) "Current location" else "Locating…"
        } else {
            binding.tvOriginLabel.text = vm.originLabel.ifEmpty { "Set starting point" }
            binding.tvOriginSub.visibility = View.GONE
        }
        binding.tvDestLabel.text = vm.destLabel
        binding.tvDestSub.visibility = View.GONE
        renderSummary()
    }

    /** Resolves the origin if it is still "my location", then lists stations. */
    private fun startTrip() {
        if (!vm.hasDest) {
            showStatus("Tap the destination above to choose where you're going")
            return
        }
        if (vm.hasOrigin) { refreshStations(); return }
        if (!vm.originIsMyLocation) {
            showStatus("Tap the top row to set where you're starting from")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) fetchMyLocation()
        else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fetchMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        fusedLocation.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                showStatus("Location unavailable — tap the top row to set a starting point")
                return@addOnSuccessListener
            }
            vm.useMyLocationAsOrigin(loc.latitude, loc.longitude)
            renderJourneyCard()
            refreshStations()
        }.addOnFailureListener {
            showStatus("Location unavailable — tap the top row to set a starting point")
        }
    }

    private fun showStatus(msg: String) {
        binding.tvOptionsStatus.visibility = View.VISIBLE
        binding.tvOptionsStatus.text = msg
    }

    private fun clearStatus() {
        binding.tvOptionsStatus.visibility = View.GONE
    }

    private fun launchPlaces() {
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("IN").build(this)
        placesLauncher.launch(intent)
    }

    private fun inMumbaiArea(lat: Double, lon: Double) =
        lat in 18.5..19.8 && lon in 72.6..73.5

    // ── Departure time ────────────────────────────────────────────────────────

    private fun pickDepartTime() {
        val now = Calendar.getInstance()
        val startHour = vm.departAfterMins?.div(60) ?: now.get(Calendar.HOUR_OF_DAY)
        val startMin = vm.departAfterMins?.rem(60) ?: now.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(this, { _, hour, minute ->
            vm.departAfterMins = hour * 60 + minute
            updateDepartTimeUi()
        }, startHour, startMin, false)

        dialog.setTitle("Show trains from")
        // Lets the commuter undo a pick without hunting for the current clock time.
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Leave now") { _, _ ->
            vm.departAfterMins = null
            updateDepartTimeUi()
        }
        dialog.show()
    }

    private fun updateDepartTimeUi() {
        val mins = vm.departAfterMins
        if (mins == null) {
            binding.tvDepartTime.text = "Now"
            binding.tvDepartHint.text = "Trains leaving in the next 2 hours will be shown."
        } else {
            val from = TransitEngine.clock12(mins)
            val to = TransitEngine.clock12(mins + TransitEngine.SEARCH_WINDOW_MIN)
            binding.tvDepartTime.text = from
            binding.tvDepartHint.text = "Trains leaving between $from and $to will be shown."
        }
        renderSummary()
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Boarding / alighting selectors
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupSwitchers() {
        binding.switcherBoard.onPageChanged = { syncSwitcherChrome(isOrigin = true) }
        binding.switcherOff.onPageChanged = { syncSwitcherChrome(isOrigin = false) }
        // The dots are the tap-sized version of the swipe, for anyone who
        // doesn't discover the gesture or can't make it comfortably.
        binding.llBoardDots.setOnClickListener { binding.switcherBoard.step(1) }
        binding.llOffDots.setOnClickListener { binding.switcherOff.step(1) }
    }

    private fun refreshStations() {
        if (!vm.hasOrigin || !vm.hasDest) return
        clearStatus()
        loadNearbyStations(vm.originLat, vm.originLon, isOrigin = true)
        loadNearbyStations(vm.destLat, vm.destLon, isOrigin = false)
    }

    private fun loadNearbyStations(lat: Double, lon: Double, isOrigin: Boolean) {
        if (vm.loadState.value !is LoadState.Ready) return
        CoroutineScope(Dispatchers.Main).launch {
            val grouped = withContext(Dispatchers.Default) {
                vm.engine.nearestStationsGrouped(lat, lon)
            }
            populateStations(grouped, isOrigin)
        }
    }

    /**
     * Fills one end's card. Rail is page 0 and metro page 1 whenever both are
     * in range; with only one mode the card stops sliding and simply names the
     * mode it is showing.
     */
    private fun populateStations(
        grouped: Map<String, List<NearestStation>>,
        isOrigin: Boolean
    ) {
        val card = if (isOrigin) binding.cardBoard else binding.cardOff
        val switcher = if (isOrigin) binding.switcherBoard else binding.switcherOff
        val gridRail = if (isOrigin) binding.gridBoardRail else binding.gridOffRail
        val gridMetro = if (isOrigin) binding.gridBoardMetro else binding.gridOffMetro

        val rail = grouped["rail"] ?: emptyList()
        val metro = grouped["metro"] ?: emptyList()

        if (isOrigin) {
            originHasRail = rail.isNotEmpty(); originHasMetro = metro.isNotEmpty()
        } else {
            destHasRail = rail.isNotEmpty(); destHasMetro = metro.isNotEmpty()
        }

        if (rail.isEmpty() && metro.isEmpty()) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE

        buildGrid(gridRail, rail, isOrigin)
        buildGrid(gridMetro, metro, isOrigin)

        // Keep whatever the commuter pinned; otherwise start on the station
        // they'd reach first on foot, whichever mode that turns out to be.
        val pinned = if (isOrigin) vm.pinnedOrigin else vm.pinnedDest
        val known = (rail + metro).map { it.canonical }
        val target = pinned?.takeIf { it in known }
            ?: (rail + metro).minByOrNull { it.distanceKm }?.canonical
        if (isOrigin) vm.pinnedOrigin = target else vm.pinnedDest = target

        switcher.swipeEnabled = rail.isNotEmpty() && metro.isNotEmpty()
        val page = when {
            target != null && metro.any { it.canonical == target } -> 1
            rail.isNotEmpty() -> 0
            else -> 1
        }
        switcher.showPage(page, animate = false)
        syncSwitcherChrome(isOrigin)
        paintSelection(isOrigin)
        renderSummary()
    }

    /** Lays the stations out two to a row, in the order the engine ranked them. */
    private fun buildGrid(grid: LinearLayout, stations: List<NearestStation>, isOrigin: Boolean) {
        grid.removeAllViews()
        var i = 0
        while (i < stations.size) {
            val top = if (i > 0) dpToPx(10) else 0
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = top }
            }

            row.addView(makeStationCell(stations[i], isOrigin, leftColumn = true))
            if (i + 1 < stations.size) {
                row.addView(makeStationCell(stations[i + 1], isOrigin, leftColumn = false))
            } else {
                // An odd count would otherwise stretch the last button across
                // the row and break the grid the eye is following.
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                        .apply { marginStart = dpToPx(10) }
                })
            }

            grid.addView(row)
            i += 2
        }
    }

    private fun makeStationCell(
        stn: NearestStation,
        isOrigin: Boolean,
        leftColumn: Boolean
    ): View {
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { if (!leftColumn) marginStart = dpToPx(10) }
            setPadding(dpToPx(12), dpToPx(13), dpToPx(12), dpToPx(13))
            isClickable = true
            isFocusable = true
            tag = stn.canonical
        }

        cell.addView(TextView(this).apply {
            text = stn.canonical
            textSize = 14.5f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
        })

        cell.addView(TextView(this).apply {
            text = formatDist(stn.distanceKm)
            textSize = 10.5f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@RoutesResultActivity, R.color.text_hint))
        })

        cell.setOnClickListener {
            if (isOrigin) vm.pinnedOrigin = stn.canonical else vm.pinnedDest = stn.canonical
            paintSelection(isOrigin)
            renderSummary()
        }
        return cell
    }

    /** Repaints both pages of one end so exactly one button reads as chosen. */
    private fun paintSelection(isOrigin: Boolean) {
        val pinned = if (isOrigin) vm.pinnedOrigin else vm.pinnedDest
        val grids = if (isOrigin) listOf(binding.gridBoardRail, binding.gridBoardMetro)
                    else listOf(binding.gridOffRail, binding.gridOffMetro)

        for (grid in grids) {
            for (r in 0 until grid.childCount) {
                val row = grid.getChildAt(r) as? LinearLayout ?: continue
                for (c in 0 until row.childCount) {
                    val cell = row.getChildAt(c) as? LinearLayout ?: continue
                    val name = cell.tag as? String ?: continue
                    val chosen = name == pinned
                    cell.setBackgroundResource(
                        if (chosen) R.drawable.bg_station_option_selected
                        else R.drawable.bg_station_option
                    )
                    (cell.getChildAt(0) as? TextView)?.apply {
                        setTextColor(
                            if (chosen) ContextCompat.getColor(this@RoutesResultActivity, R.color.accent_blue)
                            else ContextCompat.getColor(this@RoutesResultActivity, R.color.text_primary)
                        )
                        setTypeface(null, if (chosen) Typeface.BOLD else Typeface.NORMAL)
                    }
                }
            }
        }
    }

    /** Title, icon and page dots for whichever page the card is showing. */
    private fun syncSwitcherChrome(isOrigin: Boolean) {
        val switcher = if (isOrigin) binding.switcherBoard else binding.switcherOff
        val title = if (isOrigin) binding.tvBoardTitle else binding.tvOffTitle
        val icon = if (isOrigin) binding.ivBoardIcon else binding.ivOffIcon
        val dots = if (isOrigin) binding.llBoardDots else binding.llOffDots
        val hasRail = if (isOrigin) originHasRail else destHasRail
        val hasMetro = if (isOrigin) originHasMetro else destHasMetro

        val onMetro = switcher.displayedChild == 1
        val verb = if (isOrigin) "Board at" else "Get off at"
        title.text = if (onMetro) "$verb (Metro)" else "$verb (Railway)"
        icon.setImageResource(if (onMetro) R.drawable.ic_mode_metro else R.drawable.ic_mode_rail)

        if (hasRail && hasMetro) {
            dots.visibility = View.VISIBLE
            buildDots(dots, if (onMetro) 1 else 0)
        } else {
            dots.visibility = View.GONE
        }
    }

    private fun buildDots(container: LinearLayout, active: Int) {
        container.removeAllViews()
        for (i in 0..1) {
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(6))
                    .apply { if (i > 0) marginStart = dpToPx(5) }
                setBackgroundResource(
                    if (i == active) R.drawable.dot_page_on else R.drawable.dot_page_off
                )
            })
        }
    }

    // ── Summary and hand-off ──────────────────────────────────────────────────

    /** The read-back above the button — what is about to be searched for. */
    private fun renderSummary() {
        val origin = if (vm.originIsMyLocation) "My Location" else vm.originLabel.ifEmpty { "Start" }
        binding.tvSummaryEnds.text = "$origin  →  ${vm.destLabel.ifEmpty { "Destination" }}"

        val parts = mutableListOf<String>()
        vm.pinnedOrigin?.let { parts.add("Board at $it") }
        vm.pinnedDest?.let { parts.add("Get off at $it") }
        parts.add(vm.departWindowLabel()?.replace("Trains from", "Departing") ?: "Departing now")
        binding.tvSummaryDetail.text = parts.joinToString("  ·  ")
    }

    /**
     * Fires the one search this screen is responsible for and opens the list.
     *
     * Searching here rather than on arrival keeps the results screen a pure
     * reader of [TransitViewModel] — it never has to know how the trip was put
     * together, only what came out.
     */
    private fun seeRoutes() {
        if (!vm.hasDest) {
            Toast.makeText(this, "Choose where you're going first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!vm.hasOrigin) {
            Toast.makeText(this, "Set where you're starting from", Toast.LENGTH_SHORT).show()
            startTrip()
            return
        }
        if (vm.loadState.value !is LoadState.Ready) {
            Toast.makeText(this, "Still loading timetables — one moment", Toast.LENGTH_SHORT).show()
            return
        }
        vm.markSearching()
        vm.searchCurrentTrip()
        RouteListActivity.start(this)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatDist(km: Double) = when {
        km < 1.0 -> "${(km * 1000).toInt()}m"
        else     -> "${"%.1f".format(km)}km"
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // The home screen no longer carries a crosshair, so this is the way back
        // to the device's own fix once the origin has been changed to a place.
        menu.add(Menu.NONE, MENU_MY_LOCATION, Menu.NONE, "Use my location")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        if (savedRouteRepo.session.isLoggedIn()) {
            menu.add(Menu.NONE, MENU_SAVED, Menu.NONE, "Saved Routes")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            MENU_MY_LOCATION -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) fetchMyLocation()
                else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                true
            }
            MENU_SAVED -> {
                startActivity(Intent(this, SavedRoutesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val MENU_SAVED = 1001
        private const val MENU_MY_LOCATION = 1002
    }
}
