package io.github.tomasloksa.cgeowear.bridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import io.github.tomasloksa.cgeowear.common.GeoUri

/** Catches c:geo's `geo:` navigation intent, ensures location permission, and starts the bridge service. */
class NavigateActivity : Activity() {

    private var pending: GeoUri.Parsed? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parsed = intent?.dataString?.let { GeoUri.parse(it) }
        if (parsed == null) {
            Log.w(TAG, "could not parse geo uri: ${intent?.dataString}")
            Toast.makeText(this, "No coordinates in navigation request", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        pending = parsed
        if (hasLocationPermission()) {
            startBridge(parsed)
            finish()
        } else {
            requestPermissions(requiredPermissions(), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val target = pending
        if (requestCode == REQUEST_CODE && target != null && hasLocationPermission()) {
            startBridge(target)
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun startBridge(parsed: GeoUri.Parsed) {
        val intent = Intent(this, BridgeService::class.java).apply {
            putExtra(BridgeService.EXTRA_LAT, parsed.latitude)
            putExtra(BridgeService.EXTRA_LON, parsed.longitude)
            putExtra(BridgeService.EXTRA_NAME, parsed.label)
        }
        startForegroundService(intent)
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }

    private companion object {
        const val TAG = "NavigateActivity"
        const val REQUEST_CODE = 42
    }
}
