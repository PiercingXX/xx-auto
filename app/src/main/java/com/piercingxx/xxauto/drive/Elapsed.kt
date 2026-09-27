package com.piercingxx.xxauto.drive

/**
 * Pure time formatting for the card's time row: `m:ss` under an hour,
 * `h:mm:ss` from an hour up (audiobook chapters run long). Negative input
 * clamps to zero.
 */
object Elapsed {
    fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** "elapsed / duration", with the elapsed side clamped to the duration. */
    fun row(positionMs: Long, durationMs: Long): String =
        "${format(positionMs.coerceAtMost(durationMs))} / ${format(durationMs)}"
}
