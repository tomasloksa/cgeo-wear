package io.github.tomasloksa.cgeowear.common

/** The cache being navigated to. Authoritative, sent once per navigation session. */
data class NavTarget(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val geocode: String,
)

/**
 * One live navigation update.
 *
 * @param distanceMeters great-circle distance from the user to the target
 * @param bearingDeg true bearing from the user to the target, 0..360
 */
data class NavTick(
    val distanceMeters: Double,
    val bearingDeg: Float,
)

data class NavState(
    val target: NavTarget,
    val tick: NavTick,
)

/**
 * Data Layer contract between the phone bridge and the watch (M2).
 * Target goes over DataClient (durable, replays on reconnect);
 * ticks go over MessageClient (fire-and-forget, throttled to <= 1 Hz).
 */
object WearPaths {
    const val TARGET = "/cgeo/target"
    const val TICK = "/cgeo/tick"
    const val CAPABILITY_WEAR_APP = "cgeo_wear_compass"
}
