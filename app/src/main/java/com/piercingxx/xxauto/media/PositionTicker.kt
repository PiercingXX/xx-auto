package com.piercingxx.xxauto.media

import android.os.Handler
import android.os.Looper

/**
 * 1s position ticker for the now-playing card's elapsed time (Phase 2). Runs
 * only while the activity is resumed and the active session is playing —
 * xx-auto has no service and never will, so nothing ticks in the background.
 * The drive screen starts it in onResume and stops it in onPause, feeding the
 * card's elapsed-time label via [onTick].
 */
class PositionTicker(
    private val isPlaying: () -> Boolean,
    private val onTick: (Long) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (isPlaying()) onTick(System.currentTimeMillis())
            handler.postDelayed(this, TICK_MS)
        }
    }

    /** Starts ticking; safe to call when already running. */
    fun start() {
        if (running) return
        running = true
        handler.postDelayed(tick, TICK_MS)
    }

    /** Stops ticking; safe to call when not running. */
    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}