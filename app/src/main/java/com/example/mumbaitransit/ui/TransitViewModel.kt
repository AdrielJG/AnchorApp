package com.example.mumbaitransit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mumbaitransit.data.DataLoader
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
    data class Results(val routes: List<RouteCard>, val origLabel: String, val destLabel: String) : RouteState()
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
        viewModelScope.launch {
            _routeState.value = RouteState.Searching
            try {
                val routes = withContext(Dispatchers.Default) {
                    val origStns = engine.nearestStations(oLat, oLon, k = 5)
                    val destStns = engine.nearestStations(dLat, dLon, k = 5)
                    if (origStns.isEmpty()) throw Exception("No stations found near origin")
                    if (destStns.isEmpty()) throw Exception("No stations found near destination")
                    engine.buildAllRoutes(origStns, destStns, oLat, oLon, dLat, dLon,
                        pinnedOrig, pinnedDest, origLabel, destLabel)
                }
                _routeState.value = RouteState.Results(routes, origLabel, destLabel)
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

    fun resetRoutes() {
        _routeState.value = RouteState.Idle
    }
}
