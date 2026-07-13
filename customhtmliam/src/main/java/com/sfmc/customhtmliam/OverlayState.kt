package com.sfmc.customhtmliam

/** Thread-safe state machine for one-at-a-time overlay display and foreground queueing. */
class OverlayState(
    private val ttlMs: Long,
    private val now: () -> Long,
) {
    data class Pending(val id: String, val html: String, val ts: Long)

    private enum class Phase { IDLE, RESERVED, LIVE }

    private var phase = Phase.IDLE
    private var pending: Pending? = null

    @Synchronized fun reserve(): Boolean {
        if (phase != Phase.IDLE) return false
        phase = Phase.RESERVED
        return true
    }

    @Synchronized fun markLive(id: String) { phase = Phase.LIVE }

    @Synchronized fun release() {
        phase = Phase.IDLE
        pending = null
    }

    @Synchronized fun enqueue(id: String, html: String) {
        pending = Pending(id, html, now())
    }

    @Synchronized fun takeFresh(): Pending? {
        val p = pending
        pending = null
        return if (p != null && now() - p.ts <= ttlMs) p else null
    }
}
