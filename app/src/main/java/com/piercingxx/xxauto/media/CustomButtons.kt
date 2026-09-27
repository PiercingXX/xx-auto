package com.piercingxx.xxauto.media

/**
 * Pure layout model for the now-playing card's custom-button row (Phase 2).
 *
 * The row shows the session's custom buttons in the order the session gave
 * them (design: "the session's custom layout buttons, in the order the session
 * gives them... fall back to its display name"). Icon rendering itself is
 * Android-side (`createPackageContext` for `iconResId`, or `iconUri` — deferred
 * to manual QA); this seam keeps the ordering and the icon-or-label fallback
 * rule JVM-testable. The drive screen feeds the live [NowPlayingState.Model]
 * here and renders the returned [Row].
 */
object CustomButtons {

    /** One button in the row, resolved to how it should render. */
    data class Button(
        /** The `SessionCommand.customAction`, or null for a player command. */
        val commandAction: String?,
        /** The `Player.Command` code, or `-1` for a custom command. */
        val playerCommand: Int,
        /** The display-name fallback, used when [renderAsIcon] is false. */
        val label: CharSequence?,
        /** Whether the session reports the button enabled. */
        val isEnabled: Boolean,
        /**
         * True when the session shipped an icon (iconResId or iconUri) and the
         * row should draw it; false when the row must fall back to [label].
         */
        val renderAsIcon: Boolean,
        /** The session's `CommandButton.icon` constant, 0 when undefined. */
        val icon: Int = 0,
        /** Drawable id inside the owning package (load via `createPackageContext`). */
        val iconResId: Int = 0,
        /** Icon URI the session shipped, or null. */
        val iconUri: String? = null,
    )

    /** The row: the session's custom buttons in the order it gave them. */
    data class Row(
        val buttons: List<Button>,
    )

    /**
     * Resolves the card model's custom buttons into the row layout. Each button
     * keeps its session order; [Button.renderAsIcon] is true when the session
     * shipped an icon and false when the row must fall back to the display name.
     */
    fun layout(model: NowPlayingState.Model, excludeTransport: Boolean = false): Row =
        Row(
            model.customButtons
                .filterNot { excludeTransport && it.commandAction == null && it.playerCommand in TRANSPORT }
                .map { it.toButton() },
        )

    /**
     * Player commands the card's own transport row already draws
     * (`⏮ ▶︎/⏸ ⏭`). A session that also lists them in its button preferences
     * would otherwise get each one twice; the drive screen passes
     * `excludeTransport = true`. Values are media3's `Player.COMMAND_*` codes
     * (PLAY_PAUSE 1, SEEK_TO_PREVIOUS_MEDIA_ITEM 6, SEEK_TO_PREVIOUS 7,
     * SEEK_TO_NEXT_MEDIA_ITEM 8, SEEK_TO_NEXT 9), inlined so this stays pure.
     */
    private val TRANSPORT = setOf(1, 6, 7, 8, 9)

    private fun NowPlayingState.CustomButton.toButton(): Button = Button(
        commandAction = commandAction,
        playerCommand = playerCommand,
        label = displayName,
        isEnabled = isEnabled,
        renderAsIcon = iconResId != 0 || iconUri != null,
        icon = icon,
        iconResId = iconResId,
        iconUri = iconUri,
    )
}