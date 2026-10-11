package com.example.mumbaitransit.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mumbaitransit.auth.ProfileRepository
import com.example.mumbaitransit.auth.SavedPlace
import com.example.mumbaitransit.databinding.ActivityCustomAddressesBinding
import com.example.mumbaitransit.databinding.ItemSavedPlaceBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The "Custom Addresses" quick-destination tile opens here.
 *
 * Doubles as a picker and a manager: tapping a saved address hands its
 * coordinates back to whoever launched this screen (MainActivity starts a
 * search with them), while the remove button and "Add an address" button
 * manage the underlying list — the same list shown on the Profile screen,
 * since both read and write through [ProfileRepository].
 */
class CustomAddressesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomAddressesBinding
    private lateinit var repo: ProfileRepository

    private val placesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val place = Autocomplete.getPlaceFromIntent(result.data!!)
        val lat = place.latLng?.latitude ?: return@registerForActivityResult
        val lon = place.latLng?.longitude ?: return@registerForActivityResult
        val name = place.name ?: place.address ?: "Selected location"
        val address = place.address ?: name
        askLabelThenSave(name, address, lat, lon)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomAddressesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repo = ProfileRepository(this)

        val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized() && apiKey.isNotBlank()) {
            Places.initialize(applicationContext, apiKey)
        }

        binding.btnAddAddress.setOnClickListener { pickPlace() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (!repo.session.isLoggedIn()) { finish(); return }
        load()
    }

    private fun load() {
        binding.progressLoading.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val list = repo.addresses()
            withContext(Dispatchers.Main) {
                binding.progressLoading.visibility = View.GONE
                bindAddresses(list)
            }
        }
    }

    private fun bindAddresses(list: List<SavedPlace>) {
        binding.llAddresses.removeAllViews()
        binding.tvNoAddresses.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        list.forEach { place ->
            val row = ItemSavedPlaceBinding.inflate(
                LayoutInflater.from(this), binding.llAddresses, false
            )
            row.tvPlaceLabel.text = place.label
            row.tvPlaceAddress.text = place.address

            row.btnRemovePlace.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    repo.deleteAddress(place)
                    withContext(Dispatchers.Main) { load() }
                }
            }

            // Tapping the row itself (not Remove) picks this address and
            // hands it back to MainActivity to search.
            row.root.setOnClickListener { selectAndFinish(place) }

            binding.llAddresses.addView(row.root)
        }
    }

    private fun selectAndFinish(place: SavedPlace) {
        if (!place.hasFix) {
            Toast.makeText(this, "This address has no location saved", Toast.LENGTH_SHORT).show()
            return
        }
        val result = Intent().apply {
            putExtra(EXTRA_LAT, place.lat)
            putExtra(EXTRA_LON, place.lon)
            putExtra(EXTRA_LABEL, place.label.ifBlank { place.address })
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    // ── Adding a new address ─────────────────────────────────────────────────

    private fun pickPlace() {
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("IN").build(this)
        placesLauncher.launch(intent)
    }

    private fun askLabelThenSave(suggested: String, address: String, lat: Double, lon: Double) {
        val input = EditText(this).apply {
            setText(suggested)
            setSelection(text.length)
            setPadding(48, 32, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Name this address")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val label = input.text?.toString()?.trim().orEmpty().ifBlank { suggested }
                CoroutineScope(Dispatchers.IO).launch {
                    repo.addAddress(SavedPlace(label = label, address = address, lat = lat, lon = lon))
                    withContext(Dispatchers.Main) { load() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_LABEL = "extra_label"
    }
}
