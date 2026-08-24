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
        val timetable: List<Map<String, String>>
    )

    suspend fun load(context: Context): LoadedData = withContext(Dispatchers.IO) {
        val nodes    = loadNodes(context)
        val edges    = loadEdges(context)
        val mri      = loadMri(context)
        val fare     = loadFareLookup(context)
        val auto     = loadTariff(context, R.raw.tariff_auto)
        val taxi     = loadTariff(context, R.raw.tariff_taxi)
        val tt       = loadTimetable(context)
        LoadedData(nodes, edges, mri, fare, auto, taxi, tt)
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
