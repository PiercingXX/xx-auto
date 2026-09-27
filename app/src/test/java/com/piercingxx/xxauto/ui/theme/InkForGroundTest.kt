package com.piercingxx.xxauto.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** always_ink off: the suite ground, with light presets inverting the ramp (design "Theme"). */
class InkForGroundTest {

    @Test
    fun `dark grounds keep the white ramp`() {
        val forest = inkForGround(0xFF10261B.toInt())
        assertFalse(forest.isLight)
        assertEquals(Color(0xFF10261B), forest.ink)
        assertEquals(Color.White, forest.signal)
        assertEquals(Color.White.copy(alpha = 0.90f), forest.text)
    }

    @Test
    fun `paper and mist invert to a near-black signal`() {
        for (argb in listOf(0xFFF3EEE2.toInt(), 0xFFE6EDF5.toInt())) {
            val light = inkForGround(argb)
            assertTrue(light.isLight)
            assertEquals(Color.Black, light.signal)
            assertEquals(Color.Black.copy(alpha = 0.50f), light.muted)
        }
    }

    @Test
    fun `a translucent synced colour is made opaque`() {
        assertEquals(1f, inkForGround(0x80000000.toInt()).ink.alpha)
    }

    @Test
    fun `warn and error keep their brand values`() {
        val light = inkForGround(0xFFF3EEE2.toInt())
        assertEquals(InkPalette.warn, light.warn)
        assertEquals(InkPalette.error, light.error)
    }
}
