package com.piercingxx.xxauto.ui.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * The launcher theme-sync store (xx-camera's shape). The family launcher
 * (xx-launcher) broadcasts `xx.launcher.THEME_CHANGED` guarded by the
 * `com.piercingxx.xxlauncher.permission.THEME_SYNC` signature permission,
 * carrying the theme name in `xx.launcher.extra.THEME_NAME` and the background
 * colour in `xx.launcher.extra.BACKGROUND`. [ThemeSyncReceiver]'s pure `handle`
 * seam parses those extras; this store persists the result so the next Activity
 * launch can re-apply it. With `always_ink` on (the default) the drive screen
 * ignores this store and stays Ink.
 */
class ThemeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last theme name broadcast by the launcher, or null if none synced yet. */
    var themeName: String?
        get() = prefs.getString(KEY_THEME_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_THEME_NAME, value).apply()
        }

    /** Last background ARGB broadcast by the launcher; Ink if none. */
    var backgroundArgb: Int
        get() = prefs.getInt(KEY_BACKGROUND, DEFAULT_BACKGROUND)
        set(value) {
            prefs.edit().putInt(KEY_BACKGROUND, value).apply()
        }

    /** Persists a parsed [SyncedTheme] so the next launch can re-apply it. */
    fun save(theme: SyncedTheme) {
        themeName = theme.name
        backgroundArgb = theme.backgroundArgb
    }

    /**
     * Calls [onChange] with the new background whenever the launcher syncs a
     * theme while the drive screen is up. Returns the unsubscribe call. The
     * listener is held strongly here because SharedPreferences only keeps a
     * weak reference to it.
     */
    fun observeBackground(onChange: (Int) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_BACKGROUND) onChange(backgroundArgb)
        }
        listeners += listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            listeners -= listener
        }
    }

    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    companion object {
        /** Extra carrying the theme name on the THEME_CHANGED broadcast. */
        const val EXTRA_THEME_NAME = "xx.launcher.extra.THEME_NAME"

        /** Extra carrying the background colour on the THEME_CHANGED broadcast. */
        const val EXTRA_BACKGROUND = "xx.launcher.extra.BACKGROUND"

        /** Default ground: Ink — the design's #000000. */
        const val DEFAULT_BACKGROUND: Int = 0xFF000000.toInt()

        private const val PREFS = "theme_sync"
        private const val KEY_THEME_NAME = "theme_name"
        private const val KEY_BACKGROUND = "background_argb"
    }
}

/** A parsed launcher theme — its name and background ARGB. */
data class SyncedTheme(
    val name: String?,
    val backgroundArgb: Int,
)