package com.piercingxx.xxauto.display

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/**
 * The external-display owner (Phase 9, AU13). Renders the drive screen on a
 * DP-alt/HDMI head unit via DisplayManager / Presentation. Not `gms`-gated:
 * plain Android, works on a bare GrapheneOS install.
 *
 * The first-come rule is the pure [DisplayClaim.decide]; this class feeds the
 * live DisplayManager answers into it and shows / releases the Presentation the
 * decision names. The Maps tile calls [release] before launching xx-maps, so
 * the display is free for xx-maps to claim — AU13's entire handoff.
 *
 * The Presentation content (rendering the drive screen on the head unit) is the
 * deferred-to-manual-QA wiring; [ExternalDisplay] holds the ownership gate and
 * lifecycle so the first-come rule and the release-before-maps handoff are
 * exercised in production.
 */
class ExternalDisplay(context: Context) {

    private val appContext = context.applicationContext
    private val displayManager =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private var presentation: Presentation? = null

    /** The external display, or null when no DP-alt/HDMI display is attached. */
    fun externalDisplay(): Display? =
        displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull()

    /**
     * Re-evaluate the first-come rule. [displayOwnedByOther] is whether the
     * other app (xx-maps) already owns the external display; [isOpening] is
     * whether the user is opening xx-auto. Claims when [DisplayClaim.decide]
     * says so, releases otherwise.
     */
    fun sync(displayOwnedByOther: Boolean, isOpening: Boolean) {
        if (shouldRelease(displayOwnedByOther, isOpening)) release() else show()
    }

    /** Release the display so the other app (xx-maps) can claim it. */
    fun release() {
        presentation?.dismiss()
        presentation = null
    }

    private fun show() {
        val display = externalDisplay() ?: return
        if (presentation?.display != display) {
            release()
            // The Presentation owns the display so the first-come rule and the
            // release-before-maps handoff run in production. Rendering the
            // drive screen on the head unit is deferred to manual QA.
            presentation = Presentation(appContext, display)
        }
    }
}

/**
 * Pure: whether the display should be released (freed for the other app to
 * claim) under the first-come rule. [ExternalDisplay.sync] calls [release]
 * when this is true — the AU13 release-before-maps handoff. Naming the release
 * decision so the JVM tests can pin it without an Android context.
 */
fun shouldRelease(displayOwnedByOther: Boolean, isOpening: Boolean): Boolean =
    DisplayClaim.decide(displayOwnedByOther, isOpening) == DisplayClaim.Decision.STAY_ON_PHONE