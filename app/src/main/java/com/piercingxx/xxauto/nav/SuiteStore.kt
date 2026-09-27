package com.piercingxx.xxauto.nav

import android.content.Context
import android.content.Intent
import com.piercingxx.xxauto.log.AppLog

/**
 * The xx-apps store, for "not installed" tiles. xx-apps has no per-app
 * deep-link action today, so this opens its launcher intent and passes the
 * wanted package as an ignorable hint (`com.piercingxx.apps.extra.PACKAGE`,
 * the extra name xx-apps already uses internally).
 */
object SuiteStore {

    const val PACKAGE = "com.piercingxx.apps"
    private const val EXTRA_PACKAGE = "com.piercingxx.apps.extra.PACKAGE"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Opens xx-apps; false when xx-apps itself is not installed. */
    fun openListing(context: Context, wanted: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.putExtra(EXTRA_PACKAGE, wanted)
            ?: return false
        return runCatching { context.startActivity(intent) }
            .onFailure { AppLog.w("store", "open xx-apps failed", it) }
            .isSuccess
    }

    /** Opens [packageName]'s own launcher activity (tile long-press, card long-press). */
    fun openApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
