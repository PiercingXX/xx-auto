package com.piercingxx.xxauto.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Listens for the family launcher's theme-change broadcast
 * (`xx.launcher.THEME_CHANGED`) so the app can re-apply the active ground when
 * the launcher switches it (xx-camera's shape). Registered in the manifest with
 * the `com.piercingxx.xxlauncher.permission.THEME_SYNC` permission so only the
 * launcher can trigger it.
 *
 * The broadcast carries the theme name in `xx.launcher.extra.THEME_NAME` and
 * the background colour in `xx.launcher.extra.BACKGROUND`. [handle] parses
 * those extras and the receiver persists the result through [ThemeStore]. With
 * `always_ink` on (the default) the drive screen ignores the synced ground;
 * Phase 3 (`always_ink` off) re-applies it from the store.
 */
class ThemeSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val theme = ThemeSyncReceiver.handle(
            action = intent?.action,
            themeName = intent?.getStringExtra(ThemeStore.EXTRA_THEME_NAME),
            background = if (intent?.hasExtra(ThemeStore.EXTRA_BACKGROUND) == true) {
                intent.getIntExtra(ThemeStore.EXTRA_BACKGROUND, ThemeStore.DEFAULT_BACKGROUND)
            } else {
                null
            },
        ) ?: return
        ThemeStore(context).save(theme)
    }

    companion object {
        const val ACTION_THEME_CHANGED = "xx.launcher.THEME_CHANGED"
        const val PERMISSION_THEME_SYNC = "com.piercingxx.xxlauncher.permission.THEME_SYNC"

        /**
         * Pure seam: parse a THEME_CHANGED broadcast's theme payload. Returns
         * null when [action] is not the THEME_CHANGED action. The background
         * defaults to Ink ([ThemeStore.DEFAULT_BACKGROUND]) when the launcher
         * omits the BACKGROUND extra. Takes plain values (no Android framework
         * types) so it is testable in a JVM unit test.
         */
        fun handle(action: String?, themeName: String?, background: Int?): SyncedTheme? {
            if (action != ACTION_THEME_CHANGED) return null
            return SyncedTheme(
                name = themeName,
                backgroundArgb = background ?: ThemeStore.DEFAULT_BACKGROUND,
            )
        }
    }
}