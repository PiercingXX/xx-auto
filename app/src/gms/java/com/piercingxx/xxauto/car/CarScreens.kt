package com.piercingxx.xxauto.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.NowPlayingState
import com.piercingxx.xxauto.media.SessionPick

/**
 * The Android Auto session for [AutoCarAppService] (Phase 9, AU12, `gms`
 * flavor only). [onCreateScreen] returns the now-playing [CarScreen]; the
 * service is media-category-only, so this surface never draws a map.
 */
class CarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = CarScreen(carContext)
}

/**
 * The car's now-playing screen. Picks the active suite session with the same
 * pure [SessionPick.active] rule the phone drive screen uses, then renders that
 * session's title, subtitle and custom buttons — the same [NowPlayingState] and
 * [CustomButtons] pure seams (the pure seam is why they are pure). A Maps row
 * hands off to xx-maps' own Android Auto surface via the Part 1 drive intent
 * (AU12, contracts/XX-MAPS.md); xx-auto never draws a map.
 *
 * The live controller observation (resolving each [SessionHub] controller's
 * metadata / commands / button preferences into [NowPlayingState.Model]s) is
 * the deferred-to-manual-QA wiring; [states] and [models] are fed here so the
 * active-session rule and the template structure stay correct and JVM-friendly.
 */
class CarScreen(
    carContext: CarContext,
    private val states: List<SessionPick.State> = emptyList(),
    private val models: List<NowPlayingState.Model> = emptyList(),
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        // The same rule the phone drive screen uses: a playing session wins,
        // then the most recent item-holding session, else nothing.
        val active = SessionPick.active(states)
        val model = active?.let { models.getOrNull(it) } ?: NowPlayingState.Model(
            title = null,
            subtitle = null,
            artworkUri = null,
            positionMs = 0,
            durationMs = 0,
            isPlaying = false,
            isSeekable = false,
            hasPrev = false,
            hasNext = false,
            customButtons = emptyList(),
        )

        val nowPlaying = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(model.title ?: "Nothing playing")
                    .addText(model.subtitle ?: "")
                    .build()
            )

        // The session's custom buttons, in session order — the same
        // [CustomButtons.layout] rule the phone drive screen uses.
        CustomButtons.layout(model).buttons.forEach { button ->
            nowPlaying.addRow(
                Row.Builder()
                    .setTitle(button.label ?: "")
                    .setEnabled(button.isEnabled)
                    .build()
            )
        }

        // A Maps row that fires the Part 1 drive intent: xx-maps owns
        // navigation on the car screen, xx-auto hands off and does not draw it.
        nowPlaying.addAction(
            Action.Builder()
                .setTitle("Maps")
                .setOnClickListener {
                    carContext.startCarApp(
                        Intent("com.piercingxx.maps.action.DRIVE")
                            .setPackage("com.piercingxx.maps")
                            .putExtra("com.piercingxx.maps.extra.FROM", "com.piercingxx.xxauto")
                    )
                }
                .build()
        )

        return PaneTemplate.Builder(nowPlaying.build())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}