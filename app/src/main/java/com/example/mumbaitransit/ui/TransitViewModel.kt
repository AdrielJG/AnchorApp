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

    var selectedRoute: RouteCard? = null

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
            val now = Calendar.getInstance()
            val currentMins = if (departTime.isNotEmpty()) {
                val parts = departTime.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            } else {
                now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            }

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

                var cursor = currentMins + route.walkToMin
                journeySegments.map { segment ->
                    val firstEdge = segment.first()
                    val lastEdge = segment.last()

                    val trains = engine.getNextTrains(
                        firstEdge.line, firstEdge.fromStop, lastEdge.toStop, cursor, 3
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
            _timetable.value = legs
        }
    }

    fun calculateAutoFare(distanceKm: Double): Int {
        if (!::engine.isInitialized) return 0
        return engine.autoFare(distanceKm)
    }

    fun resetRoutes() {
        _routeState.value = RouteState.Idle
    }
}
