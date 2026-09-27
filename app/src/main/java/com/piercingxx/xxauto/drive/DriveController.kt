package com.piercingxx.xxauto.drive

import android.content.Context
import androidx.media3.common.MediaItem
import com.piercingxx.xxauto.calls.Favourites
import com.piercingxx.xxauto.log.AppLog
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.media.Source
import com.piercingxx.xxauto.nav.MapsHandoff
import com.piercingxx.xxauto.nav.SuiteStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The real [DriveActions]: routes taps to the [SessionHub], the Maps
 * handoff and the owning apps. Lives as long as MainActivity; [scope] is its
 * lifecycle scope, so nothing outlives the screen.
 */
class DriveController(
    private val context: Context,
    private val hub: SessionHub,
    private val scope: CoroutineScope,
    private val state: () -> DriveUiState,
    private val onNote: (String) -> Unit,
    private val beforeMaps: () -> Unit,
    private val onOpenSettings: () -> Unit,
) : DriveActions {

    /** Browse results by id, so a picked row can hand the session its real MediaItem. */
    private val browsed = mutableMapOf<String, MediaItem>()

    private val active: Source? get() = state().players.active

    override fun playPause() { active?.let(hub::playPause) }
    override fun next() { active?.let(hub::next) }
    override fun previous() { active?.let(hub::previous) }
    override fun seekTo(positionMs: Long) { active?.let { hub.seekTo(it, positionMs) } }

    override fun press(button: CustomButtons.Button) {
        active?.let { hub.press(it, button) }
    }

    override fun openActiveApp() {
        active?.let(::openApp)
    }

    override fun tileTap(source: Source) {
        if (!state().players.isInstalled(source)) {
            if (!SuiteStore.openListing(context, source.packageName)) onNote("Not installed")
            return
        }
        scope.launch {
            val played = runCatching { hub.resume(source) }.getOrDefault(false)
            // Nothing to play from here (the audiobook has no item, or the
            // session is not up yet): deep-link the app instead.
            if (!played) openApp(source)
        }
    }

    override fun openApp(source: Source) {
        if (!SuiteStore.openApp(context, source.packageName)) {
            if (!SuiteStore.openListing(context, source.packageName)) onNote("Not installed")
        }
    }

    override suspend fun browse(parentId: String?): List<QuickPickItem>? {
        val items = hub.children(Source.RADIO, parentId) ?: return null
        return items.map { item ->
            browsed[item.mediaId] = item
            QuickPickItem(
                id = item.mediaId,
                title = item.mediaMetadata.title?.toString()
                    ?: item.mediaMetadata.displayTitle?.toString()
                    ?: item.mediaId,
                subtitle = (item.mediaMetadata.subtitle ?: item.mediaMetadata.artist)?.toString(),
                browsable = item.mediaMetadata.isBrowsable == true,
                playable = item.mediaMetadata.isPlayable == true,
            )
        }
    }

    override fun pick(item: QuickPickItem) {
        val mediaItem = browsed[item.id] ?: MediaItem.Builder().setMediaId(item.id).build()
        hub.play(Source.RADIO, mediaItem)
    }

    override fun maps() {
        // AU13: release the external display first so xx-maps can claim it.
        beforeMaps()
        if (!MapsHandoff.launch(context, state().settings.alwaysInk)) {
            AppLog.w("drive", "maps: neither xx-maps nor xx-apps is installed")
            onNote("xx-maps and xx-apps are not installed")
        }
    }

    override suspend fun favourites(): List<Favourites.Contact> = withContext(Dispatchers.IO) {
        runCatching { Favourites.starred(context) }
            .onFailure { AppLog.w("calls", "starred query failed", it) }
            .getOrDefault(emptyList())
    }

    override fun openSettings() = onOpenSettings()
}
