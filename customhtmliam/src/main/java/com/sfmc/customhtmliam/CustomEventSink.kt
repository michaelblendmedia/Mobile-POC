package com.sfmc.customhtmliam

import android.util.Log
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

/** Thin seam over the SDK's event tracking, so the sink is unit-testable. */
fun interface EventTracker {
    fun track(name: String, attributes: Map<String, String>)
}

/**
 * Sends an arbitrary Marketing Cloud custom event on behalf of the page's `_track(name, attrs)` JS
 * call. `messageId` is always injected natively so a tracked event is attributable to its message
 * and the page cannot spoof it. [attributesJson] is a flat JSON object of string values, e.g.
 * `{"choice":"later","variant":"b"}`; malformed or non-flat JSON is dropped (the event still sends
 * with just `messageId`) rather than throwing.
 *
 * This is the ONLY event path in the package — nothing here knows about ratings or any other use
 * case. A concrete card emits its own events by name from HTML (a rating card calls
 * `_track('iam_rating', '{"rating":"5"}')`); no Kotlin change is needed to add a new use case.
 */
class MarketingCloudCustomEventSink(
    private val tracker: EventTracker,
) {
    fun track(messageId: String, name: String, attributesJson: String?) {
        val attrs = LinkedHashMap<String, String>()
        attrs["messageId"] = messageId
        parseFlatJson(attributesJson).forEach { (k, v) -> if (k != "messageId") attrs[k] = v }
        tracker.track(name, attrs)
    }

    /** Parse a flat JSON object into string→string. Nested objects/arrays and parse errors → empty. */
    private fun parseFlatJson(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = org.json.JSONObject(json)
            buildMap {
                obj.keys().forEach { key ->
                    val value = obj.get(key)
                    if (value !is org.json.JSONObject && value !is org.json.JSONArray) {
                        put(key, value.toString())
                    }
                }
            }
        }.onFailure { Log.w("CustomHtmlIam", "ignoring malformed _track attributes: $json") }
            .getOrDefault(emptyMap())
    }
}

/**
 * Real tracker. Emits a custom event through the SDK's EventManager.
 * CONFIRMED against the repo's own screens/CustomEventTracking.kt (line 299):
 *   EventManager.customEvent(name, attributes)?.track()
 * `customEvent(...)` returns a nullable Event; `?.track()` no-ops if it is null.
 */
class SfmcEventTracker : EventTracker {
    override fun track(name: String, attributes: Map<String, String>) {
        runCatching {
            EventManager.customEvent(name, attributes)?.track()
        }.onFailure { Log.e("CustomHtmlIam", "failed to track $name", it) }
    }
}
