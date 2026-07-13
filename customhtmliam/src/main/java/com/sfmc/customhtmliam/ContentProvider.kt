package com.sfmc.customhtmliam

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

/** Fetches raw HTML for a message. Remote first, then bundled assets, then null. */
fun interface HtmlHttpClient {
    suspend fun get(url: String): String?
}

/** Loads a UTF-8 asset file's contents, or null if missing. */
fun interface AssetLoader {
    fun load(path: String): String?
}

class ContentProvider(
    private val baseUrl: String,
    private val timeoutMs: Long,
    private val http: HtmlHttpClient,
    private val assets: AssetLoader,
    private val exactUrl: String? = null, // when set, fetched verbatim (ignores messageId) — for a single CloudPage URL
) {
    suspend fun fetch(
        messageId: String,
        inlineHtml: String? = null,
        overrideUrl: String? = null,
    ): String? {
        // OPTION 1: inline HTML (the message body IS the HTML) wins when present — no network.
        if (!inlineHtml.isNullOrBlank()) return inlineHtml

        // OPTION 2: a per-message URL (e.g. from the message body) overrides the configured URLs.
        val remoteUrl = overrideUrl?.takeIf { it.isNotBlank() } ?: exactUrl ?: "$baseUrl$messageId.html"
        val remote = try {
            withTimeoutOrNull(timeoutMs) { http.get(remoteUrl) }
        } catch (e: CancellationException) {
            throw e // never swallow an externally-triggered cancellation (structured concurrency)
        } catch (e: Throwable) {
            null // a failed remote fetch is a miss — fall through to bundled assets
        }
        if (!remote.isNullOrBlank()) return remote

        assets.load("customhtmliam/$messageId.html")?.let { if (it.isNotBlank()) return it }
        assets.load("customhtmliam/rating.html")?.let { if (it.isNotBlank()) return it }
        return null
    }
}

/**
 * Real HTTP client using HttpURLConnection on the IO dispatcher.
 *
 * Socket timeouts are derived from [timeoutMs] so the blocking connect/read
 * cannot outlive the fetch budget. Blocking socket I/O is not cooperatively
 * cancellable, so `withTimeoutOrNull` alone cannot preempt it — these socket
 * timeouts are the hard bound; the coroutine timeout is the soft bound.
 */
class UrlHtmlHttpClient(private val timeoutMs: Int = 3000) : HtmlHttpClient {
    override suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs; readTimeout = timeoutMs; requestMethod = "GET"
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally { conn.disconnect() }
        }.getOrNull()
    }
}

/** Real asset loader backed by the app's AssetManager. */
class AndroidAssetLoader(context: Context) : AssetLoader {
    private val appContext = context.applicationContext
    override fun load(path: String): String? = runCatching {
        appContext.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()
}
