package com.piercingxx.xxauto.drive

import com.piercingxx.xxauto.drive.DriveTiles.Tile
import com.piercingxx.xxauto.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveTilesTest {

    @Test
    fun `default settings draw all four tiles`() {
        // Defaults: quick-pick + nav on, calls off — so Radio, Audiobook, Maps.
        assertEquals(
            setOf(Tile.RADIO, Tile.AUDIOBOOK, Tile.MAPS),
            DriveTiles.visible(Settings()),
        )
    }

    @Test
    fun `quick-pick off hides radio and audiobook`() {
        val visible = DriveTiles.visible(Settings(surfaceQuickPick = false))
        assertTrue(Tile.RADIO !in visible)
        assertTrue(Tile.AUDIOBOOK !in visible)
        assertTrue(Tile.MAPS in visible)
    }

    @Test
    fun `nav off hides maps`() {
        val visible = DriveTiles.visible(Settings(surfaceNav = false))
        assertTrue(Tile.MAPS !in visible)
        assertTrue(Tile.RADIO in visible)
        assertTrue(Tile.AUDIOBOOK in visible)
    }

    @Test
    fun `calls on adds the calls tile`() {
        val visible = DriveTiles.visible(Settings(surfaceCalls = true))
        assertTrue(Tile.CALLS in visible)
    }

    @Test
    fun `radio tile is present so its long-press can open QuickPickSheet`() {
        // QuickPickSheet is the Radio tile's long-press sheet (DriveScreen wires
        // it to the RADIO tile's onLongPress). Pin that the tile which opens the
        // sheet is drawn whenever the quick-pick surface is on, so the sheet is
        // reachable from the default screen.
        assertTrue(Tile.RADIO in DriveTiles.visible(Settings(surfaceQuickPick = true)))
    }

    @Test
    fun `all surfaces off draws nothing`() {
        assertTrue(
            DriveTiles.visible(
                Settings(
                    surfaceQuickPick = false,
                    surfaceNav = false,
                    surfaceCalls = false,
                )
            ).isEmpty()
        )
    }
}