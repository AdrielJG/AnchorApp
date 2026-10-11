package com.example.mumbaitransit.chat

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The service day a chat room is currently on.
 *
 * Line chat is disposable by nature: a delay report from Tuesday morning is
 * noise by Tuesday evening and actively misleading by Wednesday. Rather than
 * archiving anything, every room is scoped to one day — yesterday's messages
 * are simply not part of today's room.
 *
 * The day turns over at [RESET_HOUR] rather than midnight because Mumbai
 * suburban services run past 01:00 and restart around 04:00. Cutting at 03:00
 * puts the reset in the one window where nobody is on a platform, so a
 * conversation about the 00:40 Kalyan slow never gets split in half.
 */
object ChatDay {

    /** Hour (local) at which rooms roll over to a fresh day. */
    const val RESET_HOUR = 3

    private val ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun calendar(at: Long): Calendar =
        Calendar.getInstance(ZONE, Locale.ENGLISH).apply { timeInMillis = at }

    /**
     * Stable key for the day [at] falls in, e.g. "2026-09-04".
     *
     * This is the Firebase path segment, so it must not change format once
     * messages exist under it.
     */
    fun key(at: Long = System.currentTimeMillis()): String {
        val c = calendar(at)
        // Before the reset hour we are still on yesterday's service day.
        if (c.get(Calendar.HOUR_OF_DAY) < RESET_HOUR) c.add(Calendar.DAY_OF_YEAR, -1)
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Millisecond at which the current service day began. */
    fun startOfDay(at: Long = System.currentTimeMillis()): Long {
        val c = calendar(at)
        if (c.get(Calendar.HOUR_OF_DAY) < RESET_HOUR) c.add(Calendar.DAY_OF_YEAR, -1)
        c.set(Calendar.HOUR_OF_DAY, RESET_HOUR)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** Key for a day [daysAgo] before the current one, used when pruning. */
    fun keyDaysAgo(daysAgo: Int, at: Long = System.currentTimeMillis()): String =
        key(at - daysAgo * 24L * 60L * 60L * 1000L)

    /** Human line for the room header, e.g. "Today's chat · clears at 3:00 AM". */
    fun resetNotice(): String {
        val h = if (RESET_HOUR % 12 == 0) 12 else RESET_HOUR % 12
        val suffix = if (RESET_HOUR < 12) "AM" else "PM"
        return "Today's chat · clears daily at $h:00 $suffix"
    }
}
