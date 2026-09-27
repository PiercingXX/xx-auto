package com.piercingxx.xxauto.trigger

import com.piercingxx.xxauto.trigger.CarConnectDecision.ACTION_ACL_CONNECTED
import com.piercingxx.xxauto.trigger.CarConnectDecision.ACTION_ACL_DISCONNECTED
import com.piercingxx.xxauto.trigger.CarConnectDecision.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class CarConnectDecisionTest {

    private val car = "AA:BB:CC:DD:EE:FF"

    private fun decide(
        action: String? = ACTION_ACL_CONNECTED,
        device: String? = car,
        saved: String? = car,
        autoLaunch: Boolean = true,
        overlay: Boolean = true,
        autoOpened: Boolean = false,
    ) = CarConnectDecision.decide(action, device, saved, autoLaunch, overlay, autoOpened)

    @Test
    fun `saved car connects with the overlay grant opens directly`() {
        assertEquals(Outcome.OPEN, decide())
    }

    @Test
    fun `saved car connects without the overlay grant opens via the notification`() {
        assertEquals(Outcome.OPEN_VIA_NOTIFICATION, decide(overlay = false))
    }

    @Test
    fun `address match ignores case`() {
        assertEquals(Outcome.OPEN, decide(device = car.lowercase()))
    }

    @Test
    fun `another device is ignored`() {
        assertEquals(Outcome.IGNORE, decide(device = "11:22:33:44:55:66"))
        assertEquals(Outcome.IGNORE, decide(action = ACTION_ACL_DISCONNECTED, device = "11:22:33:44:55:66", autoOpened = true))
    }

    @Test
    fun `auto-launch off never acts`() {
        assertEquals(Outcome.IGNORE, decide(autoLaunch = false))
        assertEquals(Outcome.IGNORE, decide(action = ACTION_ACL_DISCONNECTED, autoLaunch = false, autoOpened = true))
    }

    @Test
    fun `no device picked or no device reported never acts`() {
        assertEquals(Outcome.IGNORE, decide(saved = null))
        assertEquals(Outcome.IGNORE, decide(saved = ""))
        assertEquals(Outcome.IGNORE, decide(device = null))
    }

    @Test
    fun `disconnect closes only an auto-opened screen`() {
        assertEquals(Outcome.CLOSE, decide(action = ACTION_ACL_DISCONNECTED, autoOpened = true))
        assertEquals(Outcome.IGNORE, decide(action = ACTION_ACL_DISCONNECTED, autoOpened = false))
    }

    @Test
    fun `unrelated actions are ignored`() {
        assertEquals(Outcome.IGNORE, decide(action = "android.intent.action.BOOT_COMPLETED"))
        assertEquals(Outcome.IGNORE, decide(action = null))
    }
}
