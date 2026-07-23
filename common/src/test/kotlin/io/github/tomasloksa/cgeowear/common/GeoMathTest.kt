package io.github.tomasloksa.cgeowear.common

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    // Bratislava castle -> Devin castle: ~8.2 km almost due west.
    private val bratislava = 48.1419 to 17.1004
    private val devin = 48.1736 to 16.9784

    @Test
    fun `distance Bratislava to Devin is about 9,7 km`() {
        val d = GeoMath.distanceMeters(bratislava.first, bratislava.second, devin.first, devin.second)
        assertEquals(9_750.0, d, 300.0)
    }

    @Test
    fun `bearing Bratislava to Devin is roughly west-northwest`() {
        val b = GeoMath.bearingDeg(bratislava.first, bratislava.second, devin.first, devin.second)
        assertEquals(291.0, b.toDouble(), 5.0)
    }

    @Test
    fun `zero distance to self`() {
        val d = GeoMath.distanceMeters(48.0, 17.0, 48.0, 17.0)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun `bearing due north`() {
        val b = GeoMath.bearingDeg(48.0, 17.0, 49.0, 17.0)
        assertEquals(0.0, b.toDouble(), 0.01)
    }

    @Test
    fun `move then measure round-trips`() {
        val (lat, lon) = GeoMath.move(48.0, 17.0, 45.0, 500.0)
        val d = GeoMath.distanceMeters(48.0, 17.0, lat, lon)
        val b = GeoMath.bearingDeg(48.0, 17.0, lat, lon)
        assertEquals(500.0, d, 0.5)
        assertEquals(45.0, b.toDouble(), 0.1)
    }
}
