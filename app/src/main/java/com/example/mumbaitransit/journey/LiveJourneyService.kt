package com.example.mumbaitransit.journey

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.mumbaitransit.R
import com.example.mumbaitransit.navigation.NavigationSession
import com.example.mumbaitransit.ui.LiveJourneyActivity
import com.example.mumbaitransit.ui.NavigationActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The one place the app tracks location during a journey.
 *
 * Two independent features ride on it:
 *
 *  - **Live sharing** — someone else follows the rider. [JourneyTracker] turns
 *    fixes into pings that are published for the follower.
 *  - **Live navigation** — the rider is guided leg by leg.
 *    [NavigationSession] turns the same fixes into the next instruction.
 *
 * Either can run alone or both together; the service keeps a single fused
 * location subscription and stays up while at least one of them is active.
 * Stopping one never stops the other.
 *
 * This has to be a foreground service: a backgrounded activity gets its
 * location updates throttled to nothing within minutes, which would strand a
 * follower on a stale position and leave a rider on a train with no idea where
 * to get off. The ongoing notification is the honest disclosure that location
 * is in use, and the one-tap way to stop.
 */
class LiveJourneyService : Service() {

    companion object {
        private const val TAG = "AnchorJourneySvc"

        const val ACTION_START = "com.example.mumbaitransit.journey.START"
        const val ACTION_STOP = "com.example.mumbaitransit.journey.STOP"
        const val ACTION_RELOAD_LEGS = "com.example.mumbaitransit.journey.RELOAD_LEGS"
        const val ACTION_NAVIGATE = "com.example.mumbaitransit.journey.NAVIGATE"
        const val ACTION_STOP_NAVIGATION = "com.example.mumbaitransit.journey.STOP_NAVIGATION"

        private const val CHANNEL_ID = "anchor_live_journey"
        private const val NOTIFICATION_ID = 4801

        /** Sharing alone: fast enough to catch a station pass. */
        private const val SHARE_INTERVAL_MS = 10_000L
        private const val SHARE_FASTEST_MS = 5_000L
        private const val SHARE_MIN_METRES = 25f

        /** Navigating: a walker needs turns, not just stations. */
        private const val NAV_INTERVAL_MS = 5_000L
        private const val NAV_FASTEST_MS = 2_000L
        private const val NAV_MIN_METRES = 8f

        /** How often a sharing ping is published, fix or no fix. */
        private const val PUBLISH_INTERVAL_MS = 15_000L

        /** How often navigation gets a clock tick (timetable projection, ETA). */
        private const val NAV_TICK_MS = 5_000L

        /** Starts live sharing for the journey already in [JourneySession]. */
        fun start(context: Context) {
            val intent = Intent(context, LiveJourneyService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stops live sharing. Navigation, if running, carries on. */
        fun stop(context: Context) {
            context.startService(
                Intent(context, LiveJourneyService::class.java).setAction(ACTION_STOP)
            )
        }

        /** Tells the running service the rider re-picked a service on some leg. */
        fun reloadLegs(context: Context) {
            if (!JourneySession.isSharing) return
            context.startService(
                Intent(context, LiveJourneyService::class.java).setAction(ACTION_RELOAD_LEGS)
            )
        }

        /** Starts feeding [NavigationSession]. Sharing, if running, carries on. */
        fun startNavigation(context: Context) {
            val intent = Intent(context, LiveJourneyService::class.java).setAction(ACTION_NAVIGATE)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stops navigation tracking. Sharing, if running, carries on. */
        fun stopNavigation(context: Context) {
            context.startService(
                Intent(context, LiveJourneyService::class.java).setAction(ACTION_STOP_NAVIGATION)
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repo: JourneyRepository
    private lateinit var fused: FusedLocationProviderClient

    // Sharing
    private var tracker: JourneyTracker? = null
    private var shareLoop: Job? = null
    private var sharingStopping = false
    private var lastPing: JourneyPing? = null

    // Navigation
    private var navigating = false
    private var navLoop: Job? = null

    // Location
    private var locationMode: Boolean? = null   // null = not subscribed, true = nav rate, false = share rate

    private val clock = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val fix: Location = result.lastLocation ?: return
            val now = System.currentTimeMillis()
            tracker?.onFix(fix.latitude, fix.longitude, now)
            if (navigating) {
                NavigationSession.onFix(fix.latitude, fix.longitude, fix.accuracy, now)
                afterNavigationUpdate()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = JourneyRepository.get(this)
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSharing(JourneyStatus.STOPPED); return START_NOT_STICKY }
            ACTION_STOP_NAVIGATION -> { stopNavigationTracking(); return START_NOT_STICKY }
            ACTION_RELOAD_LEGS -> {
                // Re-pick keeps position and progress; only the timings change.
                JourneySession.journey.value?.let { tracker = tracker?.replaceJourneyLegs(it) }
                return START_STICKY
            }
            ACTION_NAVIGATE -> {
                // startForegroundService was used: a notification must go up now.
                startForeground(NOTIFICATION_ID, buildNotification())
                if (!NavigationSession.isActive) { stopIfIdle(); return START_NOT_STICKY }
                if (!navigating) {
                    navigating = true
                    updateLocationRequest()
                    startNavLoop()
                }
                return START_STICKY
            }
        }

        // ACTION_START — live sharing.
        startForeground(NOTIFICATION_ID, buildNotification())
        val journey = JourneySession.journey.value
        if (journey == null) {
            // Most likely a system restart after the journey had already ended.
            stopIfIdle()
            return START_NOT_STICKY
        }
        if (tracker == null) {
            sharingStopping = false
            tracker = JourneyTracker(journey).also { t ->
                // Sharing started from the navigation screen: begin where the rider is.
                val nav = NavigationSession.snapshot.value
                if (nav != null && !nav.ended && nav.plan.code == journey.code) t.startAt(nav.legIndex)
            }
            updateLocationRequest()
            startShareLoop(journey)
        }
        return START_STICKY
    }

    // ── Location ──────────────────────────────────────────────────────────────

    /** One subscription; its rate follows whichever feature needs more. */
    private fun updateLocationRequest() {
        val wantNavRate: Boolean? = when {
            navigating -> true
            tracker != null -> false
            else -> null
        }
        if (wantNavRate == locationMode) return
        runCatching { fused.removeLocationUpdates(locationCallback) }
        locationMode = null
        if (wantNavRate == null) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "No location permission — running on the timetable and taps only")
            return
        }
        val request = if (wantNavRate) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, NAV_INTERVAL_MS)
                .setMinUpdateIntervalMillis(NAV_FASTEST_MS)
                .setMinUpdateDistanceMeters(NAV_MIN_METRES)
                .build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, SHARE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(SHARE_FASTEST_MS)
                .setMinUpdateDistanceMeters(SHARE_MIN_METRES)
                .build()
        }
        try {
            fused.requestLocationUpdates(request, locationCallback, mainLooper)
            locationMode = wantNavRate
        } catch (e: SecurityException) {
            Log.w(TAG, "Location refused at request time", e)
        }
    }

    // ── Sharing ───────────────────────────────────────────────────────────────

    private fun startShareLoop(journey: LiveJourney) {
        shareLoop = scope.launch {
            runCatching { repo.start(journey) }
                .onFailure { Log.w(TAG, "Could not register journey ${journey.code}", it) }

            while (isActive && !sharingStopping) {
                val now = System.currentTimeMillis()
                val t = tracker ?: break

                // onNoFix only moves a leg that has a locked service, and only
                // forward — this is what carries a metro leg underground.
                t.onNoFix(now)

                val ping = t.ping(now)
                lastPing = ping
                JourneySession.publish(ping)
                runCatching { repo.publish(journey.code, ping) }
                    .onFailure { Log.w(TAG, "Ping not delivered: ${it.message}") }

                refreshNotification()

                if (t.isFinished()) { stopSharing(JourneyStatus.COMPLETED); return@launch }
                if (now - journey.startedAt > JourneyTracker.MAX_DURATION_MS) {
                    stopSharing(JourneyStatus.STOPPED); return@launch
                }
                delay(PUBLISH_INTERVAL_MS)
            }
        }
    }

    private fun stopSharing(status: String) {
        if (tracker == null || sharingStopping) { stopIfIdle(); return }
        sharingStopping = true

        val code = JourneySession.activeCode
        val finalPing = tracker?.ping(System.currentTimeMillis(), status)
        if (finalPing != null) JourneySession.publish(finalPing)

        // The follower must be told the journey ended, so this outlives the
        // service scope — cancelling here would abort the very write that
        // closes the loop for them.
        CoroutineScope(Dispatchers.IO).launch {
            if (code != null) {
                runCatching { finalPing?.let { repo.publish(code, it) } }
                runCatching { repo.end(code, status) }
                    .onFailure { Log.w(TAG, "Could not mark $code $status", it) }
            }
        }

        shareLoop?.cancel()
        shareLoop = null
        tracker = null
        lastPing = null
        JourneySession.clear()

        updateLocationRequest()
        if (!stopIfIdle()) refreshNotification()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun startNavLoop() {
        navLoop?.cancel()
        navLoop = scope.launch {
            val startedAt = System.currentTimeMillis()
            while (isActive && navigating) {
                NavigationSession.onTick()
                afterNavigationUpdate()
                if (System.currentTimeMillis() - startedAt > JourneyTracker.MAX_DURATION_MS) {
                    NavigationSession.stop()
                    kotlinx.coroutines.withContext(Dispatchers.Main) { stopNavigationTracking() }
                    return@launch
                }
                delay(NAV_TICK_MS)
            }
        }
    }

    private var lastNavText: String? = null

    /** Arrival, and the notification text, after any navigation change. */
    private fun afterNavigationUpdate() {
        val snap = NavigationSession.snapshot.value
        if (snap == null || snap.ended) {
            scope.launch(Dispatchers.Main) {
                // Arrived: if the same journey is being shared, sharing ends
                // with it, exactly as it would have on its own tracker.
                if (snap?.state == com.example.mumbaitransit.navigation.NavState.ARRIVED &&
                    JourneySession.activeCode != null &&
                    JourneySession.activeCode == snap.plan.code
                ) stopSharing(JourneyStatus.COMPLETED)
                stopNavigationTracking()
            }
            return
        }
        val text = "${snap.headline}|${snap.detail}"
        if (text != lastNavText) {
            lastNavText = text
            refreshNotification()
        }
    }

    /** Stops GPS for navigation. The last snapshot stays for the screen to show. */
    private fun stopNavigationTracking() {
        if (!navigating) { stopIfIdle(); return }
        navigating = false
        navLoop?.cancel()
        navLoop = null
        lastNavText = null
        if (NavigationSession.isActive) NavigationSession.stop()
        updateLocationRequest()
        if (!stopIfIdle()) refreshNotification()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Tears the service down when neither feature needs it. True if it did. */
    private fun stopIfIdle(): Boolean {
        if (tracker != null || navigating) return false
        runCatching { fused.removeLocationUpdates(locationCallback) }
        locationMode = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return true
    }

    override fun onDestroy() {
        runCatching { fused.removeLocationUpdates(locationCallback) }
        scope.cancel()
        super.onDestroy()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live journey",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while Anchor is navigating your journey or sharing it live."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val sharing = tracker != null && !sharingStopping
        val nav = if (navigating) NavigationSession.snapshot.value else null
        val journey = JourneySession.journey.value

        val openTarget = if (nav != null) NavigationActivity::class.java else LiveJourneyActivity::class.java
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, openTarget).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            nav != null -> "Navigating to ${nav.plan.destLabel.ifBlank { "your destination" }}" +
                if (sharing) " · sharing live" else ""
            journey != null && journey.destLabel.isNotBlank() -> "Sharing journey to ${journey.destLabel}"
            else -> "Live journey"
        }

        val body = when {
            nav != null -> listOf(nav.headline, nav.detail).filter { it.isNotBlank() }.joinToString(" · ")
            sharing -> lastPing?.let { ping ->
                val leg = journey?.legAt(ping.legIndex)
                val where = ping.nearestStation.ifBlank { leg?.fromStop.orEmpty() }
                buildString {
                    if (where.isNotBlank()) append("At $where · ")
                    append("${ping.progressPct}% · arriving ~${clock.format(Date(ping.etaEpochMs))}")
                }
            } ?: "Starting…"
            else -> "Starting…"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_anchor)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)

        if (nav != null) {
            val stopNav = PendingIntent.getService(
                this, 2,
                Intent(this, LiveJourneyService::class.java).setAction(ACTION_STOP_NAVIGATION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Stop navigation", stopNav)
        }
        if (sharing) {
            val stopShare = PendingIntent.getService(
                this, 1,
                Intent(this, LiveJourneyService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Stop sharing", stopShare)
        }
        return builder.build()
    }

    private fun refreshNotification() {
        if (tracker == null && !navigating) return
        runCatching {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildNotification())
        }
    }
}
