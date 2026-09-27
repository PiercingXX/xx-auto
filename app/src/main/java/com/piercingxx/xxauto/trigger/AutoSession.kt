package com.piercingxx.xxauto.trigger

/**
 * Process-wide record of how the drive screen was opened (AU2). MainActivity
 * writes it from each launch intent; [CarConnectReceiver] reads it on
 * `ACL_DISCONNECTED`. If the process has died there is no drive screen to
 * close, so the in-memory default (`false`) is the right answer.
 */
object AutoSession {

    /** Launch-intent extra: true when the car-connect trigger opened the screen. */
    const val EXTRA_AUTO_OPENED = "auto_opened"

    /** Package-private broadcast: the car went away, finish if auto-opened. */
    const val ACTION_CAR_GONE = "com.piercingxx.xxauto.action.CAR_GONE"

    @Volatile
    var autoOpened: Boolean = false
}
