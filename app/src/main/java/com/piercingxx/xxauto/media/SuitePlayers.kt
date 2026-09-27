package com.piercingxx.xxauto.media

import android.content.ComponentName

/**
 * The suite players xx-auto controls, found by component, not by scanning
 * (AU6): SKPP Radio's playback service and xx-audiobook's MediaSessionService.
 * No `MediaSessionManager`, no notification-listener access. Third-party
 * players are out of scope. The [SessionHub] connects a controller to each
 * present component.
 */
object SuitePlayers {

    /** SKPP Radio's playback service (AU6): `com.skpp.radio/.playback.PlaybackService`. */
    val RADIO: ComponentName = ComponentName(
        Source.RADIO.packageName,
        "com.skpp.radio.playback.PlaybackService",
    )

    /**
     * xx-audiobook's session service (AU6), as the produced sibling declares
     * it: `com.piercingxx.audiobook/.playback.PlaybackService` (a
     * `MediaLibraryService`, so it is browsable too).
     */
    val AUDIOBOOK: ComponentName = ComponentName(
        Source.AUDIOBOOK.packageName,
        "com.piercingxx.audiobook.playback.PlaybackService",
    )

    /** Both suite players, in the order the hub connects them. */
    val ALL: List<ComponentName> = listOf(RADIO, AUDIOBOOK)

    fun component(source: Source): ComponentName = when (source) {
        Source.RADIO -> RADIO
        Source.AUDIOBOOK -> AUDIOBOOK
    }
}
