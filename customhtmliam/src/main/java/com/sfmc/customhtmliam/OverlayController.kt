package com.sfmc.customhtmliam

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class OverlayController(
    private val tracker: ForegroundActivityTracker,
    private val state: OverlayState,
    private val mainHandler: Handler,
) {
    private var webView: WebView? = null

    fun reserve(): Boolean = state.reserve()

    /**
     * Attach the overlay for [messageId].
     * @param onShown fired on the main thread the moment the WebView is attached (both the
     *   immediate and the deferred/next-resume paths). This is the "message is now on screen"
     *   signal used to record the display back to the SDK.
     * @param onEvent fired for every JS bridge event. The controller tears the overlay down after a
     *   TERMINAL event (see [BridgeEvent.terminal]); non-terminal events (track / open-url / display)
     *   leave the card up.
     */
    fun show(
        messageId: String,
        html: String,
        placement: Placement,
        onShown: () -> Unit,
        onEvent: (BridgeEvent) -> Unit,
    ) {
        mainHandler.post {
            val activity = tracker.current()
            if (activity == null) {
                // No foreground Activity: queue and flush on next resume within TTL. The placement
                // is captured here and reused on attach (it does not need to live in OverlayState).
                state.enqueue(messageId, html)
                tracker.setOnResume { resumed ->
                    tracker.setOnResume(null)
                    state.takeFresh()?.let { attach(resumed, it.id, it.html, placement, onShown, onEvent) }
                }
            } else {
                attach(activity, messageId, html, placement, onShown, onEvent)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun attach(
        activity: Activity,
        messageId: String,
        html: String,
        placement: Placement,
        onShown: () -> Unit,
        onEvent: (BridgeEvent) -> Unit,
    ) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val wv = WebView(activity).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            // Defense-in-depth: the bridge is inert-by-design and base URL is null, but with a
            // native JS bridge attached we also deny the page any local file/content access.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(v: WebView?, code: Int, desc: String?, url: String?) {
                    dismiss() // never leave a broken shell
                }
            }
            addJavascriptInterface(CustomHtmlBridge(messageId) { event ->
                mainHandler.post {
                    onEvent(event)
                    if (event.terminal) dismiss()
                }
            }, "CustomHtmlBridge")
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }

        // Transparent canvas sized to the card's footprint. The native WebView must be no larger
        // than the card, or its extra (transparent) area eats taps — so placement, parsed from the
        // title before attach, decides Gravity + size. `full` fills the frame (modal); a band /
        // box leaves the rest of the screen live. The card's own CSS still positions WITHIN this
        // view. Percent axes are resolved against the live screen size, correct across rotation.
        val dm = activity.resources.displayMetrics
        val lp = FrameLayout.LayoutParams(
            placement.width.toPx(dm, dm.widthPixels),
            placement.height.toPx(dm, dm.heightPixels),
        ).apply { gravity = gravityFor(placement.anchor) }
        content.addView(wv, lp)
        webView = wv
        state.markLive(messageId)
        // The card is on screen: record the display (native parity) exactly once per attach.
        onShown()
    }

    /** Android FrameLayout gravity for each [Anchor]. */
    private fun gravityFor(anchor: Anchor): Int = when (anchor) {
        Anchor.FULL -> Gravity.FILL
        Anchor.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        Anchor.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        Anchor.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
        Anchor.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
        Anchor.CENTER -> Gravity.CENTER
        Anchor.TOP_LEFT -> Gravity.TOP or Gravity.START
        Anchor.TOP_RIGHT -> Gravity.TOP or Gravity.END
        Anchor.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
        Anchor.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
    }

    /** Resolve one [Dimension] to a LayoutParams pixel value; [axisPx] is that axis's screen size. */
    private fun Dimension.toPx(dm: DisplayMetrics, axisPx: Int): Int = when (this) {
        is Dimension.Match -> ViewGroup.LayoutParams.MATCH_PARENT
        is Dimension.Dp -> Math.round(value * dm.density)
        is Dimension.Percent -> Math.round(axisPx * value / 100f)
    }

    fun dismiss() {
        mainHandler.post {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.destroy()
            }
            webView = null
            state.release()
        }
    }
}
