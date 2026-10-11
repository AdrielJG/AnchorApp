package com.example.mumbaitransit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mumbaitransit.data.DataLoader
import com.example.mumbaitransit.engine.LastMile
import com.example.mumbaitransit.engine.LastMileMode
import com.example.mumbaitransit.engine.LastMileOption
import com.example.mumbaitransit.engine.TransitEngine
import com.example.mumbaitransit.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class LoadState {
    object Loading : LoadState()
    object Ready : LoadState()
    data class Error(val msg: String) : LoadState()
}

sealed class RouteState {
    object Idle : RouteState()
    object Searching : RouteState()
    /** The routes to list under each filter — already chosen and ranked by the engine. */
    data class Results(val selection: RouteSelection, val origLabel: String, val destLabel: String) : RouteState()
    data class Error(val msg: String) : RouteState()
}

class TransitViewModel(app: Application) : AndroidViewModel(app) {

    private val _loadState = MutableLiveData<LoadState>(LoadState.Loading)
    val loadState: LiveData<LoadState> = _loadState

    private val _routeState = MutableLiveData<RouteState>(RouteState.Idle)
    val routeState: LiveData<RouteState> = _routeState

    private val _timetable = MutableLiveData<List<TimetableLeg>>()
    val timetable: LiveData<List<TimetableLeg>> = _timetable

    private val _mriData = MutableLiveData<Map<String, MriScore>>()
    val mriData: LiveData<Map<String, MriScore>> = _mriData

    lateinit var engine: TransitEngine
        private set

    /** False until the CSVs finish loading — the chat screen can be opened first. */
    val isEngineReady: Boolean get() = ::engine.isInitialized

    var selectedRoute: RouteCard? = null

    /**
     * The route handed to the live-journey screen.
     *
     * Separate from [selectedRoute] on purpose: that one is what the detail
     * screen is showing, and sharing a journey from the results list must not
     * quietly change which route the user has open behind it.
     */
    var journeyCard: RouteCard? = null

    /**
     * The route handed to the map screen. Kept apart from the two above for the
     * same reason: opening a map must not disturb what the detail screen or a
     * running live journey is pointed at.
     */
    var mapCard: RouteCard? = null

    /**
     * The route handed to live navigation when the rider taps Start Walking /
     * Start Journey. Kept apart from [journeyCard] so starting navigation never
     * disturbs a journey that is being shared, and vice versa.
     */
    var navCard: RouteCard? = null

    /**
     * The origin and destination of the last search, in degrees.
     *
     * [RouteCard] carries station names but not the commuter's own two points,
     * and the map has to draw the walk at each end. Held here so any card in
     * the results list can be mapped without re-plumbing the coordinates
     * through every screen between.
     */
    var searchOriginLat = 0.0; private set
    var searchOriginLon = 0.0; private set
    var searchDestLat = 0.0; private set
    var searchDestLon = 0.0; private set

    /**
     * The trip currently being planned.
     *
     * Owned by the ViewModel rather than a screen because the home screen now
     * collects only the destination and Route Options edits both ends against
     * the results. Keeping the pair here means either screen can write to it
     * and the search always runs off one set of values.
     */
    var originLat = 0.0
    var originLon = 0.0
    var originLabel = ""
    var destLat = 0.0
    var destLon = 0.0
    var destLabel = ""

    /** Boarding / alighting stations pinned from the chips on Route Options. */
    var pinnedOrigin: String? = null
    var pinnedDest: String? = null

    /**
     * True while the origin is the device's own fix rather than a picked place.
     * Route Options shows it as "My Location" and re-reads GPS when it is set.
     */
    var originIsMyLocation = true

    val hasOrigin: Boolean get() = originLat != 0.0 || originLon != 0.0
    val hasDest: Boolean get() = destLat != 0.0 || destLon != 0.0

    fun setOrigin(lat: Double, lon: Double, label: String, isMyLocation: Boolean = false) {
        originLat = lat; originLon = lon; originLabel = label
        originIsMyLocation = isMyLocation
        pinnedOrigin = null
    }

    fun setDestination(lat: Double, lon: Double, label: String) {
        destLat = lat; destLon = lon; destLabel = label
        pinnedDest = null
    }

    /**
     * Marks the origin as the commuter's own position, seeding it with a fix
     * already in hand. A zero fix is fine — Route Options asks for a fresh one.
     */
    fun useMyLocationAsOrigin(lat: Double, lon: Double) {
        originLat = lat; originLon = lon
        originLabel = if (lat != 0.0 || lon != 0.0) "My Location" else ""
        originIsMyLocation = true
        pinnedOrigin = null
    }

    fun swapEnds() {
        val lat = originLat; val lon = originLon; val label = originLabel
        val wasMyLocation = originIsMyLocation
        val pin = pinnedOrigin
        originLat = destLat; originLon = destLon; originLabel = destLabel
        originIsMyLocation = false
        pinnedOrigin = pinnedDest
        destLat = lat; destLon = lon
        destLabel = if (wasMyLocation && label.isEmpty()) "My Location" else label
        pinnedDest = pin
    }

    /** Re-runs the search against whatever the trip state currently holds. */
    fun searchCurrentTrip() {
        if (!hasOrigin || !hasDest) return
        searchRoutes(originLat, originLon, destLat, destLon,
            originLabel, destLabel, pinnedOrigin, pinnedDest)
    }

    /**
     * Earliest boarding time the commuter asked for, in minutes past midnight.
     * Null means "leaving now", which is what the app did before the picker existed.
     */
    var departAfterMins: Int? = null

    /** e.g. "Trains from 11:00 PM to 1:00 AM", or null when leaving now. */
    fun departWindowLabel(): String? = departAfterMins?.let {
        "Trains from ${TransitEngine.clock12(it)} to " +
            TransitEngine.clock12(it + TransitEngine.SEARCH_WINDOW_MIN)
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _loadState.value = LoadState.Loading
                val data = DataLoader.load(getApplication())
                engine = TransitEngine(data)
                _mriData.value = engine.mriLookup
                _loadState.value = LoadState.Ready
            } catch (e: Exception) {
                _loadState.value = LoadState.Error("Failed to load data: ${e.message}")
            }
        }
    }

    fun searchRoutes(
        oLat: Double, oLon: Double, dLat: Double, dLon: Double,
        origLabel: String, destLabel: String,
        pinnedOrig: String? = null, pinnedDest: String? = null
    ) {
        if (!::engine.isInitialized) return
        searchOriginLat = oLat; searchOriginLon = oLon
        searchDestLat = dLat; searchDestLon = dLon
        viewModelScope.launch {
            _routeState.value = RouteState.Searching
            try {
                val selection = withContext(Dispatchers.Default) {
                    val origStns = engine.nearestStations(oLat, oLon, k = 5)
                    val destStns = engine.nearestStations(dLat, dLon, k = 5)
                    if (origStns.isEmpty()) throw Exception("No stations found near origin")
                    if (destStns.isEmpty()) throw Exception("No stations found near destination")
                    engine.planRoutes(origStns, destStns, oLat, oLon, dLat, dLon,
                        pinnedOrig, pinnedDest, origLabel, destLabel)
                }
                _routeState.value = RouteState.Results(selection, origLabel, destLabel)
            } catch (e: Exception) {
                _routeState.value = RouteState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchTimetable(route: RouteCard, departTime: String = "") {
        if (!::engine.isInitialized) return
        viewModelScope.launch {
            _timetable.value = buildTimetable(route, departTime)
        }
    }

    /**
     * The timetable for one route, returned rather than published.
     *
     * The share card needs trains for whichever card the user tapped Share on,
     * which is not necessarily the expanded one driving [timetable]. Returning
     * the legs keeps that lookup off the shared LiveData so sharing a collapsed
     * card can't overwrite the timings on screen.
     */
    suspend fun buildTimetable(route: RouteCard, departTime: String = ""): List<TimetableLeg> {
        if (!::engine.isInitialized) return emptyList()
        // An explicitly chosen time is the boarding time itself — the commuter
        // asked for trains *from* 11 PM, not for trains after walking there.
        val pickedMins = if (departTime.isNotEmpty()) {
            val parts = departTime.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } else {
            departAfterMins
        }
        val now = Calendar.getInstance()
        val currentMins = pickedMins
            ?: (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))

        val legs = withContext(Dispatchers.Default) {
            // A journey built from legs carries its own order of walks, buses
            // and trains; the engine walks the clock through all of them.
            if (route.legs.isNotEmpty()) {
                return@withContext engine.timetableFor(
                    route, currentMins, startIsBoarding = pickedMins != null
                )
            }
            val ivEdges = route.path.filter { it.edgeType == "in_vehicle" }

            val journeySegments = mutableListOf<List<PathEdge>>()
            var currentSegment = mutableListOf<PathEdge>()

            for (edge in ivEdges) {
                if (currentSegment.isEmpty() || currentSegment.last().line == edge.line) {
                    currentSegment.add(edge)
                } else {
                    journeySegments.add(currentSegment)
                    currentSegment = mutableListOf(edge)
                }
            }
            if (currentSegment.isNotEmpty()) {
                journeySegments.add(currentSegment)
            }

            var cursor = if (pickedMins != null) currentMins else currentMins + route.walkToMin
            journeySegments.map { segment ->
                // A late leg can run past midnight; keep the cursor inside one
                // day so it still matches the timetable's clock times.
                cursor = ((cursor % 1440) + 1440) % 1440
                val firstEdge = segment.first()
                val lastEdge = segment.last()

                val trains = engine.getNextTrains(
                    firstEdge.line, firstEdge.fromStop, lastEdge.toStop, cursor,
                    TransitEngine.TRAIN_OPTIONS, TransitEngine.SEARCH_WINDOW_MIN
                )

                val leg = TimetableLeg(
                    line = firstEdge.line,
                    fromStop = firstEdge.fromStop,
                    toStop = lastEdge.toStop,
                    arriveAtStopMins = cursor,
                    arriveAtStop = engine.minsToHhMm(cursor),
                    trains = trains
                )

                if (trains.isNotEmpty()) {
                    val first = trains[0]
                    val depParts = first.depFrom.split(":")
                    cursor = depParts[0].toInt() * 60 + depParts[1].toInt() + first.journeyMin
                } else {
                    cursor += segment.sumOf { it.travelMin.toInt() }
                }
                leg
            }
        }
        return legs
    }

    fun calculateAutoFare(distanceKm: Double): Int {
        if (!::engine.isInitialized) return 0
        return engine.autoFare(distanceKm)
    }

    fun calculateCabFare(distanceKm: Double): Int {
        if (!::engine.isInitialized) return 0
        return engine.taxiFare(distanceKm)
    }

    /** Walk / auto / cab options for a walk leg — see [LastMile]. */
    fun lastMileOptions(leg: RouteLeg): List<LastMileOption> =
        LastMile.options(leg.distanceKm, leg.minutes, { calculateAutoFare(it) }, { calculateCabFare(it) })

    /**
     * What the rider picked for each walk leg on the Routes screen, carried
     * into Live Navigation. Keyed by route + the leg's two ends.
     */
    private val lastMileChoices = mutableMapOf<String, LastMileMode>()

    private fun lastMileKey(card: RouteCard, leg: RouteLeg) = "${card.scenario}|${leg.from}|${leg.to}"

    fun setLastMileChoice(card: RouteCard, leg: RouteLeg, mode: LastMileMode) {
        lastMileChoices[lastMileKey(card, leg)] = mode
    }

    /** The picked option, or the default (first) one. */
    fun lastMileChoice(card: RouteCard, leg: RouteLeg): LastMileMode {
        val options = lastMileOptions(leg)
        val picked = lastMileChoices[lastMileKey(card, leg)]
        return picked?.takeIf { p -> options.any { it.mode == p } } ?: options.first().mode
    }

    fun resetRoutes() {
        _routeState.value = RouteState.Idle
    }

    /**
     * Publishes the searching state before the engine has been asked anything.
     *
     * Route Options can be opened while the CSVs are still loading, and without
     * this it would render the previous trip's results for the instant before
     * the new search starts.
     */
    fun markSearching() {
        _routeState.value = RouteState.Searching
    }
}
