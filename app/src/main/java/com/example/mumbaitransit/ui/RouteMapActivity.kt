package com.example.mumbaitransit.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mumbaitransit.R
import com.example.mumbaitransit.databinding.ActivityRouteMapBinding
import com.example.mumbaitransit.databinding.ItemMapLegBinding
import com.example.mumbaitransit.map.DirectionsClient
import com.example.mumbaitransit.map.RouteShape
import com.example.mumbaitransit.model.RouteCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The map for one chosen route.
 *
 * This replaces the old Route Preview screen, which drew a single dashed line
 * from origin to destination before any route had been picked — a shape no
 * commuter could act on. Here the map is opened from a specific card and draws
 * that journey: every ride in its own line colour, stop by stop, with the walks
 * and interchanges dashed between them.
 *
 * The card is handed over through the shared ViewModel rather than the intent,
 * as [RouteCard] carries the whole path and would not survive a parcel cleanly.
 */
class RouteMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRouteMapBinding
    private val vm: TransitViewModel by lazy { application.getSharedViewModel() }

    private var card: RouteCard? = null
    private var plan: RouteShape.Plan? = null
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val routeCard = vm.mapCard
        if (routeCard == null) { finish(); return }
        card = routeCard

        plan = RouteShape.build(
            card = routeCard,
            originLat = vm.searchOriginLat, originLon = vm.searchOriginLon,
            destLat = vm.searchDestLat, destLon = vm.searchDestLon,
            busEngine = if (vm.isEngineReady) vm.engine.busEngine else null
        )

        bindSummary(routeCard)
        bindLegend()

        binding.btnOpenInMaps.setOnClickListener { openInGoogleMaps() }
        // Worth offering wherever there is a road route to hand over — the two
        // road cards, where Google can navigate the whole journey turn by turn.
        binding.btnOpenInMaps.visibility =
            if (plan?.needsRoadPath == true) View.VISIBLE else View.GONE

        (supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment)
            .getMapAsync(this)
    }

    /**
     * Replaces the placeholder straight line on an auto or cab card with the
     * real road geometry.
     *
     * Runs only for those two cards. Everything else is drawn from the bundled
     * stop coordinates and needs no network at all, so a route list works
     * offline exactly as it did before.
     */
    private fun fetchRoadPath(map: GoogleMap) {
        val routeCard = card ?: return
        val currentPlan = plan ?: return
        val origin = currentPlan.origin ?: return
        val dest = currentPlan.destination ?: return

        binding.tvMapNotice.visibility = View.VISIBLE
        binding.tvMapNotice.text = "Finding the road route…"

        lifecycleScope.launch {
            val result = DirectionsClient.drivingRoute(
                origin.latitude, origin.longitude, dest.latitude, dest.longitude
            )
            if (isFinishing || isDestroyed) return@launch

            when (result) {
                is DirectionsClient.Result.Success -> {
                    val segment = RouteShape.roadSegment(
                        routeCard, result.points, result.distanceKm,
                        result.minutes, result.summary
                    )
                    plan = currentPlan.copy(segments = listOf(segment), needsRoadPath = false)
                    binding.tvMapNotice.visibility = View.GONE
                    bindLegend()
                    redraw(map)
                }
                is DirectionsClient.Result.Failure -> {
                    // Keep the dashed placeholder but never let it pass for a
                    // real path — say plainly that it is not one.
                    binding.tvMapNotice.text =
                        "Showing a direct line — road route unavailable (${result.reason})"
                }
            }
        }
    }

    /** Opens the same origin and destination in Google Maps for navigation. */
    private fun openInGoogleMaps() {
        val currentPlan = plan ?: return
        val origin = currentPlan.origin ?: return
        val dest = currentPlan.destination ?: return
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&travelmode=driving"
        )
        // resolveActivity needs a <queries> entry from Android 11 on, so just
        // try the intent and report a miss rather than pre-checking for one.
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "No app available to open maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSummary(routeCard: RouteCard) {
        supportActionBar?.title = routeCard.scenarioLabel.ifBlank { "Route map" }
        binding.tvRouteTitle.text = routeCard.modeStr
        binding.tvRouteSummary.text =
            "${routeCard.totalMin.roundToInt()} min  ·  ₹${routeCard.totalFare}"
        binding.tvEndpoints.text = buildString {
            append(routeCard.originLabel.ifBlank { "Start" })
            append("  →  ")
            append(routeCard.destLabel.ifBlank { "Destination" })
        }
    }

    private fun bindLegend() {
        val segments = plan?.segments.orEmpty()
        binding.llLegend.removeAllViews()
        for (seg in segments) {
            val row = ItemMapLegBinding.inflate(
                LayoutInflater.from(this), binding.llLegend, false
            )
            row.viewSwatch.setBackgroundColor(Color.parseColor(seg.colorHex))
            // Walks and interchanges read as secondary, matching the dashed line.
            row.viewSwatch.alpha = if (seg.dashed) 0.55f else 1f
            row.tvLegLabel.text = seg.label
            row.tvLegDetail.text = seg.detail
            row.tvLegMinutes.text = if (seg.minutes > 0) "${seg.minutes} min" else ""
            binding.llLegend.addView(row.root)
        }

        // Five legs is about where the panel starts eating the map. Past that,
        // cap it and let the legend scroll inside its own box.
        val maxDp = 170
        val capped = segments.size > 5
        binding.svLegend.layoutParams = binding.svLegend.layoutParams.apply {
            height = if (capped) (maxDp * resources.displayMetrics.density).roundToInt()
                     else ViewGroup.LayoutParams.WRAP_CONTENT
        }
        binding.svLegend.requestLayout()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val currentPlan = plan
        if (currentPlan == null || currentPlan.isEmpty) {
            binding.tvMapEmpty.visibility = View.VISIBLE
            return
        }

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        redraw(map)
        if (currentPlan.needsRoadPath) fetchRoadPath(map)
    }

    /** Clears and repaints every segment. Safe to call again after a redraw. */
    private fun redraw(map: GoogleMap) {
        val currentPlan = plan ?: return
        map.clear()

        for (seg in currentPlan.segments) {
            if (seg.points.size < 2) continue
            val options = PolylineOptions()
                .addAll(seg.points)
                .color(Color.parseColor(seg.colorHex))
                .width(if (seg.dashed) 8f else 12f)
                .geodesic(false)
            if (seg.dashed) {
                options.pattern(listOf(Dot(), Gap(10f), Dash(14f), Gap(10f)))
            }
            map.addPolyline(options)

            // Interchange and intermediate stations, in the leg's own colour.
            for ((index, stop) in seg.stops.withIndex()) {
                val terminal = index == 0 || index == seg.stops.lastIndex
                map.addMarker(
                    MarkerOptions()
                        .position(stop.position)
                        .title(stop.name)
                        .snippet(seg.label)
                        .icon(stationDot(seg.colorHex, terminal))
                        .anchor(0.5f, 0.5f)
                        .zIndex(if (terminal) 2f else 1f)
                )
            }
        }

        currentPlan.origin?.let {
            map.addMarker(
                MarkerOptions().position(it)
                    .title(currentPlan.originLabel)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .zIndex(3f)
            )
        }
        currentPlan.destination?.let {
            map.addMarker(
                MarkerOptions().position(it)
                    .title(currentPlan.destLabel)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .zIndex(3f)
            )
        }

        val bounds = LatLngBounds.Builder().apply {
            currentPlan.allPoints().forEach { include(it) }
        }.build()

        // Camera has to wait for layout or newLatLngBounds throws on a zero-size map.
        supportFragmentManager.findFragmentById(R.id.mapFragment)?.view?.post {
            map.setPadding(48, 96, 48, 48)
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 90))
        }
    }

    /**
     * A small filled circle for a station, drawn rather than shipped as a
     * drawable so it can take the colour of whichever line it sits on.
     */
    private fun stationDot(colorHex: String, terminal: Boolean): BitmapDescriptor {
        val density = resources.displayMetrics.density
        val sizeDp = if (terminal) 15f else 10f
        val size = (sizeDp * density).roundToInt().coerceAtLeast(6)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = size / 2f

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.STROKE
            strokeWidth = (if (terminal) 4f else 3f) * density / 2f
        }
        canvas.drawCircle(radius, radius, radius - ring.strokeWidth / 2f, fill)
        canvas.drawCircle(radius, radius, radius - ring.strokeWidth / 2f, ring)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }

    companion object {
        /** Opens the map for [card]; the card travels via the shared ViewModel. */
        fun start(context: Context, card: RouteCard) {
            (context.applicationContext as android.app.Application)
                .getSharedViewModel().mapCard = card
            context.startActivity(Intent(context, RouteMapActivity::class.java))
        }
    }
}
