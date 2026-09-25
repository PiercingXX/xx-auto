package com.piercingxx.xxauto.nav

import com.piercingxx.xxauto.nav.MapsLaunch.Target
import org.junit.Assert.assertEquals
import org.junit.Test

class MapsLaunchTest {

    @Test
    fun `drive action present resolves to Drive even when not installed`() {
        assertEquals(Target.DRIVE, MapsLaunch.resolve(hasDriveAction = true, isInstalled = true))
        assertEquals(Target.DRIVE, MapsLaunch.resolve(hasDriveAction = true, isInstalled = false))
    }

    @Test
    fun `installed but no drive action resolves to Launcher`() {
        assertEquals(Target.LAUNCHER, MapsLaunch.resolve(hasDriveAction = false, isInstalled = true))
    }

    @Test
    fun `not installed and no drive action resolves to NotInstalled`() {
        assertEquals(Target.NOT_INSTALLED, MapsLaunch.resolve(hasDriveAction = false, isInstalled = false))
    }
}