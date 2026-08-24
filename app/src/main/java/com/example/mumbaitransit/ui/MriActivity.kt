package com.example.mumbaitransit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mumbaitransit.databinding.ActivityMriBinding
import com.example.mumbaitransit.databinding.ItemMriLineBinding
import com.example.mumbaitransit.model.MriScore

class MriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMriBinding
    private val vm: TransitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMriBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "MRI — Line Reliability Scores"

        val adapter = MriAdapter()
        binding.rvMri.layoutManager = LinearLayoutManager(this)
        binding.rvMri.adapter = adapter

        vm.mriData.observe(this) { mri ->
            val sorted = mri.entries
                .sortedByDescending { it.value.mriPct }
                .map { it.key to it.value }
            adapter.items = sorted
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}

class MriAdapter : RecyclerView.Adapter<MriAdapter.VH>() {
    var items: List<Pair<String, MriScore>> = emptyList()

    inner class VH(val binding: ItemMriLineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMriLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (line, score) = items[position]
        val b = holder.binding
        b.tvLine.text = line
        b.tvMriPct.text = "${score.mriPct.toInt()}%"
        b.tvFreq.text   = "${score.freq} trains/hr"
        b.tvCv.text     = "CV: ${score.cv}"
        b.tvAc.text     = if (score.acRatio > 0.05) "AC: ${(score.acRatio * 100).toInt()}%" else ""
        b.progressMri.progress = score.mriPct.toInt()
        b.progressMri.progressTintList = android.content.res.ColorStateList.valueOf(
            when {
                score.mriPct >= 70 -> android.graphics.Color.parseColor("#0a7c42")
                score.mriPct >= 50 -> android.graphics.Color.parseColor("#b45309")
                else               -> android.graphics.Color.parseColor("#9b1c1c")
            }
        )
    }
}
