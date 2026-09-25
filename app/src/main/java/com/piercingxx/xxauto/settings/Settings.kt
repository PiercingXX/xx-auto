package com.piercingxx.xxauto.settings

/**
 * xx-auto's settings, one value per design "Settings" row. Pure data — no
 * Android imports — so [SettingsMapper] can round-trip it in a JVM unit test.
 * Defaults are exactly the design's table: `auto_launch` off, every surface
 * on except Calls, `keep_screen_on` / `follow_rotation` / `always_ink` on.
 */
data class Settings(
    /** AU2 — open xx-auto when the bonded car connects. */
    val autoLaunch: Boolean = false,
    /** AU2 — the bonded device address the trigger filters on; null = not picked. */
    val autoLaunchDevice: String? = null,
    /** AU3 — now-playing card. */
    val surfaceNowPlaying: Boolean = true,
    /** AU3 — Radio / Audiobook tiles. */
    val surfaceQuickPick: Boolean = true,
    /** AU3 — Maps tile. */
    val surfaceNav: Boolean = true,
    /** AU3 — Calls tile (off until Contacts + Phone are granted). */
    val surfaceCalls: Boolean = false,
    /** AU9 — keep the screen on. */
    val keepScreenOn: Boolean = true,
    /** AU9 — rotate with the phone; off = landscape lock. */
    val followRotation: Boolean = true,
    /** AU9 — always black; off = follow the suite theme. */
    val alwaysInk: Boolean = true,
)