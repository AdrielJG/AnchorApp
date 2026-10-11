package com.example.mumbaitransit.ui

import android.Manifest
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
import com.example.mumbaitransit.journey.JourneySession
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.example.mumbaitransit.auth.AuthBottomSheet
import com.example.mumbaitransit.auth.AuthRepository
import com.example.mumbaitransit.auth.ProfileRepository
import com.example.mumbaitransit.auth.SavedPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home screen.
 *
 * One question only: where are you going. The origin, the swap and the
 * departure-time picker now live on [RoutesResultActivity], where they can be
 * changed against a list of real options instead of guessed at beforehand.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }
    private lateinit var fusedLocation: FusedLocationProviderClient

    // Destination state
    private var destLat = 0.0; private var destLon = 0.0
    private var destLabel = ""

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
            setDest(lat, lon, name)
            // No separate "Find Routes" button anymore — picking a place
            // (the Places overlay's own enter/select action) starts the
            // search immediately.
            searchRoutes()
        }
    }

    private val customAddressesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val data = result.data!!
        val lat = data.getDoubleExtra(CustomAddressesActivity.EXTRA_LAT, 0.0)
        val lon = data.getDoubleExtra(CustomAddressesActivity.EXTRA_LON, 0.0)
        val label = data.getStringExtra(CustomAddressesActivity.EXTRA_LABEL) ?: "Selected location"
        if (lat == 0.0 && lon == 0.0) return@registerForActivityResult
        if (!inMumbaiArea(lat, lon)) {
            Toast.makeText(this, "Location must be within Mumbai area", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        setDest(lat, lon, label)
        searchRoutes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Match the status bar to the hero image's sky color instead of the
        // default white, so there's no odd strip above "Anchor".
        window.statusBarColor = android.graphics.Color.parseColor("#D8E6F5")
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        // Init Google Places
        val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized()) Places.initialize(applicationContext, apiKey)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        setupListeners()
        setupQuickDestinations()
        setupBottomNav()

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
        binding.etDest.setOnClickListener { launchPlaces() }
        binding.btnClearDest.setOnClickListener { clearDest() }
    }

    // ── Quick destinations grid ─────────────────────────────────────────────

    private lateinit var profileRepo: ProfileRepository

    private fun setupQuickDestinations() {
        profileRepo = ProfileRepository(this)

        binding.quickHome.setOnClickListener { goToSavedPlace(SavedPlace.KIND_HOME) }
        binding.quickWork.setOnClickListener { goToSavedPlace(SavedPlace.KIND_WORK) }
        binding.quickCustom.setOnClickListener { openCustomAddresses() }
        binding.quickSavedRoutes.setOnClickListener {
            startActivity(Intent(this, SavedRoutesActivity::class.java))
        }
    }

    /**
     * Home and College/Work search their saved address directly — no picker.
     * Signed-out users are asked to sign in first (the addresses live on the
     * account); signed-in users with nothing saved yet are sent to Profile to
     * add one, same as tapping the empty row there would do.
     */
    private fun goToSavedPlace(kind: String) {
        if (!authRepo.session.isLoggedIn()) {
            promptSignIn("Sign in to use your saved addresses") { goToSavedPlace(kind) }
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val profile = withContext(Dispatchers.IO) { profileRepo.load() }
            val place = if (kind == SavedPlace.KIND_HOME) profile?.home else profile?.work
            if (place != null && place.hasFix) {
                if (!inMumbaiArea(place.lat, place.lon)) {
                    Toast.makeText(this@MainActivity, "Saved address is outside the Mumbai area", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                setDest(place.lat, place.lon, place.label.ifBlank { place.address })
                searchRoutes()
            } else {
                val what = if (kind == SavedPlace.KIND_HOME) "home" else "college/work"
                Toast.makeText(this@MainActivity, "Add your $what address in your profile first", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
            }
        }
    }

    private fun openCustomAddresses() {
        if (!authRepo.session.isLoggedIn()) {
            promptSignIn("Sign in to use custom addresses") { openCustomAddresses() }
            return
        }
        customAddressesLauncher.launch(Intent(this, CustomAddressesActivity::class.java))
    }

    private fun promptSignIn(message: String, onSignedIn: () -> Unit) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        val sheet = AuthBottomSheet.newInstance()
        sheet.onAuthSuccess = {
            updateAccountNavState()
            onSignedIn()
        }
        sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
    }

    // ── Bottom navigation (Home / Chat / Follow / Account) ──────────────────

    private lateinit var authRepo: AuthRepository

    private fun setupBottomNav() {
        authRepo = AuthRepository(this)

        // Firebase keeps the account signed in across restarts; sync our own
        // session flag to it before the nav bar decides what to show.
        authRepo.restoreSession()
        updateAccountNavState()

        binding.navHome.setOnClickListener {
            binding.scrollContent.smoothScrollTo(0, 0)
        }

        binding.navFollow.setOnClickListener {
            JourneyViewerActivity.start(this)
        }

        binding.navAccount.setOnClickListener {
            if (authRepo.session.isLoggedIn()) {
                startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                val sheet = AuthBottomSheet.newInstance()
                sheet.onAuthSuccess = { username ->
                    updateAccountNavState()
                    Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()
                }
                sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
            }
        }

        // Chat carries a name against every report, so it needs a signed-in
        // user. Bounce to the auth sheet rather than posting as "anonymous".
        binding.navChat.setOnClickListener {
            if (authRepo.session.isLoggedIn()) {
                startActivity(Intent(this, ChatRoomsActivity::class.java))
            } else {
                Toast.makeText(this, "Sign in to join the line chat", Toast.LENGTH_SHORT).show()
                val sheet = AuthBottomSheet.newInstance()
                sheet.onAuthSuccess = {
                    updateAccountNavState()
                    startActivity(Intent(this, ChatRoomsActivity::class.java))
                }
                sheet.show(supportFragmentManager, AuthBottomSheet.TAG)
            }
        }
    }

    private fun updateAccountNavState() {
        if (authRepo.session.isLoggedIn()) {
            binding.tvNavAccount.text = authRepo.session.getUsername()
            val green = android.graphics.Color.parseColor("#0A7C42")
            binding.tvNavAccount.setTextColor(green)
            binding.ivNavAccount.setColorFilter(green)
        } else {
            binding.tvNavAccount.text = "Account"
            val gray = android.graphics.Color.parseColor("#8A93A0")
            binding.tvNavAccount.setTextColor(gray)
            binding.ivNavAccount.setColorFilter(gray)
        }
    }

    // ── Destination ───────────────────────────────────────────────────────────

    private fun setDest(lat: Double, lon: Double, label: String) {
        destLat = lat; destLon = lon; destLabel = label
        binding.etDest.text = label
        binding.btnClearDest.visibility = View.VISIBLE
    }

    private fun clearDest() {
        destLat = 0.0; destLon = 0.0; destLabel = ""
        binding.etDest.text = ""
        binding.btnClearDest.visibility = View.GONE
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

    // ── Places picker ─────────────────────────────────────────────────────────

    private fun launchPlaces() {
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("IN").build(this)
        placesLauncher.launch(intent)
    }

    // ── Route search ──────────────────────────────────────────────────────────

    /**
     * Hands the trip to Route Options.
     *
     * The origin is left as "my location": the fix already cached for the
     * nearby-stops list is passed through when there is one, and Route Options
     * asks for a fresh one itself when there isn't.
     */
    private fun searchRoutes() {
        if (destLat == 0.0) {
            Toast.makeText(this, "Tell us where you'd like to go", Toast.LENGTH_SHORT).show()
            return
        }
        vm.setDestination(destLat, destLon, destLabel)
        vm.useMyLocationAsOrigin(currentLat, currentLon)
        vm.markSearching()
        startActivity(Intent(this, RoutesResultActivity::class.java))
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
        menu.findItem(R.id.action_profile)?.isVisible = authRepo.session.isLoggedIn()
        // Only worth offering while something is actually being shared.
        menu.findItem(R.id.action_my_journey)?.isVisible = JourneySession.isSharing
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
        R.id.action_mri -> { startActivity(Intent(this, MriActivity::class.java)); true }
        R.id.action_follow_journey -> { JourneyViewerActivity.start(this); true }
        R.id.action_my_journey -> { LiveJourneyActivity.start(this); true }
        R.id.action_saved_routes -> { startActivity(Intent(this, SavedRoutesActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    // Re-evaluate menu visibility whenever the activity resumes (e.g. after login/logout)
    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        updateAccountNavState()
    }
}
