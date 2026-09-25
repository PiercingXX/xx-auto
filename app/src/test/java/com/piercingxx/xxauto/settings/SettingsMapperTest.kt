package com.piercingxx.xxauto.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMapperTest {

    @Test
    fun `empty prefs map yields the design defaults`() {
        val settings = SettingsMapper.fromMap(emptyMap<String, Any?>())
        assertFalse(settings.autoLaunch)
        assertNull(settings.autoLaunchDevice)
        assertTrue(settings.surfaceNowPlaying)
        assertTrue(settings.surfaceQuickPick)
        assertTrue(settings.surfaceNav)
        assertFalse(settings.surfaceCalls)
        assertTrue(settings.keepScreenOn)
        assertTrue(settings.followRotation)
        assertTrue(settings.alwaysInk)
    }

    @Test
    fun `fromMap reads every stored key`() {
        val prefs = mapOf(
            SettingsMapper.KEY_AUTO_LAUNCH to true,
            SettingsMapper.KEY_AUTO_LAUNCH_DEVICE to "AA:BB:CC:DD:EE:FF",
            SettingsMapper.KEY_SURFACE_NOW_PLAYING to false,
            SettingsMapper.KEY_SURFACE_QUICK_PICK to false,
            SettingsMapper.KEY_SURFACE_NAV to false,
            SettingsMapper.KEY_SURFACE_CALLS to true,
            SettingsMapper.KEY_KEEP_SCREEN_ON to false,
            SettingsMapper.KEY_FOLLOW_ROTATION to false,
            SettingsMapper.KEY_ALWAYS_INK to false,
        )
        val settings = SettingsMapper.fromMap(prefs)
        assertTrue(settings.autoLaunch)
        assertEquals("AA:BB:CC:DD:EE:FF", settings.autoLaunchDevice)
        assertFalse(settings.surfaceNowPlaying)
        assertFalse(settings.surfaceQuickPick)
        assertFalse(settings.surfaceNav)
        assertTrue(settings.surfaceCalls)
        assertFalse(settings.keepScreenOn)
        assertFalse(settings.followRotation)
        assertFalse(settings.alwaysInk)
    }

    @Test
    fun `toMap then fromMap round-trips a settings`() {
        val settings = Settings(
            autoLaunch = true,
            autoLaunchDevice = "11:22:33:44:55:66",
            surfaceNowPlaying = false,
            surfaceQuickPick = false,
            surfaceNav = false,
            surfaceCalls = true,
            keepScreenOn = false,
            followRotation = false,
            alwaysInk = false,
        )
        assertEquals(settings, SettingsMapper.fromMap(SettingsMapper.toMap(settings)))
    }

    @Test
    fun `null autoLaunchDevice is omitted from the map`() {
        val map = SettingsMapper.toMap(Settings(autoLaunchDevice = null))
        assertFalse(SettingsMapper.KEY_AUTO_LAUNCH_DEVICE in map)
        assertNull(SettingsMapper.fromMap(map).autoLaunchDevice)
    }

    @Test
    fun `round-trip of the default settings is stable`() {
        val defaults = Settings()
        val roundTripped = SettingsMapper.fromMap(SettingsMapper.toMap(defaults))
        assertEquals(defaults, roundTripped)
    }

    @Test
    fun `SettingsScreen initial state matches the design defaults the mapper persists`() {
        // SettingsScreen reads `autoPrefs.settings.collectAsState(initial = Settings())`,
        // so on a fresh install it renders a bare Settings() before any pref is written.
        // Pin that this first-launch state equals the defaults SettingsMapper yields from
        // an empty prefs map — otherwise the screen a fresh install shows would disagree
        // with what gets persisted and re-read.
        assertEquals(SettingsMapper.fromMap(emptyMap<String, Any?>()), Settings())
    }
}