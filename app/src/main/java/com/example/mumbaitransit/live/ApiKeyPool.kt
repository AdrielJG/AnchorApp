package com.example.mumbaitransit.live

import android.content.Context
import android.content.SharedPreferences
import com.example.mumbaitransit.BuildConfig
import java.util.Calendar

/**
 * Holds the RailRadar keys and decides which one to spend.
 *
 * Each key is capped at 1,000 requests a month, so the pool:
 *  - counts every request it hands out, per key, per calendar month;
 *  - retires a key for the rest of the month the moment the API reports it
 *    exhausted (or the local count reaches the cap);
 *  - moves to the next key and remembers that choice across app launches, so a
 *    burnt key is never retried on every cold start.
 *
 * Counters reset automatically when the month rolls over.
 */
class ApiKeyPool(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Keys come from local.properties via BuildConfig, with the known set as fallback. */
    private val keys: List<String> = BuildConfig.RAILRADAR_KEYS
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val size: Int get() = keys.size

    init {
        rollMonthIfNeeded()
    }

    /** Whether any key still has headroom this month. */
    fun hasUsableKey(): Boolean = keys.indices.any { isUsable(it) }

    /**
     * The key to try next, or null when every key is spent for the month.
     * Does not consume anything — call [recordUse] once the request is sent.
     */
    fun currentKey(): KeyHandle? {
        rollMonthIfNeeded()
        val start = prefs.getInt(KEY_ACTIVE, 0).coerceIn(0, (keys.size - 1).coerceAtLeast(0))
        for (offset in keys.indices) {
            val idx = (start + offset) % keys.size
            if (isUsable(idx)) {
                if (idx != start) prefs.edit().putInt(KEY_ACTIVE, idx).apply()
                return KeyHandle(idx, keys[idx])
            }
        }
        return null
    }

    /** Counts one request against [handle]. */
    fun recordUse(handle: KeyHandle) {
        val used = usedBy(handle.index) + 1
        prefs.edit().putInt(usageKey(handle.index), used).apply()
        if (used >= MONTHLY_CAP) retire(handle)
    }

    /**
     * Retires a key for the remainder of the month and advances the pool.
     * Called when the API answers with a quota or auth failure.
     */
    fun retire(handle: KeyHandle) {
        prefs.edit()
            .putInt(usageKey(handle.index), MONTHLY_CAP)
            .putInt(KEY_ACTIVE, (handle.index + 1) % keys.size.coerceAtLeast(1))
            .apply()
    }

    /** Requests spent this month across every key. */
    fun usedThisMonth(): Int = keys.indices.sumOf { usedBy(it).coerceAtMost(MONTHLY_CAP) }

    /** Total monthly allowance across the pool. */
    fun monthlyBudget(): Int = keys.size * MONTHLY_CAP

    /** Keys that still have requests left. */
    fun keysRemaining(): Int = keys.indices.count { isUsable(it) }

    private fun isUsable(index: Int): Boolean = usedBy(index) < MONTHLY_CAP

    private fun usedBy(index: Int): Int = prefs.getInt(usageKey(index), 0)

    private fun usageKey(index: Int) = "used_$index"

    /** Wipes counters when the calendar month changes. */
    private fun rollMonthIfNeeded() {
        val now = Calendar.getInstance()
        val stamp = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1)
        if (prefs.getInt(KEY_MONTH, -1) != stamp) {
            val editor = prefs.edit().putInt(KEY_MONTH, stamp).putInt(KEY_ACTIVE, 0)
            keys.indices.forEach { editor.remove(usageKey(it)) }
            editor.apply()
        }
    }

    data class KeyHandle(val index: Int, val value: String)

    companion object {
        private const val PREFS = "railradar_keys"
        private const val KEY_ACTIVE = "active_index"
        private const val KEY_MONTH = "month_stamp"
        const val MONTHLY_CAP = 1000
    }
}
