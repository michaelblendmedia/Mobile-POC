package com.sfmc.customhtmliam

import android.app.PendingIntent
import android.util.Log
import com.salesforce.marketingcloud.UrlHandler
import com.salesforce.marketingcloud.inappmessaging.models.InAppMessage
import com.salesforce.marketingcloud.inappmessaging.models.MessageCompletedEvent
import com.salesforce.marketingcloud.inappmessagingfeature.InAppMessageManager
import com.salesforce.marketingcloud.inappmessagingfeature.messages.iam.InternalIamManager
import java.util.Date
import kotlin.math.max

/** How a custom-HTML overlay finished, mapped to the SDK's completion-event factories. */
enum class CompletionReason { DISMISSED, CLICKED, AUTO, UNKNOWN }

/**
 * Bridges our self-rendered overlay back into the SDK's native in-app-message bookkeeping.
 *
 * WHY THIS EXISTS — decoded from the SDK's own renderer (RealIamComponent):
 * When [IamEventListener.shouldShowMessage] returns `false` to suppress the SDK's native render,
 * the SDK ALSO skips the two internal calls its own IAM Activity makes around a display. Those two
 * calls are the ONLY place the SDK records that a message was shown and completed:
 *
 *  1. [InternalIamManager.canDisplay] — the SDK's IAM Activity calls this on show. It increments
 *     the message's persisted `displayCount`, publishes the `IamDisplayed` analytics event, updates
 *     throttles, notifies `didShowMessage`, and marks the message as the in-flight `currentMessage`.
 *     It returns `false` only when a DIFFERENT message is already live, and is idempotent for the
 *     same message (a second call does not double-increment).
 *  2. [InternalIamManager.handleMessageFinished] — `MessageHandler.messageFinished` calls this on
 *     dismiss/click/auto-dismiss. It publishes the `IamCompleted` analytics event (with display
 *     duration), builds the `InAppMessageCloseAction`, notifies `didCloseMessage`, and clears the
 *     in-flight slot (`currentMessage = null`).
 *
 * Skip (1) and `displayCount` never increments, so a `displayLimit`/frequency-capped message
 * re-fires on every trigger (e.g. `$appOpen`) — exactly the "keeps showing on every app open" bug.
 * This class replays (1) on show and (2) on completion so a self-rendered overlay is recorded
 * identically to a native one. The two calls are a balanced pair — every recorded display MUST be
 * matched by a completion, or the SDK's in-flight slot stays occupied and blocks later messages.
 *
 * The [InAppMessageManager] the SDK hands back is concretely an `IamComponent`, which implements
 * the public [InternalIamManager] interface — reached here by a guarded cast. If a future SDK
 * breaks that, the cast fails, we log once, and degrade to "overlay still shows, no native
 * bookkeeping" instead of crashing.
 */
class IamLifecycleReporter(
    manager: InAppMessageManager?,
    private val tracker: ForegroundActivityTracker,
) {
    private val internal: InternalIamManager? =
        (manager as? InternalIamManager).also {
            if (it == null && manager != null) {
                Log.w(
                    TAG,
                    "InAppMessageManager is not an InternalIamManager (${manager.javaClass.name}); " +
                        "native display/completion bookkeeping is disabled — capped messages may re-show.",
                )
            }
        }

    /**
     * Record that the message was displayed (native parity: what the IAM Activity does on show).
     * @return true if the display was recorded (safe to later record completion); false if it could
     *   not be recorded — either the internal API is unreachable, or a different message is already
     *   the SDK's in-flight message. When false, DO NOT record completion (there is no slot to
     *   release, and doing so would clobber the other message's slot).
     */
    fun recordDisplayed(message: InAppMessage): Boolean {
        val im = internal ?: return false
        return runCatching { im.canDisplay(message) }
            .onFailure { Log.e(TAG, "recordDisplayed failed for ${safeId(message)}", it) }
            .getOrDefault(false)
    }

    /**
     * Record completion (native parity: what `handleMessageFinished` does on dismiss/click).
     * Publishes the completion analytics event, notifies `didCloseMessage`, and releases the slot.
     * Call ONLY after a matching [recordDisplayed] returned true.
     */
    fun recordCompleted(
        message: InAppMessage,
        reason: CompletionReason,
        buttonName: String?,
        displayStartMs: Long,
        nowMs: Long,
    ) {
        val im = internal ?: return
        val displayTime = Date(displayStartMs)
        val duration = max(0L, nowMs - displayStartMs)
        val event = when (reason) {
            CompletionReason.DISMISSED -> MessageCompletedEvent.userDismissed(displayTime, duration)
            CompletionReason.AUTO -> MessageCompletedEvent.autoDismissed(displayTime, duration)
            CompletionReason.UNKNOWN -> MessageCompletedEvent.unknown()
            CompletionReason.CLICKED ->
                // buttonClicked needs an InAppMessage.Button; we synthesize a minimal one so the
                // completion is reported as a click (not a dismiss) in MC analytics. If the SDK's
                // Button shape ever changes, fall back to a dismiss so completion is still recorded.
                runCatching { MessageCompletedEvent.buttonClicked(displayTime, duration, syntheticButton(buttonName)) }
                    .getOrElse { MessageCompletedEvent.userDismissed(displayTime, duration) }
        }
        runCatching { im.handleMessageFinished(message, event) }
            .onFailure { Log.e(TAG, "recordCompleted failed for ${safeId(message)}", it) }
    }

    /**
     * Open a URL through the SDK's own configured [UrlHandler] — the same path a native IAM button
     * uses for a deep link / CloudPage / web URL. No-ops (logs) if there is no foreground Activity
     * or no handler. Used by the JS `_open`/`_click(url)` bridge functions.
     */
    fun openUrl(url: String, source: String = UrlHandler.URL) {
        val im = internal ?: return
        val activity = tracker.current() ?: run {
            Log.w(TAG, "openUrl: no foreground Activity; ignoring $url")
            return
        }
        val handler = im.urlHandler() ?: run {
            Log.w(TAG, "openUrl: SDK has no UrlHandler configured; ignoring $url")
            return
        }
        runCatching {
            val pending: PendingIntent? = handler.handleUrl(activity, url, source)
            pending?.send() ?: Log.w(TAG, "openUrl: UrlHandler returned no intent for $url")
        }.onFailure { Log.e(TAG, "openUrl failed for $url", it) }
    }

    /** A minimal, valid Button so `buttonClicked` analytics carry a click label but nothing else. */
    private fun syntheticButton(name: String?): InAppMessage.Button =
        InAppMessage.Button(
            "customhtmliam",                          // id (required non-null)
            0,                                        // index
            name ?: "button",                         // text (required non-null)
            InAppMessage.Button.ActionType.close,     // actionType (required non-null)
            "",                                       // action
            "",                                       // backgroundColor
            InAppMessage.Size.m,                      // cornerRadius (required non-null)
            null,                                     // font
            null,                                     // border
            null,                                     // margin
            null,                                     // additionalMargin
            InAppMessage.TextAlignment.center,        // textAlignment (required non-null)
            null,                                     // shadow
        )

    private fun safeId(m: InAppMessage) = runCatching { m.id }.getOrDefault("?")

    private companion object { const val TAG = "CustomHtmlIam" }
}
