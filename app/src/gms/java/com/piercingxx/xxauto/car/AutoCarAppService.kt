package com.piercingxx.xxauto.car

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator

/**
 * The Android Auto entry point (Phase 9, AU12, `gms` flavor only).
 *
 * **Media category only.** The `gms` manifest declares this service with
 * `androidx.car.app.category.MEDIA`, and [CarSession] renders a templated media
 * surface — the active session's now-playing card, transport row and custom
 * buttons, plus a Maps handoff. It never declares `category.NAVIGATION`, never
 * uses `NAVIGATION_TEMPLATES`, never draws a map: navigation on the car screen
 * is xx-maps' (AU12, contracts/XX-MAPS.md Part 2).
 */
class AutoCarAppService : CarAppService() {

    override fun onCreateSession(sessionInfo: SessionInfo): Session = CarSession()

    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // The car library ships the canonical Android Auto / Automotive OS
            // host allowlist; a release build must not trust every host.
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
}