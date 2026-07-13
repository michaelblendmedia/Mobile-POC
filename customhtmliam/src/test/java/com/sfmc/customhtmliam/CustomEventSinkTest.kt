package com.sfmc.customhtmliam

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomEventSinkTest {
    @Test fun `custom sink injects messageId and merges flat json attributes`() {
        var captured: Pair<String, Map<String, String>>? = null
        val sink = MarketingCloudCustomEventSink { name, attrs -> captured = name to attrs }

        sink.track("m1", "cta_shown", """{"variant":"b","position":"2"}""")

        assertEquals("cta_shown", captured!!.first)
        assertEquals("m1", captured!!.second["messageId"])
        assertEquals("b", captured!!.second["variant"])
        assertEquals("2", captured!!.second["position"])
    }

    @Test fun `custom sink sends only messageId when attributes are null`() {
        var captured: Map<String, String>? = null
        val sink = MarketingCloudCustomEventSink { _, attrs -> captured = attrs }

        sink.track("m1", "cta_shown", null)

        assertEquals(mapOf("messageId" to "m1"), captured)
    }

    @Test fun `custom sink drops malformed json but still sends messageId`() {
        var captured: Map<String, String>? = null
        val sink = MarketingCloudCustomEventSink { _, attrs -> captured = attrs }

        sink.track("m1", "cta_shown", "not json {")

        assertEquals(mapOf("messageId" to "m1"), captured)
    }

    @Test fun `custom sink skips nested objects and arrays but keeps scalars`() {
        var captured: Map<String, String>? = null
        val sink = MarketingCloudCustomEventSink { _, attrs -> captured = attrs }

        sink.track("m1", "e", """{"flat":"ok","obj":{"x":1},"arr":[1,2]}""")

        assertEquals(mapOf("messageId" to "m1", "flat" to "ok"), captured)
    }

    @Test fun `custom sink does not let attributes overwrite messageId`() {
        var captured: Map<String, String>? = null
        val sink = MarketingCloudCustomEventSink { _, attrs -> captured = attrs }

        sink.track("m1", "e", """{"messageId":"spoofed"}""")

        assertEquals("m1", captured!!["messageId"])
    }

    // ---- the rating use case is now pure HTML: it emits `iam_rating` through this same sink ----

    @Test fun `rating example flows through the generic sink as a custom event`() {
        var captured: Pair<String, Map<String, String>>? = null
        val sink = MarketingCloudCustomEventSink { name, attrs -> captured = name to attrs }

        // Mirrors what rating.html sends: _track('iam_rating', '{"rating":"4"}')
        sink.track("m1", "iam_rating", """{"rating":"4"}""")

        assertEquals("iam_rating", captured!!.first)
        assertEquals("m1", captured!!.second["messageId"])
        assertEquals("4", captured!!.second["rating"]) // values are Strings per the SDK API
    }
}
