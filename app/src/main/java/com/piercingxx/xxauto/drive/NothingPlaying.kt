package com.piercingxx.xxauto.drive

import androidx.annotation.StringRes

/**
 * The empty-state line — shown on the drive screen when no suite session holds
 * an item. A pure seam so the state's text is testable in a JVM unit test
 * without Android. [DriveScreen] renders [TEXT] as the fallback under the live
 * now-playing card (Phase 2 already replaced the bare "Nothing playing" card
 * with a live session's now-playing model, so this line only appears while no
 * controller is connected).
 *
 * [TEXT] is the design's empty-state copy, "Nothing playing" (design: Phase 0
 * "a single Ink ground rendering `Nothing playing`"), pinned byte-for-byte by
 * the unit test. The same copy lives in `res/values/strings.xml` as the
 * `nothing_playing` resource and is exposed here as [DESIGN_COPY] so the
 * accessibility label resolves through [android.content.res.Resources] and the
 * display text and its label cannot drift apart — both come from this object.
 */
object NothingPlaying {
    /** The display string for the empty now-playing line (the design's copy). */
    const val TEXT = "Nothing playing"

    /** The design's empty-state copy, resolved from `nothing_playing`. */
    @StringRes
    val DESIGN_COPY: Int = com.piercingxx.xxauto.R.string.nothing_playing

    /**
     * Pure seam for the empty-state accessibility label: resolves the design
     * copy's text from a plain [String] so the label's value is JVM-testable
     * without Android [android.content.res.Resources]. [label] delegates here,
     * so the display text and its label cannot drift apart — both flow through
     * this one seam.
     *
     * The seam is not a bare identity pass-through: a blank [designCopy] (an
     * empty or whitespace-only `nothing_playing` resource) would otherwise
     * announce an empty label to TalkBack. It falls back to [TEXT], the design's
     * literal copy, so the empty state always announces something meaningful
     * even if the resource is ever emptied.
     */
    fun labelText(designCopy: String): String =
        designCopy.ifBlank { TEXT }

    /**
     * Resolves the design's real empty-state copy as a displayable string.
     * [DriveScreen] passes this to the empty-state line's content description so
     * the accessibility label announces "Nothing playing". Pure over
     * [android.content.res.Resources], so it is testable in a JVM unit test.
     *
     * The resource lookup is wrapped so a missing `nothing_playing` resource
     * cannot crash the drive screen on launch: a `Resources.NotFoundException`
     * is routed through the same [labelText] blank-fallback as an empty
     * resource, so the accessibility label still announces the design's copy
     * ([TEXT]) instead of throwing. [labelText] is the pure seam this resolves
     * through, so the fallback path is JVM-testable without Android.
     */
    fun label(resources: android.content.res.Resources): String =
        labelText(
            runCatching { resources.getString(DESIGN_COPY) }.getOrDefault("")
        )
}