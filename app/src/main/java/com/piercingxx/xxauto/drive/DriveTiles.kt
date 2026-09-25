package com.piercingxx.xxauto.drive

import com.piercingxx.xxauto.settings.Settings

/**
 * Pure model of which drive tiles are drawn (Phase 3, AU3). Design: "Tiles:
 * Radio, Audiobook, Maps, Calls; each drawn only if its surface toggle is on."
 * No Android imports, so [visible] is JVM-testable. The drive screen feeds the
 * live [Settings] here and renders exactly the returned tiles.
 */
object DriveTiles {

    /** The four tiles, in the order the design lists them. */
    enum class Tile { RADIO, AUDIOBOOK, MAPS, CALLS }

    /**
     * The tiles whose surface toggle is on. Radio and Audiobook both belong to
     * the quick-pick surface; Maps to the nav surface; Calls to the calls
     * surface — exactly the design's `surface_quick_pick` / `surface_nav` /
     * `surface_calls` rows.
     */
    fun visible(settings: Settings): Set<Tile> = buildSet {
        if (settings.surfaceQuickPick) {
            add(Tile.RADIO)
            add(Tile.AUDIOBOOK)
        }
        if (settings.surfaceNav) add(Tile.MAPS)
        if (settings.surfaceCalls) add(Tile.CALLS)
    }
}