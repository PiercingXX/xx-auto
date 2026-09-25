package com.piercingxx.xxauto.trigger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarConnectDecisionTest {

    @Test
    fun `auto-launch off never launches`() {
        assertFalse(
            CarConnectDecision.shouldLaunch(
                autoLaunch = false,
                pickedDevice = "AA:BB:CC:DD:EE:FF",
                connectedAddress = "AA:BB:CC:DD:EE:FF",
            )
        )
    }

    @Test
    fun `no device picked never launches`() {
        assertFalse(
            CarConnectDecision.shouldLaunch(
                autoLaunch = true,
                pickedDevice = null,
                connectedAddress = "AA:BB:CC:DD:EE:FF",
            )
        )
    }

    @Test
    fun `picked device matching the connected device launches`() {
        assertTrue(
            CarConnectDecision.shouldLaunch(
                autoLaunch = true,
                pickedDevice = "AA:BB:CC:DD:EE:FF",
                connectedAddress = "AA:BB:CC:DD:EE:FF",
            )
        )
    }

    @Test
    fun `a different connected device does not launch`() {
        assertFalse(
            CarConnectDecision.shouldLaunch(
                autoLaunch = true,
                pickedDevice = "AA:BB:CC:DD:EE:FF",
                connectedAddress = "11:22:33:44:55:66",
            )
        )
    }

    @Test
    fun `missing connected address does not launch`() {
        assertFalse(
            CarConnectDecision.shouldLaunch(
                autoLaunch = true,
                pickedDevice = "AA:BB:CC:DD:EE:FF",
                connectedAddress = null,
            )
        )
    }
}