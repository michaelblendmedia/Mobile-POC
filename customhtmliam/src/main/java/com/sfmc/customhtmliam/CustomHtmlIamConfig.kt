package com.sfmc.customhtmliam

/**
 * Decides, synchronously, whether an in-app message should be rendered as custom HTML.
 * Works on the message's primitive `id` / `title` values so it stays unit-testable — the SDK's
 * InAppMessage exposes bare @JvmField members that MockK cannot stub.
 */
fun interface CustomHtmlMatcher {
    fun matches(id: String, title: String?): Boolean

    companion object {
        fun byTitlePrefix(prefix: String) = CustomHtmlMatcher { _, title ->
            title?.startsWith(prefix) == true
        }

        fun byIds(vararg ids: String): CustomHtmlMatcher {
            val set = ids.toHashSet()
            return CustomHtmlMatcher { id, _ -> id in set }
        }
    }
}

/**
 * Where the custom-HTML overlay gets its HTML from. Both options read the in-app message's own
 * `body` attribute — they differ only in how that text is interpreted. Switch modes here at
 * init time; nothing else in the integration changes.
 */
enum class ContentSource {
    /**
     * OPTION 1 — the message `body.text` IS the HTML, rendered verbatim in the WebView.
     * No network. The author pastes the (minified) HTML directly into the message body.
     * Verified live: MC stores the body JSON-escaped but delivers it HTML-transparent, so markup
     * survives intact. Subject to whatever (undocumented) size limit MC's composer enforces.
     */
    MESSAGE_BODY_HTML,

    /**
     * OPTION 2 — the message `body.text` is a URL (e.g. a Marketing Cloud CloudPage such as
     * https://…pub.sfmc-content.com/…); the HTML is fetched from it over HTTPS and rendered.
     * The author pastes only the URL into the message body — no size concern, edit the page
     * without an app release.
     */
    MESSAGE_BODY_URL,
}

data class CustomHtmlIamConfig(
    val matcher: CustomHtmlMatcher,
    /**
     * Selects how the overlay HTML is sourced from the message body. See [ContentSource].
     * SECURITY: whichever mode you pick, the resulting HTML runs in a WebView with the native
     * `CustomHtmlBridge` in reach. Only enable custom HTML for messages authored by a trusted
     * party (your own MC org), and in [ContentSource.MESSAGE_BODY_URL] mode point the body URL at
     * a TRUSTED HTTPS origin (API 28+ blocks cleartext http:// by default).
     */
    val contentSource: ContentSource = ContentSource.MESSAGE_BODY_HTML,
    /**
     * Optional fallback base URL for remote HTML, fetched as "$contentBaseUrl$messageId.html" when
     * the message body yields no usable content. Leave empty to disable. Same HTTPS trust boundary
     * as [contentSource].
     */
    val contentBaseUrl: String = "",
    /**
     * Optional fixed fallback URL fetched VERBATIM when the message body yields no usable content
     * (ignores messageId; takes precedence over [contentBaseUrl]). Same HTTPS trust boundary.
     */
    val contentUrl: String? = null,
    val fetchTimeoutMs: Long = 3000,
    val foregroundTtlMs: Long = 30000,
    /**
     * Emit diagnostic Logcat lines about what the message body delivered (content source, length,
     * whether it looks like HTML). Off by default; wire it to your app's `BuildConfig.DEBUG` so a
     * release build never logs message-body characteristics.
     */
    val debugLogging: Boolean = false,
    /**
     * Default thickness (in dp) for an EDGE anchor (`top`/`bottom`/`left`/`right`) whose title
     * omits a size — e.g. `chtml:bottom` becomes a [defaultBandDp]-tall bottom band, while
     * `chtml:bottom-120px` overrides it. Only affects edge anchors; boxes and `full` ignore it.
     */
    val defaultBandDp: Int = 220,
    /**
     * The title prefix that a placement spec follows (see [Placement.parse]). Should match the
     * prefix used by [CustomHtmlMatcher.byTitlePrefix]; the default aligns with the default matcher.
     * With an id-based matcher, titles typically won't start with this, so placement parsing simply
     * falls back to full-screen — the safe default.
     */
    val placementPrefix: String = "chtml:",
)
