package com.sfmc.customhtmliam

import android.util.Log
import com.salesforce.marketingcloud.inappmessaging.models.InAppMessage
import com.salesforce.marketingcloud.inappmessagingfeature.InAppMessageCloseAction
import com.salesforce.marketingcloud.inappmessagingfeature.InAppMessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single in-app listener. For our custom-HTML messages it suppresses the native render and
 * shows the overlay; for everything else it defers to [delegate] (the app's pre-existing
 * listener). [delegate] is the SDK's own interface type, so this class has no dependency on
 * app code.
 *
 * DISPLAY BOOKKEEPING — see [IamLifecycleReporter]. Returning `false` from [shouldShowMessage]
 * suppresses the SDK's native UI, but that ALSO skips the SDK's own display/completion recording,
 * which is where `displayCount` is incremented and the display-limit / frequency cap is enforced.
 * Without replaying it, a capped message re-fires on every trigger (e.g. `$appOpen`). So when our
 * overlay actually attaches we record the display via [reporter], and on the terminal bridge event
 * we record completion — giving a self-rendered overlay the same lifecycle as a native message.
 */
class IamEventListener(
    private val classifier: Classifier,
    private val contentProvider: ContentProvider,
    private val overlay: OverlayController,
    private val customEventSink: MarketingCloudCustomEventSink?,
    private val reporter: IamLifecycleReporter?,
    private val scope: CoroutineScope,
    private val delegate: InAppMessageManager.EventListener? = null,
    private val contentSource: ContentSource = ContentSource.MESSAGE_BODY_HTML,
    private val debugLogging: Boolean = false,
    private val placementPrefix: String = "chtml:",
    private val defaultBandDp: Int = 220,
    private val now: () -> Long = { System.currentTimeMillis() },
) : InAppMessageManager.EventListener {

    override fun shouldShowMessage(message: InAppMessage): Boolean {
        // Not ours → let the previous listener decide (preserves IamState suppression); default true.
        if (!classifier.isCustomHtml(message)) return delegate?.shouldShowMessage(message) ?: true

        if (overlay.reserve()) {
            val id = message.id
            // The message body carries either the HTML itself (OPTION 1) or a URL to fetch it
            // from (OPTION 2), decided by contentSource. Anything else falls back to the
            // configured contentUrl/contentBaseUrl/asset inside ContentProvider.fetch.
            val body = Classifier.bodyText(message)
            val inlineHtml = if (contentSource == ContentSource.MESSAGE_BODY_HTML) body else null
            val overrideUrl = if (contentSource == ContentSource.MESSAGE_BODY_URL) body else null
            // Placement is parsed from the title BEFORE attach, so the overlay picks Gravity + size
            // up front (no JS round-trip). Anything unparseable falls back to full-screen.
            val title = try { message.title?.text } catch (t: Throwable) { null }
            val placement = Placement.parse(title, placementPrefix, defaultBandDp)
            // Diagnostic only (never in release): summarizes what the message body delivered.
            // Gated so a production build never logs message-body content characteristics.
            if (debugLogging) {
                Log.i(
                    TAG,
                    "content source=$contentSource for $id: bodyLength=${body?.length ?: 0}" +
                        " startsWithDoctype=${body?.trimStart()?.startsWith("<!DOCTYPE", ignoreCase = true) == true}" +
                        " escaped=${body?.contains("&lt;") == true}",
                )
                // Placement diagnostic: what title MC delivered and how it resolved. This is the
                // only view into the server-side title (which drives Gravity + size) from the device.
                Log.i(TAG, "placement for $id: title=${title?.let { "\"$it\"" } ?: "<null>"} -> $placement")
            }
            scope.launch {
                val html = contentProvider.fetch(id, inlineHtml, overrideUrl)
                if (html == null) {
                    overlay.dismiss() // releases the reservation
                    return@launch
                }
                // Per-display state, closed over by the show callbacks below.
                var displayStartMs = 0L
                var displayRecorded = false
                overlay.show(
                    messageId = id,
                    html = html,
                    placement = placement,
                    onShown = {
                        // The card is now on screen — record the display exactly as the native
                        // renderer would (increments displayCount, fires IamDisplayed, notifies
                        // didShowMessage). Only pair a completion with it if it was truly recorded.
                        displayStartMs = now()
                        displayRecorded = reporter?.recordDisplayed(message) ?: false
                    },
                    onEvent = { event ->
                        handleBridgeEvent(message, event, displayStartMs) { displayRecorded }
                    },
                )
            }
        }
        return false // suppress the SDK's native render for our messages
    }

    /**
     * Route one JS bridge event. Non-terminal events (track/open/display echo) are side effects that
     * leave the card up. Terminal events (click/dismiss) record native completion — but only when the
     * matching display was actually recorded, so we never release a slot we don't own.
     */
    private fun handleBridgeEvent(
        message: InAppMessage,
        event: BridgeEvent,
        displayStartMs: Long,
        displayRecorded: () -> Boolean,
    ) {
        when (event) {
            is BridgeEvent.Displayed -> { /* already recorded on attach; explicit echo, no-op */ }
            is BridgeEvent.Track ->
                customEventSink?.track(event.messageId, event.name, event.attributesJson)
            is BridgeEvent.OpenUrl -> reporter?.openUrl(event.url)
            is BridgeEvent.Clicked ->
                complete(message, CompletionReason.CLICKED, event.label, displayStartMs, displayRecorded)
            is BridgeEvent.Dismissed ->
                complete(message, CompletionReason.DISMISSED, null, displayStartMs, displayRecorded)
        }
    }

    private fun complete(
        message: InAppMessage,
        reason: CompletionReason,
        label: String?,
        displayStartMs: Long,
        displayRecorded: () -> Boolean,
    ) {
        if (!displayRecorded()) return
        reporter?.recordCompleted(message, reason, label, displayStartMs, now())
    }

    override fun didShowMessage(message: InAppMessage) {
        // We never let the SDK natively show OUR messages, so any didShow is the delegate's.
        delegate?.didShowMessage(message)
    }

    override fun didCloseMessage(message: InAppMessage, action: InAppMessageCloseAction) {
        delegate?.didCloseMessage(message, action)
    }

    private companion object {
        const val TAG = "CustomHtmlIam"
    }
}
