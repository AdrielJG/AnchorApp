package com.example.mumbaitransit.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mumbaitransit.databinding.ItemShareStepBinding
import com.example.mumbaitransit.databinding.ItemShareTrainBinding
import com.example.mumbaitransit.databinding.ShareRouteCardBinding
import com.example.mumbaitransit.model.PathEdge
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.model.TimetableLeg
import com.example.mumbaitransit.model.TrainTiming
import com.example.mumbaitransit.ui.LineStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Everything the share card needs that isn't already on the [RouteCard].
 *
 * Timetable legs and auto fares are produced by the ViewModel, so they're
 * gathered once by the caller and handed over rather than looked up here.
 */
data class ShareContext(
    val legs: List<TimetableLeg> = emptyList(),
    val departWindow: String? = null,
    val autoFareTo: Int = 0,
    val autoFareFrom: Int = 0
)

/**
 * Turns a [RouteCard] into the picture that gets sent to someone else.
 *
 * The card is inflated, bound and measured off-screen, then drawn straight onto
 * a Bitmap — it is never added to the activity's view tree, so the user sees the
 * share sheet and nothing else. Output is always [OUTPUT_WIDTH_PX] wide whatever
 * the device density, so the same route looks identical shared from any phone.
 */
object RouteShareCard {

    /** Width the card is laid out at, in dp. Roughly a phone's content width. */
    private const val LAYOUT_WIDTH_DP = 400

    /** Width of the PNG that actually gets shared. Sharp on any messenger. */
    private const val OUTPUT_WIDTH_PX = 1080

    /** Walking further than this is worth an auto instead — same rule as the app. */
    private const val AUTO_SUGGEST_KM = 1.0
    private const val WALK_KMPH = 5.0

    private const val COLOR_FAST = "#DC2626"
    private const val COLOR_SLOW = "#0A7C42"
    private const val COLOR_MEDIUM = "#B45309"
    private const val COLOR_AC = "#0891B2"

    fun render(context: Context, card: RouteCard, ctx: ShareContext): Bitmap {
        val b = ShareRouteCardBinding.inflate(LayoutInflater.from(context))
        bind(context, b, card, ctx)
        return draw(context, b.root)
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Binding
    // ──────────────────────────────────────────────────────────────────────────

    private fun bind(
        context: Context,
        b: ShareRouteCardBinding,
        card: RouteCard,
        ctx: ShareContext
    ) {
        val accent = if (card.type == "transit") LineStyle.scenarioColor(card.scenario)
                     else LineStyle.modeColor(card.type)
        b.stripBand.setBackgroundColor(accent)
        b.tvScenario.text = card.scenarioLabel.uppercase(Locale.ENGLISH)

        val origin = card.originLabel.ifEmpty { card.originStation }.ifEmpty { "Origin" }
        val dest   = card.destLabel.ifEmpty { card.destStation }.ifEmpty { "Destination" }
        b.tvRoutePair.text = "$origin  →  $dest"
        b.tvModes.text = card.modeStr
        b.tvTotalTime.text = "${card.totalMin.roundToInt()} min"

        b.tvFare.text = buildString {
            append("₹${card.totalFare}")
            if (card.type == "transit") {
                append(" · ")
                append(
                    when (card.transfers) {
                        0    -> "direct"
                        1    -> "1 transfer"
                        else -> "${card.transfers} transfers"
                    }
                )
            }
        }

        card.mriScores.values.minByOrNull { it.mriPct }?.let {
            b.tvMri.visibility = View.VISIBLE
            b.tvMri.text = "MRI ${it.mriPct.toInt()}%"
            b.tvMri.setTextColor(LineStyle.mriColor(it.mriPct))
        }

        if (card.linesUsed.isEmpty()) {
            b.tvLines.visibility = View.GONE
        } else {
            b.tvLines.text = card.linesUsed.joinToString("  →  ") { LineStyle.shorten(it) }
        }

        val walkBits = mutableListOf<String>()
        if (card.walkToMin > 0) walkBits += "🚶 ${card.walkToMin} min to ${card.originStation}"
        if (card.walkFromMin > 0) walkBits += "🚶 ${card.walkFromMin} min from ${card.destStation}"
        if (walkBits.isEmpty()) {
            b.tvWalkSummary.visibility = View.GONE
        } else {
            b.tvWalkSummary.text = walkBits.joinToString("   ·   ")
        }

        // ── Timeline ──────────────────────────────────────────────────────────
        b.llSteps.removeAllViews()
        val steps = if (card.type == "transit") transitSteps(card, ctx) else simpleSteps(card)
        steps.forEachIndexed { i, step ->
            addStep(context, b.llSteps, step, isFirst = i == 0, isLast = i == steps.lastIndex)
        }

        if (card.note.isNotEmpty()) {
            b.tvNote.visibility = View.VISIBLE
            b.tvNote.text = card.note
        }

        // ── Footer ────────────────────────────────────────────────────────────
        val date = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date())
        val firstDep = ctx.legs.firstOrNull()?.trains?.firstOrNull()?.depFrom
        b.tvFooterMain.text = buildString {
            append("Journey for $date")
            if (firstDep != null) append(" · departs $firstDep")
        }
        b.tvFooterSub.text = ctx.departWindow?.takeIf { it.isNotBlank() }
            ?.let { "$it · times are indicative, check live status before travelling." }
            ?: "Times are indicative — check live status before travelling."
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Timeline construction
    // ──────────────────────────────────────────────────────────────────────────

    private data class Step(
        val title: String,
        val sub: String? = null,
        val pill: String? = null,
        val dotColor: Int,
        /** Emoji shown in place of the dot — walk / auto legs read faster this way. */
        val icon: String? = null,
        val train: TrainTiming? = null,
        val lineColor: Int = 0
    )

    /**
     * Mirrors RoutesResultActivity.renderJourney: consecutive in-vehicle edges on
     * the same line collapse into one segment, and each segment becomes a single
     * boarding node carrying the train to catch. The alighting station is the
     * next node down — the transfer, or the destination — so it isn't repeated.
     */
    private fun transitSteps(card: RouteCard, ctx: ShareContext): List<Step> {
        val steps = mutableListOf<Step>()
        val origin = card.originLabel.ifEmpty { card.originStation }
        val dest   = card.destLabel.ifEmpty { card.destStation }

        val ivEdges   = card.path.filter { it.edgeType == "in_vehicle" }
        val xferEdges = card.path.filter { it.edgeType == "transfer" }

        val segments = mutableListOf<List<PathEdge>>()
        var current = mutableListOf<PathEdge>()
        for (edge in ivEdges) {
            if (current.isEmpty() || current.last().line == edge.line) {
                current.add(edge)
            } else {
                segments.add(current)
                current = mutableListOf(edge)
            }
        }
        if (current.isNotEmpty()) segments.add(current)

        // ── Start ─────────────────────────────────────────────────────────────
        val startsWithAuto = suggestAuto(card.walkToMin) && ctx.autoFareTo > 0
        steps.add(
            Step(
                title = origin,
                sub = when {
                    startsWithAuto      -> "Auto ~₹${ctx.autoFareTo} to ${card.originStation}"
                    card.walkToMin > 0  -> "Walk ~${card.walkToMin} min to ${card.originStation}"
                    else                -> null
                },
                dotColor = Color.parseColor("#6b7280"),
                icon = when {
                    startsWithAuto     -> "🛺"
                    card.walkToMin > 0 -> "🚶"
                    else               -> null
                }
            )
        )

        // ── Legs ──────────────────────────────────────────────────────────────
        var xferIdx = 0
        for ((idx, segment) in segments.withIndex()) {
            val first = segment.first()
            val last  = segment.last()
            val leg   = ctx.legs.getOrNull(idx)
            val train = leg?.trains?.firstOrNull()
            val travelMin = segment.sumOf { it.travelMin }.roundToInt()
            val stopCount = segment.size

            steps.add(
                Step(
                    title = first.fromStop,
                    // With a train block below, repeating the duration here is noise.
                    sub = if (train == null)
                        "Towards ${last.toStop} · $travelMin min · $stopCount stop${if (stopCount > 1) "s" else ""}"
                    else null,
                    pill = LineStyle.shorten(first.line),
                    dotColor = LineStyle.color(first.line),
                    train = train,
                    lineColor = LineStyle.color(first.line)
                )
            )

            if (idx != segments.lastIndex && xferIdx < xferEdges.size) {
                val xfer = xferEdges[xferIdx++]
                steps.add(
                    Step(
                        title = "Change at ${xfer.toStop}",
                        sub = "Walk ${xfer.travelMin.roundToInt()} min to connect lines",
                        dotColor = Color.parseColor("#7c3aed"),
                        icon = "🚶"
                    )
                )
            }
        }

        // ── End ───────────────────────────────────────────────────────────────
        val lastStop = segments.lastOrNull()?.last()?.toStop ?: card.destStation
        val endsWithAuto = suggestAuto(card.walkFromMin) && ctx.autoFareFrom > 0
        steps.add(
            if (card.walkFromMin > 0) {
                Step(
                    title = lastStop,
                    sub = if (endsWithAuto) "Auto ~₹${ctx.autoFareFrom} to $dest"
                          else "Walk ~${card.walkFromMin} min to $dest",
                    dotColor = Color.parseColor("#16a34a"),
                    icon = if (endsWithAuto) "🛺" else "🚶"
                )
            } else {
                Step(title = dest, dotColor = Color.parseColor("#16a34a"))
            }
        )
        return steps
    }

    /** Bus / auto / cab cards carry no path, so the card is just the two ends. */
    private fun simpleSteps(card: RouteCard): List<Step> {
        val origin = card.originLabel.ifEmpty { card.originStation }.ifEmpty { "Origin" }
        val dest   = card.destLabel.ifEmpty { card.destStation }.ifEmpty { "Destination" }
        val accent = LineStyle.modeColor(card.type)
        return listOf(
            Step(
                title = origin,
                sub = if (card.roadKm > 0) "≈ %.1f km by road".format(card.roadKm) else null,
                pill = card.modeStr,
                dotColor = accent,
                icon = if (card.type == "auto") "🛺" else null
            ),
            Step(title = dest, dotColor = Color.parseColor("#16a34a"))
        )
    }

    private fun suggestAuto(walkMin: Int): Boolean =
        walkMin / 60.0 * WALK_KMPH > AUTO_SUGGEST_KM

    // ──────────────────────────────────────────────────────────────────────────
    //  View helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun addStep(
        context: Context,
        container: LinearLayout,
        step: Step,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        val s = ItemShareStepBinding.inflate(LayoutInflater.from(context), container, false)
        s.tvStepTitle.text = step.title

        if (step.icon != null) {
            s.viewStepDot.visibility = View.GONE
            s.tvStepIcon.visibility = View.VISIBLE
            s.tvStepIcon.text = step.icon
        } else {
            s.viewStepDot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(step.dotColor)
            }
        }

        step.sub?.let { s.tvStepSub.visibility = View.VISIBLE; s.tvStepSub.text = it }

        step.pill?.let {
            s.tvStepPill.visibility = View.VISIBLE
            s.tvStepPill.text = it
            s.tvStepPill.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(step.dotColor)
                cornerRadius = dp(context, 10).toFloat()
            }
        }

        step.train?.let {
            s.llStepTrain.visibility = View.VISIBLE
            s.llStepTrain.addView(buildTrainBlock(context, s.llStepTrain, it, step.lineColor))
        }

        if (isFirst) s.viewStepLineTop.visibility = View.INVISIBLE
        if (isLast)  s.viewStepLineBottom.visibility = View.INVISIBLE
        container.addView(s.root)
    }

    /**
     * The train to board. Train numbers are left off deliberately — the
     * destination board shows the terminus, which is what a commuter looks for.
     */
    private fun buildTrainBlock(
        context: Context,
        parent: LinearLayout,
        train: TrainTiming,
        lineColor: Int
    ): View {
        val t = ItemShareTrainBinding.inflate(LayoutInflater.from(context), parent, false)

        t.tvShareTrainName.text = train.terminus.takeIf { it.isNotBlank() }
            ?.let { "Towards ${it.uppercase(Locale.ENGLISH)}" }
            ?: "Next train"

        t.tvShareTrainTimes.text = "${train.depFrom}  →  ${train.arrTo}"
        t.tvShareTrainTimes.setTextColor(lineColor)
        t.tvShareTrainDuration.text = "${train.journeyMin} min journey"

        when {
            train.trainType.contains("Fast", true)   -> badge(context, t.tvShareSpeedBadge, "F", COLOR_FAST)
            train.trainType.contains("Slow", true)   -> badge(context, t.tvShareSpeedBadge, "S", COLOR_SLOW)
            train.trainType.contains("Medium", true) -> badge(context, t.tvShareSpeedBadge, "M", COLOR_MEDIUM)
        }
        if (train.ac || train.trainType.contains("AC", true)) {
            badge(context, t.tvShareAcBadge, "AC", COLOR_AC)
        }
        return t.root
    }

    /** Same F / S / AC chip the route list draws, so the card reads identically. */
    private fun badge(context: Context, view: TextView, label: String, colorHex: String) {
        val color = Color.parseColor(colorHex)
        view.visibility = View.VISIBLE
        view.text = label
        view.setTextColor(color)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)))
            setStroke(dp(context, 1), Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)))
            cornerRadius = dp(context, 6).toFloat()
        }
    }

    /**
     * Lays the card out at a fixed dp width and draws it onto a bitmap scaled to
     * [OUTPUT_WIDTH_PX]. Scaling the canvas rather than the finished bitmap keeps
     * the text vector-sharp instead of resampled.
     */
    private fun draw(context: Context, view: View): Bitmap {
        val density = context.resources.displayMetrics.density
        val layoutWidthPx = (LAYOUT_WIDTH_DP * density).roundToInt()

        view.measure(
            View.MeasureSpec.makeMeasureSpec(layoutWidthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val scale = OUTPUT_WIDTH_PX.toFloat() / view.measuredWidth
        val bitmap = Bitmap.createBitmap(
            OUTPUT_WIDTH_PX,
            (view.measuredHeight * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        view.draw(canvas)
        return bitmap
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
