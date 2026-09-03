package com.example.mumbaitransit.ui

import android.graphics.Color
import android.util.Log
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mumbaitransit.auth.AuthRepository
import com.example.mumbaitransit.chat.ChatMessage
import com.example.mumbaitransit.chat.ChatRepository
import com.example.mumbaitransit.chat.ChatRoom
import com.example.mumbaitransit.chat.ChatRooms
import com.example.mumbaitransit.chat.QuickReport
import com.example.mumbaitransit.engine.TransitEngine
import com.example.mumbaitransit.databinding.ActivityChatRoomBinding
import com.example.mumbaitransit.databinding.ItemChatMessageBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One line, one direction.
 *
 * Free text and the nine one-tap reports post into the same stream, so a
 * report and the follow-up question about it sit next to each other.
 */
class ChatRoomActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        private const val TAG = "AnchorChat"
    }

    private lateinit var binding: ActivityChatRoomBinding
    private lateinit var room: ChatRoom
    private lateinit var adapter: ChatAdapter

    private val repo by lazy { ChatRepository.get(this) }
    private val authRepo by lazy { AuthRepository(this) }

    private var lineColor = Color.GRAY

    /** Loaded at splash; null only if the user got here before data finished. */
    private val engine: TransitEngine?
        get() = application.getSharedViewModel().let { if (it.isEngineReady) it.engine else null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        val found = roomId?.let { ChatRooms.byId(it) }
        if (found == null) { finish(); return }
        room = found
        lineColor = LineStyle.color(room.lineName)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "${room.shortName} · ${room.direction.label}"
            subtitle = room.towards
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.viewLineStrip.setBackgroundColor(lineColor)

        setupList()
        setupQuickReports()
        setupInput()
        observeMessages()
    }

    // ── Message list ──────────────────────────────────────────────────────────

    private fun setupList() {
        adapter = ChatAdapter(authRepo.session.getUid(), lineColor)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            repo.observeMessages(room.id)
                // A refused read is almost always the Realtime Database rules.
                // Say that plainly instead of leaving an empty room on screen.
                .catch { e ->
                    Log.w(TAG, "Could not read ${room.id}", e)
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvMessages.visibility = View.GONE
                    binding.tvEmpty.text =
                        "Couldn't load this room.\nCheck your connection, and that the " +
                        "database rules in FIREBASE_SETUP.md have been published."
                }
                .collectLatest { messages ->
                    adapter.submitList(messages) {
                        if (messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(messages.size - 1)
                        }
                    }
                    val empty = messages.isEmpty()
                    binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.rvMessages.visibility = if (empty) View.GONE else View.VISIBLE
                }
        }
    }

    // ── Quick reports ─────────────────────────────────────────────────────────

    private fun setupQuickReports() {
        QuickReport.values().forEach { report ->
            val color = Color.parseColor(report.tint)
            val chip = Chip(this).apply {
                text = "${report.emoji}  ${report.label}"
                textSize = 12.5f
                isCheckable = false
                isClickable = true
                chipStrokeWidth = resources.displayMetrics.density
                setChipStrokeColorResource(com.example.mumbaitransit.R.color.border)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
                )
                setTextColor(color)
                setOnClickListener { compose(report) }
            }
            binding.chipGroupReports.addView(chip)
        }

        binding.btnToggleReports.setOnClickListener {
            val showing = binding.panelQuickReports.visibility == View.VISIBLE
            binding.panelQuickReports.visibility = if (showing) View.GONE else View.VISIBLE
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private fun setupInput() {
        binding.btnSend.setOnClickListener { send() }
        // Nothing to send on an empty box, so dim rather than post a blank row.
        binding.etMessage.doAfterTextChanged {
            binding.btnSend.alpha = if (it.isNullOrBlank()) 0.45f else 1f
        }
        binding.btnSend.alpha = 0.45f
    }

    /**
     * Opens the composer so the report can be pinned to a train, a station or a
     * platform. Reports that need nothing attached post straight away.
     */
    private fun compose(report: QuickReport) {
        binding.panelQuickReports.visibility = View.GONE
        if (report.uses.isEmpty()) {
            post(report, binding.etMessage.text?.toString().orEmpty(), null, null, null, null)
            binding.etMessage.setText("")
            return
        }
        ReportComposerSheet.show(supportFragmentManager, report, room, engine) { r ->
            post(r.report, r.note, r.trainNo, r.trainLabel, r.station, r.platform)
        }
    }

    /** Plain typed message, no report attached. */
    private fun send() {
        val text = binding.etMessage.text?.toString().orEmpty()
        if (text.isBlank()) return
        post(null, text, null, null, null, null)
        binding.etMessage.setText("")
    }

    private fun post(
        report: QuickReport?,
        text: String,
        trainNo: String?,
        trainLabel: String?,
        station: String?,
        platform: String?
    ) {
        val session = authRepo.session
        lifecycleScope.launch {
            repo.send(
                roomId = room.id,
                uid = session.getUid().ifBlank { "local" },
                username = session.getUsername().ifBlank { "Commuter" },
                text = text,
                report = report,
                trainNo = trainNo,
                trainLabel = trainLabel,
                station = station,
                platform = platform
            )
        }
    }
}

/**
 * Renders the stream. Own messages sit right and filled; everyone else's sit
 * left with the sender's name in the line colour.
 */
private class ChatAdapter(
    private val myUid: String,
    private val lineColor: Int
) : ListAdapter<ChatMessage, ChatAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.id == b.id
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
        private val CLOCK = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    }

    class VH(val b: ItemChatMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = getItem(position)
        val b = holder.b
        val ctx = b.root.context
        val mine = msg.uid.isNotEmpty() && msg.uid == myUid
        val report = QuickReport.from(msg.reportType)

        b.rowChatMessage.gravity = if (mine) android.view.Gravity.END else android.view.Gravity.START

        // Reports keep their own tint whoever sent them — the colour is the
        // signal, and a delay report should look the same from either side.
        when {
            report != null -> {
                val tint = Color.parseColor(report.tint)
                b.bubbleChat.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.argb(22, Color.red(tint), Color.green(tint), Color.blue(tint)))
                    setStroke(
                        (ctx.resources.displayMetrics.density).toInt(),
                        Color.argb(70, Color.red(tint), Color.green(tint), Color.blue(tint))
                    )
                    cornerRadius = 14 * ctx.resources.displayMetrics.density
                }
                b.tvChatReport.visibility = View.VISIBLE
                b.tvChatReport.text = "${report.emoji}  ${report.label}"
                b.tvChatReport.setTextColor(tint)
                b.tvChatText.setTextColor(Color.parseColor("#1A1A18"))
                b.tvChatTime.setTextColor(Color.parseColor("#9E9B93"))
                b.tvChatUser.setTextColor(tint)
            }
            mine -> {
                b.bubbleChat.setBackgroundResource(
                    com.example.mumbaitransit.R.drawable.bg_bubble_mine
                )
                b.tvChatReport.visibility = View.GONE
                b.tvChatText.setTextColor(Color.WHITE)
                b.tvChatTime.setTextColor(Color.argb(180, 255, 255, 255))
            }
            else -> {
                b.bubbleChat.setBackgroundResource(
                    com.example.mumbaitransit.R.drawable.bg_bubble_other
                )
                b.tvChatReport.visibility = View.GONE
                b.tvChatText.setTextColor(Color.parseColor("#1A1A18"))
                b.tvChatTime.setTextColor(Color.parseColor("#9E9B93"))
                b.tvChatUser.setTextColor(lineColor)
            }
        }

        // The sender's name only helps on messages that aren't the user's own.
        if (mine) {
            b.tvChatUser.visibility = View.GONE
        } else {
            b.tvChatUser.visibility = View.VISIBLE
            b.tvChatUser.text = msg.username.ifBlank { "Commuter" }
        }

        if (msg.text.isBlank()) {
            b.tvChatText.visibility = View.GONE
        } else {
            b.tvChatText.visibility = View.VISIBLE
            b.tvChatText.text = msg.text
        }

        // The attachment is the substance of a report, so it gets its own
        // panel rather than being folded into the message text.
        val attachment = msg.attachmentLine()
        if (attachment.isNullOrBlank()) {
            b.tvChatAttachment.visibility = View.GONE
        } else {
            b.tvChatAttachment.visibility = View.VISIBLE
            b.tvChatAttachment.text = attachment
            val base = if (report != null) Color.parseColor(report.tint)
                       else if (mine) Color.WHITE else Color.parseColor("#6B6860")
            b.tvChatAttachment.setTextColor(base)
            b.tvChatAttachment.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(
                    if (mine && report == null) Color.argb(38, 255, 255, 255)
                    else Color.argb(20, Color.red(base), Color.green(base), Color.blue(base))
                )
                cornerRadius = 9 * ctx.resources.displayMetrics.density
            }
        }

        b.tvChatTime.text = CLOCK.format(Date(msg.sentAt))
    }
}
