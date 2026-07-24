package io.github.tomasloksa.cgeowear.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoUriTest {

    @Test
    fun `coords in path with encoded label`() {
        val parsed = GeoUri.parse("geo:49.2308,18.746?q=Test%20Cache")!!
        assertEquals(49.2308, parsed.latitude, 1e-9)
        assertEquals(18.746, parsed.longitude, 1e-9)
        assertEquals("Test Cache", parsed.label)
    }

    @Test
    fun `coords in query when path is zero-zero`() {
        val parsed = GeoUri.parse("geo:0,0?q=49.2308,18.746(Test Cache)")!!
        assertEquals(49.2308, parsed.latitude, 1e-9)
        assertEquals(18.746, parsed.longitude, 1e-9)
        assertEquals("Test Cache", parsed.label)
    }

    @Test
    fun `path only`() {
        val parsed = GeoUri.parse("geo:49.2308,18.746")!!
        assertEquals(49.2308, parsed.latitude, 1e-9)
        assertEquals(18.746, parsed.longitude, 1e-9)
        assertEquals("", parsed.label)
    }

    @Test
    fun `path coords win over label-only query`() {
        val parsed = GeoUri.parse("geo:49.2308,18.746?q=Some%20Cache")!!
        assertEquals(49.2308, parsed.latitude, 1e-9)
        assertEquals("Some Cache", parsed.label)
    }

    @Test
    fun `non-geo uri returns null`() {
        assertNull(GeoUri.parse("https://example.com"))
    }
}
