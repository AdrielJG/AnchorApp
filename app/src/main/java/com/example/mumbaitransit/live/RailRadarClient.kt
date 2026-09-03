package com.example.mumbaitransit.live

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Talks to RailRadar's live-status endpoint.
 *
 * Two things this class is deliberately careful about:
 *
 * 1. **Key failover.** Each key allows 1,000 requests a month. On a quota or auth
 *    failure the key is retired for the month and the next one is tried inside the
 *    same call, so the user never sees a failure that a spare key could have covered.
 *
 * 2. **Loose parsing.** Field names are read from a list of candidates rather than
 *    one fixed spelling, and anything missing simply comes back null. A payload
 *    that changes shape degrades the screen instead of crashing it.
 */
class RailRadarClient(context: Context) {

    private val pool = ApiKeyPool(context)

    val quotaUsed: Int get() = pool.usedThisMonth()
    val quotaTotal: Int get() = pool.monthlyBudget()
    val keysRemaining: Int get() = pool.keysRemaining()

    /**
     * Fetches live status for [trainNumber], serving a recent cached copy when one
     * exists so that back-navigation and rotation don't spend requests.
     */
    suspend fun fetchLive(trainNumber: String, forceRefresh: Boolean = false): LiveResult {
        val cached = if (forceRefresh) null else LiveStatusCache.get(trainNumber)
        if (cached != null) return LiveResult.Success(cached, fromCache = true)

        return withContext<LiveResult>(Dispatchers.IO) {
            var lastError: LiveResult.Error? = null

            // One attempt per key at most, so a dead pool fails fast.
            repeat(pool.size.coerceAtLeast(1)) {
                val handle = pool.currentKey() ?: return@withContext LiveResult.Error(
                    if (pool.size == 0) {
                        "No RailRadar key is configured. Add RAILRADAR_KEYS to local.properties."
                    } else {
                        "All ${pool.size} RailRadar keys have spent their 1,000 requests for " +
                            "this month. Live tracking resumes when the quota resets."
                    },
                    retryable = false
                )

                when (val attempt = request(trainNumber, handle)) {
                    is Attempt.Ok -> {
                        pool.recordUse(handle)
                        val status = parse(trainNumber, attempt.body)
                            ?: return@withContext LiveResult.Error(
                                "RailRadar answered, but the response didn't contain a route for " +
                                    "train $trainNumber. Check the number and try again."
                            )
                        LiveStatusCache.put(trainNumber, status)
                        return@withContext LiveResult.Success(status)
                    }

                    // Key is spent or rejected — burn it and loop to the next one.
                    is Attempt.KeyDead -> {
                        pool.retire(handle)
                        lastError = LiveResult.Error(attempt.message)
                    }

                    is Attempt.Failed -> {
                        pool.recordUse(handle)
                        return@withContext LiveResult.Error(attempt.message, attempt.retryable)
                    }
                }
            }

            lastError ?: LiveResult.Error("Couldn't reach RailRadar. Check your connection.")
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private sealed class Attempt {
        data class Ok(val body: String) : Attempt()
        data class KeyDead(val message: String) : Attempt()
        data class Failed(val message: String, val retryable: Boolean = true) : Attempt()
    }

    private fun request(trainNumber: String, handle: ApiKeyPool.KeyHandle): Attempt {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL/v1/trains/$trainNumber/live")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ${handle.value}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12_000
                readTimeout = 12_000
            }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            when {
                code == 401 || code == 403 ->
                    Attempt.KeyDead("RailRadar rejected that key.")

                code == 429 ->
                    Attempt.KeyDead("That key hit its request limit.")

                code == 404 -> Attempt.Failed(
                    "RailRadar has no train $trainNumber. Check the number — Mumbai locals are " +
                        "five digits, usually starting with 9.",
                    retryable = false
                )

                code in 500..599 ->
                    Attempt.Failed("RailRadar is having trouble right now. Try again in a moment.")

                code !in 200..299 ->
                    Attempt.Failed("RailRadar returned an error ($code).")

                body.isBlank() ->
                    Attempt.Failed("RailRadar returned an empty response.")

                else -> {
                    // A 200 can still carry success:false with a quota message inside.
                    val json = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
                    val ok = json?.get("success")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                    if (ok) {
                        Attempt.Ok(body)
                    } else {
                        val err = json.str("error", "message").orEmpty()
                        if (looksLikeQuotaOrAuth(err)) {
                            Attempt.KeyDead(err.ifBlank { "That key was refused." })
                        } else {
                            Attempt.Failed(err.ifBlank { "RailRadar couldn't return that train." })
                        }
                    }
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Attempt.Failed("No internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            Attempt.Failed("RailRadar took too long to answer. Try again.")
        } catch (e: Exception) {
            Attempt.Failed("Couldn't reach RailRadar (${e.javaClass.simpleName}).")
        } finally {
            conn?.disconnect()
        }
    }

    private fun looksLikeQuotaOrAuth(message: String): Boolean {
        val m = message.lowercase()
        return listOf("quota", "limit", "exceed", "unauthor", "invalid key", "invalid api",
            "forbidden", "expired", "subscription", "credit").any { it in m }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parse(trainNumber: String, body: String): LiveTrainStatus? {
        val root = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull() ?: return null
        val data = root.obj("data") ?: root

        val routeArray = data.arr("route", "stations", "stops", "schedule")
        val stops = routeArray?.mapNotNull { parseStop(it) }.orEmpty()
        if (stops.isEmpty() && data.obj("currentLocation", "current_location") == null) return null

        val current = data.obj("currentLocation", "current_location")?.let { cl ->
            CurrentPosition(
                stationCode = cl.str("stationCode", "station_code", "code"),
                stationName = cl.str("stationName", "station_name", "name"),
                status = cl.str("status", "state"),
                isActualPosition = cl.bool("isActualPosition", "is_actual_position", "actual"),
                delayMinutes = cl.int("delayMinutes", "delay_minutes", "delay")
            )
        }

        val source = data.obj("source", "sourceStation", "from")
        val dest = data.obj("destination", "destinationStation", "to")

        return LiveTrainStatus(
            trainNumber = data.str("trainNumber", "trainNo", "number") ?: trainNumber,
            trainName = data.str("trainName", "name", "train_name"),
            sourceCode = source.str("code", "stationCode")
                ?: data.str("sourceCode", "sourceStationCode"),
            sourceName = source.str("name", "stationName")
                ?: data.str("sourceName", "sourceStationName"),
            destCode = dest.str("code", "stationCode")
                ?: data.str("destinationCode", "destinationStationCode"),
            destName = dest.str("name", "stationName")
                ?: data.str("destinationName", "destinationStationName"),
            isLive = data.bool("isLive", "is_live", "live") ?: false,
            trackingMode = data.str("trackingMode", "tracking_mode", "mode"),
            statusText = data.str("status", "runningStatus"),
            lastUpdatedAt = data.epoch("lastUpdatedAt", "last_updated_at", "updatedAt"),
            current = current,
            previousHalt = parseHalt(data.get("previousHalt") ?: data.get("previous_halt")),
            nextHalt = parseHalt(data.get("nextHalt") ?: data.get("next_halt")),
            route = stops
        )
    }

    /** Halts arrive either as an object or as a bare station code string. */
    private fun parseHalt(element: JsonElement?): Halt? = when {
        element == null || element.isJsonNull -> null
        element.isJsonPrimitive -> Halt(element.asString, null, null, null)
        element.isJsonObject -> {
            val o = element.asJsonObject
            val nested = o.obj("station")
            Halt(
                stationCode = o.str("stationCode", "station_code", "code")
                    ?: nested.str("code", "stationCode"),
                stationName = o.str("stationName", "station_name", "name")
                    ?: nested.str("name", "stationName"),
                distanceKm = o.dbl("distance", "distanceKm", "distance_km", "distanceFromCurrent"),
                scheduledTime = o.str("scheduledArrival", "scheduled_arrival", "arrival", "time")
                    ?.let(::formatTime)
            )
        }
        else -> null
    }

    private fun parseStop(element: JsonElement): RouteStop? {
        if (!element.isJsonObject) return null
        val o = element.asJsonObject
        val nested = o.obj("station")

        val code = o.str("stationCode", "station_code", "code")
            ?: nested.str("code", "stationCode") ?: return null
        val name = o.str("stationName", "station_name", "name")
            ?: nested.str("name", "stationName") ?: code

        return RouteStop(
            sequence = o.int("sequence", "stopNumber", "serialNo", "seq") ?: 0,
            stationCode = code,
            stationName = name,
            status = toStopStatus(o.str("status", "state")),
            scheduledArrival = o.str("scheduledArrival", "scheduled_arrival", "schArrival",
                "arrivalTime", "sta")?.let(::formatTime),
            scheduledDeparture = o.str("scheduledDeparture", "scheduled_departure", "schDeparture",
                "departureTime", "std")?.let(::formatTime),
            actualArrival = o.str("actualArrival", "actual_arrival", "expectedArrival", "eta")
                ?.let(::formatTime),
            actualDeparture = o.str("actualDeparture", "actual_departure", "expectedDeparture", "etd")
                ?.let(::formatTime),
            delayArrival = o.int("delayArrival", "delay_arrival", "arrivalDelay"),
            delayDeparture = o.int("delayDeparture", "delay_departure", "departureDelay"),
            platform = o.str("platform", "platformNumber", "pf"),
            distanceKm = o.dbl("distance", "distanceKm", "distance_km", "distanceFromSource")
        )
    }

    private fun toStopStatus(raw: String?): StopStatus {
        val s = raw?.lowercase()?.replace('_', ' ') ?: return StopStatus.UNKNOWN
        return when {
            "depart" in s || "cross" in s || "pass" in s || "done" in s || "complet" in s ->
                StopStatus.DEPARTED
            "current" in s || "at station" in s || "arriv" in s || "halt" in s ->
                StopStatus.CURRENT
            "upcoming" in s || "pending" in s || "expect" in s || "future" in s ->
                StopStatus.UPCOMING
            "skip" in s || "cancel" in s -> StopStatus.SKIPPED
            else -> StopStatus.UNKNOWN
        }
    }

    companion object {
        private const val BASE_URL = "https://api.railradar.in"

        private val ISO_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )

        /** IST — every station on this network is in one zone, so this is safe. */
        private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

        /**
         * Normalises whatever the API sends into a "01:04 AM" display string.
         * Accepts ISO-8601, epoch millis/seconds, and plain "HH:mm".
         */
        fun formatTime(raw: String?): String? {
            if (raw.isNullOrBlank() || raw == "null") return null
            val trimmed = raw.trim()

            // Already a clock time.
            Regex("^(\\d{1,2}):(\\d{2})").find(trimmed)?.let { m ->
                if (!trimmed.contains('-') && !trimmed.contains('T')) {
                    val h = m.groupValues[1].toInt()
                    val min = m.groupValues[2]
                    return render(h, min)
                }
            }

            trimmed.toLongOrNull()?.let { return render(if (it > 1e11) it else it * 1000) }

            for (pattern in ISO_FORMATS) {
                runCatching {
                    val fmt = SimpleDateFormat(pattern, Locale.US).apply { timeZone = IST }
                    return render(fmt.parse(trimmed)!!.time)
                }
            }
            return trimmed
        }

        private fun render(millis: Long): String =
            SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = IST }.format(Date(millis))

        private fun render(hour24: Int, minute: String): String {
            val suffix = if (hour24 < 12) "AM" else "PM"
            val h = when {
                hour24 % 12 == 0 -> 12
                else -> hour24 % 12
            }
            return "%02d:%s %s".format(h, minute, suffix)
        }
    }
}

// ── Gson helpers ──────────────────────────────────────────────────────────────
// Each reads the first key that is present and non-null, so the parser survives
// a rename on RailRadar's side without a code change.

private fun JsonObject?.pick(vararg keys: String): JsonElement? {
    val obj = this ?: return null
    for (k in keys) {
        val v = obj.get(k)
        if (v != null && !v.isJsonNull) return v
    }
    return null
}

internal fun JsonObject?.str(vararg keys: String): String? =
    pick(*keys)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() && it != "null" }

internal fun JsonObject?.int(vararg keys: String): Int? =
    pick(*keys)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }

internal fun JsonObject?.dbl(vararg keys: String): Double? =
    pick(*keys)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asDouble }.getOrNull() }

internal fun JsonObject?.bool(vararg keys: String): Boolean? =
    pick(*keys)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asBoolean }.getOrNull() }

internal fun JsonObject?.obj(vararg keys: String): JsonObject? =
    pick(*keys)?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonObject?.arr(vararg keys: String): JsonArray? =
    pick(*keys)?.takeIf { it.isJsonArray }?.asJsonArray

/** Epoch millis from either a numeric timestamp or an ISO string. */
internal fun JsonObject?.epoch(vararg keys: String): Long? {
    val el = pick(*keys)?.takeIf { it.isJsonPrimitive } ?: return null
    runCatching { el.asLong }.getOrNull()?.let { return if (it > 1e11) it else it * 1000 }
    val raw = runCatching { el.asString }.getOrNull() ?: return null
    for (pattern in listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"
    )) {
        runCatching {
            val fmt = SimpleDateFormat(pattern, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
            return fmt.parse(raw)!!.time
        }
    }
    return null
}
