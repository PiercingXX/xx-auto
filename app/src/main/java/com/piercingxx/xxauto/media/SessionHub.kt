package com.piercingxx.xxauto.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.piercingxx.xxauto.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns one controller per suite player (AU4/AU6) and publishes what they are
 * doing as a [Snapshot] flow the drive screen (and the gms car screen) render.
 *
 * - Found by component (AU6): a [SessionToken] per [SuitePlayers] entry,
 *   skipped when the package is absent (`getServiceInfo` catch).
 * - Connected asynchronously: every future completes on the main executor and
 *   nothing ever blocks on `get()` before it is done — a blocking `get()` on
 *   the main thread deadlocks, because media3 completes the future on the
 *   main looper.
 * - Both suite players are `MediaLibraryService`s, so each controller is a
 *   [MediaBrowser]; that is what the Radio tile's "first playable child" and
 *   the quick-pick sheet browse with. A plain session falls back to a
 *   [MediaController].
 * - Reconnects when a suite player is installed / removed, and after the
 *   session drops while the hub is running.
 *
 * The owner calls [start] / [stop] (MainActivity: onStart / onStop). Nothing
 * runs in the background: xx-auto has no service and never will.
 */
@OptIn(UnstableApi::class)
class SessionHub(context: Context) {

    /** What one suite player is doing right now. */
    data class PlayerState(
        val source: Source,
        val installed: Boolean,
        val connected: Boolean,
        val isPlaying: Boolean,
        val hasItem: Boolean,
        val model: NowPlayingState.Model?,
        val lastActiveAt: Long,
    )

    /** Everything the drive screen needs from the players. */
    data class Snapshot(
        val players: Map<Source, PlayerState> = emptyMap(),
        /** The design's "active" session ([SessionPick]); null = Nothing playing. */
        val active: Source? = null,
        /** A one-line Warn for the card (a failed custom command), or null. */
        val warn: String? = null,
    ) {
        val activeModel: NowPlayingState.Model? get() = active?.let { players[it]?.model }
        val anyPlaying: Boolean get() = players.values.any { it.isPlaying }
        fun isPlaying(source: Source): Boolean = players[source]?.isPlaying == true
        fun isInstalled(source: Source): Boolean = players[source]?.installed == true
    }

    private val appContext = context.applicationContext
    private val main = ContextCompat.getMainExecutor(appContext)
    private val handler = Handler(Looper.getMainLooper())

    private val futures = mutableMapOf<Source, ListenableFuture<out MediaController>>()
    private val controllers = mutableMapOf<Source, MediaController>()
    private val installed = mutableMapOf<Source, Boolean>()
    private val lastActive = mutableMapOf<Source, Long>()
    private var warn: String? = null
    private var started = false

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private val clearWarn = Runnable {
        warn = null
        publish()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            val source = Source.entries.firstOrNull { it.packageName == pkg } ?: return
            AppLog.i(TAG, "package change ${intent.action} for $pkg — reconnecting")
            disconnect(source)
            connect(source)
            publish()
        }
    }

    /** Connects a controller for every suite player whose package is present. */
    fun start() {
        if (started) return
        started = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                appContext, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        Source.entries.forEach(::connect)
        publish()
    }

    /** Releases every controller and unregisters the package receiver. */
    fun stop() {
        if (!started) return
        started = false
        handler.removeCallbacksAndMessages(null)
        Source.entries.forEach(::disconnect)
        runCatching { appContext.unregisterReceiver(packageReceiver) }
        warn = null
        publish()
    }

    /** Re-reads every controller (the 1s position ticker calls this). */
    fun refresh() = publish()

    /** The connected controller for [source], or null. */
    fun controller(source: Source): MediaController? = controllers[source]

    // ── Transport (Phase 2) ─────────────────────────────────────────────

    fun playPause(source: Source) {
        val c = controllers[source] ?: return
        if (Util.shouldShowPlayButton(c)) Util.handlePlayButtonAction(c) else Util.handlePauseButtonAction(c)
    }

    fun next(source: Source) {
        controllers[source]?.let { Transport(it).seekToNext() }
    }

    fun previous(source: Source) {
        controllers[source]?.let { Transport(it).seekToPrevious() }
    }

    fun seekTo(source: Source, positionMs: Long) {
        controllers[source]?.let { Transport(it).seekTo(positionMs) }
    }

    /**
     * Presses one of the session's custom buttons. A custom command goes out
     * as `sendCustomCommand(SessionCommand(action, EMPTY), EMPTY)`; a failed
     * [SessionResult] becomes a one-line Warn on the card, nothing modal.
     */
    fun press(source: Source, button: CustomButtons.Button) {
        val c = controllers[source] ?: return
        val action = button.commandAction
        if (action == null) {
            when (button.playerCommand) {
                Player.COMMAND_PLAY_PAUSE -> playPause(source)
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> c.seekToNext()
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> c.seekToPrevious()
                Player.COMMAND_STOP -> c.stop()
            }
            return
        }
        val future = Transport(c).sendCustomCommand(action)
        future.addListener({
            val code = runCatching { future.get().resultCode }.getOrDefault(SessionResult.RESULT_ERROR_UNKNOWN)
            if (code != SessionResult.RESULT_SUCCESS) {
                AppLog.w(TAG, "custom command $action on $source failed: $code")
                showWarn("${button.label ?: "That button"} didn't go through")
            }
        }, main)
    }

    // ── Browsing (Phase 3: Radio tap + quick-pick) ──────────────────────

    /**
     * The children of [parentId] (null = the library root) in [source]'s
     * browse tree, or null when the session is not browsable / not connected.
     * The root is used as-is — no special-casing the radio's ids.
     */
    suspend fun children(source: Source, parentId: String? = null): List<MediaItem>? {
        val browser = controllers[source] as? MediaBrowser ?: return null
        val parent = parentId ?: runCatching {
            browser.getLibraryRoot(null).await().value?.mediaId
        }.getOrNull() ?: return null
        val result = runCatching { browser.getChildren(parent, 0, PAGE, null).await() }.getOrNull()
            ?: return null
        if (result.resultCode != LibraryResult.RESULT_SUCCESS) return null
        return result.value?.toList().orEmpty()
    }

    /**
     * The design's "first playable child of the library root". The radio's
     * root holds folders (speed dials, playlists), so when the root has no
     * playable child this looks one level down, into each browsable child in
     * order.
     */
    suspend fun firstPlayable(source: Source): MediaItem? {
        val root = children(source) ?: return null
        root.firstOrNull { it.mediaMetadata.isPlayable == true }?.let { return it }
        for (folder in root.filter { it.mediaMetadata.isBrowsable == true }) {
            children(source, folder.mediaId)
                ?.firstOrNull { it.mediaMetadata.isPlayable == true }
                ?.let { return it }
        }
        return null
    }

    /** `setMediaItem(item)` + `play()` — the quick-pick row tap. */
    fun play(source: Source, item: MediaItem) {
        val c = controllers[source] ?: return
        c.setMediaItem(item)
        c.prepare()
        c.play()
        lastActive[source] = now()
    }

    /**
     * The Radio / Audiobook tile tap: `play()` when the session holds an item,
     * else the first playable child of its library. Returns false when there
     * is nothing to play here, and the caller should open the app instead.
     */
    suspend fun resume(source: Source): Boolean {
        val c = controllers[source] ?: return false
        if (c.currentMediaItem != null) {
            Util.handlePlayButtonAction(c)
            lastActive[source] = now()
            publish()
            return true
        }
        val item = firstPlayable(source) ?: return false
        play(source, item)
        return true
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun isInstalled(source: Source): Boolean = try {
        appContext.packageManager.getServiceInfo(SuitePlayers.component(source), 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun connect(source: Source, plain: Boolean = false) {
        if (!started || futures.containsKey(source)) return
        val present = isInstalled(source)
        installed[source] = present
        // Skip a suite player whose package is absent (AU6) — nothing to control.
        if (!present) return
        val token = SessionToken(appContext, SuitePlayers.component(source))
        val listener = ControllerListener(source)
        val future: ListenableFuture<out MediaController> = if (plain) {
            MediaController.Builder(appContext, token).setListener(listener).buildAsync()
        } else {
            MediaBrowser.Builder(appContext, token).setListener(listener).buildAsync()
        }
        futures[source] = future
        future.addListener({ onConnected(source, future, plain) }, main)
    }

    private fun onConnected(source: Source, future: ListenableFuture<out MediaController>, plain: Boolean) {
        if (futures[source] !== future) {
            // Stopped or reconnected while connecting: drop this one.
            MediaController.releaseFuture(future)
            return
        }
        val controller = runCatching { future.get() }.getOrNull()
        if (controller == null) {
            futures.remove(source)
            if (!plain) {
                AppLog.w(TAG, "$source: browser connection refused, trying a plain controller")
                connect(source, plain = true)
            } else {
                AppLog.w(TAG, "$source: session refused the connection")
                publish()
            }
            return
        }
        AppLog.i(TAG, "$source connected")
        controller.addListener(PlayerListener(source))
        controllers[source] = controller
        if (controller.isPlaying) lastActive[source] = now()
        else if (controller.currentMediaItem != null) lastActive.putIfAbsent(source, 1L)
        publish()
    }

    private fun disconnect(source: Source) {
        controllers.remove(source)
        futures.remove(source)?.let { MediaController.releaseFuture(it) }
    }

    private fun showWarn(text: String) {
        warn = text
        handler.removeCallbacks(clearWarn)
        handler.postDelayed(clearWarn, WARN_MS)
        publish()
    }

    private fun publish() {
        val players = Source.entries.associateWith { source ->
            val c = controllers[source]
            PlayerState(
                source = source,
                installed = installed[source] ?: false,
                connected = c != null,
                isPlaying = c != null && isActivelyPlaying(c),
                hasItem = c?.currentMediaItem != null,
                model = c?.let(::model),
                lastActiveAt = lastActive[source] ?: 0L,
            )
        }
        // SessionPick wants most-recently-active first.
        val ordered = players.values.sortedByDescending { it.lastActiveAt }
        val index = SessionPick.active(ordered.map { SessionPick.State(it.isPlaying, it.hasItem) })
        _state.value = Snapshot(players = players, active = index?.let { ordered[it].source }, warn = warn)
    }

    /** Playing, or about to (buffering with play requested). */
    private fun isActivelyPlaying(c: Player): Boolean =
        c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING)

    private fun model(c: MediaController): NowPlayingState.Model {
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        return NowPlayingState.map(
            metadata = c.mediaMetadata,
            commands = c.availableCommands,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            // The card's play/pause glyph: ⏸ while playing or about to play.
            isPlaying = !Util.shouldShowPlayButton(c),
            isSeekable = duration > 0 && c.isCurrentMediaItemSeekable &&
                c.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            hasPrev = c.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS),
            hasNext = c.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT),
            mediaButtonPreferences = c.mediaButtonPreferences,
            customLayout = c.customLayout,
        )
    }

    private inner class PlayerListener(private val source: Source) : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (isActivelyPlaying(player) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && player.currentMediaItem != null
            ) {
                lastActive[source] = now()
            }
            publish()
        }
    }

    private inner class ControllerListener(private val source: Source) : MediaBrowser.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (controllers[source] !== controller) return
            AppLog.i(TAG, "$source disconnected")
            disconnect(source)
            publish()
            // The player's session went away (its process died, or it released
            // the session). Try again shortly while the drive screen is up.
            handler.postDelayed({ if (started) { connect(source); publish() } }, RECONNECT_MS)
        }

        override fun onMediaButtonPreferencesChanged(
            controller: MediaController,
            mediaButtonPreferences: MutableList<CommandButton>,
        ) = publish()

        override fun onCustomLayoutChanged(controller: MediaController, layout: MutableList<CommandButton>) =
            publish()

        override fun onAvailableSessionCommandsChanged(controller: MediaController, commands: SessionCommands) =
            publish()
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        addListener({
            runCatching { get() }
                .onSuccess { cont.resume(it) }
                .onFailure { cont.resumeWithException(it) }
        }, main)
        cont.invokeOnCancellation { cancel(false) }
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        const val TAG = "hub"
        const val PAGE = 100
        const val WARN_MS = 4_000L
        const val RECONNECT_MS = 3_000L
    }
}
