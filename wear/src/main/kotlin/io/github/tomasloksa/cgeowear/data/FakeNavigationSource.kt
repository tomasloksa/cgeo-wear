package io.github.tomasloksa.cgeowear.data

import io.github.tomasloksa.cgeowear.common.GeoMath
import io.github.tomasloksa.cgeowear.common.NavState
import io.github.tomasloksa.cgeowear.common.NavTarget
import io.github.tomasloksa.cgeowear.common.NavTick
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Simulates a walk towards a hardcoded cache at ~1.2 m/s with a slight
 * wander, ticking at 1 Hz — the same rate the phone bridge will use.
 * On arrival (< 3 m) it stops and holds the last state.
 */
class FakeNavigationSource(
    startLat: Double = 49.2231,
    startLon: Double = 18.7394,
) : NavigationSource {

    private val target = NavTarget(
        latitude = 49.2308,
        longitude = 18.7460,
        name = "Test Cache",
        geocode = "GC1A2B3",
    )

    private var lat = startLat
    private var lon = startLon
    private var step = 0

    override val state: Flow<NavState> = flow {
        while (true) {
            val distance = GeoMath.distanceMeters(lat, lon, target.latitude, target.longitude)
            val bearing = GeoMath.bearingDeg(lat, lon, target.latitude, target.longitude)
            emit(NavState(target, NavTick(distance, bearing)))

            if (distance > ARRIVED_M) {
                // Walk towards the cache, weaving +-25 deg like a real trail.
                val wander = 25.0 * sin(step / 7.0)
                val stride = minOf(WALK_SPEED_MPS, distance)
                val (newLat, newLon) = GeoMath.move(lat, lon, bearing + wander, stride)
                lat = newLat
                lon = newLon
                step++
            }
            delay(1_000.milliseconds)
        }
    }

    private companion object {
        const val WALK_SPEED_MPS = 1.2
        const val ARRIVED_M = 3.0
    }
}
