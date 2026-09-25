package com.piercingxx.xxauto.settings

/**
 * Pure prefs ⇄ [Settings] mapper (the seam pattern every sibling uses). It
 * works on a `Map<String, *>` keyed by the DataStore preference names so a JVM
 * unit test can exercise it without Android; [AutoPrefs] feeds it the live
 * DataStore contents and writes its output back. Keys and defaults are exactly
 * the design "Settings" table.
 */
object SettingsMapper {

    const val KEY_AUTO_LAUNCH = "auto_launch"
    const val KEY_AUTO_LAUNCH_DEVICE = "auto_launch_device"
    const val KEY_SURFACE_NOW_PLAYING = "surface_now_playing"
    const val KEY_SURFACE_QUICK_PICK = "surface_quick_pick"
    const val KEY_SURFACE_NAV = "surface_nav"
    const val KEY_SURFACE_CALLS = "surface_calls"
    const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    const val KEY_FOLLOW_ROTATION = "follow_rotation"
    const val KEY_ALWAYS_INK = "always_ink"

    /**
     * Reads a [Settings] from a prefs map. Missing keys fall back to the
     * design defaults, so a fresh DataStore file yields the default [Settings].
     */
    fun fromMap(prefs: Map<String, *>): Settings = Settings(
        autoLaunch = prefs.bool(KEY_AUTO_LAUNCH, false),
        autoLaunchDevice = prefs.string(KEY_AUTO_LAUNCH_DEVICE),
        surfaceNowPlaying = prefs.bool(KEY_SURFACE_NOW_PLAYING, true),
        surfaceQuickPick = prefs.bool(KEY_SURFACE_QUICK_PICK, true),
        surfaceNav = prefs.bool(KEY_SURFACE_NAV, true),
        surfaceCalls = prefs.bool(KEY_SURFACE_CALLS, false),
        keepScreenOn = prefs.bool(KEY_KEEP_SCREEN_ON, true),
        followRotation = prefs.bool(KEY_FOLLOW_ROTATION, true),
        alwaysInk = prefs.bool(KEY_ALWAYS_INK, true),
    )

    /**
     * Serializes a [Settings] to a prefs map. Only the stored keys appear;
     * a null [Settings.autoLaunchDevice] is omitted so the DataStore entry is
     * removed rather than left stale.
     */
    fun toMap(settings: Settings): Map<String, Any?> = buildMap {
        put(KEY_AUTO_LAUNCH, settings.autoLaunch)
        settings.autoLaunchDevice?.let { put(KEY_AUTO_LAUNCH_DEVICE, it) }
        put(KEY_SURFACE_NOW_PLAYING, settings.surfaceNowPlaying)
        put(KEY_SURFACE_QUICK_PICK, settings.surfaceQuickPick)
        put(KEY_SURFACE_NAV, settings.surfaceNav)
        put(KEY_SURFACE_CALLS, settings.surfaceCalls)
        put(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
        put(KEY_FOLLOW_ROTATION, settings.followRotation)
        put(KEY_ALWAYS_INK, settings.alwaysInk)
    }

    private fun Map<String, *>.bool(key: String, default: Boolean): Boolean =
        (this[key] as? Boolean) ?: default

    private fun Map<String, *>.string(key: String): String? = this[key] as? String
}