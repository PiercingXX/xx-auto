package com.piercingxx.xxauto.nav

import android.content.Context
import android.content.Intent
import com.piercingxx.xxauto.log.AppLog

/**
 * The Android side of the Maps tile (AU7), exactly per
 * `contracts/XX-MAPS.md` Part 1: the explicit DRIVE intent if it resolves,
 * else the package's launcher, else the xx-apps listing. Never a `geo:`
 * chooser, never a third-party maps app. The pure order is [MapsLaunch].
 */
object MapsHandoff {

    const val PACKAGE = "com.piercingxx.maps"
    const val ACTION_DRIVE = "com.piercingxx.maps.action.DRIVE"
    const val EXTRA_FROM = "com.piercingxx.maps.extra.FROM"
    const val EXTRA_INK = "com.piercingxx.maps.extra.INK"

    /** Where a tap would go right now (drives the tile label too). */
    fun target(context: Context): MapsLaunch.Target {
        val pm = context.packageManager
        val hasDrive = pm.resolveActivity(Intent(ACTION_DRIVE).setPackage(PACKAGE), 0) != null
        val installed = pm.getLaunchIntentForPackage(PACKAGE) != null
        return MapsLaunch.resolve(hasDriveAction = hasDrive, isInstalled = installed)
    }

    /** The drive intent, verbatim from the contract, plus the ink hint. */
    fun driveIntent(context: Context, alwaysInk: Boolean): Intent =
        Intent(ACTION_DRIVE)
            .setPackage(PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_FROM, context.packageName)
            .putExtra(EXTRA_INK, alwaysInk)

    /** Fires the tile. Returns false only when nothing at all could be opened. */
    fun launch(context: Context, alwaysInk: Boolean): Boolean {
        val intent = when (target(context)) {
            MapsLaunch.Target.DRIVE -> driveIntent(context, alwaysInk)
            MapsLaunch.Target.LAUNCHER -> context.packageManager.getLaunchIntentForPackage(PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            MapsLaunch.Target.NOT_INSTALLED -> return SuiteStore.openListing(context, PACKAGE)
        } ?: return false
        return runCatching { context.startActivity(intent) }
            .onFailure { AppLog.w("maps", "handoff failed", it) }
            .isSuccess
    }
}
