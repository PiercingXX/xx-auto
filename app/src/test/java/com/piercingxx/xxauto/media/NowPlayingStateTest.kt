package com.piercingxx.xxauto.media

import android.net.TestUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingStateTest {

    private val metadata = MediaMetadata.Builder()
        .setTitle("Signal FM")
        .setArtist("PiercingXX")
        .build()

    private val commands = Player.Commands.Builder().build()

    private fun button(
        action: String? = null,
        playerCommand: Int = -1,
        name: String = "Thumb",
        iconResId: Int = 0,
        enabled: Boolean = true,
    ): CommandButton {
        val builder = CommandButton.Builder()
        if (action != null) {
            builder.setSessionCommand(SessionCommand(action, android.os.Bundle()))
        } else {
            builder.setPlayerCommand(playerCommand)
        }
        return builder
            .setDisplayName(name)
            .setIconResId(iconResId)
            .setEnabled(enabled)
            .build()
    }

    @Test
    fun `maps metadata title and falls back to artist for subtitle`() {
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 12_000,
            durationMs = 60_000,
            isPlaying = true,
            isSeekable = true,
            hasPrev = true,
            hasNext = true,
            mediaButtonPreferences = emptyList(),
            customLayout = emptyList(),
        )
        assertEquals("Signal FM", model.title)
        assertEquals("PiercingXX", model.subtitle)
        assertEquals(12_000L, model.positionMs)
        assertEquals(60_000L, model.durationMs)
        assertTrue(model.isPlaying)
        assertTrue(model.isSeekable)
        assertTrue(model.hasPrev)
        assertTrue(model.hasNext)
    }

    @Test
    fun `uses explicit subtitle over artist when present`() {
        val withSubtitle = metadata.buildUpon().setSubtitle("Chapter 3").build()
        val model = NowPlayingState.map(
            metadata = withSubtitle,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = emptyList(),
            customLayout = emptyList(),
        )
        assertEquals("Chapter 3", model.subtitle)
    }

    @Test
    fun `mediaButtonPreferences win over customLayout when both present`() {
        val pref = button(action = "THUMB_UP", name = "Like", iconResId = 1)
        val layout = button(action = "THUMB_DOWN", name = "Dislike", iconResId = 2)
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = listOf(pref),
            customLayout = listOf(layout),
        )
        assertEquals(1, model.customButtons.size)
        assertEquals("Like", model.customButtons[0].displayName)
        assertEquals("THUMB_UP", model.customButtons[0].commandAction)
        assertEquals(1, model.customButtons[0].iconResId)
    }

    @Test
    fun `falls back to customLayout when mediaButtonPreferences is empty`() {
        val layout = button(action = "THUMB_DOWN", name = "Dislike", iconResId = 2)
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = emptyList(),
            customLayout = listOf(layout),
        )
        assertEquals(1, model.customButtons.size)
        assertEquals("Dislike", model.customButtons[0].displayName)
    }

    @Test
    fun `player command button carries playerCommand and no action`() {
        val playButton = button(playerCommand = Player.COMMAND_PLAY_PAUSE, name = "Play")
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = listOf(playButton),
            customLayout = emptyList(),
        )
        val b = model.customButtons[0]
        assertNull(b.commandAction)
        assertEquals(Player.COMMAND_PLAY_PAUSE, b.playerCommand)
        assertTrue(b.isEnabled)
    }

    @Test
    fun `disabled custom button is surfaced as disabled`() {
        val disabled = button(action = "THUMB_REST", name = "Neutral", enabled = false)
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = listOf(disabled),
            customLayout = emptyList(),
        )
        assertFalse(model.customButtons[0].isEnabled)
    }

    @Test
    fun `artwork uri is carried through as a string`() {
        // The mapper's only interaction with an artwork Uri is
        // metadata.artworkUri?.toString(). A real android.net.Uri cannot be
        // built in a plain JVM test (package-private constructor; Uri.parse is
        // a stub returning null under returnDefaultValues, and the project has
        // no Robolectric/mocking dependency). TestUri (in android.net) supplies
        // a non-null Uri whose toString is its string form — a real Uri's
        // toString is exactly that — so the real map() runs its positive
        // artworkUri path and the assertion is on the mapper's real output.
        val artwork = TestUri("content://art/1")
        val withArtwork = metadata.buildUpon().setArtworkUri(artwork).build()
        val model = NowPlayingState.map(
            metadata = withArtwork,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = emptyList(),
            customLayout = emptyList(),
        )
        assertEquals("content://art/1", model.artworkUri)
    }

    @Test
    fun `artwork uri maps to null when metadata carries none`() {
        val model = NowPlayingState.map(
            metadata = metadata,
            commands = commands,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            mediaButtonPreferences = emptyList(),
            customLayout = emptyList(),
        )
        assertNull(model.artworkUri)
    }
}