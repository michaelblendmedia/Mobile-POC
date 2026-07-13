package com.sfmc.customhtmliam

import android.util.Log
import android.webkit.JavascriptInterface

/**
 * Events raised by the page's JavaScript through the [CustomHtmlBridge] (exposed as the JS global
 * `CustomHtmlBridge`). These are the seam between arbitrary custom-HTML content and the SDK's native
 * in-app-message lifecycle; see the README section "JavaScript bridge" for the mapping to
 * `displayCount` / completion analytics. Nothing here is specific to any one use case (rating,
 * survey, promo, …) — a use case is authored entirely in HTML against these generic functions.
 */
sealed interface BridgeEvent {
    val messageId: String

    /** The content signals it is on screen. Display is auto-recorded on attach; this is an explicit, idempotent echo. */
    data class Displayed(override val messageId: String) : BridgeEvent

    /** Send an arbitrary Marketing Cloud custom event. Non-terminal. [attributesJson] is a flat JSON object of strings. */
    data class Track(override val messageId: String, val name: String, val attributesJson: String?) : BridgeEvent

    /** Open a URL (deep link / CloudPage / web) via the SDK's UrlHandler. Non-terminal. */
    data class OpenUrl(override val messageId: String, val url: String) : BridgeEvent

    /** A tap/click that ends the message. Terminal: completes as a button click (analytics parity). */
    data class Clicked(override val messageId: String, val label: String?) : BridgeEvent

    /** The user dismissed the content. Terminal: completes as a user-dismiss. */
    data class Dismissed(override val messageId: String) : BridgeEvent
}

/**
 * Whether this event ends the message. Terminal events tear the overlay down and record a native
 * completion (releasing the SDK's in-flight slot); non-terminal events leave the content on screen.
 */
val BridgeEvent.terminal: Boolean
    get() = when (this) {
        is BridgeEvent.Clicked, is BridgeEvent.Dismissed -> true
        is BridgeEvent.Displayed, is BridgeEvent.Track, is BridgeEvent.OpenUrl -> false
    }

/**
 * Injected into the overlay WebView as the JS global `CustomHtmlBridge`. Every `@JavascriptInterface`
 * method is callable from the page. This class is deliberately dumb: it validates the minimum, then
 * forwards a typed [BridgeEvent] to [onEvent]. All policy (what to record, when to tear down) lives
 * in the controller/listener so this stays fully unit-testable with no Android or SDK dependency.
 *
 * The whole bridge is use-case agnostic: it exposes generic display/click/dismiss/open/track hooks,
 * never anything named for a particular activity. A concrete card (rating, survey, promo, …) is just
 * HTML that calls these functions — e.g. a rating card calls
 * `_track('iam_rating', '{"rating":"5"}')` then `_click('rating')`. New use cases need no Kotlin
 * change.
 *
 * SECURITY: any HTML loaded into the overlay can call these methods, so the surface is minimal and
 * each method's authority is bounded (see README "SECURITY"). `messageId` is captured natively here
 * and never taken from JS, so the page cannot spoof which message it is acting on.
 */
class CustomHtmlBridge(
    private val messageId: String,
    private val onEvent: (BridgeEvent) -> Unit,
) {
    /** `CustomHtmlBridge._display()` — mark the message displayed. Optional; display is auto-recorded on show. */
    @JavascriptInterface
    fun _display() = onEvent(BridgeEvent.Displayed(messageId))

    /** `CustomHtmlBridge._click(label?)` — record a click completion and dismiss the overlay. */
    @JavascriptInterface
    fun _click(label: String?) = onEvent(BridgeEvent.Clicked(messageId, label))

    /** `CustomHtmlBridge._click()` — no-label overload. */
    @JavascriptInterface
    fun _click() = onEvent(BridgeEvent.Clicked(messageId, null))

    /** `CustomHtmlBridge._dismiss()` — record a user-dismiss completion and dismiss the overlay. */
    @JavascriptInterface
    fun _dismiss() = onEvent(BridgeEvent.Dismissed(messageId))

    /** `CustomHtmlBridge._open(url)` — open a URL through the SDK's UrlHandler. Does NOT dismiss. */
    @JavascriptInterface
    fun _open(url: String?) {
        if (!url.isNullOrBlank()) onEvent(BridgeEvent.OpenUrl(messageId, url))
        else Log.w(TAG, "_open ignored: blank url")
    }

    /** `CustomHtmlBridge._track(name, attributesJson)` — send a custom MC event. Does NOT dismiss. */
    @JavascriptInterface
    fun _track(name: String?, attributesJson: String?) {
        if (!name.isNullOrBlank()) onEvent(BridgeEvent.Track(messageId, name, attributesJson))
        else Log.w(TAG, "_track ignored: blank event name")
    }

    /** `CustomHtmlBridge._track(name)` — no-attributes overload. */
    @JavascriptInterface
    fun _track(name: String?) = _track(name, null)

    /** `CustomHtmlBridge._log(msg)` — write to Logcat only; never raises an event. */
    @JavascriptInterface
    fun _log(msg: String) { Log.d(TAG, msg) }

    private companion object { const val TAG = "CustomHtmlIam" }
}
