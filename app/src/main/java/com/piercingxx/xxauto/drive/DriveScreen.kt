package com.piercingxx.xxauto.drive

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piercingxx.xxauto.calls.Favourites
import com.piercingxx.xxauto.calls.FavouritesSheet
import com.piercingxx.xxauto.media.ButtonIcons
import com.piercingxx.xxauto.media.CustomButtons
import com.piercingxx.xxauto.media.Source
import com.piercingxx.xxauto.nav.MapsLaunch
import com.piercingxx.xxauto.ui.components.Glyph
import com.piercingxx.xxauto.ui.components.GlyphButton
import com.piercingxx.xxauto.ui.components.Target
import com.piercingxx.xxauto.ui.icons.LineGlyphs
import com.piercingxx.xxauto.ui.icons.SessionGlyphs
import com.piercingxx.xxauto.ui.theme.InkColors
import com.piercingxx.xxauto.ui.theme.LocalInk
import kotlinx.coroutines.launch

/** Outer margin of the drive screen; tight enough for "Audiobook" at 24sp on a 360dp phone. */
private val Edge = 20.dp

/** Gap between tiles, and between the card and the tiles. */
private val Gap = 12.dp

/** Portrait tile height; in landscape the tiles share the column's height instead. */
private val TileHeight = 96.dp

/** Below this card height the card goes compact: one-line title, subtitle and time on one row. */
private val CompactCard = 420.dp

/** Transport and custom rows never stretch past this, so they stay under one thumb on a wide screen. */
private val RowMax = 440.dp

/**
 * The drive screen (design "Drive"). Ink ground; portrait is one column (card
 * over tiles), landscape is two (card left, tiles right). Big targets, black
 * ground, nothing else: no app bar, no status text, no clock — only the `⚙`
 * glyph in the bottom corner, which shares its row with the one-line note.
 *
 * Pure rendering over [state]; every tap goes through [actions].
 */
@Composable
fun DriveScreen(
    state: DriveUiState,
    actions: DriveActions,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles = DriveTiles.visible(state.settings)

    var quickPickOpen by remember { mutableStateOf(false) }
    var favourites by remember { mutableStateOf<List<Favourites.Contact>?>(null) }

    // AU8: the sheet reads the contacts provider. The Settings toggle asks for
    // the permission when Calls is switched on; if it was revoked since, the
    // tile asks again rather than failing silently.
    //
    // Only where there is something to ask through: the external display's
    // Presentation (AU13) has no ActivityResultRegistryOwner, and
    // rememberLauncherForActivityResult throws without one. There, a revoked
    // permission just leaves the tile inert; the phone screen asks.
    val canAsk = LocalActivityResultRegistryOwner.current != null
    val requestContacts = if (canAsk) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) scope.launch { favourites = actions.favourites() }
        }
    } else {
        null
    }
    val openFavourites = {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            scope.launch { favourites = actions.favourites() }
        } else {
            requestContacts?.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ink.ink)
            .safeDrawingPadding()
            .padding(Edge),
    ) {
        val landscape = maxWidth > maxHeight
        val showCard = state.settings.surfaceNowPlaying
        val showTiles = tiles.isNotEmpty()

        val card: @Composable (Modifier) -> Unit = { m ->
            NowPlayingCard(state = state, actions = actions, modifier = m)
        }
        val tileGrid: @Composable (Modifier, Boolean) -> Unit = { m, fill ->
            TileGrid(
                tiles = tiles,
                state = state,
                actions = actions,
                fill = fill,
                onQuickPick = { quickPickOpen = true },
                onCalls = { openFavourites() },
                modifier = m,
            )
        }
        val bottomBar: @Composable () -> Unit = {
            BottomBar(note = state.note, onSettings = actions::openSettings)
        }

        if (landscape && showCard && showTiles) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Edge),
            ) {
                card(Modifier.weight(1f).fillMaxHeight())
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    tileGrid(Modifier.fillMaxWidth().weight(1f), true)
                    bottomBar()
                }
            }
        } else {
            // Portrait, or landscape with one of the two halves switched off:
            // whatever is left takes the room.
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    showCard -> {
                        card(Modifier.fillMaxWidth().weight(1f))
                        if (showTiles) {
                            Spacer(Modifier.height(Gap))
                            tileGrid(Modifier.fillMaxWidth(), false)
                        }
                    }
                    showTiles -> tileGrid(Modifier.fillMaxWidth().weight(1f), true)
                    else -> Spacer(Modifier.weight(1f))
                }
                bottomBar()
            }
        }
    }

    if (quickPickOpen) {
        QuickPickSheet(
            browse = actions::browse,
            onPick = { item ->
                actions.pick(item)
                quickPickOpen = false
            },
            onDismiss = { quickPickOpen = false },
        )
    }

    favourites?.let { contacts ->
        FavouritesSheet(contacts = contacts, onDismiss = { favourites = null })
    }
}

/**
 * The bottom row: the one-line note (an action's transient warn, e.g.
 * "xx-apps not installed") on the left, the ⚙ glyph in the corner. The glyph
 * sits flush with the tiles' right edge; its 72dp target reaches inward.
 */
@Composable
private fun BottomBar(note: String?, onSettings: () -> Unit) {
    val ink = LocalInk.current
    Row(
        modifier = Modifier.fillMaxWidth().height(Target),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.warnText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GlyphButton(
            vector = LineGlyphs.Settings,
            description = "Settings",
            onClick = onSettings,
            tint = ink.muted,
            glyphSize = 30.dp,
            contentAlignment = Alignment.CenterEnd,
        )
    }
}

/**
 * The now-playing card (design item 1). Title, subtitle, a time row with a
 * thin progress line for seekable media, the transport row, and the session's
 * custom buttons in the order it gives them. Long-press opens the owning app.
 *
 * Left-aligned and vertically centred in whatever room it gets. Below
 * [CompactCard] (landscape phones) the title and subtitle drop to one line and
 * the time moves up beside the subtitle; if even that doesn't fit, the card
 * scrolls rather than clip the custom row (AU5: the thumbs must be there).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingCard(
    state: DriveUiState,
    actions: DriveActions,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val model = state.players.activeModel
    val source = state.players.active

    BoxWithConstraints(modifier = modifier) {
        val available = if (constraints.hasBoundedHeight) maxHeight else CompactCard
        val compact = available < CompactCard

        if (model == null || source == null) {
            // Nothing playing sits where a title would, in the title face, so
            // the empty card reads as a state, not a hole.
            val label = NothingPlaying.label(context.resources)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.displayLarge,
                    color = ink.muted,
                    modifier = Modifier.semantics { contentDescription = label },
                )
            }
            return@BoxWithConstraints
        }

        val title = model.title?.toString().orEmpty().ifBlank { "Untitled" }
        val subtitle = model.subtitle?.toString()?.takeIf { it.isNotBlank() }
        val timed = model.isSeekable && model.durationMs > 0
        val elapsed = model.positionMs.coerceIn(0L, model.durationMs.coerceAtLeast(0L))
        val timeText = if (timed) "${Elapsed.format(elapsed)} / ${Elapsed.format(model.durationMs)}" else null

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = available)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = actions::openActiveApp,
                        onLongClickLabel = "Open the app",
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge,
                    color = ink.text,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (compact) {
                    // Subtitle and time share a row.
                    if (subtitle != null || timeText != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = subtitle.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = ink.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            timeText?.let {
                                Spacer(Modifier.width(16.dp))
                                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ink.muted, maxLines = 1)
                            }
                        }
                    }
                    if (timed) {
                        Spacer(Modifier.height(8.dp))
                        Progress(elapsed, model.durationMs, ink)
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    subtitle?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (timed) {
                        Spacer(Modifier.height(24.dp))
                        Progress(elapsed, model.durationMs, ink)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(Elapsed.format(elapsed), style = MaterialTheme.typography.bodyMedium, color = ink.muted)
                            Spacer(Modifier.weight(1f))
                            Text(Elapsed.format(model.durationMs), style = MaterialTheme.typography.bodyMedium, color = ink.muted)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                TransportRow(
                    playing = model.isPlaying,
                    hasPrev = model.hasPrev,
                    hasNext = model.hasNext,
                    compact = compact,
                    actions = actions,
                )

                // AU5: for the radio this row is the three thumbs, and it must be here.
                val row = CustomButtons.layout(model, excludeTransport = true)
                if (row.buttons.isNotEmpty()) {
                    Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
                    CustomRow(row.buttons, ownerPackage = source.packageName, onPress = actions::press)
                }

                state.players.warn?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.warnText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A 2dp line: the ground's hairline, filled with text ink up to the position. */
@Composable
private fun Progress(positionMs: Long, durationMs: Long, ink: InkColors) {
    val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(ink.line),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(ink.text),
        )
    }
}

/**
 * `⏮ ▶/⏸ ⏭`, drawn. Play/pause is the one Signal element on the card, in a
 * Signal ring; previous / next are text ink, and shade when the session can't.
 * Spread across [RowMax] so the three sit over the three custom buttons below.
 */
@Composable
private fun TransportRow(
    playing: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    compact: Boolean,
    actions: DriveActions,
) {
    val ink = LocalInk.current
    val side = if (compact) Target else 80.dp
    val centre = if (compact) 80.dp else 96.dp
    Row(
        modifier = Modifier.widthIn(max = RowMax).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(LineGlyphs.Previous, "Previous", hasPrev, side, 36.dp, ink.text, actions::previous)
        Box(
            modifier = Modifier
                .size(centre)
                .border(2.dp, ink.signal, CircleShape)
                .clickable(role = Role.Button, onClick = actions::playPause)
                .semantics { contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) {
            Glyph(if (playing) LineGlyphs.Pause else LineGlyphs.Play, ink.signal, if (compact) 36.dp else 44.dp)
        }
        TransportButton(LineGlyphs.Next, "Next", hasNext, side, 36.dp, ink.text, actions::next)
    }
}

@Composable
private fun TransportButton(
    vector: ImageVector,
    label: String,
    enabled: Boolean,
    size: Dp,
    glyph: Dp,
    tint: Color,
    onClick: () -> Unit,
) {
    val ink = LocalInk.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(vector, if (enabled) tint else ink.shade, glyph)
    }
}

/**
 * The session's custom buttons, in its order. Three (the radio's thumbs)
 * spread under the transport row so each thumb sits under a transport
 * button; any other count packs from the start and scrolls if it must.
 */
@Composable
private fun CustomRow(
    buttons: List<CustomButtons.Button>,
    ownerPackage: String,
    onPress: (CustomButtons.Button) -> Unit,
) {
    if (buttons.size == 3) {
        Row(
            modifier = Modifier.widthIn(max = RowMax).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons.forEach { CustomButton(it, ownerPackage, onClick = { onPress(it) }) }
        }
    } else {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons.forEach { CustomButton(it, ownerPackage, onClick = { onPress(it) }) }
        }
    }
}

/**
 * One custom button. What it draws, in order: the session's own drawable
 * (loaded from the owning package, tinted to the ink); else a house line glyph
 * for its `CommandButton.ICON_*` constant; else its display name. A "filled"
 * icon (a thumb already given, a heart already set) inverts the button —
 * Signal ground, Ink glyph — instead of filling the glyph.
 */
@Composable
private fun CustomButton(button: CustomButtons.Button, ownerPackage: String, onClick: () -> Unit) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { GlyphSize.roundToPx() }
    val icon = remember(button, ownerPackage, sizePx) {
        if (button.renderAsIcon) {
            ButtonIcons.load(context, ownerPackage, button.iconResId, button.iconUri, sizePx)
        } else {
            null
        }
    }
    val glyph = remember(button.icon) { SessionGlyphs.forIcon(button.icon) }
    val label = button.label?.toString().orEmpty().ifBlank { glyph?.name.orEmpty() }
    val active = glyph?.active == true
    val tint = when {
        !button.isEnabled -> ink.shade
        active -> ink.ink
        else -> ink.text
    }
    val drawsText = icon == null && glyph?.vector == null
    Box(
        modifier = Modifier
            .sizeIn(minWidth = Target, minHeight = Target)
            .clip(if (drawsText) RoundedCornerShape(Target / 2) else CircleShape)
            .then(if (active && button.isEnabled) Modifier.background(ink.signal) else Modifier)
            .clickable(enabled = button.isEnabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                if (active) selected = true
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null -> Image(
                bitmap = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(GlyphSize),
            )
            glyph?.vector != null -> Box(contentAlignment = Alignment.Center) {
                Glyph(glyph.vector, tint, GlyphSize)
                glyph.badge?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            else -> Text(
                text = glyph?.text ?: label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 160.dp),
            )
        }
    }
}

/** Custom-button glyph size: a notch under the transport glyphs. */
private val GlyphSize = 34.dp

/**
 * The tiles (design item 2), two per row, in the design's order. [fill]: the
 * rows share the grid's height (landscape, or when the card is off);
 * otherwise each row is [TileHeight].
 */
@Composable
private fun TileGrid(
    tiles: Set<DriveTiles.Tile>,
    state: DriveUiState,
    actions: DriveActions,
    fill: Boolean,
    onQuickPick: () -> Unit,
    onCalls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = DriveTiles.Tile.entries.filter { it in tiles }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Gap)) {
        ordered.chunked(2).forEach { pair ->
            val rowModifier = if (fill) {
                Modifier.fillMaxWidth().weight(1f).heightIn(min = Target)
            } else {
                Modifier.fillMaxWidth().height(TileHeight)
            }
            Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(Gap)) {
                pair.forEach { tile ->
                    Tile(tile, state, actions, onQuickPick, onCalls, Modifier.weight(1f).fillMaxHeight())
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile(
    tile: DriveTiles.Tile,
    state: DriveUiState,
    actions: DriveActions,
    onQuickPick: () -> Unit,
    onCalls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInk.current
    val players = state.players
    val spec = when (tile) {
        DriveTiles.Tile.RADIO -> TileSpec(
            label = "Radio",
            detail = if (!players.isInstalled(Source.RADIO)) "not installed" else null,
            active = players.isPlaying(Source.RADIO),
            onClick = { actions.tileTap(Source.RADIO) },
            onLongClick = onQuickPick,
        )
        DriveTiles.Tile.AUDIOBOOK -> TileSpec(
            label = "Audiobook",
            detail = if (!players.isInstalled(Source.AUDIOBOOK)) "not installed" else null,
            active = players.isPlaying(Source.AUDIOBOOK),
            onClick = { actions.tileTap(Source.AUDIOBOOK) },
            onLongClick = { actions.openApp(Source.AUDIOBOOK) },
        )
        // AU7: the label switches Maps → xx-maps only in the not-installed state.
        DriveTiles.Tile.MAPS -> if (state.maps == MapsLaunch.Target.NOT_INSTALLED) {
            TileSpec(label = "xx-maps", detail = "not installed", active = false, onClick = actions::maps)
        } else {
            TileSpec(label = "Maps", detail = null, active = false, onClick = actions::maps)
        }
        DriveTiles.Tile.CALLS -> TileSpec(label = "Calls", detail = null, active = false, onClick = onCalls)
    }
    TileBox(spec, ink, modifier)
}

private data class TileSpec(
    val label: String,
    val detail: String?,
    val active: Boolean,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

/**
 * A Signal-outlined block. Active (that player is playing) inverts per
 * BRAND-GUIDE §3.1: Signal ground, Ink label. Not installed steps back: a
 * shade outline, a muted label, and "not installed" in Warn — the one place
 * the drive screen uses it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileBox(spec: TileSpec, ink: InkColors, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val notInstalled = spec.detail != null
    val ground = if (spec.active) ink.signal else ink.ink
    val outline = when {
        spec.active -> ink.signal
        notInstalled -> ink.shade
        else -> ink.signal
    }
    val labelColor = when {
        spec.active -> ink.ink
        notInstalled -> ink.muted
        else -> ink.text
    }
    // The press ripple takes the label's colour, so it shows on either ground.
    CompositionLocalProvider(LocalContentColor provides labelColor) {
        Column(
            modifier = modifier
                .heightIn(min = Target)
                .clip(shape)
                .background(ground)
                .border(2.dp, outline, shape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = spec.onClick,
                    onLongClick = spec.onLongClick,
                )
                // "action_" prefix: the smoke walker treats these as in-place
                // actions, not navigation targets.
                .semantics {
                    contentDescription = "action_" + spec.label.lowercase()
                    if (spec.active) stateDescription = "Playing"
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = spec.label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            spec.detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.warnText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
