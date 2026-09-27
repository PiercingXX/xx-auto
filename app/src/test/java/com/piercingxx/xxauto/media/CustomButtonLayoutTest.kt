package com.piercingxx.xxauto.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomButtonLayoutTest {

    private fun model(buttons: List<NowPlayingState.CustomButton>) = NowPlayingState.Model(
        title = "Signal FM",
        subtitle = "PiercingXX",
        artworkUri = null,
        positionMs = 12_000,
        durationMs = 60_000,
        isPlaying = true,
        isSeekable = true,
        hasPrev = true,
        hasNext = true,
        customButtons = buttons,
    )

    private fun thumb(
        action: String,
        name: String,
        iconResId: Int = 0,
        iconUri: String? = null,
        enabled: Boolean = true,
    ) = NowPlayingState.CustomButton(
        commandAction = action,
        playerCommand = -1,
        displayName = name,
        iconResId = iconResId,
        iconUri = iconUri,
        isEnabled = enabled,
    )

    @Test
    fun `renders exactly three radio buttons in session order`() {
        // The radio ships three thumbs; the row must keep the session's order.
        val row = CustomButtons.layout(
            model(
                listOf(
                    thumb("THUMB_UP", "Like"),
                    thumb("THUMB_REST", "Neutral"),
                    thumb("THUMB_DOWN", "Dislike"),
                )
            )
        )
        assertEquals(3, row.buttons.size)
        assertEquals(listOf("THUMB_UP", "THUMB_REST", "THUMB_DOWN"), row.buttons.map { it.commandAction })
        assertEquals(listOf("Like", "Neutral", "Dislike"), row.buttons.map { it.label })
    }

    @Test
    fun `empty buttons produce an empty row`() {
        val row = CustomButtons.layout(model(emptyList()))
        assertTrue(row.buttons.isEmpty())
    }

    @Test
    fun `button without an icon falls back to its display name`() {
        val row = CustomButtons.layout(model(listOf(thumb("THUMB_UP", "Like"))))
        val button = row.buttons.single()
        assertFalse(button.renderAsIcon)
        assertEquals("Like", button.label)
    }

    @Test
    fun `button with an icon resource renders as an icon`() {
        val row = CustomButtons.layout(
            model(listOf(thumb("THUMB_UP", "Like", iconResId = 0x7f010001)))
        )
        assertTrue(row.buttons.single().renderAsIcon)
    }

    @Test
    fun `button with an icon uri renders as an icon`() {
        val row = CustomButtons.layout(
            model(listOf(thumb("THUMB_UP", "Like", iconUri = "content://radio/thumb_up")))
        )
        assertTrue(row.buttons.single().renderAsIcon)
    }

    @Test
    fun `disabled button is surfaced as disabled`() {
        val row = CustomButtons.layout(
            model(listOf(thumb("THUMB_REST", "Neutral", enabled = false)))
        )
        assertFalse(row.buttons.single().isEnabled)
    }

    @Test
    fun `player command button keeps its command code and no action`() {
        val play = NowPlayingState.CustomButton(
            commandAction = null,
            playerCommand = androidx.media3.common.Player.COMMAND_PLAY_PAUSE,
            displayName = "Play",
            iconResId = 0,
            iconUri = null,
            isEnabled = true,
        )
        val row = CustomButtons.layout(model(listOf(play)))
        val button = row.buttons.single()
        assertNull(button.commandAction)
        assertEquals(androidx.media3.common.Player.COMMAND_PLAY_PAUSE, button.playerCommand)
    }
}
class CustomButtonTransportDedupeTest {

    private fun button(action: String?, command: Int) = NowPlayingState.CustomButton(
        commandAction = action,
        playerCommand = command,
        displayName = action ?: "cmd$command",
        iconResId = 0,
        iconUri = null,
        isEnabled = true,
    )

    private fun model(buttons: List<NowPlayingState.CustomButton>) = NowPlayingState.Model(
        title = null, subtitle = null, artworkUri = null, positionMs = 0, durationMs = 0,
        isPlaying = false, isSeekable = false, hasPrev = false, hasNext = false,
        customButtons = buttons,
    )

    @Test
    fun `excludeTransport drops player commands the transport row already draws`() {
        val transport = listOf(
            androidx.media3.common.Player.COMMAND_PLAY_PAUSE,
            androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS,
            androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT,
            androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        ).map { button(null, it) }
        val thumbs = listOf("THUMB_UP", "THUMB_REST", "THUMB_DOWN").map { button(it, -1) }
        val row = CustomButtons.layout(model(transport + thumbs), excludeTransport = true)
        assertEquals(listOf("THUMB_UP", "THUMB_REST", "THUMB_DOWN"), row.buttons.map { it.commandAction })
    }

    @Test
    fun `other player commands survive the dedupe`() {
        val stop = button(null, androidx.media3.common.Player.COMMAND_STOP)
        assertEquals(1, CustomButtons.layout(model(listOf(stop)), excludeTransport = true).buttons.size)
    }

    @Test
    fun `without the flag the session's list is untouched`() {
        val play = button(null, androidx.media3.common.Player.COMMAND_PLAY_PAUSE)
        assertEquals(1, CustomButtons.layout(model(listOf(play))).buttons.size)
    }

    @Test
    fun `icon data rides through to the row`() {
        val withIcon = button("THUMB_UP", -1).copy(iconResId = 42, iconUri = "content://x", icon = 7)
        val b = CustomButtons.layout(model(listOf(withIcon))).buttons.single()
        assertEquals(42, b.iconResId)
        assertEquals("content://x", b.iconUri)
        assertEquals(7, b.icon)
    }
}
