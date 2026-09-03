package com.example.mumbaitransit.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mumbaitransit.databinding.ActivityRoutePreviewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlin.math.*

class RoutePreviewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRoutePreviewBinding
    private lateinit var googleMap: GoogleMap

    // Passed in via intent
    private var originLat  = 0.0
    private var originLon  = 0.0
    private var destLat    = 0.0
    private var destLon    = 0.0
    private var originLabel = ""
    private var destLabel   = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        originLat   = intent.getDoubleExtra(EXTRA_ORIGIN_LAT,  0.0)
        originLon   = intent.getDoubleExtra(EXTRA_ORIGIN_LON,  0.0)
        destLat     = intent.getDoubleExtra(EXTRA_DEST_LAT,    0.0)
        destLon     = intent.getDoubleExtra(EXTRA_DEST_LON,    0.0)
        originLabel = intent.getStringExtra(EXTRA_ORIGIN_LABEL) ?: "Origin"
        destLabel   = intent.getStringExtra(EXTRA_DEST_LABEL)   ?: "Destination"

        binding.tvFrom.text = originLabel
        binding.tvTo.text   = destLabel

        // Accurate haversine distance
        val distKm = haversineKm(originLat, originLon, destLat, destLon)
        binding.tvDistance.text = if (distKm < 1.0) "%.0f m".format(distKm * 1000)
                                  else "%.1f km".format(distKm)
        binding.tvDistanceSub.text = "straight-line distance"

        val mapFragment = supportFragmentManager
            .findFragmentById(com.example.mumbaitransit.R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnViewOptions.setOnClickListener {
            startActivity(Intent(this, RoutesResultActivity::class.java))
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled   = false

        val origin = LatLng(originLat, originLon)
        val dest   = LatLng(destLat, destLon)

        // Origin marker — red pin
        googleMap.addMarker(
            MarkerOptions()
                .position(origin)
                .title(originLabel)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        // Destination marker — red pin
        googleMap.addMarker(
            MarkerOptions()
                .position(dest)
                .title(destLabel)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        // Curved polyline approximating a road path between the two points
        val polylinePoints = buildCurvedPath(origin, dest, steps = 40)
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(polylinePoints)
                .color(Color.parseColor("#1A56DB"))
                .width(6f)
                .geodesic(true)
                .pattern(listOf(Dash(20f), Gap(10f)))
        )

        // Fit both markers in view with padding for the bottom card
        val bounds = LatLngBounds.Builder()
            .include(origin)
            .include(dest)
            .build()
        // Wait for map layout before moving camera
        val mapFragment = supportFragmentManager
            .findFragmentById(com.example.mumbaitransit.R.id.mapFragment)
        mapFragment?.view?.post {
            val bottomPadding = binding.bottomCard.height + 40
            googleMap.setPadding(80, 160, 80, bottomPadding)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    /**
     * Generates a smooth arc path between two LatLng points.
     * Adds a slight perpendicular offset midway to make it look like a road curve.
     */
    private fun buildCurvedPath(start: LatLng, end: LatLng, steps: Int): List<LatLng> {
        val points = mutableListOf<LatLng>()

        val midLat = (start.latitude  + end.latitude)  / 2
        val midLon = (start.longitude + end.longitude) / 2

        // Perpendicular offset — subtle curve, scaled to distance
        val distKm = haversineKm(start.latitude, start.longitude, end.latitude, end.longitude)
        val offsetDegrees = (distKm / 111.0) * 0.08  // ~8% of distance as curve offset

        val dLat = end.latitude  - start.latitude
        val dLon = end.longitude - start.longitude
        // Perpendicular direction
        val perpLat = -dLon
        val perpLon =  dLat
        val perpLen = sqrt(perpLat * perpLat + perpLon * perpLon).coerceAtLeast(1e-10)

        val ctrlLat = midLat + (perpLat / perpLen) * offsetDegrees
        val ctrlLon = midLon + (perpLon / perpLen) * offsetDegrees

        // Quadratic Bezier: B(t) = (1-t)²·P0 + 2(1-t)t·P1 + t²·P2
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val lat = (1 - t).pow(2) * start.latitude  + 2 * (1 - t) * t * ctrlLat + t.pow(2) * end.latitude
            val lon = (1 - t).pow(2) * start.longitude + 2 * (1 - t) * t * ctrlLon + t.pow(2) * end.longitude
            points.add(LatLng(lat, lon))
        }
        return points
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }

    companion object {
        const val EXTRA_ORIGIN_LAT   = "origin_lat"
        const val EXTRA_ORIGIN_LON   = "origin_lon"
        const val EXTRA_DEST_LAT     = "dest_lat"
        const val EXTRA_DEST_LON     = "dest_lon"
        const val EXTRA_ORIGIN_LABEL = "origin_label"
        const val EXTRA_DEST_LABEL   = "dest_label"
    }
}
