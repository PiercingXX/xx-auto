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
    fun layout(model: NowPlayingState.Model): Row =
        Row(model.customButtons.map { it.toButton() })

    private fun NowPlayingState.CustomButton.toButton(): Button = Button(
        commandAction = commandAction,
        playerCommand = playerCommand,
        label = displayName,
        isEnabled = isEnabled,
        renderAsIcon = iconResId != 0 || iconUri != null,
    )
}