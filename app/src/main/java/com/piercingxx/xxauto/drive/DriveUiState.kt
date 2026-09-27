package com.piercingxx.xxauto.drive

import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.nav.MapsLaunch
import com.piercingxx.xxauto.settings.Settings

/**
 * Everything the drive screen draws, in one value. MainActivity assembles it
 * from the settings flow, the [SessionHub] snapshot and the Maps resolution;
 * the composables only read it and call [DriveActions].
 */
data class DriveUiState(
    val settings: Settings = Settings(),
    val players: SessionHub.Snapshot = SessionHub.Snapshot(),
    /** Where the Maps tile goes; NOT_INSTALLED flips its label (AU7). */
    val maps: MapsLaunch.Target = MapsLaunch.Target.NOT_INSTALLED,
    /** A transient one-line note from an action (e.g. "xx-apps not installed"). */
    val note: String? = null,
)

/** One row of the quick-pick sheet: a browse child of the radio's library. */
data class QuickPickItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val browsable: Boolean,
    val playable: Boolean,
)
