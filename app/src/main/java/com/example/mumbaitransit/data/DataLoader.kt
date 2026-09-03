package com.example.mumbaitransit.data

import android.content.Context
import com.example.mumbaitransit.R
import com.example.mumbaitransit.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object DataLoader {

    data class LoadedData(
        val nodes: List<GraphNode>,
        val edges: List<GraphEdge>,
        val mriRows: List<MriRow>,
        val fareLookup: Map<String, Map<String, String>>,
        val autoTariff: List<Pair<Double, Double>>,   // (km, fare_day)
        val taxiTariff: List<Pair<Double, Double>>,
        val timetable: List<Map<String, String>>,
        /**
         * service_id -> (station name -> clock time in minutes past midnight).
         *
         * Rail only; metro and bus have no stop-time rows and fall back to the
         * offset tables. Times are raw as published, so a service running past
         * midnight has small values at its later stops — [TransitEngine]
         * unwraps that when it builds its index.
         */
        val stopTimes: Map<String, Map<String, Int>>
    )

    suspend fun load(context: Context): LoadedData = withContext(Dispatchers.IO) {
        val nodes    = loadNodes(context)
        val edges    = loadEdges(context)
        val mri      = loadMri(context)
        val fare     = loadFareLookup(context)
        val auto     = loadTariff(context, R.raw.tariff_auto)
        val taxi     = loadTariff(context, R.raw.tariff_taxi)
        val tt       = loadTimetable(context)
        val stops    = loadStopTimes(context)
        LoadedData(nodes, edges, mri, fare, auto, taxi, tt, stops)
    }

    private fun loadNodes(context: Context): List<GraphNode> {
        val result = mutableListOf<GraphNode>()
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.graph_nodes)))
        val header = reader.readLine()?.split(",") ?: return result
        val idx = header.map { it.trim().lowercase() }
        fun col(row: List<String>, name: String) = row.getOrElse(idx.indexOf(name)) { "" }.trim().trim('"')
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = parseCsvLine(line)
            result.add(GraphNode(
                nodeId       = col(cols, "node_id"),
                stopName     = col(cols, "stop_name"),
                line         = col(cols, "line"),
                mode         = col(cols, "mode"),
                canonical    = col(cols, "canonical"),
                isInterchange= col(cols, "is_interchange").lowercase() in listOf("true","1","yes")
            ))
        }
        return result
    }

    private fun loadEdges(context: Context): List<GraphEdge> {
        val result = mutableListOf<GraphEdge>()
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.graph_edges)))
        val header = reader.readLine()?.split(",") ?: return result
        val idx = header.map { it.trim().lowercase() }
        fun col(row: List<String>, name: String) = row.getOrElse(idx.indexOf(name)) { "" }.trim().trim('"')
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = parseCsvLine(line)
            val mode = col(cols, "mode")
            if (mode == "BEST_Bus") return@forEachLine
            val travelMin = col(cols, "median_travel_min").toDoubleOrNull()?.coerceAtMost(120.0) ?: return@forEachLine
            result.add(GraphEdge(
                fromNodeId       = col(cols, "from_node_id"),
                toNodeId         = col(cols, "to_node_id"),
                edgeType         = col(cols, "edge_type"),
                line             = col(cols, "line"),
                mode             = mode,
                fromStop         = col(cols, "from_stop"),
                toStop           = col(cols, "to_stop"),
                medianTravelMin  = travelMin,
                estFareInr       = col(cols, "est_fare_inr").toDoubleOrNull()?.toInt() ?: 0,
                reliabilityProxy = col(cols, "reliability_proxy").toDoubleOrNull() ?: 0.5,
                freqPerHour      = col(cols, "freq_per_hour").toDoubleOrNull() ?: 0.0,
                direction        = col(cols, "direction")
            ))
        }
        return result
    }

    private fun loadMri(context: Context): List<MriRow> {
        val result = mutableListOf<MriRow>()
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.phase4_mri)))
        val header = reader.readLine()?.split(",") ?: return result
        val idx = header.map { it.trim().lowercase() }
        fun col(row: List<String>, name: String) = row.getOrElse(idx.indexOf(name)) { "" }.trim().trim('"')
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = parseCsvLine(line)
            result.add(MriRow(
                line           = col(cols, "line"),
                mriPct         = col(cols, "mri_pct").toDoubleOrNull() ?: 0.0,
                cv             = col(cols, "cv").toDoubleOrNull() ?: 0.0,
                avgFreqPerHour = col(cols, "avg_freq_per_hour").toDoubleOrNull() ?: 0.0,
                acRatio        = col(cols, "ac_ratio").toDoubleOrNull() ?: 0.0,
                mode           = col(cols, "mode")
            ))
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFareLookup(context: Context): Map<String, Map<String, String>> {
        val json = context.resources.openRawResource(R.raw.fare_lookup).bufferedReader().readText()
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun loadTariff(context: Context, resId: Int): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(resId)))
        val header = reader.readLine()?.split(",") ?: return result
        val idx = header.map { it.trim().lowercase() }
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = parseCsvLine(line)
            val km   = cols.getOrElse(idx.indexOf("distance_km")) { "" }.trim().toDoubleOrNull() ?: return@forEachLine
            val fare = cols.getOrElse(idx.indexOf("fare_new_normal")) { "" }.trim().toDoubleOrNull() ?: return@forEachLine
            result.add(Pair(km, fare))
        }
        return result
    }

    fun loadTimetable(context: Context): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.phase2_unified_enriched)))
        val header = reader.readLine()?.split(",")?.map { it.trim().trim('"') } ?: return result
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = parseCsvLine(line)
            val mode = cols.getOrElse(header.indexOf("mode")) { "" }.trim()
            if (mode !in listOf("CR_Train", "WR_Train", "Metro")) return@forEachLine
            val row = mutableMapOf<String, String>()
            header.forEachIndexed { i, col -> row[col] = cols.getOrElse(i) { "" }.trim().trim('"') }
            result.add(row)
        }
        return result
    }

    /**
     * Reads the wide stop-times matrix: one row per service, one column per
     * station, blank where the service doesn't call there.
     *
     * Split by hand rather than through [parseCsvLine] — the file has no quoted
     * fields and this runs across ~350k cells, so the cheap path is worth it.
     * Blank cells are skipped, which is what makes a fast train's skipped stops
     * simply absent from its map.
     */
    fun loadStopTimes(context: Context): Map<String, Map<String, Int>> {
        val result = HashMap<String, Map<String, Int>>(4096)
        val reader = BufferedReader(
            InputStreamReader(context.resources.openRawResource(R.raw.phase2_stop_times))
        )
        val stations = reader.readLine()?.split(",")?.map { it.trim() } ?: return result
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val cols = line.split(",")
            val serviceId = cols.getOrNull(0)?.trim().orEmpty()
            if (serviceId.isEmpty()) return@forEachLine
            val stops = HashMap<String, Int>(24)
            for (i in 1 until minOf(cols.size, stations.size)) {
                val cell = cols[i]
                if (cell.length < 4) continue          // blank, or not an HH:MM
                val mins = hhmmToMins(cell) ?: continue
                stops[stations[i]] = mins
            }
            if (stops.isNotEmpty()) result[serviceId] = stops
        }
        return result
    }

    /** "07:39" -> 459. Null for anything that isn't a clock time. */
    private fun hhmmToMins(raw: String): Int? {
        val t = raw.trim()
        val colon = t.indexOf(':')
        if (colon !in 1..2) return null
        val h = t.substring(0, colon).toIntOrNull() ?: return null
        val m = t.substring(colon + 1).toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    /** Minimal CSV line parser handling quoted fields */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuote = false
        val cur = StringBuilder()
        for (c in line) {
            when {
                c == '"'  -> inQuote = !inQuote
                c == ',' && !inQuote -> { result.add(cur.toString()); cur.clear() }
                else -> cur.append(c)
            }
        }
        result.add(cur.toString())
        return result
    }
}
