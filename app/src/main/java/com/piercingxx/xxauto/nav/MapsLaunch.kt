package com.piercingxx.xxauto.nav

/**
 * Pure model of how the Maps tile hands off to xx-maps (Phase 4, AU7).
 * Resolution order from `contracts/XX-MAPS.md` Part 1, unchanged by the
 * 2026-09-21 amendment: the explicit `com.piercingxx.maps.action.DRIVE`
 * intent if it resolves; else the package's launcher if it is installed but
 * predates the action; else NotInstalled. No Android imports, so [resolve]
 * is JVM-testable. The drive screen feeds the live PackageManager answers
 * here and fires the intent the returned target names.
 */
object MapsLaunch {

    /** The three handoff targets, in the contract's resolution order. */
    enum class Target { DRIVE, LAUNCHER, NOT_INSTALLED }

    /**
     * Which target the Maps tile should hit, given whether the DRIVE action
     * resolves and whether the package is installed.
     */
    fun resolve(hasDriveAction: Boolean, isInstalled: Boolean): Target = when {
        hasDriveAction -> Target.DRIVE
        isInstalled -> Target.LAUNCHER
        else -> Target.NOT_INSTALLED
    }
}