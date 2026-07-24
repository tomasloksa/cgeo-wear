package io.github.tomasloksa.cgeowear.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import io.github.tomasloksa.cgeowear.common.GeoMath
import io.github.tomasloksa.cgeowear.common.NavCodec
import io.github.tomasloksa.cgeowear.common.NavTarget
import io.github.tomasloksa.cgeowear.common.NavTick
import io.github.tomasloksa.cgeowear.common.WearPaths

/** Foreground service that owns GPS and streams the target plus distance/bearing ticks to the watch. */
class BridgeService : Service() {

    private lateinit var fusedLocation: FusedLocationProviderClient
    private var target: NavTarget? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            pushTick(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lat = intent?.getDoubleExtra(EXTRA_LAT, Double.NaN) ?: Double.NaN
        val lon = intent?.getDoubleExtra(EXTRA_LON, Double.NaN) ?: Double.NaN
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { "Cache" }
        val navTarget = NavTarget(lat, lon, name, "")
        target = navTarget

        startForegroundNotification(navTarget)

        if (lat.isNaN() || lon.isNaN()) {
            Log.w(TAG, "missing coordinates, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "target: ${navTarget.name} @ ${navTarget.latitude},${navTarget.longitude}")
        publishTarget(navTarget)
        requestLocation()
        return START_REDELIVER_INTENT
    }

    private fun publishTarget(navTarget: NavTarget) {
        val request = PutDataRequest.create(WearPaths.TARGET)
        request.data = NavCodec.encodeTarget(navTarget)
        request.setUrgent()
        Wearable.getDataClient(this).putDataItem(request)
            .addOnSuccessListener { Log.d(TAG, "target published to Data Layer") }
            .addOnFailureListener { Log.w(TAG, "failed to publish target", it) }
    }

    private fun requestLocation() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TICK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(TICK_INTERVAL_MS)
            .build()
        try {
            Log.d(TAG, "requesting location updates every ${TICK_INTERVAL_MS}ms")
            fusedLocation.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "missing location permission", e)
            stopSelf()
        }
    }

    private fun pushTick(location: Location) {
        val navTarget = target ?: return
        val distance = GeoMath.distanceMeters(
            location.latitude, location.longitude, navTarget.latitude, navTarget.longitude,
        )
        val bearing = GeoMath.bearingDeg(
            location.latitude, location.longitude, navTarget.latitude, navTarget.longitude,
        )
        val payload = NavCodec.encodeTick(NavTick(distance, bearing))
        val messageClient = Wearable.getMessageClient(this)
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(TAG, "no connected nodes - is a watch paired?")
                }
                for (node in nodes) {
                    Log.d(TAG, "tick -> ${node.displayName}: ${distance.toInt()}m ${bearing.toInt()}deg")
                    messageClient.sendMessage(node.id, WearPaths.TICK, payload)
                }
            }
            .addOnFailureListener { Log.w(TAG, "connectedNodes failed", it) }
    }

    private fun startForegroundNotification(navTarget: NavTarget) {
        val notification = buildNotification(navTarget)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(navTarget: NavTarget): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Navigating on watch")
            .setContentText(navTarget.name)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Navigation", NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        fusedLocation.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_NAME = "name"

        private const val TAG = "BridgeService"
        private const val CHANNEL_ID = "navigation"
        private const val NOTIFICATION_ID = 1
        private const val TICK_INTERVAL_MS = 1_000L
    }
}
