package com.piercingxx.xxauto.media

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/**
 * Owns one [MediaController] per suite player (AU4/AU6). Builds a
 * [SessionToken] for each component, skipping any whose package is absent
 * (`PackageManager.getServiceInfo` catch), and connects a controller via
 * [MediaController.Builder.buildAsync]. Releases every controller on [stop].
 * A runtime receiver reconnects when a suite player is installed or removed
 * (`ACTION_PACKAGE_ADDED` / `ACTION_PACKAGE_REMOVED` for the two packages).
 *
 * Wired into [com.piercingxx.xxauto.MainActivity]'s lifecycle: [start] on
 * activity start, [stop] on activity stop — nothing runs in the background.
 */
class SessionHub(private val context: Context) {

    private val controllers = mutableMapOf<ComponentName, ListenableFuture<MediaController>>()

    /**
     * 1s position ticker, alive only while the hub is started (the activity is
     * visible). Its playing source is empty until the drive screen resolves a
     * controller and calls [transport]; the ticker itself is wired here so it
     * is lifecycle-managed with the controllers and never runs in the
     * background.
     */
    private val positionTicker = PositionTicker(
        isPlaying = { false },
        onTick = { /* the drive screen feeds elapsed time from a resolved controller */ },
    )

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (SuitePlayers.ALL.any { it.packageName == pkg }) reconnect()
        }
    }

    /** Connects a controller for every suite player whose package is present. */
    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        runCatching { context.registerReceiver(packageReceiver, filter) }
        SuitePlayers.ALL.forEach(::connect)
        positionTicker.start()
    }

    private fun connect(component: ComponentName) {
        // Skip a suite player whose package is absent (AU6) — there is nothing
        // to control and buildAsync would fail to resolve its service.
        try {
            context.packageManager.getServiceInfo(component, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return
        }
        val token = SessionToken(context, component)
        controllers[component] = MediaController.Builder(context, token).buildAsync()
    }

    private fun reconnect() {
        stop()
        start()
    }

    /**
     * The index of the active suite player in [states] (most recently active
     * first), or null when nothing is playing and no session holds an item.
     * The drive screen feeds the observed state of each connected player here
     * and uses the result to pick the now-playing card; the rule itself lives
     * in [SessionPick] so it stays pure and unit-testable.
     */
    fun active(states: List<SessionPick.State>): Int? = SessionPick.active(states)

    /**
     * The connected controllers, keyed by suite-player component. The drive
     * screen resolves the active one (via [active]) and hands it to [transport]
     * to build the transport row; exposing the map here keeps the controller
     * futures lifecycle-managed by the hub.
     */
    fun controllers(): Map<ComponentName, ListenableFuture<MediaController>> = controllers

    /**
     * A [Transport] over one connected controller, for the drive screen's
     * transport row and custom buttons. The caller passes the resolved
     * [MediaController] it holds from [controllers].
     */
    fun transport(controller: MediaController): Transport = Transport(controller)

    /**
     * Maps one connected controller's live state to the now-playing card model.
     * The drive screen reads the controller's [MediaMetadata], command
     * availability and custom-button preferences and feeds them here; the pure
     * rule lives in [NowPlayingState] so it stays unit-testable. The button
     * preference list wins over the layout list whenever it is non-empty.
     */
    fun nowPlaying(
        metadata: MediaMetadata?,
        commands: Player.Commands?,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        isSeekable: Boolean,
        hasPrev: Boolean,
        hasNext: Boolean,
        mediaButtonPreferences: List<CommandButton>,
        customLayout: List<CommandButton>,
    ): NowPlayingState.Model = NowPlayingState.map(
        metadata = metadata ?: MediaMetadata.EMPTY,
        commands = commands ?: Player.Commands.EMPTY,
        positionMs = positionMs,
        durationMs = durationMs,
        isPlaying = isPlaying,
        isSeekable = isSeekable,
        hasPrev = hasPrev,
        hasNext = hasNext,
        mediaButtonPreferences = mediaButtonPreferences,
        customLayout = customLayout,
    )

    /** Releases every controller and unregisters the package receiver. */
    fun stop() {
        positionTicker.stop()
        controllers.values.forEach { future ->
            MediaController.releaseFuture(future)
        }
        controllers.clear()
        runCatching { context.unregisterReceiver(packageReceiver) }
    }
}