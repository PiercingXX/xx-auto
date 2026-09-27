package com.piercingxx.xxauto.drive

import com.piercingxx.xxauto.calls.Favourites
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.Source

/**
 * Every thing a thumb can do on the drive screen. [DriveController] is the
 * real implementation; previews and the external display can pass their own.
 * All transport calls act on the active session (design: "which session is
 * active").
 */
interface DriveActions {
    fun playPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)

    /** A custom-layout button on the card (the radio's thumbs). */
    fun press(button: CustomButtons.Button)

    /** Card long-press: open the owning app. */
    fun openActiveApp()

    /** Radio / Audiobook tile tap (play, or open the app when there is nothing to play). */
    fun tileTap(source: Source)

    /** Audiobook tile long-press: open the app. (Radio long-press is the quick-pick sheet.) */
    fun openApp(source: Source)

    /** Quick-pick: the radio's browse children of [parentId] (null = root); null = unavailable. */
    suspend fun browse(parentId: String?): List<QuickPickItem>?

    /** Quick-pick row tap on a playable item: `setMediaItem` + `play()`. */
    fun pick(item: QuickPickItem)

    /** Maps tile (AU7, AU13 release-before-launch). */
    fun maps()

    /** Starred contacts (AU8); requires READ_CONTACTS, checked by the caller. */
    suspend fun favourites(): List<Favourites.Contact>

    fun openSettings()
}
