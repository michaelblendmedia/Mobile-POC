package com.sfmc.customhtmliam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomHtmlBridgeTest {
    private val events = mutableListOf<BridgeEvent>()
    private fun bridge(id: String = "m1") = CustomHtmlBridge(id) { events += it }

    // ---- lifecycle bridge ----

    @Test fun `_display emits Displayed`() {
        bridge()._display()
        assertEquals(listOf(BridgeEvent.Displayed("m1")), events)
    }

    @Test fun `_dismiss emits Dismissed`() {
        bridge()._dismiss()
        assertEquals(listOf(BridgeEvent.Dismissed("m1")), events)
    }

    @Test fun `_click with label emits Clicked with label`() {
        bridge()._click("cta")
        assertEquals(listOf(BridgeEvent.Clicked("m1", "cta")), events)
    }

    @Test fun `_click without label emits Clicked with null label`() {
        bridge()._click()
        assertEquals(listOf(BridgeEvent.Clicked("m1", null)), events)
    }

    @Test fun `_open with url emits OpenUrl`() {
        bridge()._open("https://example.com")
        assertEquals(listOf(BridgeEvent.OpenUrl("m1", "https://example.com")), events)
    }

    @Test fun `_open with blank url is ignored`() {
        bridge()._open("")
        bridge()._open(null)
        assertTrue(events.isEmpty())
    }

    @Test fun `_track with name and attributes emits Track`() {
        bridge()._track("cta_shown", """{"variant":"b"}""")
        assertEquals(listOf(BridgeEvent.Track("m1", "cta_shown", """{"variant":"b"}""")), events)
    }

    @Test fun `_track with name only emits Track with null attributes`() {
        bridge()._track("cta_shown")
        assertEquals(listOf(BridgeEvent.Track("m1", "cta_shown", null)), events)
    }

    @Test fun `_track with blank name is ignored`() {
        bridge()._track("")
        bridge()._track(null)
        assertTrue(events.isEmpty())
    }

    @Test fun `_log does not emit an event`() {
        bridge()._log("hello")
        assertTrue(events.isEmpty())
    }

    // ---- terminal classification ----

    @Test fun `click and dismiss are terminal`() {
        assertTrue(BridgeEvent.Clicked("m1", null).terminal)
        assertTrue(BridgeEvent.Dismissed("m1").terminal)
    }

    @Test fun `display track and open are not terminal`() {
        assertTrue(!BridgeEvent.Displayed("m1").terminal)
        assertTrue(!BridgeEvent.Track("m1", "e", null).terminal)
        assertTrue(!BridgeEvent.OpenUrl("m1", "https://x").terminal)
    }
}
