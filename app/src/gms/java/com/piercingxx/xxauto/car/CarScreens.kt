package com.piercingxx.xxauto.car

import android.content.ComponentName
import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.piercingxx.xxauto.drive.NothingPlaying
import com.piercingxx.xxauto.log.AppLog
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.media.Source
import com.piercingxx.xxauto.nav.MapsHandoff
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The Android Auto session for [AutoCarAppService]. Owns one [SessionHub] for
 * as long as the car screen is up — the same hub, the same
 * [com.piercingxx.xxauto.media.SessionPick] rule and the same
 * [com.piercingxx.xxauto.media.NowPlayingState] / [CustomButtons] seams the
 * phone drive screen uses (the pure seam is why they are pure).
 */
class CarSession : Session() {

    lateinit var hub: SessionHub
        private set

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                hub = SessionHub(carContext).also { it.start() }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                hub.stop()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = NowPlayingCarScreen(carContext, hub)
}

/**
 * The car's now-playing screen: a ListTemplate of the active session's
 * title / subtitle, play-pause, skip, and its custom buttons in session order
 * (the radio's three thumbs, AU5). The header strip holds the radio
 * quick-pick and the Maps handoff. Re-renders on every hub change.
 */
class NowPlayingCarScreen(carContext: CarContext, private val hub: SessionHub) : Screen(carContext) {

    init {
        // Re-render on what the template shows, not on every position tick:
        // the host rate-limits template updates.
        lifecycleScope.launch {
            hub.state
                .map { s -> Triple(s.active, s.activeModel?.copy(positionMs = 0), Source.entries.map(s::isInstalled)) }
                .distinctUntilChanged()
                .collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val snapshot = hub.state.value
        val source = snapshot.active
        val model = snapshot.activeModel
        val rows = mutableListOf<Row>()

        if (source == null || model == null) {
            rows += Row.Builder()
                .setTitle(NothingPlaying.TEXT)
                .addText("Pick a station, or resume a book")
                .build()
            if (snapshot.isInstalled(Source.RADIO)) {
                rows += actionRow("Play the radio") { resume(Source.RADIO) }
            }
            if (snapshot.isInstalled(Source.AUDIOBOOK)) {
                rows += actionRow("Resume the audiobook") { resume(Source.AUDIOBOOK) }
            }
        } else {
            rows += Row.Builder()
                .setTitle(model.title?.toString()?.ifBlank { null } ?: "Untitled")
                .apply { model.subtitle?.toString()?.takeIf { it.isNotBlank() }?.let(::addText) }
                .build()
            rows += actionRow(if (model.isPlaying) "Pause" else "Play") { hub.playPause(source) }
            if (model.hasNext) rows += actionRow("Skip") { hub.next(source) }
            CustomButtons.layout(model, excludeTransport = true).buttons.forEach { button ->
                rows += Row.Builder()
                    .setTitle(button.label?.toString()?.ifBlank { null } ?: button.commandAction ?: "Button")
                    .setEnabled(button.isEnabled)
                    .setOnClickListener { hub.press(source, button) }
                    .build()
            }
            if (model.hasPrev) rows += actionRow("Previous") { hub.previous(source) }
        }

        val list = ItemList.Builder()
        rows.take(listLimit()).forEach(list::addItem)

        val strip = ActionStrip.Builder()
        if (snapshot.isInstalled(Source.RADIO)) {
            strip.addAction(
                Action.Builder()
                    .setTitle("Stations")
                    .setOnClickListener { screenManager.push(QuickPickCarScreen(carContext, hub, parentId = null, title = "Radio")) }
                    .build(),
            )
        }
        strip.addAction(
            Action.Builder()
                .setTitle("Maps")
                .setOnClickListener(::openMaps)
                .build(),
        )

        return ListTemplate.Builder()
            .setTitle("xx-auto")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list.build())
            .setActionStrip(strip.build())
            .build()
    }

    private fun resume(source: Source) {
        lifecycleScope.launch {
            if (!hub.resume(source)) {
                CarToast.makeText(carContext, "Open it on the phone first", CarToast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * The Maps entry fires the same Part 1 drive intent, aimed at xx-maps'
     * own car surface. xx-auto does not draw, wrap or proxy anything xx-maps
     * renders (contracts/XX-MAPS.md, "Crossing over on the car screen").
     */
    private fun openMaps() {
        val intent = Intent(MapsHandoff.ACTION_DRIVE)
            .setComponent(ComponentName(MapsHandoff.PACKAGE, XX_MAPS_CAR_SERVICE))
            .putExtra(MapsHandoff.EXTRA_FROM, carContext.packageName)
        runCatching { carContext.startCarApp(intent) }.onFailure {
            AppLog.w("car", "xx-maps car handoff failed", it)
            CarToast.makeText(carContext, "xx-maps isn't on this car screen", CarToast.LENGTH_LONG).show()
        }
    }

    private fun actionRow(title: String, onClick: () -> Unit): Row =
        Row.Builder().setTitle(title).setOnClickListener(onClick).build()

    private fun listLimit(): Int = runCatching {
        carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
    }.getOrDefault(6)

    private companion object {
        /** xx-maps' CarAppService (its gms source set). */
        const val XX_MAPS_CAR_SERVICE = "com.piercingxx.maps.car.CarAppService"
    }
}

/** The radio's browse tree on the car screen; the same children the phone quick-pick shows. */
class QuickPickCarScreen(
    carContext: CarContext,
    private val hub: SessionHub,
    private val parentId: String?,
    private val title: String,
) : Screen(carContext) {

    private var rows: List<androidx.media3.common.MediaItem>? = null
    private var failed = false

    init {
        lifecycleScope.launch {
            val result = hub.children(Source.RADIO, parentId)
            failed = result == null
            rows = result.orEmpty()
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val builder = ListTemplate.Builder().setTitle(title).setHeaderAction(Action.BACK)
        val items = rows ?: return builder.setLoading(true).build()
        val list = ItemList.Builder()
        if (failed || items.isEmpty()) {
            list.setNoItemsMessage(if (failed) "Radio isn't available" else "Nothing here")
        }
        items.forEach { item ->
            val meta = item.mediaMetadata
            val name = (meta.title ?: meta.displayTitle)?.toString() ?: item.mediaId
            val row = Row.Builder().setTitle(name)
            (meta.subtitle ?: meta.artist)?.toString()?.takeIf { it.isNotBlank() }?.let(row::addText)
            when {
                meta.isPlayable == true -> row.setOnClickListener {
                    hub.play(Source.RADIO, item)
                    screenManager.popToRoot()
                }
                meta.isBrowsable == true -> {
                    row.setBrowsable(true)
                    row.setOnClickListener {
                        screenManager.push(QuickPickCarScreen(carContext, hub, item.mediaId, name))
                    }
                }
            }
            list.addItem(row.build())
        }
        return builder.setSingleList(list.build()).build()
    }
}
