package com.sfmc.customhtmliam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayStateTest {
    private var clock = 0L
    private fun state(ttl: Long = 30_000) = OverlayState(ttl) { clock }

    @Test fun `first reserve succeeds, second fails until release`() {
        val s = state()
        assertTrue(s.reserve())
        assertFalse(s.reserve())
        s.release()
        assertTrue(s.reserve())
    }

    @Test fun `reserve fails while live`() {
        val s = state()
        s.reserve(); s.markLive("m1")
        assertFalse(s.reserve())
        s.release()
        assertTrue(s.reserve())
    }

    @Test fun `takeFresh returns pending within ttl`() {
        val s = state(ttl = 1000)
        s.enqueue("m1", "<b/>")
        clock = 999
        assertEquals(OverlayState.Pending("m1", "<b/>", 0), s.takeFresh())
    }

    @Test fun `takeFresh drops pending past ttl`() {
        val s = state(ttl = 1000)
        s.enqueue("m1", "<b/>")
        clock = 1001
        assertNull(s.takeFresh())
    }

    @Test fun `takeFresh clears pending after read`() {
        val s = state()
        s.enqueue("m1", "<b/>")
        assertEquals("m1", s.takeFresh()?.id)
        assertNull(s.takeFresh())
    }
}
