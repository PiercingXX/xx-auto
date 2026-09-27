package com.piercingxx.xxauto

import android.app.Application
import com.piercingxx.xxauto.log.AppLog
import com.piercingxx.xxauto.settings.AutoPrefs
import com.piercingxx.xxauto.trigger.CarConnectReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application entry point. Starts the `AppLog` logger so the
 * `LogDumpProvider` door and the rotating log files exist for the suite store
 * to collect, and re-syncs the auto-launch receiver with the saved toggle —
 * a suite restore kills the process, and the next start is where the
 * receiver's PackageManager state is brought back in line with `auto_launch`.
 */
class XxAutoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.installFieldDiagnostics(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                CarConnectReceiver.setEnabled(this@XxAutoApp, AutoPrefs(this@XxAutoApp).settings.first().autoLaunch)
            }.onFailure { AppLog.w("app", "receiver resync failed", it) }
        }
    }
}
