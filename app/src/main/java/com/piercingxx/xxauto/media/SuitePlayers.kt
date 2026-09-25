package com.piercingxx.xxauto.media

import android.content.ComponentName

/**
 * The suite players xx-auto controls, found by component, not by scanning
 * (AU6): SKPP Radio's playback service and xx-audiobook's MediaSessionService.
 * No `MediaSessionManager`, no notification-listener access. Third-party
 * players are out of scope. The [SessionHub] connects a [androidx.media3.session.MediaController]
 * to each present component.
 */
object SuitePlayers {

    /** SKPP Radio's playback service (AU6): `com.skpp.radio/.playback.PlaybackService`. */
    val RADIO: ComponentName = ComponentName(
        "com.skpp.radio",
        "com.skpp.radio.playback.PlaybackService",
    )

    /**
     * xx-audiobook's MediaSessionService (AU6): `com.piercingxx.audiobook/<its
     * MediaSessionService>`. The sibling adds this service (design sibling
     * table); the class name follows the radio's `PlaybackService` convention.
     */
    val AUDIOBOOK: ComponentName = ComponentName(
        "com.piercingxx.audiobook",
        "com.piercingxx.audiobook.playback.PlaybackService",
    )

    /** Both suite players, in the order the hub connects them. */
    val ALL: List<ComponentName> = listOf(RADIO, AUDIOBOOK)
}