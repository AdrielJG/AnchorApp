package com.example.mumbaitransit.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mumbaitransit.R
import com.example.mumbaitransit.databinding.ActivityMainBinding
import com.example.mumbaitransit.model.NearestStation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.example.mumbaitransit.auth.AuthBottomSheet
import com.example.mumbaitransit.auth.AuthRepository
import com.example.mumbaitransit.engine.TransitEngine
import java.util.Calendar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }
    private lateinit var fusedLocation: FusedLocationProviderClient

    // Location state
    private var originLat = 0.0; private var originLon = 0.0
    private var destLat   = 0.0; private var destLon   = 0.0
    private var originLabel = ""; private var destLabel = ""

    // Pinned station (from chip selection)
    private var pinnedOrigin: String? = null
    private var pinnedDest: String?   = null

    // Whether the Places picker is being opened for origin or dest
    private var pickingOrigin = true

    // Earliest boarding time in minutes past midnight; null = leave now
    private var departAfterMins: Int? = null

    private var currentLat = 0.0
    private var currentLon = 0.0

    private val locationPermissionForNearby = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocationForNearbyStops()
        else {
            binding.sectionNearbyStops.visibility = View.GONE
            Toast.makeText(this, "Location permission denied – nearby stops unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) getCurrentLocation() else
        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show() }

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
            if (pickingOrigin) setOrigin(lat, lon, name) else setDest(lat, lon, name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Init Google Places
        val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized()) Places.initialize(applicationContext, apiKey)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        setupListeners()
        setupAuthFab()
        // Survives back-navigation and rotation — the VM outlives this activity.
        departAfterMins = vm.departAfterMins
        updateDepartTimeUi()

        // Show nearby stops section and auto-load when data is ready
        binding.sectionNearbyStops.visibility = View.VISIBLE
        binding.tvNearbyLoading.visibility = View.VISIBLE
        vm.loadState.observe(this) { state ->
            if (state is LoadState.Ready) {
                requestNearbyStopsLocation()
            }
        }
    }

    private fun setupListeners() {
        binding.etOrigin.setOnClickListener { pickingOrigin = true; launchPlaces() }
        binding.etDest.setOnClickListener   { pickingOrigin = false; launchPlaces() }

        binding.btnClearOrigin.setOnClickListener { clearOrigin() }
        binding.btnClearDest.setOnClickListener   { clearDest() }

        binding.btnMyLocation.setOnClickListener {
            pickingOrigin = true
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) getCurrentLocation()
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.rowDepartTime.setOnClickListener { pickDepartTime() }

        binding.btnSwap.setOnClickListener { swapFields() }
        binding.btnSearch.setOnClickListener { searchRoutes() }
        binding.btnMri.setOnClickListener { startActivity(Intent(this, MriActivity::class.java)) }
    }

    // ── Auth FAB ─────────────────────────────────────────────────────────────

    private lateinit var authRepo: AuthRepository

    private fun setupAuthFab() {
        authRepo = AuthRepository(this)

        // Firebase keeps the account signed in across restarts; sync our own
        // session flag to it before the FAB decides what to show.
        authRepo.restoreSession()
        updateFabState()

        binding.fabAuth.setOnClickListener {
            if (authRepo.session.isLoggedIn()) {
                showLogoutDialog()
            } else {
                val sheet = AuthBottomSheet.newInstance()
                sheet.onAuthSuccess = { username ->
                    updateFabState()
                    Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()
                }
                sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
            }
        }

        // Chat carries a name against every report, so it needs a signed-in
        // user. Bounce to the auth sheet rather than posting as "anonymous".
        binding.fabChat.setOnClickListener {
            if (authRepo.session.isLoggedIn()) {
                startActivity(Intent(this, ChatRoomsActivity::class.java))
            } else {
                Toast.makeText(this, "Sign in to join the line chat", Toast.LENGTH_SHORT).show()
                val sheet = AuthBottomSheet.newInstance()
                sheet.onAuthSuccess = {
                    updateFabState()
                    startActivity(Intent(this, ChatRoomsActivity::class.java))
                }
                sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
            }
        }
    }

    private fun updateFabState() {
        if (authRepo.session.isLoggedIn()) {
            val username = authRepo.session.getUsername()
            binding.fabAuth.text = username
            binding.fabAuth.setIconResource(R.drawable.ic_person)
            binding.fabAuth.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#0A7C42"))
        } else {
            binding.fabAuth.text = "Sign In"
            binding.fabAuth.setIconResource(R.drawable.ic_person)
            binding.fabAuth.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1A56DB"))
        }
    }

    private fun showLogoutDialog() {
        val username = authRepo.session.getUsername()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Signed in as $username")
            .setMessage("Would you like to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                authRepo.logOut()
                updateFabState()
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Set origin/destination ────────────────────────────────────────────────

    private fun setOrigin(lat: Double, lon: Double, label: String) {
        originLat = lat; originLon = lon; originLabel = label
        pinnedOrigin = null
        binding.etOrigin.text = label
        binding.btnClearOrigin.visibility = View.VISIBLE
        loadNearbyStations(lat, lon, isOrigin = true)
    }

    private fun setDest(lat: Double, lon: Double, label: String) {
        destLat = lat; destLon = lon; destLabel = label
        pinnedDest = null
        binding.etDest.text = label
        binding.btnClearDest.visibility = View.VISIBLE
        loadNearbyStations(lat, lon, isOrigin = false)
    }

    private fun clearOrigin() {
        originLat = 0.0; originLon = 0.0; originLabel = ""; pinnedOrigin = null
        binding.etOrigin.text = ""
        binding.btnClearOrigin.visibility = View.GONE
        binding.panelOriginStations.visibility = View.GONE
    }

    private fun clearDest() {
        destLat = 0.0; destLon = 0.0; destLabel = ""; pinnedDest = null
        binding.etDest.text = ""
        binding.btnClearDest.visibility = View.GONE
        binding.panelDestStations.visibility = View.GONE
    }

    // ── Nearby station chips ──────────────────────────────────────────────────

    private fun loadNearbyStations(lat: Double, lon: Double, isOrigin: Boolean) {
        if (!vm.loadState.value.let { it is LoadState.Ready }) return
        CoroutineScope(Dispatchers.Main).launch {
            val grouped = withContext(Dispatchers.Default) {
                vm.engine.nearestStationsGrouped(lat, lon)
            }
            populateChips(grouped, isOrigin)
        }
    }

    private fun populateChips(grouped: Map<String, List<NearestStation>>, isOrigin: Boolean) {
        val panel       = if (isOrigin) binding.panelOriginStations else binding.panelDestStations
        val rowRail     = if (isOrigin) binding.rowOriginRail     else binding.rowDestRail
        val rowMetro    = if (isOrigin) binding.rowOriginMetro    else binding.rowDestMetro
        val cgRail      = if (isOrigin) binding.chipGroupOriginRail  else binding.chipGroupDestRail
        val cgMetro     = if (isOrigin) binding.chipGroupOriginMetro else binding.chipGroupDestMetro

        val rail  = grouped["rail"]  ?: emptyList()
        val metro = grouped["metro"] ?: emptyList()

        if (rail.isEmpty() && metro.isEmpty()) {
            panel.visibility = View.GONE
            return
        }

        cgRail.removeAllViews()
        cgMetro.removeAllViews()

        if (rail.isNotEmpty()) {
            rowRail.visibility = View.VISIBLE
            rail.forEach { stn -> cgRail.addView(makeChip(stn, isOrigin)) }
        } else {
            rowRail.visibility = View.GONE
        }

        if (metro.isNotEmpty()) {
            rowMetro.visibility = View.VISIBLE
            metro.forEach { stn -> cgMetro.addView(makeChip(stn, isOrigin)) }
        } else {
            rowMetro.visibility = View.GONE
        }

        panel.visibility = View.VISIBLE

        // Auto-select the closest station as the default pin
        val closest = (rail + metro).minByOrNull { it.distanceKm }
        if (closest != null) selectChip(cgRail, cgMetro, closest.canonical, isOrigin)
    }

    private fun makeChip(stn: NearestStation, isOrigin: Boolean): Chip {
        val dist = formatDist(stn.distanceKm)
        val chip = Chip(this).apply {
            text = "${stn.canonical}  $dist"
            isCheckable = true
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setTextColor(Color.parseColor("#1a1a18"))
            chipStrokeWidth = 1.5f
            chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E0DA"))
            textSize = 11.5f
            chipMinHeight = 32f
            setCheckedIconVisible(false)
            // Style the distance part smaller — done via SpannableString
            val nameEnd = stn.canonical.length
            val span = android.text.SpannableString(text)
            span.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#9E9B93")),
                nameEnd + 2, text.length, 0)
            span.setSpan(android.text.style.RelativeSizeSpan(0.85f),
                nameEnd + 2, text.length, 0)
            setText(span, android.widget.TextView.BufferType.SPANNABLE)
        }
        chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#EFF4FF"))
                chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#1A56DB"))
                chip.setTextColor(Color.parseColor("#1A56DB"))
                if (isOrigin) pinnedOrigin = stn.canonical else pinnedDest = stn.canonical
            } else {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
                chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#E2E0DA"))
                chip.setTextColor(Color.parseColor("#1a1a18"))
            }
        }
        return chip
    }

    /** Programmatically check the chip matching `canonical` in one or both chip groups */
    private fun selectChip(cgRail: ChipGroup, cgMetro: ChipGroup, canonical: String, isOrigin: Boolean) {
        listOf(cgRail, cgMetro).forEach { cg ->
            for (i in 0 until cg.childCount) {
                val chip = cg.getChildAt(i) as? Chip ?: continue
                if (chip.text.toString().startsWith(canonical)) {
                    chip.isChecked = true
                    if (isOrigin) pinnedOrigin = canonical else pinnedDest = canonical
                    return
                }
            }
        }
    }

    // ── Nearby Stops (home screen) ────────────────────────────────────────────

    private fun requestNearbyStopsLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fetchLocationForNearbyStops()
        } else {
            locationPermissionForNearby.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchLocationForNearbyStops() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        binding.tvNearbyLoading.visibility = View.VISIBLE
        binding.containerNearbyStops.visibility = View.GONE

        fusedLocation.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                binding.tvNearbyLoading.text = "Could not get location. Tap the location button above."
                return@addOnSuccessListener
            }
            currentLat = loc.latitude
            currentLon = loc.longitude
            binding.tvNearbyLoading.visibility = View.GONE
            binding.containerNearbyStops.visibility = View.VISIBLE
            showNearbyStops(loc.latitude, loc.longitude)
        }.addOnFailureListener {
            binding.tvNearbyLoading.text = "Location unavailable. Try again."
        }
    }

    private fun showNearbyStops(lat: Double, lon: Double) {
        if (!vm.loadState.value.let { it is LoadState.Ready }) return
        CoroutineScope(Dispatchers.Main).launch {
            val grouped = withContext(Dispatchers.Default) {
                vm.engine.nearestStationsGrouped(lat, lon)
            }
            binding.containerNearbyStops.removeAllViews()

            val allStops = mutableListOf<NearestStation>()
            grouped["rail"]?.let { allStops.addAll(it) }
            grouped["metro"]?.let { allStops.addAll(it) }
            grouped["bus"]?.let { allStops.addAll(it) }

            // Sort all stops by distance
            allStops.sortBy { it.distanceKm }

            if (allStops.isEmpty()) {
                val tv = TextView(this@MainActivity)
                tv.text = "No stops found nearby"
                tv.textSize = 13f
                tv.setTextColor(Color.parseColor("#9E9B93"))
                binding.containerNearbyStops.addView(tv)
                return@launch
            }

            for (stop in allStops) {
                val cardView = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_nearby_stop, binding.containerNearbyStops, false)

                val tvIcon = cardView.findViewById<TextView>(R.id.tvModeIcon)
                val tvName = cardView.findViewById<TextView>(R.id.tvStopName)
                val tvSubtitle = cardView.findViewById<TextView>(R.id.tvStopSubtitle)
                val tvDist = cardView.findViewById<TextView>(R.id.tvDistance)

                tvIcon.text = when (stop.modeType) {
                    "metro" -> "🚇"
                    "bus" -> "🚌"
                    else -> "🚆"
                }
                tvName.text = stop.canonical
                val modeLabel = when (stop.modeType) {
                    "metro" -> "Metro Station"
                    "bus" -> "Bus Stop"
                    else -> "Railway Station"
                }
                val lines = stop.lines.take(2).joinToString(", ")
                tvSubtitle.text = if (lines.isNotEmpty()) "$modeLabel · $lines" else modeLabel
                tvDist.text = "· ${formatDist(stop.distanceKm)}"

                // Click → open Google Maps walking directions to this stop
                cardView.setOnClickListener {
                    openGoogleMapsDirections(stop)
                }

                binding.containerNearbyStops.addView(cardView)
            }
        }
    }

    private fun openGoogleMapsDirections(stop: NearestStation) {
        val modeLabel = when (stop.modeType) {
            "metro" -> "Metro Station"
            "bus"   -> "Bus Stop"
            else    -> "Railway Station"
        }
        val encodedDest = Uri.encode("${stop.canonical} $modeLabel, Mumbai")

        fun launchMaps(fromLat: Double, fromLon: Double) {
            val uri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                "&origin=$fromLat,$fromLon" +
                "&destination=$encodedDest" +
                "&travelmode=walking"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        // Request a fresh GPS fix so the origin is current, not the cached value from
        // when the nearby-stops list was first loaded (user may have moved since then).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocation.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    // Update cache while we're at it
                    currentLat = loc.latitude
                    currentLon = loc.longitude
                    launchMaps(loc.latitude, loc.longitude)
                } else if (currentLat != 0.0) {
                    // Fresh fix unavailable — fall back to last known
                    launchMaps(currentLat, currentLon)
                } else {
                    Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                if (currentLat != 0.0) launchMaps(currentLat, currentLon)
                else Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show()
            }
        } else if (currentLat != 0.0) {
            // No permission for a fresh fix but we have a cached value — use it
            launchMaps(currentLat, currentLon)
        } else {
            Toast.makeText(this, "Location permission needed for directions", Toast.LENGTH_SHORT).show()
        }
    }

    // ── GPS location ──────────────────────────────────────────────────────────

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        fusedLocation.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Toast.makeText(this, "Location unavailable. Try again.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            if (!inMumbaiArea(loc.latitude, loc.longitude)) {
                Toast.makeText(this, "You appear to be outside Mumbai", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            // Keep currentLat/currentLon in sync so Maps directions always have a valid origin
            currentLat = loc.latitude
            currentLon = loc.longitude
            setOrigin(loc.latitude, loc.longitude,
                "My Location (%.4f, %.4f)".format(loc.latitude, loc.longitude))
        }
    }

    // ── Places picker ─────────────────────────────────────────────────────────

    private fun launchPlaces() {
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("IN").build(this)
        placesLauncher.launch(intent)
    }

    // ── Swap ──────────────────────────────────────────────────────────────────

    private fun swapFields() {
        val tmpLat = originLat; val tmpLon = originLon; val tmpLabel = originLabel
        val tmpPin = pinnedOrigin
        setOrigin(destLat, destLon, destLabel)
        setDest(tmpLat, tmpLon, tmpLabel)
        // Restore pinned selections after swap
        pinnedOrigin = pinnedDest
        pinnedDest   = tmpPin
    }

    // ── Departure time ────────────────────────────────────────────────────────

    private fun pickDepartTime() {
        val now = Calendar.getInstance()
        val startHour = departAfterMins?.div(60) ?: now.get(Calendar.HOUR_OF_DAY)
        val startMin  = departAfterMins?.rem(60) ?: now.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(this, { _, hour, minute ->
            departAfterMins = hour * 60 + minute
            updateDepartTimeUi()
        }, startHour, startMin, false)

        dialog.setTitle("Show trains from")
        // Lets the commuter undo a pick without hunting for the current clock time.
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Leave now") { _, _ ->
            departAfterMins = null
            updateDepartTimeUi()
        }
        dialog.show()
    }

    private fun updateDepartTimeUi() {
        val mins = departAfterMins
        if (mins == null) {
            binding.tvDepartTime.text = "Now"
            binding.tvDepartHint.text = "Trains leaving in the next 2 hours will be shown."
        } else {
            val from = TransitEngine.clock12(mins)
            val to   = TransitEngine.clock12(mins + TransitEngine.SEARCH_WINDOW_MIN)
            binding.tvDepartTime.text = from
            binding.tvDepartHint.text = "Trains leaving between $from and $to will be shown."
        }
    }

    // ── Route search ──────────────────────────────────────────────────────────

    private fun searchRoutes() {
        if (originLat == 0.0) { Toast.makeText(this, "Set your origin first", Toast.LENGTH_SHORT).show(); return }
        if (destLat   == 0.0) { Toast.makeText(this, "Set your destination first", Toast.LENGTH_SHORT).show(); return }
        vm.departAfterMins = departAfterMins
        vm.searchRoutes(originLat, originLon, destLat, destLon,
            originLabel, destLabel, pinnedOrigin, pinnedDest)
        val intent = Intent(this, RoutePreviewActivity::class.java).apply {
            putExtra(RoutePreviewActivity.EXTRA_ORIGIN_LAT,   originLat)
            putExtra(RoutePreviewActivity.EXTRA_ORIGIN_LON,   originLon)
            putExtra(RoutePreviewActivity.EXTRA_DEST_LAT,     destLat)
            putExtra(RoutePreviewActivity.EXTRA_DEST_LON,     destLon)
            putExtra(RoutePreviewActivity.EXTRA_ORIGIN_LABEL, originLabel)
            putExtra(RoutePreviewActivity.EXTRA_DEST_LABEL,   destLabel)
        }
        startActivity(intent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatDist(km: Double) = when {
        km < 1.0 -> "${(km * 1000).toInt()}m"
        else     -> "${"%.1f".format(km)}km"
    }

    private fun inMumbaiArea(lat: Double, lon: Double) =
        lat in 18.5..19.8 && lon in 72.6..73.5

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        // Only show Saved Routes entry when someone is logged in
        menu.findItem(R.id.action_saved_routes)?.isVisible = authRepo.session.isLoggedIn()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_mri -> { startActivity(Intent(this, MriActivity::class.java)); true }
        R.id.action_saved_routes -> { startActivity(Intent(this, SavedRoutesActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    // Re-evaluate menu visibility whenever the activity resumes (e.g. after login/logout)
    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        updateFabState()
    }
}
