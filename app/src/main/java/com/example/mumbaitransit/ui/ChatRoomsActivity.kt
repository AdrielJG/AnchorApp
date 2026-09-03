package com.example.mumbaitransit.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
 * The eight line rooms.
 *
 * The rows are drawn from [ChatRooms.all], which is static data, so they render
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
    }

    override fun onResume() {
        super.onResume()
        renderRooms()
        loadPreviews()
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    private fun renderRooms() {
        binding.containerRooms.removeAllViews()
        rows.clear()
        ChatRooms.all.forEach { room -> rows[room.id] = addRoomRow(room) }
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
                rows[roomId]?.tvRoomPreview?.text = preview(message)
            }
            rows.forEach { (roomId, row) ->
                if (roomId !in latest) row.tvRoomPreview.text = "No messages yet"
            }
        }
    }

    private fun preview(message: ChatMessage): String {
        val who = message.username.ifBlank { "Someone" }
        val body = QuickReport.from(message.reportType)
            ?.let { "${it.emoji} ${it.label}" }
            ?: message.text
        return "$who: $body  ·  ${relativeTime(message.sentAt)}"
    }

    private fun relativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            mins < 1L   -> "just now"
            mins < 60L  -> "${mins}m ago"
            hours < 24L -> "${hours}h ago"
            days < 7L   -> "${days}d ago"
            else        -> "a while ago"
        }
    }
}
