package com.example.mumbaitransit.live

import android.content.Context

/**
 * Short-lived in-memory cache of live responses.
 *
 * Live positions only move every few tens of seconds, and every request costs
 * quota, so repeated opens of the same train inside the window are served from here.
 */
object LiveStatusCache {

    private const val TTL_MS = 30_000L

    private val entries = mutableMapOf<String, Entry>()

    private data class Entry(val status: LiveTrainStatus, val at: Long)

    @Synchronized
    fun get(trainNumber: String): LiveTrainStatus? {
        val e = entries[trainNumber] ?: return null
        if (System.currentTimeMillis() - e.at > TTL_MS) {
            entries.remove(trainNumber)
            return null
        }
        return e.status
    }

    @Synchronized
    fun put(trainNumber: String, status: LiveTrainStatus) {
        entries[trainNumber] = Entry(status, System.currentTimeMillis())
    }

    /** Age of the cached copy in millis, or null when nothing is held. */
    @Synchronized
    fun ageOf(trainNumber: String): Long? =
        entries[trainNumber]?.let { System.currentTimeMillis() - it.at }
}

/**
 * Fallback store for train numbers the timetable can't supply.
 *
 * Rail services now carry a real `train_no` from the timetable, so this is
 * rarely reached. It still covers the gaps: a service with a blank number, or a
 * corrected number the user enters after RailRadar rejects the one on file.
 * Numbers are keyed to a leg signature (line, stops, departure time), so a
 * correction sticks to that service and no other.
 */
class TrainNumberStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("train_numbers", Context.MODE_PRIVATE)

    /** Stable identifier for a leg, independent of how it was rendered. */
    fun signature(line: String, from: String, to: String, departure: String): String =
        listOf(line, from, to, departure)
            .joinToString("|") { it.trim().lowercase() }

    fun lookup(signature: String): String? =
        prefs.getString("leg_$signature", null)?.takeIf { it.isNotBlank() }

    fun remember(signature: String, trainNumber: String) {
        val recents = (listOf(trainNumber) + recents()).distinct().take(MAX_RECENTS)
        prefs.edit()
            .putString("leg_$signature", trainNumber)
            .putString(KEY_RECENTS, recents.joinToString(","))
            .apply()
    }

    fun forget(signature: String) {
        prefs.edit().remove("leg_$signature").apply()
    }

    /** Recently tracked numbers, newest first — offered as one-tap chips. */
    fun recents(): List<String> =
        prefs.getString(KEY_RECENTS, "")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    private companion object {
        const val KEY_RECENTS = "recents"
        const val MAX_RECENTS = 8
    }
}
