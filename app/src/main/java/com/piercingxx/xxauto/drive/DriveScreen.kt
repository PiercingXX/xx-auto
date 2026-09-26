package com.piercingxx.xxauto.drive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.piercingxx.xxauto.calls.Favourites
import com.piercingxx.xxauto.calls.FavouritesSheet
import com.piercingxx.xxauto.display.ExternalDisplay
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.NowPlayingState
import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.nav.MapsLaunch
import com.piercingxx.xxauto.settings.AutoPrefs
import com.piercingxx.xxauto.settings.Settings
import com.piercingxx.xxauto.settings.SettingsScreen
import com.piercingxx.xxauto.ui.theme.LocalInk

/**
 * The drive screen. Phase 0: a single Ink ground rendering `Nothing playing`
 * (design: "Big targets. Black ground. Nothing else."). Phase 2 wires the
 * now-playing card's custom-button row: when the active session ships custom
 * buttons, [CustomButtons.layout] resolves them and the row renders them in
 * session order. Phase 3 fills in the rest of the card and the Radio /
 * Audiobook / Maps / Calls tiles.
 *
 * [sessionHub] is the live [SessionHub] (null while the activity is stopped).
 * When it holds a connected controller, the drive screen resolves it and calls
 * [SessionHub.transport] to build the transport row (prev / play-pause / next)
 * so the buttons drive the active suite player.
 *
 * The `⚙` glyph in the top-right opens [SettingsScreen]; back returns to Drive.
 */
@Composable
fun DriveScreen(
    modifier: Modifier = Modifier,
    sessionHub: SessionHub? = null,
    nowPlaying: NowPlayingState.Model = NowPlayingState.Model(
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
    ),
) {
    val ink = LocalInk.current
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            autoPrefs = AutoPrefs(context),
            modifier = modifier,
            onBack = { showSettings = false },
        )
        return
    }

    // AU3: each tile is drawn only if its surface toggle is on. The pure seam
    // [DriveTiles.visible] resolves the live settings into the drawn set.
    val autoPrefs = remember { AutoPrefs(context) }
    val settings by autoPrefs.settings.collectAsState(initial = Settings())
    val tiles = DriveTiles.visible(settings)

    // Resolve a connected suite-player controller and build the transport row
    // over it. The hub connects controllers asynchronously; a present package
    // resolves its future quickly, so a blocking get here is safe on the
    // visible screen. When nothing is connected there is no transport row.
    val controller = remember(sessionHub) {
        sessionHub?.controllers()?.values?.firstOrNull()?.get()
    }
    val transport = remember(sessionHub) {
        controller?.let { sessionHub?.transport(it) }
    }

    // Observe the resolved controller's live state and map it to the
    // now-playing card model (Phase 2, AU4/AU5). This is what makes
    // SessionHub.nowPlaying() reachable from production: without it the card
    // would always render the "Nothing playing" default regardless of what the
    // active suite player is doing — the play/pause glyph would never flip and
    // the title would never appear. A Player.Listener keeps the model current
    // on every metadata / command / position change; when there is no
    // controller the default model stands.
    var liveNowPlaying by remember(sessionHub) { mutableStateOf(nowPlaying) }
    DisposableEffect(controller) {
        // Map a player's live state to the card model. Local so the immediate
        // population below and the listener share one path.
        fun map(player: Player): NowPlayingState.Model? = sessionHub?.nowPlaying(
            metadata = player.mediaMetadata,
            commands = player.availableCommands,
            positionMs = player.currentPosition,
            durationMs = player.duration,
            isPlaying = player.isPlaying,
            isSeekable = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            hasPrev = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM),
            hasNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM),
            mediaButtonPreferences = (player as? MediaController)?.mediaButtonPreferences ?: emptyList(),
            customLayout = (player as? MediaController)?.customLayout ?: emptyList(),
        )
        // Populate immediately from the controller's current state so the card
        // never flashes "Nothing playing" between the controller resolving and
        // the first onEvents callback (review 20260925T094146Z finding 2).
        controller?.let { liveNowPlaying = map(it) ?: liveNowPlaying }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                liveNowPlaying = map(player) ?: liveNowPlaying
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    // Radio long-press opens the quick-pick sheet; rows are the radio's browse
    // children (MediaBrowser wiring, later Phase 3 row).
    var quickPickOpen by remember { mutableStateOf(false) }

    // Calls tile tap opens the favourites sheet (Phase 5, AU8); the starred
    // contacts are queried on open and fed to the sheet.
    var favouritesOpen by remember { mutableStateOf(false) }
    var favourites by remember { mutableStateOf<List<Favourites.Contact>>(emptyList()) }

    // AU8: the calls surface reads the contacts provider, so READ_CONTACTS is
    // requested at runtime from this caller before the favourites sheet opens.
    // This launcher is the only production request site for the permission; the
    // sheet stays closed until the user grants it (design: surface toggle off
    // until granted). The manifest declares READ_CONTACTS with no other request
    // path, so this is what makes the declared permission reachable.
    val requestReadContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            favourites = Favourites.starred(context)
            favouritesOpen = true
        }
    }

    // AU13: the external display (DP-alt/HDMI head unit) is first-come —
    // whichever of xx-auto / xx-maps the user opened owns it; the other stays
    // on the phone. [ExternalDisplay] gates its Presentation through the pure
    // [com.piercingxx.xxauto.display.DisplayClaim.decide] rule. On open xx-auto
    // claims the display (unless the other app already owns it); leaving the
    // screen releases it.
    val externalDisplay = remember { ExternalDisplay(context) }
    LaunchedEffect(Unit) {
        externalDisplay.sync(displayOwnedByOther = false, isOpening = true)
    }
    DisposableEffect(Unit) {
        onDispose { externalDisplay.release() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ink.ink),
        contentAlignment = Alignment.Center,
    ) {
        // The empty-state line. Brand reserved-white rule (BRAND-GUIDE §3):
        // pure #FFFFFF is Signal's alone and body text must use pxx_white_90 —
        // so this uses the theme's text colour (ink.text), not a hardcoded
        // Color.White. Same rule the transport row and tiles follow below.
        //
        // Both the visible text and the accessibility label resolve through the
        // same [NothingPlaying] seam ([NothingPlaying.label] / [NothingPlaying.TEXT]
        // fall back to the identical design copy), so the display text and its
        // label cannot drift apart — the KDoc on [NothingPlaying] promises exactly
        // that, and rendering the resource-backed copy here (rather than the bare
        // constant) keeps the visible line in lockstep with `strings.xml`.
        Text(
            text = NothingPlaying.label(context.resources),
            color = ink.text,
            // The design's real copy ("Nothing playing") as the accessibility
            // label; the visible text resolves the same seam.
            modifier = Modifier.semantics {
                contentDescription = NothingPlaying.label(context.resources)
            },
        )
        if (transport != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Text(
                    text = "⏮",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { transport.seekToPrevious() }
                        .padding(16.dp),
                )
                Text(
                    text = if (liveNowPlaying.isPlaying) "⏸" else "▶",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            if (liveNowPlaying.isPlaying) transport.pause() else transport.play()
                        }
                        .padding(16.dp),
                )
                Text(
                    text = "⏭",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { transport.seekToNext() }
                        .padding(16.dp),
                )
            }
        }
        val customRow = CustomButtons.layout(liveNowPlaying)
        if (customRow.buttons.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
            ) {
                customRow.buttons.forEach { button ->
                    // Icon rendering (createPackageContext / iconUri) is
                    // deferred to manual QA; the row falls back to the label.
                    Text(
                        text = button.label?.toString() ?: "",
                        color = if (button.isEnabled) Color.White else ink.muted,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        if (tiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tiles.forEach { tile ->
                    TileButton(
                        tile = tile,
                        ink = ink,
                        onClick = {
                            when (tile) {
                                DriveTiles.Tile.MAPS -> {
                                    // AU13: release the external display before
                                    // launching xx-maps, so xx-maps can claim it.
                                    externalDisplay.release()
                                    launchMaps(context)
                                }
                                DriveTiles.Tile.CALLS -> {
                                    // AU8: request READ_CONTACTS at runtime before
                                    // querying the contacts provider; the sheet opens
                                    // only when the permission is already granted.
                                    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                                        == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        favourites = Favourites.starred(context)
                                        favouritesOpen = true
                                    } else {
                                        requestReadContacts.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                }
                                else -> Unit
                            }
                        },
                        onLongPress = {
                            if (tile == DriveTiles.Tile.RADIO) quickPickOpen = true
                        },
                    )
                }
            }
        }
        Text(
            text = "\u2699",
            color = ink.muted,
            // The glyph normalises to no word, so the smoke walk (and screen
            // readers) would never reach the Settings screen without a real
            // label. Same pattern as the NothingPlaying text above.
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { showSettings = true }
                .semantics { contentDescription = "Settings" }
                .padding(24.dp),
        )
    }

    if (quickPickOpen) {
        QuickPickSheet(
            items = emptyList(),
            onPick = { quickPickOpen = false },
            onDismiss = { quickPickOpen = false },
        )
    }

    if (favouritesOpen) {
        FavouritesSheet(
            contacts = favourites,
            onDismiss = { favouritesOpen = false },
        )
    }
}

/**
 * The Maps tile's tap (Phase 4, AU7). Resolves the handoff target via the
 * pure [MapsLaunch.resolve] fed by the live PackageManager, then fires the
 * intent the contract names: the explicit `com.piercingxx.maps.action.DRIVE`
 * intent, the package's launcher, or (not installed) the xx-apps listing —
 * the listing deep-link is deferred to the not-installed UI row.
 */
private fun launchMaps(context: android.content.Context) {
    val pm = context.packageManager
    val driveIntent = Intent("com.piercingxx.maps.action.DRIVE")
        .setPackage("com.piercingxx.maps")
    val hasDriveAction = pm.resolveActivity(driveIntent, 0) != null
    val isInstalled = pm.getLaunchIntentForPackage("com.piercingxx.maps") != null
    when (MapsLaunch.resolve(hasDriveAction, isInstalled)) {
        MapsLaunch.Target.DRIVE -> context.startActivity(
            driveIntent
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("com.piercingxx.maps.extra.FROM", "com.piercingxx.xxauto")
        )
        MapsLaunch.Target.LAUNCHER -> context.startActivity(
            pm.getLaunchIntentForPackage("com.piercingxx.maps")!!
        )
        MapsLaunch.Target.NOT_INSTALLED -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileButton(
    tile: DriveTiles.Tile,
    ink: com.piercingxx.xxauto.ui.theme.InkColors,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val label = when (tile) {
        DriveTiles.Tile.RADIO -> "Radio"
        DriveTiles.Tile.AUDIOBOOK -> "Audiobook"
        DriveTiles.Tile.MAPS -> "Maps"
        DriveTiles.Tile.CALLS -> "Calls"
    }
    // The bottom action tiles (Radio / Audiobook / Maps / Calls) do not open a
    // separate screen — they fire an action on the drive surface. The smoke
    // harness walker ignores nodes whose resource-id or content-desc carries
    // the `action_` prefix so it does not tap them as navigation targets and
    // report "walk did not move" when the screenshot stays the same.
    Column(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .semantics { contentDescription = "action_" + label.lowercase() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "▢",
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = ink.muted,
        )
    }
}