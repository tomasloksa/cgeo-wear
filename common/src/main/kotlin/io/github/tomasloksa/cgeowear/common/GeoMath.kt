package io.github.tomasloksa.cgeowear.common

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Great-circle distance and bearing on a spherical Earth. Accuracy is more
 * than enough for geocaching distances; c:geo itself uses the same model
 * for its compass.
 */
object GeoMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Haversine distance in meters between two WGS84 coordinates. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    /** Initial true bearing in degrees (0..360) from point 1 towards point 2. */
    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (((deg % 360) + 360) % 360).toFloat()
    }

    /** Destination point after moving [distanceMeters] on [bearingDeg] from the start. */
    fun move(lat: Double, lon: Double, bearingDeg: Double, distanceMeters: Double): Pair<Double, Double> {
        val delta = distanceMeters / EARTH_RADIUS_M
        val theta = Math.toRadians(bearingDeg)
        val phi1 = Math.toRadians(lat)
        val lambda1 = Math.toRadians(lon)
        val phi2 = asin(sin(phi1) * cos(delta) + cos(phi1) * sin(delta) * cos(theta))
        val lambda2 = lambda1 + atan2(
            sin(theta) * sin(delta) * cos(phi1),
            cos(delta) - sin(phi1) * sin(phi2),
        )
        return Math.toDegrees(phi2) to Math.toDegrees(lambda2)
    }
}
