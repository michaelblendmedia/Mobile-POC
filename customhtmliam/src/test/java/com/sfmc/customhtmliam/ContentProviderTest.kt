package com.sfmc.customhtmliam

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentProviderTest {
    private fun provider(
        http: HtmlHttpClient,
        assets: AssetLoader = AssetLoader { null },
    ) = ContentProvider("https://h/iam/", 3000, http, assets)

    @Test fun `returns remote html on success`() = runTest {
        val p = provider(http = { url ->
            assertEquals("https://h/iam/id1.html", url); "<b>remote</b>"
        })
        assertEquals("<b>remote</b>", p.fetch("id1"))
    }

    @Test fun `falls back to per-id asset when remote fails`() = runTest {
        val p = provider(
            http = { null },
            assets = AssetLoader { path -> if (path == "customhtmliam/id1.html") "<b>asset</b>" else null },
        )
        assertEquals("<b>asset</b>", p.fetch("id1"))
    }

    @Test fun `falls back to default asset when remote and per-id fail`() = runTest {
        val p = provider(
            http = { null },
            assets = AssetLoader { path -> if (path == "customhtmliam/rating.html") "<b>default</b>" else null },
        )
        assertEquals("<b>default</b>", p.fetch("id1"))
    }

    @Test fun `times out slow remote then uses asset`() = runTest {
        val p = provider(
            http = { delay(10_000); "<b>too-late</b>" },
            assets = AssetLoader { "<b>default</b>" },
        )
        assertEquals("<b>default</b>", p.fetch("id1"))
    }

    @Test fun `returns null when nothing available`() = runTest {
        assertNull(provider(http = { null }).fetch("id1"))
    }

    @Test fun `blank remote is a miss and falls through to asset`() = runTest {
        val p = provider(
            http = { "   " }, // whitespace-only body must not count as a hit
            assets = AssetLoader { path -> if (path == "customhtmliam/id1.html") "<b>asset</b>" else null },
        )
        assertEquals("<b>asset</b>", p.fetch("id1"))
    }

    @Test fun `blank asset is a miss and returns null`() = runTest {
        val p = provider(
            http = { null },
            assets = AssetLoader { "" }, // blank asset body must not count as a hit
        )
        assertNull(p.fetch("id1"))
    }

    @Test fun `exactUrl is fetched verbatim and ignores messageId`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { url -> assertEquals("https://cp/page", url); "<b>cloudpage</b>" },
            assets = AssetLoader { null },
            exactUrl = "https://cp/page",
        )
        assertEquals("<b>cloudpage</b>", p.fetch("any-id-here"))
    }

    @Test fun `exactUrl blank remote still falls back to per-id asset`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { "   " },
            assets = AssetLoader { path -> if (path == "customhtmliam/id1.html") "<b>asset</b>" else null },
            exactUrl = "https://cp/page",
        )
        assertEquals("<b>asset</b>", p.fetch("id1"))
    }

    @Test fun `inlineHtml wins over remote and is never fetched`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { throw AssertionError("remote must not be fetched when inlineHtml is present") },
            assets = AssetLoader { null },
            exactUrl = "https://cp/page",
        )
        assertEquals("<b>inline</b>", p.fetch("id1", inlineHtml = "<b>inline</b>"))
    }

    @Test fun `blank inlineHtml is ignored and falls through to remote`() = runTest {
        val p = provider(http = { url ->
            assertEquals("https://h/iam/id1.html", url); "<b>remote</b>"
        })
        assertEquals("<b>remote</b>", p.fetch("id1", inlineHtml = "   "))
    }

    @Test fun `overrideUrl is fetched instead of configured url`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { url -> assertEquals("https://body/page", url); "<b>from-body-url</b>" },
            assets = AssetLoader { null },
            exactUrl = "https://cp/page", // must be overridden by the per-message URL
        )
        assertEquals("<b>from-body-url</b>", p.fetch("id1", overrideUrl = "https://body/page"))
    }

    @Test fun `blank overrideUrl falls back to configured exactUrl`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { url -> assertEquals("https://cp/page", url); "<b>cloudpage</b>" },
            assets = AssetLoader { null },
            exactUrl = "https://cp/page",
        )
        assertEquals("<b>cloudpage</b>", p.fetch("id1", overrideUrl = "  "))
    }

    @Test fun `inlineHtml wins even when overrideUrl is present`() = runTest {
        val p = ContentProvider(
            baseUrl = "https://h/iam/", timeoutMs = 3000,
            http = { throw AssertionError("no network when inlineHtml present") },
            assets = AssetLoader { null },
        )
        assertEquals("<b>inline</b>", p.fetch("id1", inlineHtml = "<b>inline</b>", overrideUrl = "https://body/page"))
    }
}
