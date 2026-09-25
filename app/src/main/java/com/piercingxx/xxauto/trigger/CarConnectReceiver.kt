package com.piercingxx.xxauto.trigger

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import com.piercingxx.xxauto.MainActivity
import com.piercingxx.xxauto.settings.AutoPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AU2 auto-launch trigger: when the bonded car connects (ACTION_ACL_CONNECTED),
 * reads the current settings, runs the pure [CarConnectDecision], and opens
 * [MainActivity] if it decides to launch. The activity is `singleTask`, so the
 * launch reuses the existing instance rather than stacking a second.
 *
 * The receiver is registered disabled in the manifest and enabled at runtime
 * only while the auto_launch toggle is on ([setEnabled]) — it never wakes for a
 * connection the user has not opted into. [CarConnectDecision] still re-checks
 * the persisted setting, so an enabled receiver with auto-launch off is inert.
 */
class CarConnectReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        @Suppress("DEPRECATION") // minSdk 26 — the typed overload needs API 33.
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            ?: return
        val connectedAddress = device.address
        val appContext = context.applicationContext
        val prefs = AutoPrefs(appContext)

        // The settings read and launch happen on a background coroutine after
        // onReceive returns; hold a wake lock so the process is not killed for
        // going async before the launch fires.
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "xx-auto:CarConnectReceiver",
        )
        wakeLock.acquire()

        scope.launch {
            try {
                val settings = prefs.settings.first()
                if (CarConnectDecision.shouldLaunch(
                        autoLaunch = settings.autoLaunch,
                        pickedDevice = settings.autoLaunchDevice,
                        connectedAddress = connectedAddress,
                    )
                ) {
                    val launch = Intent(appContext, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(launch)
                }
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }
    }

    companion object {
        /**
         * Enables or disables the receiver so it only fires while auto-launch is
         * on. Called from the settings screen whenever the auto_launch toggle
         * changes; the manifest registers the receiver disabled by default.
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            val component = ComponentName(context, CarConnectReceiver::class.java)
            context.packageManager.setComponentEnabledSetting(
                component,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}