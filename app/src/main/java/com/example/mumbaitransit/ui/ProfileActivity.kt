package com.example.mumbaitransit.ui

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
import com.example.mumbaitransit.auth.AuthRepository
import com.example.mumbaitransit.auth.Phone
import com.example.mumbaitransit.auth.ProfileRepository
import com.example.mumbaitransit.auth.SavedPlace
import com.example.mumbaitransit.databinding.ActivityProfileBinding
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
 * The account screen.
 *
 * Name, username and phone came from the sign-up form and are shown as they are.
 * Everything below them is optional and starts empty: home, college/work, any
 * number of named addresses, and the saved routes list. A place is picked
 * through the same Google Places autocomplete the home screen uses, so it lands
 * with real coordinates and can be used as a route endpoint later.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var repo: ProfileRepository
    private lateinit var authRepo: AuthRepository

    /** What the Places picker is currently being opened for. */
    private enum class Target { HOME, WORK, CUSTOM }
    private var pickingFor = Target.HOME

    private val placesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val place = Autocomplete.getPlaceFromIntent(result.data!!)
        val lat = place.latLng?.latitude ?: return@registerForActivityResult
        val lon = place.latLng?.longitude ?: return@registerForActivityResult
        val name = place.name ?: place.address ?: "Selected location"
        val address = place.address ?: name

        when (pickingFor) {
            Target.HOME   -> save(SavedPlace(label = "Home", address = address, lat = lat, lon = lon), Target.HOME)
            Target.WORK   -> save(SavedPlace(label = "College / Work", address = address, lat = lat, lon = lon), Target.WORK)
            Target.CUSTOM -> askLabelThenSave(name, address, lat, lon)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repo = ProfileRepository(this)
        authRepo = AuthRepository(this)

        val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized() && apiKey.isNotBlank()) {
            Places.initialize(applicationContext, apiKey)
        }

        binding.rowHome.setOnClickListener { pick(Target.HOME) }
        binding.rowWork.setOnClickListener { pick(Target.WORK) }
        binding.btnAddAddress.setOnClickListener { pick(Target.CUSTOM) }

        binding.btnClearHome.setOnClickListener { clear(Target.HOME) }
        binding.btnClearWork.setOnClickListener { clear(Target.WORK) }

        binding.btnSavedRoutes.setOnClickListener {
            startActivity(Intent(this, SavedRoutesActivity::class.java))
        }

        binding.btnSignOut.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setMessage("Your saved places and routes stay on your account.")
                .setPositiveButton("Sign Out") { _, _ ->
                    authRepo.logOut()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
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

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun load() {
        binding.progressLoading.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val profile = repo.load()
            withContext(Dispatchers.Main) {
                binding.progressLoading.visibility = View.GONE
                if (profile == null) {
                    Toast.makeText(this@ProfileActivity, "Couldn't load your account", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                binding.tvName.text = profile.name.ifBlank { profile.username }
                binding.tvUsername.text = "@${profile.username}"
                binding.tvPhone.text = Phone.pretty(profile.phone)
                binding.tvSyncNote.text =
                    if (authRepo.isCloud) "Synced to your account"
                    else "Saved on this phone only"

                bindSingleton(profile.home, binding.tvHome, binding.btnClearHome)
                bindSingleton(profile.work, binding.tvWork, binding.btnClearWork)
                bindAddresses(profile.addresses)
            }
        }
    }

    private fun bindSingleton(
        place: SavedPlace?,
        label: android.widget.TextView,
        remove: com.google.android.material.button.MaterialButton
    ) {
        if (place == null) {
            label.text = "Not set — tap to add"
            remove.visibility = View.GONE
        } else {
            label.text = place.address.ifBlank { place.label }
            remove.visibility = View.VISIBLE
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
            binding.llAddresses.addView(row.root)
        }
    }

    // ── Picking a place ───────────────────────────────────────────────────────

    private fun pick(target: Target) {
        pickingFor = target
        val fields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .setCountry("IN").build(this)
        placesLauncher.launch(intent)
    }

    /**
     * A custom address is the user's own shorthand — "Gym", "Mum's place" — so
     * the label is asked for rather than taken from the map result.
     */
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
                save(SavedPlace(label = label, address = address, lat = lat, lon = lon), Target.CUSTOM)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun save(place: SavedPlace, target: Target) {
        CoroutineScope(Dispatchers.IO).launch {
            when (target) {
                Target.HOME   -> repo.setHome(place)
                Target.WORK   -> repo.setWork(place)
                Target.CUSTOM -> repo.addAddress(place)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProfileActivity, "Saved", Toast.LENGTH_SHORT).show()
                load()
            }
        }
    }

    private fun clear(target: Target) {
        CoroutineScope(Dispatchers.IO).launch {
            if (target == Target.HOME) repo.clearHome() else repo.clearWork()
            withContext(Dispatchers.Main) { load() }
        }
    }
}
