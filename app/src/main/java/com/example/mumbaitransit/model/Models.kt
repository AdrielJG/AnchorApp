package com.example.mumbaitransit.model

data class GraphNode(
    val nodeId: String,
    val stopName: String,
    val line: String,
    val mode: String,
    val canonical: String,
    val isInterchange: Boolean
)

data class GraphEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val edgeType: String,   // in_vehicle | transfer | egress | access
    val line: String,
    val mode: String,
    val fromStop: String,
    val toStop: String,
    val medianTravelMin: Double,
    val estFareInr: Int,
    val reliabilityProxy: Double,
    val freqPerHour: Double,
    val direction: String,
    val isFast: Boolean = false,
    val skippedStops: List<String> = emptyList()
)

data class MriRow(
    val line: String,
    val mriPct: Double,
    val cv: Double,
    val avgFreqPerHour: Double,
    val acRatio: Double,
    val mode: String
)

data class NearestStation(
    val canonical: String,
    val distanceKm: Double,
    val lines: List<String>,
    val twins: List<String>,
    val modeType: String,   // rail | metro | bus
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class PathEdge(
    val edgeType: String,
    val line: String,
    val mode: String,
    val fromStop: String,
    val toStop: String,
    val travelMin: Double,
    var fareInr: Int,
    val reliability: Double,
    val freq: Double,
    val direction: String,
    val isFast: Boolean,
    val skippedStops: List<String>,
    var fareReal: Boolean = false
)

data class RouteResult(
    val path: List<PathEdge>,
    val totalMin: Double,
    val totalFare: Int,
    val transfers: Int,
    val linesUsed: List<String>,
    val modesUsed: List<String>,
    val isMultimodal: Boolean
)

data class RouteCard(
    val type: String,            // transit | bus | auto | cab
    val scenario: String,
    val scenarioLabel: String,
    val modeStr: String,
    val originLabel: String = "",      // User's origin input (e.g., "My Location")
    val destLabel: String = "",         // User's destination input (e.g., "Cat Cafe Studio")
    val originStation: String = "",     // Nearest station to origin
    val destStation: String = "",       // Nearest station to destination
    val walkToMin: Int = 0,
    val walkFromMin: Int = 0,
    val transitMin: Double = 0.0,
    val totalMin: Double,
    val totalFare: Int,
    val transfers: Int = 0,
    val linesUsed: List<String> = emptyList(),
    val modesUsed: List<String> = emptyList(),
    val isMultimodal: Boolean = false,
    val path: List<PathEdge> = emptyList(),
    val mriScores: Map<String, MriScore> = emptyMap(),
    val roadKm: Double = 0.0,
    val note: String = ""
)

data class MriScore(
    val mriPct: Double,
    val cv: Double,
    val freq: Double,
    val acRatio: Double
)

data class TimetableLeg(
    val line: String,
    val fromStop: String,
    val toStop: String,
    val arriveAtStopMins: Int,
    val arriveAtStop: String,
    val trains: List<TrainTiming>
)

data class TrainTiming(
    val depFrom: String,
    val arrTo: String,
    val journeyMin: Int,
    val trainType: String,
    val ac: Boolean,
    val waitMin: Int,
    val terminus: String = "",
    /**
     * Indian Railways train number, e.g. "91045". The bundled timetable has no
     * such column, so this is null today; LiveStatusActivity asks the user once
     * and remembers it. Populate here if the CSV ever gains train numbers and
     * live tracking will open with no prompt.
     */
    val trainNo: String? = null
)
