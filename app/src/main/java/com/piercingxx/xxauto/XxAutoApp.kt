package com.piercingxx.xxauto

import android.app.Application
import com.piercingxx.xxauto.log.AppLog

/**
 * Application entry point. Phase 6: starts the `AppLog` logger so the
 * `LogDumpProvider` door and the rotating log files exist for the suite store
 * to collect. Registered in the manifest as the process Application.
 */
class XxAutoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.installFieldDiagnostics(this)
    }
}