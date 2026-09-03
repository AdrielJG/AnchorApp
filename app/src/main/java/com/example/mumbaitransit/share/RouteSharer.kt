package com.example.mumbaitransit.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.model.RouteCard
import com.example.mumbaitransit.ui.LineStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Shares one route as a picture plus a readable caption.
 *
 * A picture is the only form of "shareable route" that works for every
 * recipient: WhatsApp, Telegram, email and Drive all render it, and the person
 * on the other end needs nothing installed. The caption carries the same
 * information as plain text, so it still reads sensibly if the image is
 * stripped or the recipient is on a screen reader.
 */
object RouteSharer {

    private const val CACHE_DIR = "shared_routes"

    fun share(activity: AppCompatActivity, card: RouteCard, ctx: ShareContext) {
        val bitmap = try {
            // Rendering walks a view tree, so it has to happen on the main thread.
            RouteShareCard.render(activity, card, ctx)
        } catch (e: Exception) {
            shareTextOnly(activity, card, ctx)
            return
        }

        activity.lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) { writePng(activity, bitmap, card) }
            if (uri == null) {
                shareTextOnly(activity, card, ctx)
                return@launch
            }
            launchChooser(activity, uri, caption(card, ctx), subject(card))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  File + intent
    // ──────────────────────────────────────────────────────────────────────────

    private fun writePng(context: Context, bitmap: Bitmap, card: RouteCard): Uri? = try {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        // One file per route, overwritten on re-share, so the cache can't grow
        // without limit as the user shares route after route.
        val name = "route_${slug(card.originLabel)}_${slug(card.destLabel)}_${card.scenario}.png"
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        null
    }

    private fun launchChooser(
        activity: AppCompatActivity,
        uri: Uri,
        text: String,
        subject: String
    ) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            // clipData is what makes the URI grant survive on some OEM builds.
            clipData = ClipData.newRawUri("Route card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            activity.startActivity(Intent.createChooser(send, "Share this route"))
        } catch (e: Exception) {
            Toast.makeText(activity, "No app available to share with", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareTextOnly(activity: AppCompatActivity, card: RouteCard, ctx: ShareContext) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption(card, ctx))
            putExtra(Intent.EXTRA_SUBJECT, subject(card))
        }
        try {
            activity.startActivity(Intent.createChooser(send, "Share this route"))
        } catch (e: Exception) {
            Toast.makeText(activity, "No app available to share with", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Text
    // ──────────────────────────────────────────────────────────────────────────

    private fun subject(card: RouteCard): String =
        "${card.originLabel.ifEmpty { card.originStation }} → " +
            card.destLabel.ifEmpty { card.destStation }

    /** The same route in plain text, for previews and image-less recipients. */
    private fun caption(card: RouteCard, ctx: ShareContext): String = buildString {
        appendLine(subject(card))
        appendLine("${card.scenarioLabel} · ${card.totalMin.roundToInt()} min · ₹${card.totalFare}")
        appendLine()

        if (card.modeStr.isNotEmpty()) appendLine(card.modeStr)

        if (card.type == "transit") {
            // One line per leg, with the train to catch where we know it.
            ctx.legs.forEachIndexed { i, leg ->
                val train = leg.trains.firstOrNull()
                val head = "${i + 1}. ${LineStyle.shorten(leg.line)}: ${leg.fromStop} → ${leg.toStop}"
                if (train == null) {
                    appendLine(head)
                } else {
                    val towards = train.terminus.takeIf { it.isNotBlank() }?.let { " (towards $it)" } ?: ""
                    appendLine("$head — ${train.depFrom} → ${train.arrTo}$towards")
                }
            }
            if (ctx.legs.isEmpty() && card.linesUsed.isNotEmpty()) {
                appendLine(card.linesUsed.joinToString(" → ") { LineStyle.shorten(it) })
            }

            val bits = mutableListOf<String>()
            bits += when (card.transfers) {
                0    -> "Direct"
                1    -> "1 transfer"
                else -> "${card.transfers} transfers"
            }
            if (card.walkToMin > 0) bits += "${card.walkToMin} min walk to ${card.originStation}"
            if (card.walkFromMin > 0) bits += "${card.walkFromMin} min walk from ${card.destStation}"
            card.mriScores.values.minByOrNull { it.mriPct }?.let {
                bits += "reliability ${it.mriPct.toInt()}%"
            }
            appendLine(bits.joinToString(" · "))
        } else if (card.note.isNotEmpty()) {
            appendLine(card.note)
        }

        ctx.departWindow?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine(it)
        }

        appendLine()
        append("Shared from Anchor")
    }

    private fun slug(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(24)
            .ifEmpty { "route" }
}
