package io.github.tomasloksa.cgeowear.common

import org.junit.Assert.assertEquals
import org.junit.Test

class NavCodecTest {

    @Test
    fun `target round-trips`() {
        val target = NavTarget(49.2308, 18.7460, "Žilina Cache", "GC1A2B3")
        val decoded = NavCodec.decodeTarget(NavCodec.encodeTarget(target))
        assertEquals(target, decoded)
    }

    @Test
    fun `tick round-trips`() {
        val tick = NavTick(123.45, 271.5f)
        val decoded = NavCodec.decodeTick(NavCodec.encodeTick(tick))
        assertEquals(tick.distanceMeters, decoded.distanceMeters, 0.0001)
        assertEquals(tick.bearingDeg, decoded.bearingDeg, 0.0001f)
    }
}
