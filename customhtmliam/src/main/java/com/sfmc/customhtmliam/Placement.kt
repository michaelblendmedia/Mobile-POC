package com.sfmc.customhtmliam

/**
 * Placement of the overlay's native WebView, parsed from the message-title grammar
 *   chtml:<anchor>[-<size>][:<name>]
 * This file is PURE Kotlin (no android.* types) so the entire grammar is JVM-unit-testable. The
 * mapping from [Anchor] to an Android Gravity + LayoutParams lives in OverlayController.
 *
 * See AUTHORING-GUIDE.md ("Title-driven placement") for the author-facing description.
 */

/** The nine author-selectable anchors, plus [FULL] (the default, full-screen). */
enum class Anchor {
    FULL, TOP, BOTTOM, LEFT, RIGHT, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * One size axis of the native view.
 * - [Match]   fill that axis (MATCH_PARENT)
 * - [Dp]      a fixed density-independent size (bare number / `dp` / `px` all map here)
 * - [Percent] a fraction (0..100) of the screen dimension for that axis (`40%` -> Percent(40))
 */
sealed interface Dimension {
    data object Match : Dimension
    data class Dp(val value: Int) : Dimension
    data class Percent(val value: Int) : Dimension
}

/** Anchor + resolved width/height axes. Build with [Placement.parse]. */
data class Placement(
    val anchor: Anchor,
    val width: Dimension,
    val height: Dimension,
) {
    companion object {
        /** The fallback placement: full-screen (preserves the pre-feature behavior). */
        fun default(): Placement = Placement(Anchor.FULL, Dimension.Match, Dimension.Match)

        // Two-word anchors MUST precede one-word so `top-left-320x140` matches `top-left`, not `top`.
        private val ANCHOR_TOKENS: List<Pair<String, Anchor>> = listOf(
            "top-left" to Anchor.TOP_LEFT,
            "top-right" to Anchor.TOP_RIGHT,
            "bottom-left" to Anchor.BOTTOM_LEFT,
            "bottom-right" to Anchor.BOTTOM_RIGHT,
            "top" to Anchor.TOP,
            "bottom" to Anchor.BOTTOM,
            "left" to Anchor.LEFT,
            "right" to Anchor.RIGHT,
            "center" to Anchor.CENTER,
            "full" to Anchor.FULL,
        )

        /**
         * Parse [title] into a [Placement]. [prefix] is the classifier prefix (e.g. "chtml:");
         * [defaultBandDp] is the thickness used for an edge anchor with no explicit size. ANY
         * unparseable input returns [default] — never throws, never a partial placement.
         */
        fun parse(title: String?, prefix: String, defaultBandDp: Int): Placement {
            if (title == null || !title.startsWith(prefix)) return default()

            // Segment after the prefix, up to the next ':' (the rest is the cosmetic name).
            val afterPrefix = title.substring(prefix.length).removePrefix(":")
            val spec = afterPrefix.substringBefore(':').trim().lowercase()
            if (spec.isEmpty()) return default()

            // Longest-known-token prefix match (two-word before one-word).
            val match = ANCHOR_TOKENS.firstOrNull { (token, _) ->
                spec == token || spec.startsWith("$token-")
            } ?: return default()
            val (token, anchor) = match

            // Remainder after the anchor token is the optional "-<size>".
            val sizeStr = spec.substring(token.length).removePrefix("-")
            // Split on 'x' when it's between dimension tokens (e.g., "320x180", "40%x30%"),
            // but not within unit suffixes (e.g., "120px"). Use lookahead/lookbehind to match
            // 'x' preceded by digit or '%' and followed by digit.
            val tokens = if (sizeStr.isEmpty()) emptyList() else {
                sizeStr.split(Regex("(?<=[0-9%])x(?=[0-9])"))
            }
            // Parse each axis token; any malformed axis -> whole placement falls back to default.
            val dims = tokens.map { parseDim(it) ?: return default() }

            return when (anchor) {
                Anchor.FULL -> default()

                // Edge bands: the perpendicular axis is sized, the other fills. Single value only.
                Anchor.TOP, Anchor.BOTTOM -> when (dims.size) {
                    0 -> Placement(anchor, Dimension.Match, Dimension.Dp(defaultBandDp))
                    1 -> Placement(anchor, Dimension.Match, dims[0])
                    else -> default()
                }
                Anchor.LEFT, Anchor.RIGHT -> when (dims.size) {
                    0 -> Placement(anchor, Dimension.Dp(defaultBandDp), Dimension.Match)
                    1 -> Placement(anchor, dims[0], Dimension.Match)
                    else -> default()
                }

                // Boxes (center + corners): need WxH; a single value is an NxN square.
                Anchor.CENTER, Anchor.TOP_LEFT, Anchor.TOP_RIGHT,
                Anchor.BOTTOM_LEFT, Anchor.BOTTOM_RIGHT -> when (dims.size) {
                    0 -> default()                            // no box -> full screen (decision #3)
                    1 -> Placement(anchor, dims[0], dims[0])  // N -> NxN (decision #4)
                    2 -> Placement(anchor, dims[0], dims[1])
                    else -> default()
                }
            }
        }

        /** Parse one axis token: "320"/"320dp"/"320px" -> Dp(320); "40%" -> Percent(40); else null. */
        private fun parseDim(token: String): Dimension? {
            val t = token.trim()
            if (t.isEmpty()) return null
            return when {
                t.endsWith("%") -> t.dropLast(1).toIntOrNull()?.takeIf { it in 1..100 }?.let { Dimension.Percent(it) }
                t.endsWith("dp") -> t.dropLast(2).toIntOrNull()?.takeIf { it > 0 }?.let { Dimension.Dp(it) }
                t.endsWith("px") -> t.dropLast(2).toIntOrNull()?.takeIf { it > 0 }?.let { Dimension.Dp(it) }
                else -> t.toIntOrNull()?.takeIf { it > 0 }?.let { Dimension.Dp(it) }
            }
        }
    }
}
