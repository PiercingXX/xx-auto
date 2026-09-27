package com.piercingxx.xxauto.drive

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.piercingxx.xxauto.ui.components.InkSheet
import com.piercingxx.xxauto.ui.components.SheetBack
import com.piercingxx.xxauto.ui.components.SheetRow
import com.piercingxx.xxauto.ui.components.SheetStatus
import com.piercingxx.xxauto.ui.icons.LineGlyphs

/**
 * Quick-pick sheet (design: Radio long-press lists the radio's top-level
 * browse children — stations, speed dials — via `MediaBrowser`). Name rows;
 * a playable row plays (`setMediaItem` + `play()`), a browsable row opens
 * its children (marked with a chevron), and back — system back or the back
 * glyph in the header — steps out one level. The radio's library root is
 * used as-is: no special-casing its ids.
 */
@Composable
fun QuickPickSheet(
    browse: suspend (parentId: String?) -> List<QuickPickItem>?,
    onPick: (QuickPickItem) -> Unit,
    onDismiss: () -> Unit,
) {
    // The drill-down path; the last entry is the level being shown.
    val path = remember { mutableStateListOf<QuickPickItem?>(null) }
    var rows by remember { mutableStateOf<List<QuickPickItem>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val level = path.last()
    val stepOut = { path.removeAt(path.lastIndex) }

    LaunchedEffect(level) {
        rows = null
        failed = false
        val result = browse(level?.id)
        failed = result == null
        rows = result.orEmpty()
    }

    InkSheet(
        title = level?.title ?: "Radio",
        onDismiss = onDismiss,
        leading = if (path.size > 1) {
            { SheetBack(onClick = { stepOut() }) }
        } else {
            null
        },
    ) {
        // Inside the sheet: while it is up, system back goes to the sheet's
        // own dialog, whose default is dismiss; this later callback wins while
        // there is a level to step out of. Guarded because BackHandler throws
        // where no dispatcher can be found.
        if (LocalOnBackPressedDispatcherOwner.current != null) {
            BackHandler(enabled = path.size > 1) { stepOut() }
        }
        val current = rows
        when {
            current == null -> SheetStatus("Loading…")
            failed -> SheetStatus("Radio isn't available")
            current.isEmpty() -> SheetStatus("Nothing here")
            else -> LazyColumn {
                items(current, key = { it.id }) { item ->
                    val opens = item.browsable && !item.playable
                    SheetRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        trailing = if (opens) LineGlyphs.Chevron else null,
                        onClick = {
                            if (item.playable) onPick(item) else if (item.browsable) path.add(item)
                        },
                    )
                }
            }
        }
    }
}
