package com.piercingxx.xxauto.media

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture

/**
 * Transport commands over one suite-player controller (Phase 2, AU4). The
 * drive screen's transport row calls these; each is a thin pass-through to the
 * [MediaController] the [SessionHub] connected. Custom buttons (the radio's
 * thumbs) go through [sendCustomCommand], which returns the [SessionResult]
 * future so the card can surface a failure as a one-line Warn — nothing modal.
 */
class Transport(private val controller: MediaController) {

    fun play() = controller.play()

    fun pause() = controller.pause()

    fun seekToNext() = controller.seekToNext()

    fun seekToPrevious() = controller.seekToPrevious()

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    /**
     * Sends a custom session command (e.g. a radio thumb) with empty extras.
     * The caller inspects the [SessionResult] future for a non-success code and
     * shows a one-line Warn on the card.
     */
    fun sendCustomCommand(action: String): ListenableFuture<SessionResult> =
        controller.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), Bundle.EMPTY)
}