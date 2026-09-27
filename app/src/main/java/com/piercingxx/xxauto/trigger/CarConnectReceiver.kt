package com.piercingxx.xxauto.trigger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.piercingxx.xxauto.MainActivity
import com.piercingxx.xxauto.R
import com.piercingxx.xxauto.log.AppLog
import com.piercingxx.xxauto.settings.AutoPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AU2 auto-launch trigger. Manifest-declared for `ACL_CONNECTED` /
 * `ACL_DISCONNECTED`, registered **disabled**; [setEnabled] turns it on only
 * while the `auto_launch` toggle is on, so off means nothing runs.
 *
 * The pure [CarConnectDecision] picks the outcome; this class only carries it
 * out. The settings read is async, so the receiver holds the broadcast open
 * with `goAsync()` (no wake lock — the design keeps `WAKE_LOCK` out).
 */
class CarConnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != CarConnectDecision.ACTION_ACL_CONNECTED &&
            action != CarConnectDecision.ACTION_ACL_DISCONNECTED
        ) return

        @Suppress("DEPRECATION") // minSdk 26 — the typed overload needs API 33.
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val address = device.address
        val appContext = context.applicationContext
        val pending = goAsync()

        scope.launch {
            try {
                val settings = AutoPrefs(appContext).settings.first()
                // Self-heal: an enabled receiver with the toggle off (a restore
                // raced the DataStore cache) switches itself back off.
                if (!settings.autoLaunch) setEnabled(appContext, false)
                val outcome = CarConnectDecision.decide(
                    action = action,
                    deviceAddress = address,
                    savedAddress = settings.autoLaunchDevice,
                    autoLaunch = settings.autoLaunch,
                    overlayGranted = Settings.canDrawOverlays(appContext),
                    wasAutoOpened = AutoSession.autoOpened,
                )
                AppLog.i(TAG, "$action -> $outcome")
                // The car is gone either way: a stale "Car connected" is noise.
                if (action == CarConnectDecision.ACTION_ACL_DISCONNECTED &&
                    address.equals(settings.autoLaunchDevice, ignoreCase = true)
                ) {
                    NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
                }
                when (outcome) {
                    CarConnectDecision.Outcome.OPEN -> appContext.startActivity(launchIntent(appContext))
                    CarConnectDecision.Outcome.OPEN_VIA_NOTIFICATION -> notifyConnected(appContext)
                    CarConnectDecision.Outcome.CLOSE -> appContext.sendBroadcast(
                        Intent(AutoSession.ACTION_CAR_GONE).setPackage(appContext.packageName),
                    )
                    CarConnectDecision.Outcome.IGNORE -> Unit
                }
            } catch (t: Throwable) {
                AppLog.e(TAG, "car-connect handling failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notifyConnected(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            AppLog.w(TAG, "no overlay grant and no notification grant — nothing to show")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.car_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        val content = PendingIntent.getActivity(
            context,
            0,
            launchIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wheel)
            .setContentTitle(context.getString(R.string.car_connected_title))
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(content)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
            .onFailure { AppLog.w(TAG, "notify failed", it) }
    }

    companion object {
        private const val TAG = "trigger"
        const val CHANNEL_ID = "car"
        private const val NOTIFICATION_ID = 1

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** The drive screen, flagged as auto-opened (AU2 close rule). */
        fun launchIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(AutoSession.EXTRA_AUTO_OPENED, true)

        /**
         * Enables or disables the receiver so it only fires while auto-launch is
         * on. [AutoPrefs] calls this on every write of `auto_launch`, and the
         * backup door calls it after a restore.
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
