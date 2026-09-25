package com.piercingxx.xxauto.display

import com.piercingxx.xxauto.display.DisplayClaim.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayClaimTest {

    @Test
    fun `opening xx-auto with a free display claims it`() {
        assertEquals(
            Decision.CLAIM,
            DisplayClaim.decide(displayOwnedByOther = false, isOpening = true),
        )
    }

    @Test
    fun `opening xx-auto when the other app owns the display stays on the phone`() {
        assertEquals(
            Decision.STAY_ON_PHONE,
            DisplayClaim.decide(displayOwnedByOther = true, isOpening = true),
        )
    }

    @Test
    fun `not opening never claims the display`() {
        assertEquals(
            Decision.STAY_ON_PHONE,
            DisplayClaim.decide(displayOwnedByOther = false, isOpening = false),
        )
        assertEquals(
            Decision.STAY_ON_PHONE,
            DisplayClaim.decide(displayOwnedByOther = true, isOpening = false),
        )
    }

    @Test
    fun `release frees the display for the other app to claim`() {
        // ExternalDisplay.release hands the display back under the first-come
        // rule: when the decision is STAY_ON_PHONE, sync calls release so
        // xx-maps can claim it — AU13's release-before-maps handoff.
        assertTrue(shouldRelease(displayOwnedByOther = true, isOpening = true))
        assertTrue(shouldRelease(displayOwnedByOther = false, isOpening = false))
        assertFalse(shouldRelease(displayOwnedByOther = false, isOpening = true))
    }
}