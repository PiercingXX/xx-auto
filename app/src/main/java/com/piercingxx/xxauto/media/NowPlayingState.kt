package com.piercingxx.xxauto.media

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.CommandButton

/**
 * Pure \"what the now-playing card shows\" model and mapper (Phase 2, AU4/AU5).
 * No Android framework imports beyond the media3 data holders, so a JVM unit
 * test can build a [MediaMetadata] / [Player.Commands] / [CommandButton] and
 * exercise every branch. The drive screen extracts the live values from the
 * active [SessionHub] controller and calls [map]; the card renders the result.
 *
 * Custom buttons come from the session's `mediaButtonPreferences` when it
 * publishes any (the radio's three thumbs), falling back to `customLayout` when
 * that list is empty — both APIs exist on media3 1.8.1 and the preference list
 * is the richer of the two.
 */
object NowPlayingState {

    /** One custom transport button the session offers (e.g. a radio thumb). */
    data class CustomButton(
        /** The `SessionCommand.customAction`, or null when this is a player command. */
        val commandAction: String?,
        /** The `Player.Command` code, or `-1` when this is a custom command. */
        val playerCommand: Int,
        /** The button's label; icons fall back to this in Signal when no icon is shipped. */
        val displayName: CharSequence?,
        /** The `CommandButton.iconResId`, or 0 when the button ships an iconUri instead. */
        val iconResId: Int,
        /** The `CommandButton.iconUri` as a string, or null when none. */
        val iconUri: String?,
        /** Whether the session reports the button enabled. */
        val isEnabled: Boolean,
        /**
         * The `CommandButton.icon` constant (e.g. `ICON_THUMB_UP_UNFILLED`), or
         * `CommandButton.ICON_UNDEFINED` (0). Lets the card draw a known glyph
         * when the owning package's [iconResId] cannot be loaded.
         */
        val icon: Int = 0,
    )

    /** Everything the now-playing card needs to draw itself. */
    data class Model(
        val title: CharSequence?,
        val subtitle: CharSequence?,
        val artworkUri: String?,
        val positionMs: Long,
        val durationMs: Long,
        val isPlaying: Boolean,
        val isSeekable: Boolean,
        val hasPrev: Boolean,
        val hasNext: Boolean,
        val customButtons: List<CustomButton>,
    )

    /**
     * Maps live session state to the card model. [mediaButtonPreferences] wins
     * over [customLayout] whenever it is non-empty (media3 1.8.1 exposes both);
     * an empty preference list falls back to the layout list.
     */
    fun map(
        metadata: MediaMetadata,
        commands: Player.Commands,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        isSeekable: Boolean,
        hasPrev: Boolean,
        hasNext: Boolean,
        mediaButtonPreferences: List<CommandButton>,
        customLayout: List<CommandButton>,
    ): Model {
        val buttons = if (mediaButtonPreferences.isNotEmpty()) mediaButtonPreferences else customLayout
        return Model(
            title = metadata.title,
            subtitle = metadata.subtitle ?: metadata.artist,
            artworkUri = metadata.artworkUri?.toString(),
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            isSeekable = isSeekable,
            hasPrev = hasPrev,
            hasNext = hasNext,
            customButtons = buttons.map { it.toCustomButton() },
        )
    }

    private fun CommandButton.toCustomButton(): CustomButton {
        val custom = sessionCommand
        return CustomButton(
            commandAction = custom?.customAction,
            playerCommand = if (custom == null) playerCommand else -1,
            displayName = displayName,
            iconResId = iconResId,
            iconUri = iconUri?.toString(),
            isEnabled = isEnabled,
            icon = icon,
        )
    }
}