package com.piercingxx.xxauto.display

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.piercingxx.xxauto.log.AppLog

/**
 * The external-display owner (AU13). Renders the drive screen on a DP-alt /
 * HDMI head unit through `DisplayManager` + `Presentation`. Not `gms`-gated:
 * plain Android, works on a bare GrapheneOS install.
 *
 * First-come, no arbitration ([DisplayClaim.decide]): xx-auto claims a
 * presentation display while its screen is up, unless it has handed the
 * display to xx-maps. The Maps tile calls [releaseForMaps] — one
 * `Presentation.dismiss()` before one `startActivity` — and xx-auto does not
 * reclaim it until the user opens xx-auto afresh ([onFreshOpen]).
 */
class ExternalDisplay(
    private val activity: ComponentActivity,
    private val content: @Composable () -> Unit,
) {
    private val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var presentation: Presentation? = null
    private var visible = false
    private var handedToMaps = false

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = sync()
        override fun onDisplayRemoved(displayId: Int) {
            if (presentation?.display?.displayId == displayId) dismiss()
            sync()
        }
        override fun onDisplayChanged(displayId: Int) = Unit
    }

    /** The external display, or null when no DP-alt/HDMI display is attached. */
    fun externalDisplay(): Display? =
        displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()

    /** The drive screen became visible (onStart). */
    fun start() {
        visible = true
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        sync()
    }

    /** The drive screen left (onStop): give the display back. */
    fun stop() {
        visible = false
        displayManager.unregisterDisplayListener(listener)
        dismiss()
    }

    /** A launcher / auto-launch open: xx-auto may claim the display again. */
    fun onFreshOpen() {
        handedToMaps = false
    }

    /** AU13's entire handoff: dismiss before the Maps tile's startActivity. */
    fun releaseForMaps() {
        handedToMaps = true
        dismiss()
    }

    private fun sync() {
        if (shouldRelease(displayOwnedByOther = handedToMaps, isOpening = visible)) dismiss() else show()
    }

    private fun show() {
        val display = externalDisplay() ?: return
        if (presentation?.display?.displayId == display.displayId) return
        dismiss()
        runCatching {
            val p = Presentation(activity, display)
            val view = ComposeView(p.context).apply { setContent(content) }
            // Compose needs the owners on the view tree; the Presentation's
            // window is not the activity's, so hand them over explicitly.
            p.window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(activity)
                decor.setViewTreeViewModelStoreOwner(activity)
                decor.setViewTreeSavedStateRegistryOwner(activity)
            }
            p.setContentView(view)
            p.show()
            presentation = p
            AppLog.i(TAG, "claimed display ${display.displayId} (${display.name})")
        }.onFailure { AppLog.w(TAG, "could not show on external display", it) }
    }

    private fun dismiss() {
        presentation?.let {
            runCatching { it.dismiss() }
            AppLog.i(TAG, "released external display")
        }
        presentation = null
    }

    private companion object {
        const val TAG = "display"
    }
}

/**
 * Pure: whether the display should be released (freed for the other app to
 * claim) under the first-come rule. [ExternalDisplay] dismisses its
 * Presentation when this is true.
 */
fun shouldRelease(displayOwnedByOther: Boolean, isOpening: Boolean): Boolean =
    DisplayClaim.decide(displayOwnedByOther, isOpening) == DisplayClaim.Decision.STAY_ON_PHONE
