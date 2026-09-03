package com.example.mumbaitransit.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.example.mumbaitransit.chat.Attachment
import com.example.mumbaitransit.chat.ChatRoom
import com.example.mumbaitransit.chat.QuickReport
import com.example.mumbaitransit.chat.RoomTimetable
import com.example.mumbaitransit.chat.TrainOption
import com.example.mumbaitransit.databinding.SheetReportComposerBinding
import com.example.mumbaitransit.engine.TransitEngine
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Pins a one-tap report to the thing it is about.
 *
 * "Delay" on its own tells nobody anything; "delay on the 09:14 Kalyan fast at
 * Ghatkopar" is actionable. Each [QuickReport] declares what it requires, and
 * the post button stays disabled until those are filled — which is also why the
 * sheet never has to validate anything by hand.
 */
class ReportComposerSheet : BottomSheetDialogFragment() {

    data class Result(
        val report: QuickReport,
        val note: String,
        val trainNo: String?,
        val trainLabel: String?,
        val station: String?,
        val platform: String?
    )

    private var _binding: SheetReportComposerBinding? = null
    private val binding get() = _binding!!

    lateinit var report: QuickReport
    lateinit var room: ChatRoom
    var engine: TransitEngine? = null
    var onPost: ((Result) -> Unit)? = null

    private var train: TrainOption? = null
    private var station: String? = null
    private var platform: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = SheetReportComposerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, saved: Bundle?) {
        val tint = Color.parseColor(report.tint)
        binding.tvComposerTitle.text = "${report.emoji}  ${report.label}"
        binding.tvComposerTitle.setTextColor(tint)
        binding.tvComposerRoom.text = "${room.shortName} · ${room.direction.label} — ${room.towards}"
        binding.btnPostReport.setBackgroundColor(tint)

        setupTrainRow(tint)
        setupStationRow()
        setupPlatformRow()

        binding.btnPostReport.setOnClickListener {
            onPost?.invoke(
                Result(
                    report = report,
                    note = binding.etComposerNote.text?.toString().orEmpty(),
                    trainNo = train?.trainNo,
                    trainLabel = train?.label,
                    station = station,
                    platform = platform
                )
            )
            dismiss()
        }
        refreshPostButton()
    }

    // ── Attachment rows ───────────────────────────────────────────────────────

    private fun setupTrainRow(tint: Int) {
        if (Attachment.TRAIN !in report.uses) return
        binding.rowTrain.visibility = View.VISIBLE
        binding.tvTrainLabel.text = labelFor(Attachment.TRAIN, "TRAIN")
        binding.tvTrainValue.text = "Tap to choose a train"
        binding.tvTrainValue.setTextColor(Color.parseColor("#9E9B93"))

        binding.rowTrain.setOnClickListener {
            val e = engine
            if (e == null) {
                binding.tvTrainValue.text = "Timetable still loading — try again in a moment"
                return@setOnClickListener
            }
            // Already filtered to this room's line and direction, so a Central
            // Up report can only ever name a Central Up service.
            val trains = RoomTimetable.trainsFromNow(e, room)
            openPicker(
                title = "Choose a train",
                subtitle = "${room.shortName} · ${room.direction.label} — ${trains.size} services",
                options = trains.map {
                    OptionPickerSheet.Option(
                        key = it.trainNo,
                        title = it.depTime,
                        subtitle = it.subtitle,
                        tag = it.trainType.take(4).uppercase()
                    )
                }
            ) { picked ->
                train = trains.firstOrNull { it.trainNo == picked.key && it.depTime == picked.title }
                    ?: trains.firstOrNull { it.trainNo == picked.key }
                binding.tvTrainValue.text = train?.label ?: "Tap to choose a train"
                binding.tvTrainValue.setTextColor(tint)
                refreshPostButton()
            }
        }
    }

    private fun setupStationRow() {
        if (Attachment.STATION !in report.uses) return
        binding.rowStation.visibility = View.VISIBLE
        binding.tvStationLabel.text = labelFor(Attachment.STATION, "STATION")
        binding.tvStationValue.text = "Tap to choose a station"
        binding.tvStationValue.setTextColor(Color.parseColor("#9E9B93"))

        binding.rowStation.setOnClickListener {
            val e = engine ?: return@setOnClickListener
            val stations = RoomTimetable.stationsFor(e, room)
            openPicker(
                title = "Choose a station",
                subtitle = "Stations on ${room.shortName}, in running order",
                options = stations.map { OptionPickerSheet.Option(key = it, title = it) }
            ) { picked ->
                station = picked.key
                binding.tvStationValue.text = picked.title
                binding.tvStationValue.setTextColor(Color.parseColor("#1A1A18"))
                refreshPostButton()
            }
        }
    }

    private fun setupPlatformRow() {
        if (Attachment.PLATFORM !in report.uses) return
        binding.rowPlatform.visibility = View.VISIBLE
        binding.tvPlatformLabel.text = labelFor(Attachment.PLATFORM, "MOVED TO PLATFORM")
        binding.tvPlatformValue.text = "Tap to choose a platform"
        binding.tvPlatformValue.setTextColor(Color.parseColor("#9E9B93"))

        binding.rowPlatform.setOnClickListener {
            openPicker(
                title = "New platform",
                subtitle = station?.let { "At $it" } ?: "Which platform is it on now?",
                options = RoomTimetable.platforms.map {
                    OptionPickerSheet.Option(key = it, title = "Platform $it")
                }
            ) { picked ->
                platform = picked.key
                binding.tvPlatformValue.text = "Platform ${picked.key}"
                binding.tvPlatformValue.setTextColor(Color.parseColor("#1A1A18"))
                refreshPostButton()
            }
        }
    }

    private fun labelFor(attachment: Attachment, base: String): String =
        if (attachment in report.requires) base else "$base (OPTIONAL)"

    private fun openPicker(
        title: String,
        subtitle: String,
        options: List<OptionPickerSheet.Option>,
        onPicked: (OptionPickerSheet.Option) -> Unit
    ) {
        OptionPickerSheet().apply {
            this.sheetTitle = title
            this.sheetSubtitle = subtitle
            this.options = options
            this.onPicked = onPicked
        }.show(parentFragmentManager, OptionPickerSheet.TAG)
    }

    /** A report is postable once everything it requires has been chosen. */
    private fun refreshPostButton() {
        val ready = report.requires.all {
            when (it) {
                Attachment.TRAIN    -> train != null
                Attachment.STATION  -> station != null
                Attachment.PLATFORM -> platform != null
            }
        }
        binding.btnPostReport.isEnabled = ready
        binding.btnPostReport.alpha = if (ready) 1f else 0.45f
        binding.btnPostReport.text = if (ready) "Post report" else "Choose the details above"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReportComposerSheet"

        fun show(
            fm: FragmentManager,
            report: QuickReport,
            room: ChatRoom,
            engine: TransitEngine?,
            onPost: (Result) -> Unit
        ) {
            ReportComposerSheet().apply {
                this.report = report
                this.room = room
                this.engine = engine
                this.onPost = onPost
            }.show(fm, TAG)
        }
    }
}
