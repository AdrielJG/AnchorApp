package com.example.mumbaitransit.engine

import com.example.mumbaitransit.data.DataLoader
import com.example.mumbaitransit.model.*
import java.util.PriorityQueue
import kotlin.math.*

class TransitEngine(data: DataLoader.LoadedData) {

    // ── Graph structures ───────────────────────────────────────────────────────
    private val adj = mutableMapOf<String, MutableList<AdjEdge>>()
    private val nodeInfo = mutableMapOf<String, GraphNode>()
    private val canonicalMap = mutableMapOf<String, MutableList<String>>()  // canonical -> node IDs
    val canonicals: List<String> get() = canonicalMap.keys.sorted()

    // ── MRI ───────────────────────────────────────────────────────────────────
    val mriLookup = mutableMapOf<String, MriScore>()

    // ── Station geocoding ─────────────────────────────────────────────────────
    private val stationPoints = mutableListOf<StationPoint>()
    private val canonicalLines = mutableMapOf<String, MutableList<String>>()
    private val canonicalMode  = mutableMapOf<String, String>()
    val twinMap = mutableMapOf<String, MutableList<String>>()

    // ── Fare / tariff ─────────────────────────────────────────────────────────
    private val fareLookup: Map<String, Map<String, String>>
    private val autoKm:    DoubleArray
    private val autoFares: DoubleArray
    private val taxiKm:    DoubleArray
    private val taxiFares: DoubleArray

    // ── Timetable ─────────────────────────────────────────────────────────────
    private val timetable: List<Map<String, String>>

    /**
     * service_id -> (station -> minutes past the service's own start), with
     * midnight crossings unwrapped so times increase along the run.
     *
     * Built once from the stop-times matrix. A station absent from a service's
     * map means that service does not call there, which is how fast trains are
     * filtered out of a stop pair without any special-casing.
     */
    private val stopIndex = mutableMapOf<String, Map<String, Int>>()

    /** Lines that have real stop times. Everything else uses the offset tables. */
    private val stopTimeLines = mutableSetOf<String>()

    /**
     * line -> (lowercased station -> the exact key used in that line's stop maps).
     *
     * The matrix spells the same physical station differently per line — Western
     * Railway writes BANDRA, the Harbour line writes Bandra — and the routing
     * graph already uses the matching spelling for each. This resolves a stop
     * name within its own line so those never cross over.
     */
    private val stopNamesByLine = mutableMapOf<String, MutableMap<String, String>>()

    data class AdjEdge(
        val to: String, val edgeType: String, val line: String, val mode: String,
        val travelMin: Double, val fareInr: Int, val reliability: Double, val freq: Double,
        val fromStop: String, val toStop: String, val direction: String,
        val isFast: Boolean, val skippedStops: List<String>
    )

    data class StationPoint(val lat: Double, val lon: Double, val canonical: String, val line: String)

    private val badEdges = setOf(
        Triple("GTB Nagar", "Andheri",   "Harbour Line CSMT"),
        Triple("Bandra",    "Santacruz", "Harbour Line CSMT")
    )

    init {
        fareLookup = data.fareLookup
        autoKm    = data.autoTariff.map { it.first  }.toDoubleArray()
        autoFares = data.autoTariff.map { it.second }.toDoubleArray()
        taxiKm    = data.taxiTariff.map { it.first  }.toDoubleArray()
        taxiFares = data.taxiTariff.map { it.second }.toDoubleArray()
        timetable = data.timetable
        buildStopIndex(data)

        // Build node info + canonical map
        for (node in data.nodes) {
            nodeInfo[node.nodeId] = node
            canonicalMap.getOrPut(node.canonical) { mutableListOf() }.add(node.nodeId)
        }

        // Build adjacency list
        for (e in data.edges) {
            if (e.fromNodeId.isEmpty() || e.toNodeId.isEmpty()) continue
            if (e.edgeType == "in_vehicle" &&
                badEdges.contains(Triple(e.fromStop, e.toStop, e.line))) continue
            adj.getOrPut(e.fromNodeId) { mutableListOf() }.add(
                AdjEdge(e.toNodeId, e.edgeType, e.line, e.mode,
                    e.medianTravelMin, e.estFareInr, e.reliabilityProxy, e.freqPerHour,
                    e.fromStop, e.toStop, e.direction, false, emptyList())
            )
        }

        // Inject missing Harbour ↔ Trans-Harbour transfers
        val missingTransfers = listOf(
            arrayOf("N0067","N0161","Sanpada","SANPADA",12),
            arrayOf("N0057","N0159","Juinagar","JUINAGAR",3),
            arrayOf("N0080","N0164","Kharghar","KHARGHAR",3),
            arrayOf("N0070","N0156","Khandeshwar","KHANDESHWAR",3),
            arrayOf("N0077","N0160","Mansarovar","MANSAROVAR",3),
            arrayOf("N0069","N0168","Seawood Darave","SEAWOODS-DARAVE",3)
        )
        for (t in missingTransfers) {
            val (hNid, tNid, hStop, tStop, walkMin) = t
            val baseEdge = { from: String, to: String, fs: String, ts: String ->
                AdjEdge(to, "transfer", "Harbour Line CSMT ↔ Trans-Harbour Line", "CR_Train",
                    (walkMin as Int).toDouble(), 0, 1.0, 0.0, fs, ts, "both", false, emptyList())
            }
            adj.getOrPut(hNid as String) { mutableListOf() }.add(baseEdge(hNid, tNid as String, hStop as String, tStop as String))
            adj.getOrPut(tNid) { mutableListOf() }.add(baseEdge(tNid, hNid, tStop, hStop))
        }

        // Build MRI lookup
        for (row in data.mriRows) {
            mriLookup[row.line] = MriScore(row.mriPct, row.cv, row.avgFreqPerHour, row.acRatio)
        }
        mriLookup["BEST_Bus (AC)"] = MriScore(55.0, 0.0, 2.5, 1.0)

        // Station geocoding
        val modeRank = mapOf("CR_Train" to 0, "WR_Train" to 0, "Metro" to 1, "BEST_Bus" to 2)
        val seen = mutableSetOf<String>()
        for (node in data.nodes) {
            val can = node.canonical
            canonicalLines.getOrPut(can) { mutableListOf() }.let { list ->
                if (node.line !in list) list.add(node.line)
            }
            val existingRank = modeRank[canonicalMode[can]] ?: 3
            val newRank = modeRank[node.mode] ?: 3
            if (newRank < existingRank) canonicalMode[can] = node.mode
            if (can !in seen) {
                val coords = StationCoords.coords[can]
                if (coords != null) {
                    stationPoints.add(StationPoint(coords.first, coords.second, can, node.line))
                    seen.add(can)
                }
            }
        }

        // Build twin map
        val coordToCanonicals = mutableMapOf<String, MutableList<String>>()
        for (sp in stationPoints) {
            val key = "${sp.lat.toBigDecimal().setScale(4)}|${sp.lon.toBigDecimal().setScale(4)}"
            coordToCanonicals.getOrPut(key) { mutableListOf() }.add(sp.canonical)
        }
        for ((_, cans) in coordToCanonicals) {
            if (cans.size > 1) registerTwins(cans)
        }
    }

    private fun registerTwins(group: List<String>) {
        for (can in group) {
            val others = group.filter { it != can }
            val list = twinMap.getOrPut(can) { mutableListOf() }
            for (o in others) if (o !in list) list.add(o)
        }
    }

    // ── Haversine distance ────────────────────────────────────────────────────
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val a = sin(Math.toRadians(lat2 - lat1) / 2).pow(2) +
                cos(phi1) * cos(phi2) * sin(Math.toRadians(lon2 - lon1) / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ── Nearest stations ──────────────────────────────────────────────────────
    fun nearestStations(lat: Double, lon: Double, k: Int = 5): List<NearestStation> {
        return stationPoints
            .map { sp -> Pair(haversineKm(lat, lon, sp.lat, sp.lon), sp) }
            .sortedBy { it.first }
            .take(k)
            .map { (dist, sp) ->
                val mode = canonicalMode[sp.canonical] ?: "CR_Train"
                NearestStation(sp.canonical, dist.roundTo(2),
                    canonicalLines[sp.canonical] ?: listOf(sp.line),
                    twinMap[sp.canonical] ?: emptyList(),
                    when (mode) { "Metro" -> "metro"; "BEST_Bus" -> "bus"; else -> "rail" },
                    sp.lat, sp.lon)
            }
    }

    fun nearestStationsGrouped(lat: Double, lon: Double): Map<String, List<NearestStation>> {
        val all = stationPoints
            .map { sp -> Pair(haversineKm(lat, lon, sp.lat, sp.lon), sp) }
            .sortedBy { it.first }

        val rail = mutableListOf<NearestStation>()
        val metro = mutableListOf<NearestStation>()
        val bus = mutableListOf<NearestStation>()

        for ((dist, sp) in all) {
            val mode = canonicalMode[sp.canonical] ?: "CR_Train"
            val modeType = when (mode) { "Metro" -> "metro"; "BEST_Bus" -> "bus"; else -> "rail" }
            val nearThresh = if (modeType == "bus") 1.5 else 2.5
            val bucket = when (modeType) { "metro" -> metro; "bus" -> bus; else -> rail }
            val maxK = when (modeType) { "metro" -> 3; "bus" -> 3; else -> 4 }
            if (dist > nearThresh || bucket.size >= maxK) continue
            bucket.add(NearestStation(sp.canonical, dist.roundTo(2),
                canonicalLines[sp.canonical] ?: listOf(sp.line),
                twinMap[sp.canonical] ?: emptyList(), modeType,
                sp.lat, sp.lon))
        }
        return mapOf("rail" to rail, "metro" to metro, "bus" to bus)
    }

    // ── Dijkstra ──────────────────────────────────────────────────────────────
    private val DIRECTION_CHANGE_PENALTY = 40.0

    data class State(val nodeId: String, val prevLine: String, val prevDir: String)
    data class HeapEntry(val cost: Double, val state: State) : Comparable<HeapEntry> {
        override fun compareTo(other: HeapEntry) = cost.compareTo(other.cost)
    }

    fun dijkstra(startNids: List<String>, endNids: List<String>,
                 alpha: Double, beta: Double, gamma: Double): RouteResult? {
        val INF = Double.MAX_VALUE
        val dist = mutableMapOf<State, Double>()
        val prev = mutableMapOf<State, Pair<State, AdjEdge>>()
        val heap = PriorityQueue<HeapEntry>()
        val endSet = endNids.toSet()

        for (nid in startNids) {
            val s = State(nid, "", "")
            dist[s] = 0.0
            heap.add(HeapEntry(0.0, s))
        }

        val visited = mutableSetOf<State>()
        var bestEnd: State? = null

        while (heap.isNotEmpty()) {
            val (cost, cur) = heap.poll()
            if (cur in visited) continue
            visited.add(cur)
            if (cur.nodeId in endSet) { bestEnd = cur; break }

            for (e in adj[cur.nodeId] ?: emptyList()) {
                var penalty = 0.0
                if (e.edgeType == "in_vehicle" && cur.prevLine == e.line &&
                    cur.prevDir.isNotEmpty() && e.direction.isNotEmpty() &&
                    cur.prevDir != e.direction && e.direction != "both")
                    penalty = DIRECTION_CHANGE_PENALTY

                val ec = alpha * e.travelMin + beta * (e.fareInr / 10.0) +
                        gamma * (1 - e.reliability) * 10 + penalty
                val nc = cost + ec
                val npl = if (e.edgeType == "in_vehicle") e.line else cur.prevLine
                val npd = if (e.edgeType == "in_vehicle") e.direction else cur.prevDir
                val nst = State(e.to, npl, npd)
                if (nc < (dist[nst] ?: INF)) {
                    dist[nst] = nc
                    prev[nst] = Pair(cur, e)
                    heap.add(HeapEntry(nc, nst))
                }
            }
        }

        if (bestEnd == null) {
            val cands = dist.entries.filter { it.key.nodeId in endSet }
            if (cands.isEmpty()) return null
            bestEnd = cands.minByOrNull { it.value }?.key ?: return null
        }

        val pathEdges = mutableListOf<AdjEdge>()
        var cur = bestEnd
        while (cur in prev) {
            val (par, e) = prev[cur]!!
            pathEdges.add(e); cur = par
        }
        pathEdges.reverse()

        // Apply real fares
        val resultPath = pathEdges.map { e ->
            var fareInr = e.fareInr
            var fareReal = false
            if (e.edgeType == "in_vehicle") {
                val real = lookupFare(e.fromStop, e.toStop)
                if (real != null) { fareInr = real; fareReal = true }
            }
            PathEdge(e.edgeType, e.line, e.mode, e.fromStop, e.toStop,
                e.travelMin, fareInr, e.reliability, e.freq, e.direction,
                e.isFast, e.skippedStops, fareReal)
        }

        val linesUsed = resultPath.filter { it.edgeType == "in_vehicle" }
            .map { it.line }.distinct()
        val modesUsed = resultPath.filter { it.edgeType == "in_vehicle" }
            .map { it.mode }.distinct()

        return RouteResult(
            path         = resultPath,
            totalMin     = resultPath.sumOf { it.travelMin }.roundTo(1),
            totalFare    = resultPath.sumOf { it.fareInr },
            transfers    = resultPath.count { it.edgeType == "transfer" },
            linesUsed    = linesUsed,
            modesUsed    = modesUsed,
            isMultimodal = modesUsed.toSet().size > 1
        )
    }

    private fun lookupFare(from: String, to: String): Int? {
        val entry = fareLookup["$from|$to"] ?: fareLookup["$to|$from"] ?: return null
        return entry["2nd"]?.toIntOrNull()
    }

    // ── Scenario weights ──────────────────────────────────────────────────────
    private val scenarioWeights = mapOf(
        "fastest"  to Triple(1.0, 0.0, 0.0),
        "cheapest" to Triple(0.1, 1.0, 0.0),
        "reliable" to Triple(0.5, 0.0, 1.0),
        "balanced" to Triple(0.5, 0.3, 0.2)
    )

    val scenarioLabels = mapOf(
        "fastest"  to "⚡ Fastest",
        "cheapest" to "💰 Affordable",
        "reliable" to "🛡️ Most Reliable",
        "balanced" to "⚖️ Balanced"
    )

    // ── Build all routes ──────────────────────────────────────────────────────
    fun buildAllRoutes(
        origStations: List<NearestStation>, destStations: List<NearestStation>,
        oLat: Double, oLon: Double, dLat: Double, dLon: Double,
        pinnedOrig: String? = null, pinnedDest: String? = null,
        origLabel: String = "", destLabel: String = ""
    ): List<RouteCard> {
        val roadKm = (haversineKm(oLat, oLon, dLat, dLon) * 1.3).roundTo(2)

        val origCandidates = if (pinnedOrig != null)
            (origStations.filter { it.canonical == pinnedOrig }.takeIf { it.isNotEmpty() } ?: origStations.take(1))
        else origStations.take(3)

        val destCandidates = if (pinnedDest != null)
            (destStations.filter { it.canonical == pinnedDest }.takeIf { it.isNotEmpty() } ?: destStations.take(1))
        else destStations.take(3)

        val scenarioBest = mutableMapOf<String, RouteCard>()
        for (sc in listOf("fastest", "cheapest", "reliable", "balanced")) {
            val (a, b, g) = scenarioWeights[sc]!!
            var bestCard: RouteCard? = null
            var bestTotal = Double.MAX_VALUE
            for (oInfo in origCandidates) {
                val oNids = canonicalMap[oInfo.canonical] ?: continue
                for (dInfo in destCandidates) {
                    val dNids = canonicalMap[dInfo.canonical] ?: continue
                    if (dInfo.canonical == oInfo.canonical) continue
                    val result = dijkstra(oNids, dNids, a, b, g) ?: continue
                    val walkTo   = max(1, (oInfo.distanceKm / 5.0 * 60).roundToInt())
                    val walkFrom = max(1, (dInfo.distanceKm / 5.0 * 60).roundToInt())
                    val total    = (result.totalMin + walkTo + walkFrom).roundTo(1)
                    if (total < bestTotal) {
                        bestTotal = total
                        val mriScores = result.linesUsed.mapNotNull { line ->
                            mriLookup[line]?.let { line to it }
                        }.toMap()
                        val labels = result.modesUsed.map { modeLabel(it) }.distinct()
                        var modeStr = labels.joinToString(" + ")
                        if (walkTo > 0) modeStr = "Walk + $modeStr"
                        if (walkFrom > 0) modeStr = "$modeStr + Walk"
                        bestCard = RouteCard(
                            type           = "transit",
                            scenario       = sc,
                            scenarioLabel  = scenarioLabels[sc]!!,
                            modeStr        = modeStr,
                            originLabel    = origLabel,
                            destLabel      = destLabel,
                            originStation  = oInfo.canonical,
                            destStation    = dInfo.canonical,
                            walkToMin      = walkTo,
                            walkFromMin    = walkFrom,
                            transitMin     = result.totalMin,
                            totalMin       = total,
                            totalFare      = result.totalFare,
                            transfers      = result.transfers,
                            linesUsed      = result.linesUsed,
                            modesUsed      = result.modesUsed,
                            isMultimodal   = result.isMultimodal,
                            path           = result.path,
                            mriScores      = mriScores
                        )
                    }
                }
            }
            if (bestCard != null) scenarioBest[sc] = bestCard
        }

        // Deduplicate by path signature
        val seenSigs = mutableSetOf<List<Pair<String,String>>>()
        val transitRoutes = mutableListOf<RouteCard>()
        for (sc in listOf("fastest", "cheapest", "reliable", "balanced")) {
            val card = scenarioBest[sc] ?: continue
            val sig = card.path.filter { it.edgeType == "in_vehicle" }.map { it.fromStop to it.toStop }
            if (sig in seenSigs) continue
            seenSigs.add(sig)
            transitRoutes.add(card)
            if (transitRoutes.size == 3) break
        }

        val routes = mutableListOf<RouteCard>()
        routes.addAll(transitRoutes)

        // Bus
        // BEST Bus — avg Mumbai bus speed ~13 km/h in traffic; fares stage-based ~₹6 min, ~₹1/km after
        val busMin = (roadKm / 13.0 * 60).roundToInt()
        routes.add(RouteCard("bus", "bus", "🚌 BEST Bus", "Bus",
            originLabel = origLabel, destLabel = destLabel,
            totalMin = busMin.toDouble(),
            totalFare = max(6, (5 + roadKm * 0.8).toInt()),
            roadKm = roadKm,
            note = "Approx $roadKm km road · Bus times vary by route and traffic"))

        // Auto — tariff from MMRTA CSV; avg Mumbai auto speed ~20 km/h; CSV covers up to 30 km
        if (roadKm <= 30.0) {
            routes.add(RouteCard("auto", "auto", "🛺 Auto-rickshaw", "Auto-rickshaw",
                originLabel = origLabel, destLabel = destLabel,
                totalMin = (roadKm / 20.0 * 60).roundTo(0),
                totalFare = interp(roadKm, autoKm, autoFares).toInt(),
                roadKm = roadKm,
                note = "MMRTA 2025 tariff · subject to traffic"))
        }

        // Kali-Peeli / App Cab — use metered taxi tariff CSV; avg speed ~22 km/h
        // Ola/Uber fares will be higher; taxi CSV reflects metered kali-peeli rates
        routes.add(RouteCard("cab", "cab", "🚕 Kali-Peeli / App Cab", "Cab",
            originLabel = origLabel, destLabel = destLabel,
            totalMin = (roadKm / 22.0 * 60).roundTo(0),
            totalFare = interp(roadKm, taxiKm, taxiFares).toInt(),
            roadKm = roadKm,
            note = "Kali-Peeli meter tariff shown · Ola/Uber will vary with surge"))

        return routes
    }

    private fun modeLabel(mode: String) = when (mode) {
        "CR_Train", "WR_Train" -> "Train"
        "Metro" -> "Metro"
        "BEST_Bus" -> "Bus"
        else -> mode
    }

    private fun interp(x: Double, xs: DoubleArray, ys: DoubleArray): Double {
        if (x <= 0 || xs.isEmpty()) return 0.0
        if (x <= xs.last()) {
            val i = xs.indexOfFirst { it >= x }.takeIf { it > 0 } ?: return ys[0]
            val t = (x - xs[i-1]) / (xs[i] - xs[i-1])
            return ys[i-1] + t * (ys[i] - ys[i-1])
        }
        val n = xs.size
        val rate = (ys[n-1] - ys[n-2]) / (xs[n-1] - xs[n-2])
        return ys[n-1] + rate * (x - xs[n-1])
    }

    fun autoFare(km: Double)  = interp(km, autoKm, autoFares).toInt()
    fun taxiFare(km: Double)  = interp(km, taxiKm, taxiFares).toInt()

    // ── Timetable ─────────────────────────────────────────────────────────────
    private val stationAliases = mapOf(
        // Seawoods variations
        "seawood darave"  to "Seawoods-Darave",
        "seawoods darave" to "Seawoods-Darave",
        // Standard station names - case normalization
        "churchgate"      to "CHURCHGATE",
        "virar"           to "VIRAR",
        "dadar"           to "DADAR",
        "bandra"          to "BANDRA",
        "andheri"         to "ANDHERI",
        "borivali"        to "BORIVALI",
        "kurla"           to "Kurla",
        "thane"           to "Thane",
        "ghatkopar"       to "Ghatkopar",
        "versova"         to "Versova",
        // Metro stations
        "mumbai csmt"     to "Mumbai CSMT",
        "csmt"            to "CSMT",
        // Harbour line variations
        "sanpada"         to "Sanpada",
        "vashi"           to "Vashi",
        "panvel"          to "Panvel",
        "nerul"           to "Nerul",
        "mankhurd"        to "Mankhurd",
        // CR Main Line
        "csmt"            to "CSMT",
        "kalyan"          to "Kalyan",
        "wadala"          to "Wadala",
        "byculla"         to "Byculla"
    )

    private fun normalizeStop(name: String): String {
        val lower = name.trim().lowercase()
        return stationAliases[lower] ?: name.trim()
    }

    fun minsToHhMm(m: Int): String {
        val mm = ((m % 1440) + 1440) % 1440
        return "%02d:%02d".format(mm / 60, mm % 60)
    }

    /**
     * Normalises the published stop-times matrix into [stopIndex].
     *
     * Two corrections are applied, both driven by what the source data actually
     * contains rather than by assumption:
     *
     *  1. **"00:00" is a null sentinel.** 16 services carry a 00:00 at a station
     *     they don't serve — CR_Up_95210 shows "Kasara 00:00" while genuinely
     *     running Badlapur 10:42 to CSMT 12:12. Left in, it stretches that
     *     service's span from 90 minutes to 732 and corrupts the ordering. It is
     *     dropped unless the service really does depart or arrive at midnight,
     *     which is true of exactly one service (CR_Up_97436, Thane 00:00).
     *
     *  2. **Midnight crossings are unwrapped.** A run that starts 23:38 and ends
     *     00:14 has times that appear to go backwards. Sorting the stop times
     *     and splitting at the largest gap recovers the true order; after this,
     *     no service spans more than 170 minutes and only 2 of 3,069 disagree
     *     with their own journey_min by more than 20 minutes.
     */
    private fun buildStopIndex(data: DataLoader.LoadedData) {
        if (data.stopTimes.isEmpty()) return

        for (row in timetable) {
            val serviceId = row["service_id"] ?: continue
            val raw = data.stopTimes[serviceId] ?: continue

            // Keep a literal midnight only when the service's own endpoints say so.
            val midnightIsReal =
                row["departure_time"] == "00:00" || row["arrival_time"] == "00:00"
            val stops = if (midnightIsReal) raw else raw.filterValues { it != 0 }
            if (stops.isEmpty()) continue

            val unwrapped = unwrapMidnight(stops)
            stopIndex[serviceId] = unwrapped
            row["line"]?.let { line ->
                stopTimeLines.add(line)
                val names = stopNamesByLine.getOrPut(line) { mutableMapOf() }
                for (stop in unwrapped.keys) names[stop.lowercase()] = stop
            }
        }
    }

    /**
     * Returns the same stops with times made monotonic along the run.
     *
     * The stop order isn't given by the matrix (columns are alphabetical), so it
     * is recovered from the times themselves: sort them, find the largest gap,
     * and treat that gap as the midnight boundary. Everything before it belongs
     * to the earlier day and gets a day added.
     */
    private fun unwrapMidnight(stops: Map<String, Int>): Map<String, Int> {
        val times = stops.values.distinct().sorted()
        if (times.size < 2) return stops

        var widestGap = times[0] + 1440 - times[times.size - 1]   // the wrap itself
        var splitAfter = times.size - 1
        for (i in 0 until times.size - 1) {
            val gap = times[i + 1] - times[i]
            if (gap > widestGap) {
                widestGap = gap
                splitAfter = i
            }
        }

        // No real wrap: the widest gap is the one across midnight, so every stop
        // shifts by the same day and the relative order is already correct.
        if (splitAfter == times.size - 1) return stops

        val cutoff = times[splitAfter]
        return stops.mapValues { (_, t) -> if (t <= cutoff) t + 1440 else t }
    }

    /**
     * Next departures on [line] from [fromStop] to [toStop], soonest first.
     *
     * Rail uses the real stop-times matrix, so a train appears only if it
     * actually calls at both stations — that is what keeps fast trains out of
     * results for the stations they skip. Metro has no stop times published, so
     * it still uses the interpolated offset tables.
     */
    fun getNextTrains(line: String, fromStop: String, toStop: String,
                      arriveAtMins: Int, n: Int = TRAIN_OPTIONS,
                      windowMin: Int = MAX_WAIT_MIN): List<TrainTiming> =
        if (line in stopTimeLines) {
            nextTrainsFromStopTimes(line, fromStop, toStop, arriveAtMins, n, windowMin)
        } else {
            nextTrainsFromOffsets(line, fromStop, toStop, arriveAtMins, n, windowMin)
        }

    private fun nextTrainsFromStopTimes(line: String, fromStop: String, toStop: String,
                                        arriveAtMins: Int, n: Int,
                                        windowMin: Int): List<TrainTiming> {
        // Deliberately not normalizeStop(): its alias map rewrites Dadar to DADAR
        // and THANE to Thane, which is right for the offset tables but would look
        // up another line's column here and quietly return nothing.
        val from = resolveStop(line, fromStop)
        val to   = resolveStop(line, toStop)
        if (from == null || to == null) {
            // The graph names a stop this line's data doesn't know — rather than
            // show an empty timetable, fall back to the interpolated tables.
            return nextTrainsFromOffsets(line, fromStop, toStop, arriveAtMins, n, windowMin)
        }
        val results = mutableListOf<Pair<Int, TrainTiming>>()

        for (row in timetable) {
            if (row["line"] != line) continue
            val stops = stopIndex[row["service_id"]] ?: continue

            // Absent from the map means this service doesn't call there.
            val dep = stops[from] ?: continue
            val arr = stops[to] ?: continue
            // Both stations served, but in the other order — wrong direction.
            if (arr <= dep) continue

            // Modular wait, so a search at 23:50 still finds the 00:05.
            val wait = ((dep - arriveAtMins) % 1440 + 1440) % 1440
            if (wait > windowMin) continue

            results.add(wait to TrainTiming(
                depFrom    = minsToHhMm(dep),
                arrTo      = minsToHhMm(arr),
                journeyMin = arr - dep,
                trainType  = row["train_type"] ?: "",
                ac         = row["ac"]?.lowercase() in listOf("true", "1", "yes"),
                waitMin    = wait,
                terminus   = row["destination"] ?: "",
                trainNo    = row["train_no"]?.takeIf { it.isNotBlank() }
            ))
        }
        return results.sortedBy { it.first }.take(n).map { it.second }
    }

    /** Matches a stop to the spelling this line's stop maps use, or null. */
    private fun resolveStop(line: String, stop: String): String? =
        stopNamesByLine[line]?.get(stop.trim().lowercase())

    private fun nextTrainsFromOffsets(line: String, fromStop: String, toStop: String,
                                      arriveAtMins: Int, n: Int = TRAIN_OPTIONS,
                                      windowMin: Int = MAX_WAIT_MIN): List<TrainTiming> {
        val from = normalizeStop(fromStop)
        val to   = normalizeStop(toStop)
        val revOffsets = getRevOffsets(line)
        val downOffsets = getDownOffsets(line)
        val results = mutableListOf<Pair<Int, TrainTiming>>()

        for (row in timetable) {
            if (row["line"] != line) continue
            val tt        = row["train_type"] ?: ""
            val direction = row["direction"] ?: "Down"
            val depOrigin = row["dep_mins"]?.toIntOrNull() ?: continue
            val isAc      = row["ac"]?.lowercase() in listOf("true", "1", "yes")
            val journeyMin = row["journey_min"]?.toDoubleOrNull()?.toInt() ?: continue
            val trainDest = row["destination"] ?: ""

            if (direction == "Up" && revOffsets.isNotEmpty()) {
                // For Up direction, use reverse offsets
                val revFrom = revOffsets[from] ?: continue
                val revTo   = revOffsets[to]   ?: continue
                // Train goes from terminus toward origin, so revFrom should be >= revTo
                if (revFrom < revTo) continue

                // Calculate times using offset tables (more accurate than journey_min)
                val segMin = revFrom - revTo
                val arrTerminus = depOrigin + journeyMin
                val depFrom = arrTerminus - revFrom  // When train passes fromStop
                val arrTo   = arrTerminus - revTo   // When train arrives at toStop

                // Filter: ensure train actually serves this segment
                // (depFrom should be after origin and arrTo should be before terminus)
                if (depFrom < arriveAtMins) continue
                if (depFrom - arriveAtMins > windowMin) continue  // outside the asked-for window
                if (depFrom > 1440 || arrTo > 1440) continue  // sanity check

                results.add(depFrom to TrainTiming(minsToHhMm(depFrom), minsToHhMm(arrTo),
                    segMin, tt, isAc, depFrom - arriveAtMins, trainDest))
            } else {
                // For Down direction, use down offsets
                val offFrom = downOffsets[from] ?: continue
                val offTo   = downOffsets[to]   ?: continue

                // For Down direction, from should come before to on the line
                // For Down direction: offsets always increase away from the origin terminus,
                // so toStop must have a higher offset than fromStop.
                val validSeg = offTo >= offFrom
                if (!validSeg) continue

                // Use offset table for segment time (most accurate)
                val segMin = kotlin.math.abs(offTo - offFrom)
                val depFrom = depOrigin + offFrom
                val arrTo   = depOrigin + offTo

                // Filter: ensure train actually serves this segment
                // (offTo should be within the train's journey)
                if (depFrom < arriveAtMins) continue
                if (depFrom - arriveAtMins > windowMin) continue  // outside the asked-for window
                if (arrTo > depOrigin + journeyMin) continue  // segment should fit within journey

                results.add(depFrom to TrainTiming(minsToHhMm(depFrom), minsToHhMm(arrTo),
                    segMin, tt, isAc, depFrom - arriveAtMins, trainDest))
            }
        }
        return results.sortedBy { it.first }.take(n).map { it.second }
    }

    // ── Line chat lookups ─────────────────────────────────────────────────────

    /**
     * Every service on one line running one direction, earliest first.
     *
     * The line chat pins reports to a specific train, and a room only ever
     * covers one line in one direction — offering a commuter on the Up platform
     * a list of Down trains would make the attachment worse than useless.
     */
    fun servicesFor(line: String, direction: String): List<Map<String, String>> =
        timetable
            .filter { it["line"] == line && it["direction"] == direction }
            .sortedBy { it["dep_mins"]?.toIntOrNull() ?: Int.MAX_VALUE }

    /**
     * Stations on one line, in running order.
     *
     * Taken from the longest slow service on the line: a fast train's stop map
     * has gaps, so ordering by the most complete run is what produces a list a
     * commuter can scan down the way the line actually runs.
     */
    fun stationsOn(line: String, direction: String): List<String> {
        val services = timetable.filter { it["line"] == line && it["direction"] == direction }
        val longest = services
            .mapNotNull { stopIndex[it["service_id"]] }
            .maxByOrNull { it.size }
            ?: return emptyList()
        return longest.entries.sortedBy { it.value }.map { it.key }
    }

    companion object {
        /** Beyond this a "next train" isn't useful — it's tomorrow's timetable. */
        const val MAX_WAIT_MIN = 240

        /** How far past the chosen departure time train options are listed. */
        const val SEARCH_WINDOW_MIN = 120

        /** Train options offered per rail leg. */
        const val TRAIN_OPTIONS = 5

        /** 1385 → "11:05 PM". Wraps past midnight, so 1500 → "1:00 AM". */
        fun clock12(totalMins: Int): String {
            val m = ((totalMins % 1440) + 1440) % 1440
            val h24 = m / 60
            val h12 = if (h24 % 12 == 0) 12 else h24 % 12
            return "%d:%02d %s".format(h12, m % 60, if (h24 < 12) "AM" else "PM")
        }
    }

    // Offset tables ported from Python
    private fun getRevOffsets(line: String): Map<String, Int> = when (line) {
        "Central Railway Main" -> CR_REV
        "Western Railway"      -> WR_REV
        "Harbour Line CSMT"    -> HL_REV
        "Trans-Harbour Line"   -> TH_REV
        "Metro Line 1 (Blue)"    -> ML1_REV
        "Metro Line 2A (Yellow)" -> ML2A_REV
        "Metro Line 3 (Aqua)"    -> ML3_REV
        "Metro Line 7 (Red)"     -> ML7_REV
        else -> emptyMap()
    }

    private val downIsNorth = setOf("Harbour Line CSMT", "Trans-Harbour Line")

    private fun getDownOffsets(line: String): Map<String, Int> {
        return when (line) {
            "Central Railway Main" -> CR_SLOW  // Use slow for general calculation
            "Western Railway"      -> WR_SLOW
            "Harbour Line CSMT"    -> HL_DOWN
            "Trans-Harbour Line"   -> TH_DOWN
            "Metro Line 1 (Blue)"    -> ML1_DOWN
            "Metro Line 2A (Yellow)" -> ML2A_DOWN
            "Metro Line 3 (Aqua)"    -> ML3_DOWN
            "Metro Line 7 (Red)"     -> ML7_DOWN
            else -> emptyMap()
        }
    }

    // ── Offset tables (ported from Python) ────────────────────────────────────
    private val CR_SLOW = mapOf(
        "CSMT" to 0,"Masjid" to 3,"Sandhurst Road" to 5,"Byculla" to 7,"Chinchpokli" to 9,
        "Currey Road" to 11,"Parel" to 13,"Dadar" to 14,"Matunga" to 17,"Sion" to 20,
        "Kurla" to 21,"Vidyavihar" to 24,"Ghatkopar" to 25,"Vikhroli" to 28,"Kanjur Marg" to 31,
        "Bhandup" to 33,"Nahur" to 36,"Mulund" to 37,"Thane" to 37,"Kalva" to 42,
        "Mumbra" to 48,"Diva" to 48,"Kopar" to 53,"Dombivli" to 52,"Thakurli" to 56,"Kalyan" to 60,
        "Vithalwadi" to 64,"Ulhas Nagar" to 67,"Ambernath" to 71,"Shahad" to 64,
        "Ambivli" to 67,"Titwala" to 74,"Khadavli" to 81,"Vasind" to 88,"Asangaon" to 96,
        "Atgaon" to 105,"Khardi" to 115,"Kasara" to 133,
        "Badlapur" to 79,"Vangani" to 87,"Shelu" to 91,"Neral" to 95,
        "Bhivpuri Road" to 102,"Karjat" to 112,"Palasdhari" to 117,"Kelavli" to 124,
        "Dolavli" to 127,"Lowjee" to 131,"Khopoli" to 137
    )
    private val CR_FAST = mapOf(
        "CSMT" to 0,"Dadar" to 12,"Kurla" to 18,"Thane" to 34,"Dombivli" to 48,
        "Kalyan" to 56,"Vithalwadi" to 60,"Ulhas Nagar" to 63,"Ambernath" to 67,
        "Shahad" to 70,"Ambivli" to 73,"Titwala" to 77
    )
    private val CR_AC_FAST = mapOf(
        "CSMT" to 0,"Dadar" to 11,"Kurla" to 16,"Thane" to 32,"Dombivli" to 44,
        "Kalyan" to 50,"Vithalwadi" to 54,"Ulhas Nagar" to 57,"Ambernath" to 61,
        "Shahad" to 64,"Ambivli" to 67,"Titwala" to 71
    )
    private val CR_REV = mapOf(
        "CSMT" to 0,"Masjid" to 3,"Sandhurst Road" to 6,"Byculla" to 9,"Currey Road" to 10,
        "Chinchpokli" to 11,"Dadar" to 15,"DADAR" to 15,"Parel" to 15,"Matunga" to 19,"Sion" to 22,
        "Kurla" to 23,"Vidyavihar" to 26,"Ghatkopar" to 27,"Vikhroli" to 31,"Bhandup" to 32,
        "Kanjur Marg" to 34,"Nahur" to 35,"Mulund" to 39,"Thane" to 40,"Kalva" to 45,
        "Diva" to 51,"Mumbra" to 51,"Dombivli" to 55,"Kopar" to 56,"Thakurli" to 58,"Kalyan" to 61,
        "Shahad" to 67,"Vithalwadi" to 67,"Ambivli" to 70,"Ulhas Nagar" to 70,"Ambernath" to 74,
        "Titwala" to 78,"Khadavli" to 85,"Vasind" to 92,"Asangaon" to 100,"Atgaon" to 110,
        "Kasara" to 163,"Kasara/Karjat" to 163,"Karjat" to 112,"Khardi" to 146,
        "Badlapur" to 240,"Vangani" to 248,"Shelu" to 252,"Neral" to 256,
        "Bhivpuri Road" to 263,"Khopoli" to 222,"Lowjee" to 218,
        "Dolavli" to 129,"Kelavli" to 126,"Palasdhari" to 119
    )
    private val WR_SLOW = mapOf(
        "CHURCHGATE" to 0,"Marine Lines" to 3,"Charni Road" to 5,"Grant Road" to 8,
        "Mumbai Central" to 10,"Mahalakshmi" to 13,"Lower Parel" to 16,"Prabhadevi" to 19,
        "DADAR" to 17,"Matunga Road" to 19,"Mahim Jn." to 22,"BANDRA" to 23,
        "Khar Road" to 26,"Santa Cruz" to 28,"Vile Parle" to 31,"ANDHERI" to 34,
        "Jogeshwari" to 37,"Ram Mandir" to 39,"Goregaon" to 41,"Malad" to 45,
        "Kandivli" to 48,"BORIVALI" to 51,"Dahisar" to 55,"Mira Road" to 60,
        "Bhayandar" to 65,"Naigaon" to 71,"Vasai Road" to 76,"Nalla Sopara" to 81,"VIRAR" to 88
    )
    private val WR_FAST = mapOf(
        "CHURCHGATE" to 0,"Mumbai Central" to 8,"DADAR" to 17,"BANDRA" to 26,
        "ANDHERI" to 34,"BORIVALI" to 51,"Vasai Road" to 76,"VIRAR" to 88
    )
    private val WR_REV = mapOf(
        "CHURCHGATE" to 0,"Marine Lines" to 3,"Charni Road" to 5,"Grant Road" to 8,
        "Mumbai Central" to 11,"Mahalakshmi" to 14,"Lower Parel" to 17,"Prabhadevi" to 20,
        "DADAR" to 22,"Dadar" to 22,"Matunga Road" to 24,"Mahim Jn." to 27,
        "BANDRA" to 31,"Bandra" to 31,"Khar Road" to 34,"Santa Cruz" to 36,"Vile Parle" to 39,
        "ANDHERI" to 42,"Andheri" to 42,"Jogeshwari" to 45,"Ram Mandir" to 48,"Goregaon" to 51,"Malad" to 55,
        "Kandivli" to 58,"BORIVALI" to 61,"Borivali" to 61,"Dahisar" to 66,"Mira Road" to 71,
        "Bhayandar" to 76,"Naigaon" to 82,"Vasai Road" to 87,"Nalla Sopara" to 92,"VIRAR" to 99,"Virar" to 99
    )
    private val HL_DOWN = mapOf(
        // Panvel branch (Down = away from CSMT, toward Panvel)
        "Mumbai CSMT" to 0,"Masjid" to 3,"Sandhurst Road" to 6,"Dockyard Road" to 8,
        "Reay Road" to 10,"Cotton Green" to 12,"Sewri" to 15,"Vadala Road" to 18,
        "King's Circle" to 22,"GTB Nagar" to 23,"Mahim Jn" to 25,"Chunabhatti" to 26,
        "Kurla" to 29,"Tilaknagar" to 32,"Chembur" to 34,"Govandi" to 37,
        "Mankhurd" to 40,"Vashi" to 44,"Sanpada" to 47,"Juinagar" to 50,
        "Nerul" to 53,"Seawoods-Darave" to 57,"Seawood Darave" to 57,"Belapur CBD" to 61,
        "Kharghar" to 65,"Mansarovar" to 68,"Khandeshwar" to 71,"Panvel" to 75,
        // Bandra/Andheri branch (Down = away from CSMT, toward Goregaon/Andheri)
        "Bandra" to 29,"Khar Road" to 33,"Santacruz" to 36,"Santa Cruz" to 36,
        "Vileparle" to 39,"Vile Parle" to 39,"Andheri" to 42,"Jogeshwari" to 46,
        "Ramnagar" to 50,"Goregaon" to 54
    )
    private val HL_REV = mapOf(
        "Mumbai CSMT" to 0,"Masjid" to 3,"Sandhurst Road" to 6,"Dockyard Road" to 8,
        "Reay Road" to 10,"Cotton Green" to 12,"Sewri" to 15,"Vadala Road" to 18,
        "King's Circle" to 22,"GTB Nagar" to 23,"Mahim Jn" to 25,"Chunabhatti" to 26,
        "Kurla" to 29,"Tilaknagar" to 32,"Chembur" to 34,"Govandi" to 37,
        "Mankhurd" to 40,"Vashi" to 44,"Sanpada" to 47,"Juinagar" to 50,
        "Nerul" to 53,"Seawoods-Darave" to 57,"Seawood Darave" to 57,"Belapur CBD" to 61,
        "Kharghar" to 65,"Mansarovar" to 68,"Khandeshwar" to 71,"Panvel" to 75,
        "Bandra" to 29,"Khar Road" to 33,"Santacruz" to 36,"Andheri" to 42,"Vileparle" to 39
    )
    private val TH_DOWN = mapOf(
        "THANE" to 0,"Thane" to 0,"Digha Gaon" to 5,"AIROLI" to 8,"RABALE" to 11,"GHANSOLI" to 14,
        "KOPAR KHAIRANE" to 17,"TURBHE" to 21,"Juinagar" to 25,"Sanpada" to 25,
        "Vashi" to 29,"Nerul" to 30,"Seawoods-Darave" to 34,"Seawood Darave" to 34,
        "Belapur CBD" to 38,"Kharghar" to 42,"Mansarovar" to 45,"Khandeshwar" to 48,"Panvel" to 53
    )
    private val TH_REV = mapOf(
        "THANE" to 0,"Thane" to 0,"Digha Gaon" to 5,"AIROLI" to 8,"RABALE" to 11,"GHANSOLI" to 14,
        "KOPAR KHAIRANE" to 17,"TURBHE" to 21,"Juinagar" to 25,"Sanpada" to 25,
        "Vashi" to 28,"Nerul" to 30,"Seawoods-Darave" to 34,"Seawood Darave" to 34,
        "Belapur CBD" to 38,"Kharghar" to 42,"Mansarovar" to 45,"Khandeshwar" to 48,"Panvel" to 53
    )
    private val ML1_DOWN = mapOf(
        "Versova" to 0,"D.N. Nagar" to 2,"Azad Nagar" to 4,"Andheri" to 6,"ANDHERI" to 6,
        "Western Express Highway" to 8,"Chakala (J.B. Nagar)" to 10,"Airport Road" to 12,
        "Marol Naka" to 14,"Saki Naka" to 16,"Asalpha" to 18,"Jagruti Nagar" to 20,"Ghatkopar" to 22
    )
    private val ML1_REV = mapOf(
        "Ghatkopar" to 22,"Jagruti Nagar" to 20,"Asalpha" to 18,"Saki Naka" to 16,
        "Marol Naka" to 14,"Airport Road" to 12,"Chakala (J.B. Nagar)" to 10,
        "Western Express Highway" to 8,"Andheri" to 6,"ANDHERI" to 6,"Azad Nagar" to 4,
        "D.N. Nagar" to 2,"Versova" to 0
    )
    private val ML2A_DOWN = mapOf(
        "Dahisar East" to 0,"Ovaripada" to 2,"Eksar (Shimpoli)" to 4,"Kandarpada" to 6,
        "IC Colony (Mandapeshwar)" to 8,"Borivali West" to 10,"Pahadi Eksar (Bangur Nagar)" to 12,
        "Kandivali West" to 14,"Dahanukarwadi (Kamraj Nagar)" to 16,"Malad West" to 18,
        "Lower Malad" to 20,"Goregaon West (Bangur Nagar)" to 22,"Oshiwara" to 24,
        "Lower Oshiwara" to 26,"Andheri West" to 28,"Lower Juhu" to 30,"D.N. Nagar" to 32
    )
    private val ML2A_REV = mapOf(
        "D.N. Nagar" to 32,"Lower Juhu" to 30,"Andheri West" to 28,"Lower Oshiwara" to 26,
        "Oshiwara" to 24,"Goregaon West (Bangur Nagar)" to 22,"Lower Malad" to 20,"Malad West" to 18,
        "Dahanukarwadi (Kamraj Nagar)" to 16,"Kandivali West" to 14,"Pahadi Eksar (Bangur Nagar)" to 12,
        "Borivali West" to 10,"IC Colony (Mandapeshwar)" to 8,"Kandarpada" to 6,
        "Eksar (Shimpoli)" to 4,"Ovaripada" to 2,"Dahisar East" to 0
    )
    private val ML3_DOWN = mapOf(
        "Aarey JVLR" to 0,"SEEPZ" to 2,"MIDC (Andheri)" to 4,"Marol Naka" to 6,
        "CSIA T2 (Int'l Airport)" to 8,"Sahar Road" to 10,"CSIA T1 (Dom. Airport)" to 12,
        "Santacruz Metro" to 14,"Bandra Colony" to 16,"Bandra Kurla Complex" to 18,
        "Dharavi" to 20,"Shitala Devi Mandir" to 22,"Dadar Metro" to 24,"Siddhivinayak" to 26,
        "Worli" to 28,"Acharya Atre Chowk" to 30,"Nehru Science Centre" to 32,"Mahalaxmi" to 34,
        "Mumbai Central Metro" to 36,"Grant Road Metro" to 38,"Girgaon" to 40,"Kalbadevi" to 42,
        "CSMT Metro" to 44,"Hutatma Chowk" to 46,"Churchgate Metro" to 48,"Vidhan Bhavan" to 50,
        "Cuffe Parade" to 52
    )
    private val ML3_REV = mapOf(
        "Cuffe Parade" to 52,"Vidhan Bhavan" to 50,"Churchgate Metro" to 48,"Hutatma Chowk" to 46,
        "CSMT Metro" to 44,"Kalbadevi" to 42,"Girgaon" to 40,"Grant Road Metro" to 38,
        "Mumbai Central Metro" to 36,"Mahalaxmi" to 34,"Nehru Science Centre" to 32,
        "Acharya Atre Chowk" to 30,"Worli" to 28,"Siddhivinayak" to 26,"Dadar Metro" to 24,
        "Shitala Devi Mandir" to 22,"Dharavi" to 20,"Bandra Kurla Complex" to 18,
        "Bandra Colony" to 16,"Santacruz Metro" to 14,"CSIA T1 (Dom. Airport)" to 12,
        "Sahar Road" to 10,"CSIA T2 (Int'l Airport)" to 8,"Marol Naka" to 6,
        "MIDC (Andheri)" to 4,"SEEPZ" to 2,"Aarey JVLR" to 0
    )
    private val ML7_DOWN = mapOf(
        "Dahisar East" to 0,"Ovaripada" to 2,"Rashtriya Udyan (National Park)" to 4,
        "Devipada" to 6,"Magathane" to 8,"Poisar" to 10,"Akurli" to 12,"Kurar" to 14,
        "Dindoshi" to 16,"Aarey (JVLR)" to 18,"Goregaon East" to 21,
        "Jogeshwari East (JVLR)" to 24,"Gundavali (Andheri East)" to 27,"Gundavali" to 27
    )
    private val ML7_REV = mapOf(
        "Gundavali (Andheri East)" to 27,"Gundavali" to 27,"Jogeshwari East (JVLR)" to 24,
        "Goregaon East" to 21,"Aarey (JVLR)" to 18,"Dindoshi" to 16,"Kurar" to 14,
        "Akurli" to 12,"Poisar" to 10,"Magathane" to 8,"Devipada" to 6,
        "Rashtriya Udyan (National Park)" to 4,"Ovaripada" to 2,"Dahisar East" to 0
    )
}

fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}
fun Double.roundToInt() = kotlin.math.round(this).toInt()
