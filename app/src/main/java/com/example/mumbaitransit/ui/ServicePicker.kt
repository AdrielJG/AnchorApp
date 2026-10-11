package com.example.mumbaitransit.ui

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.model.TrainTiming
import java.util.Calendar

/**
 * "Which train did you board?" — the one picker both live sharing and live
 * navigation use to lock in or correct a train/metro on a leg.
 *
 * Options come from the route's timetable, or — mid-journey, when that
 * timetable has usually gone stale — straight from the engine off the current
 * clock, with a short look-back to catch the service already at the platform.
 * Train numbers are never shown; a service is named by its time and heading.
 */
object ServicePicker {

    fun show(
        activity: AppCompatActivity,
        vm: TransitViewModel,
        line: String,
        lineShort: String,
        fromStop: String,
        toStop: String,
        isMetro: Boolean,
        timetable: List<TimetableLeg>,
        preferLive: Boolean,
        onTrain: (TrainTiming) -> Unit
    ) {
        val noun = if (isMetro) "metro" else "train"
        val fromTimetable = timetable.firstOrNull { it.fromStop.equals(fromStop, true) }?.trains
        val trains = if (preferLive) liveOptions(vm, line, fromStop, toStop).ifEmpty { fromTimetable.orEmpty() }
                     else fromTimetable ?: liveOptions(vm, line, fromStop, toStop)

        if (trains.isEmpty()) {
            Toast.makeText(activity, "No $noun timings for this leg", Toast.LENGTH_SHORT).show()
            return
        }

        val options = trains.map { t ->
            OptionPickerSheet.Option(
                key = "${t.depFrom}|${t.arrTo}",
                title = "${t.depFrom}  →  ${t.arrTo}",
                subtitle = buildString {
                    t.terminus.takeIf { it.isNotBlank() }?.let { append("towards $it") }
                    if (t.trainType.isNotBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(t.trainType)
                    }
                    if (t.ac && !t.trainType.contains("AC", true)) {
                        if (isNotEmpty()) append("  ·  ")
                        append("AC")
                    }
                },
                tag = "${t.journeyMin}m"
            )
        }

        OptionPickerSheet().apply {
            sheetTitle = "Which $noun did you board?"
            sheetSubtitle = "$lineShort · $fromStop → $toStop"
            this.options = options
            this.onPicked = { picked ->
                trains.firstOrNull { "${it.depFrom}|${it.arrTo}" == picked.key }?.let(onTrain)
            }
        }.show(activity.supportFragmentManager, OptionPickerSheet.TAG)
    }

    private fun liveOptions(vm: TransitViewModel, line: String, from: String, to: String): List<TrainTiming> {
        if (!vm.isEngineReady) return emptyList()
        val now = Calendar.getInstance()
        val mins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return vm.engine.getNextTrains(line, from, to, (mins - 10 + 1440) % 1440)
    }
}
