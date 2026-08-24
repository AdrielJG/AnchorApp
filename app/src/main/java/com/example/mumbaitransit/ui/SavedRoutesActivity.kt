package com.example.mumbaitransit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mumbaitransit.auth.SavedRouteEntity
import com.example.mumbaitransit.auth.SavedRouteRepository
import com.example.mumbaitransit.databinding.ActivitySavedRoutesBinding
import com.example.mumbaitransit.databinding.ItemSavedRouteBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SavedRoutesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRoutesBinding
    private lateinit var repo: SavedRouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repo = SavedRouteRepository(this)
        loadSavedRoutes()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun loadSavedRoutes() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.llSavedContainer.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            val routes = repo.getSavedRoutes()
            withContext(Dispatchers.Main) {
                binding.progressLoading.visibility = View.GONE
                if (routes.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    routes.forEach { addRouteCard(it) }
                }
            }
        }
    }

    private fun addRouteCard(entity: SavedRouteEntity) {
        val b = ItemSavedRouteBinding.inflate(
            LayoutInflater.from(this),
            binding.llSavedContainer,
            false
        )

        b.tvRouteTitle.text = "${entity.originLabel}  →  ${entity.destLabel}"
        b.tvScenarioLabel.text = buildString {
            append(entity.scenarioLabel)
            if (entity.modeStr.isNotEmpty()) append("  ·  ${entity.modeStr}")
        }
        b.tvTotalTime.text = "${entity.totalMin.roundToInt()} min"
        b.tvFare.text = "₹${entity.totalFare}"

        val lines = entity.linesUsed.split(",").filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            b.tvLines.visibility = View.VISIBLE
            b.tvLines.text = lines.joinToString(" → ") { shortenLine(it) }
        } else {
            b.tvLines.visibility = View.GONE
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        b.tvSavedAt.text = "Saved ${sdf.format(Date(entity.savedAt))}"

        // Accent colour by scenario / type
        val accentHex = when (entity.scenario) {
            "fastest"  -> "#1a56db"
            "cheapest" -> "#0a7c42"
            "reliable" -> "#6d28d9"
            else -> when (entity.routeType) {
                "bus"  -> "#d97706"
                "auto" -> "#b45309"
                "cab"  -> "#1f2937"
                else   -> "#b45309"
            }
        }
        b.viewAccent.setBackgroundColor(android.graphics.Color.parseColor(accentHex))

        b.btnDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                repo.savedRouteDao().delete(entity)
                withContext(Dispatchers.Main) {
                    binding.llSavedContainer.removeView(b.root)
                    if (binding.llSavedContainer.childCount == 0) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.llSavedContainer.addView(b.root)
    }

    private fun shortenLine(line: String): String = when {
        line.startsWith("WR")    -> "WR"
        line.startsWith("CR")    -> "CR"
        line.startsWith("HBR") || line.startsWith("Harbour") -> "Harbour"
        line.contains("Metro")   -> line.replace("Mumbai Metro ", "M").take(6)
        else                     -> line.take(12)
    }
}
