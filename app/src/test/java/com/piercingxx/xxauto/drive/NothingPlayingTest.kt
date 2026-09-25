package com.piercingxx.xxauto.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NothingPlayingTest {

    @Test
    fun `nothing playing state renders the expected text`() {
        assertEquals("Nothing playing", NothingPlaying.TEXT)
    }

    @Test
    fun `labelText passes the design copy through unchanged`() {
        val designCopy = "Nothing playing"
        assertEquals(designCopy, NothingPlaying.labelText(designCopy))
    }

    @Test
    fun `labelText falls back to the design copy when the resource is blank`() {
        // label() routes a missing or empty `nothing_playing` resource through
        // this seam (runCatching -> getOrDefault("")), so a blank resolved
        // string must yield the design's literal copy rather than an empty
        // accessibility label. Pins the fallback the drive screen's label now
        // relies on for a missing resource.
        assertEquals("Nothing playing", NothingPlaying.labelText(""))
        assertEquals("Nothing playing", NothingPlaying.labelText("   "))
    }

    @Test
    fun `drive screen empty-state line is the NothingPlaying seam text`() {
        // DriveScreen renders NothingPlaying.TEXT as its empty-state line (the
        // fallback shown under the live now-playing card while no controller is
        // connected). Pin that the seam the screen draws is non-blank, so the
        // visible line can never render empty on the phone.
        assertFalse(NothingPlaying.TEXT.isBlank())
        assertEquals("Nothing playing", NothingPlaying.TEXT)
    }
}