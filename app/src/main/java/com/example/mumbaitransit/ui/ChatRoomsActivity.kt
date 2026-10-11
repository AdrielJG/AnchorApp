package com.example.mumbaitransit.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.R
import com.example.mumbaitransit.chat.ChatDay
import com.example.mumbaitransit.chat.ChatMessage
import com.example.mumbaitransit.chat.ChatRepository
import com.example.mumbaitransit.chat.ChatRoom
import com.example.mumbaitransit.chat.ChatRooms
import com.example.mumbaitransit.chat.QuickReport
import com.example.mumbaitransit.databinding.ActivityChatRoomsBinding
import com.example.mumbaitransit.databinding.ItemChatRoomBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * The sixteen line rooms — suburban rail and metro, two directions each.
 *
 * The rows are drawn from [ChatRooms], which is static data, so they render
 * before anything touches the network. Last-message previews arrive afterwards
 * and are allowed to fail — a slow or refused backend read should cost you the
 * preview text, never the list itself.
 */
class ChatRoomsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnchorChatRooms"

        /** Past this, previews aren't worth waiting for. */
        private const val PREVIEW_TIMEOUT_MS = 6_000L
    }

    private lateinit var binding: ActivityChatRoomsBinding
    private val repo by lazy { ChatRepository.get(this) }

    /** Room id -> its row, so previews can be filled in after the fact. */
    private val rows = mutableMapOf<String, ItemChatRoomBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Line Chat"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvDailyNotice.text = ChatDay.resetNotice()
    }

    override fun onResume() {
        super.onResume()
        renderRooms()
        loadPreviews()
        // Housekeeping only — never blocks the list.
        lifecycleScope.launch { runCatching { repo.pruneOldDays() } }
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    private fun renderRooms() {
        binding.containerRooms.removeAllViews()
        rows.clear()
        addSectionHeader("SUBURBAN RAIL")
        ChatRooms.rail.forEach { room -> rows[room.id] = addRoomRow(room) }
        addSectionHeader("METRO")
        ChatRooms.metro.forEach { room -> rows[room.id] = addRoomRow(room) }
    }

    /** Keeps sixteen rooms scannable — rail and metro read as two lists, not one long one. */
    private fun addSectionHeader(title: String) {
        val density = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = title
            textSize = 10.5f
            letterSpacing = 0.08f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (if (binding.containerRooms.childCount == 0) 0 else 10 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
        }
        binding.containerRooms.addView(tv)
    }

    private fun addRoomRow(room: ChatRoom): ItemChatRoomBinding {
        val b = ItemChatRoomBinding.inflate(
            LayoutInflater.from(this), binding.containerRooms, false
        )
        val lineColor = LineStyle.color(room.lineName)

        b.viewRoomAccent.setBackgroundColor(lineColor)
        b.tvRoomLine.text = room.shortName
        b.tvRoomTowards.text = room.towards
        b.tvRoomPreview.text = "Tap to open"

        b.tvRoomDirection.text = room.direction.label
        b.tvRoomDirection.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(lineColor)
            cornerRadius = 20 * resources.displayMetrics.density
        }

        b.root.setOnClickListener {
            startActivity(
                Intent(this, ChatRoomActivity::class.java)
                    .putExtra(ChatRoomActivity.EXTRA_ROOM_ID, room.id)
            )
        }
        binding.containerRooms.addView(b.root)
        return b
    }

    // ── Previews ──────────────────────────────────────────────────────────────

    private fun loadPreviews() {
        lifecycleScope.launch {
            val latest = try {
                withTimeoutOrNull(PREVIEW_TIMEOUT_MS) { repo.latestPerRoom() }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load room previews", e)
                null
            } ?: return@launch

            latest.forEach { (roomId, message) ->
                rows[roomId]?.tvRoomPreview?.text = preview(roomId, message)
            }
            rows.forEach { (roomId, row) ->
                if (roomId !in latest) row.tvRoomPreview.text = "No messages today"
            }
        }
    }

    private fun preview(roomId: String, message: ChatMessage): String {
        val mode = ChatRooms.byId(roomId)?.mode
        val who = message.username.ifBlank { "Someone" }
        val body = QuickReport.from(message.reportType)
            ?.let { r -> "${r.emoji} ${mode?.let { r.labelIn(it) } ?: r.label}" }
            ?: message.text
        return "$who: $body  ·  ${relativeTime(message.sentAt)}"
    }

    private fun relativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        return when {
            mins < 1L   -> "just now"
            mins < 60L  -> "${mins}m ago"
            else        -> "${hours}h ago"
        }
    }
}
