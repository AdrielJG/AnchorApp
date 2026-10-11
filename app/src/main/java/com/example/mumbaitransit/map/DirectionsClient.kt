package com.example.mumbaitransit.map

import com.example.mumbaitransit.BuildConfig
import com.example.mumbaitransit.navigation.WalkStep
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Road geometry from the Google Directions API.
 *
 * The bundled CSVs describe rail, metro and bus corridors, so those legs can be
 * drawn from the stops they call at. An auto or a cab follows the road network,
 * which the app carries no data for — without this the only honest thing to
 * draw was a straight line, which is why the map button used to be hidden on
 * those two cards.
 *
 * Shares the Maps key already in local.properties. That key must have the
 * **Directions API** enabled alongside Maps SDK for Android, and the request is
 * billed per call; a failure here is not fatal, the caller falls back to the
 * straight line and says so.
 */
object DirectionsClient {

    private const val BASE = "https://maps.googleapis.com/maps/api/directions/json"
    private const val TIMEOUT_MS = 12_000

    sealed class Result {
        /** [points] is the decoded road path; [distanceKm] and [minutes] come from the leg. */
        data class Success(
            val points: List<LatLng>,
            val distanceKm: Double,
            val minutes: Int,
            val summary: String
        ) : Result()

        data class Failure(val reason: String) : Result()
    }

    /**
     * A driving route between two points.
     *
     * [mode] is always driving — an auto and a cab use the same road network and
     * the API has no auto-rickshaw mode, so the two cards differ in fare and
     * colour rather than in geometry.
     */
    suspend fun drivingRoute(
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double
    ): Result = withContext(Dispatchers.IO) {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank()) {
            return@withContext Result.Failure("No Maps API key configured")
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "$BASE?origin=${enc("$originLat,$originLon")}" +
                    "&destination=${enc("$destLat,$destLon")}" +
                    "&mode=driving&departure_time=now&region=in&key=${enc(key)}"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            if (conn.responseCode !in 200..299) {
                return@withContext Result.Failure("Directions request failed (${conn.responseCode})")
            }

            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JsonParser.parseString(body).asJsonObject

            when (val status = json.get("status")?.asString.orEmpty()) {
                "OK" -> Unit
                "ZERO_RESULTS" -> return@withContext Result.Failure("No road route found")
                "REQUEST_DENIED" -> return@withContext Result.Failure(
                    "Directions API not enabled for this key"
                )
                "OVER_QUERY_LIMIT" -> return@withContext Result.Failure("Directions quota exhausted")
                else -> return@withContext Result.Failure("Directions error: $status")
            }

            val route = json.getAsJsonArray("routes")?.takeIf { it.size() > 0 }
                ?.get(0)?.asJsonObject
                ?: return@withContext Result.Failure("No road route found")

            val encoded = route.getAsJsonObject("overview_polyline")
                ?.get("points")?.asString
                ?: return@withContext Result.Failure("Route carried no geometry")

            val leg = route.getAsJsonArray("legs")?.takeIf { it.size() > 0 }
                ?.get(0)?.asJsonObject

            // duration_in_traffic is present only when departure_time was sent
            // and the key is billing-enabled; fall back to the static duration.
            val seconds = leg?.getAsJsonObject("duration_in_traffic")?.get("value")?.asInt
                ?: leg?.getAsJsonObject("duration")?.get("value")?.asInt
                ?: 0
            val metres = leg?.getAsJsonObject("distance")?.get("value")?.asInt ?: 0

            Result.Success(
                points = decodePolyline(encoded),
                distanceKm = metres / 1000.0,
                minutes = (seconds / 60.0).toInt(),
                summary = route.get("summary")?.asString.orEmpty()
            )
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Could not reach Directions")
        } finally {
            conn?.disconnect()
        }
    }

    /** A walked path with its turn-by-turn steps, for live navigation. */
    sealed class WalkResult {
        data class Success(
            val points: List<LatLng>,
            val steps: List<WalkStep>,
            val distanceM: Int,
            val minutes: Int
        ) : WalkResult()
        data class Failure(val reason: String) : WalkResult()
    }

    /**
     * A walking route between two points, with its steps.
     *
     * Same endpoint and key as [drivingRoute], mode=walking. Used once per walk
     * leg by live navigation; on failure the navigation screen falls back to a
     * compass heading and a straight-line distance, and labels it as such.
     */
    suspend fun walkingRoute(
        originLat: Double, originLon: Double,
        destLat: Double, destLon: Double
    ): WalkResult = withContext(Dispatchers.IO) {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank()) return@withContext WalkResult.Failure("No Maps API key configured")

        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "$BASE?origin=${enc("$originLat,$originLon")}" +
                    "&destination=${enc("$destLat,$destLon")}" +
                    "&mode=walking&region=in&key=${enc(key)}"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            if (conn.responseCode !in 200..299) {
                return@withContext WalkResult.Failure("Directions request failed (${conn.responseCode})")
            }
            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JsonParser.parseString(body).asJsonObject
            val status = json.get("status")?.asString.orEmpty()
            if (status != "OK") return@withContext WalkResult.Failure("Directions error: $status")

            val route = json.getAsJsonArray("routes")?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject
                ?: return@withContext WalkResult.Failure("No walking route found")
            val leg = route.getAsJsonArray("legs")?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject
                ?: return@withContext WalkResult.Failure("No walking route found")

            val steps = leg.getAsJsonArray("steps")?.mapNotNull { el ->
                val o = el.asJsonObject
                val end = o.getAsJsonObject("end_location") ?: return@mapNotNull null
                WalkStep(
                    instruction = stripHtml(o.get("html_instructions")?.asString.orEmpty()),
                    maneuver = o.get("maneuver")?.asString.orEmpty(),
                    distanceM = o.getAsJsonObject("distance")?.get("value")?.asInt ?: 0,
                    endLat = end.get("lat").asDouble,
                    endLon = end.get("lng").asDouble
                )
            }.orEmpty()

            val encoded = route.getAsJsonObject("overview_polyline")?.get("points")?.asString.orEmpty()
            WalkResult.Success(
                points = if (encoded.isNotBlank()) decodePolyline(encoded) else emptyList(),
                steps = steps,
                distanceM = leg.getAsJsonObject("distance")?.get("value")?.asInt ?: 0,
                minutes = ((leg.getAsJsonObject("duration")?.get("value")?.asInt ?: 0) / 60.0).toInt()
            )
        } catch (e: Exception) {
            WalkResult.Failure(e.message ?: "Could not reach Directions")
        } finally {
            conn?.disconnect()
        }
    }

    /** "Turn <b>left</b> onto <b>Palm Beach Rd</b><div>Destination on the right</div>" -> plain text. */
    private fun stripHtml(html: String): String =
        html.replace(Regex("<div[^>]*>"), " · ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /**
     * Decodes Google's encoded polyline format into coordinates.
     *
     * Values are stored as signed offsets from the previous point, each split
     * into five-bit chunks with a continuation flag, so every point has to be
     * rebuilt by accumulating onto the one before it.
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return points
    }
}
