package com.piercingxx.xxauto.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piercingxx.xxauto.ui.theme.LocalInk

/**
 * Quick-pick sheet (design "Radio long-press: `QuickPickSheet` from
 * `MediaBrowser.getChildren(root)` — name rows, tap = `setMediaItem(child)` +
 * `play()`"). A bottom sheet of name rows; tapping a row calls [onPick] with
 * that item's name. The drive screen wires it to the Radio tile's long-press;
 * the rows are the radio's top-level browse children, populated by the
 * MediaBrowser wiring (Phase 3, later row).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPickSheet(
    items: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = LocalInk.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            items.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(name) }
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }
        }
    }
}