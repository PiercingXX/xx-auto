package com.piercingxx.xxauto.calls

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piercingxx.xxauto.ui.theme.LocalInk

/**
 * The calls surface's favourites sheet (Phase 5, AU8). Design: "Starred contacts
 * as tiles, `ACTION_CALL` on tap." Name + number rows, each ≥ 72dp, at most 8
 * shown (more scrolls). Tapping a row fires `ACTION_CALL` with a `tel:` URI;
 * if `CALL_PHONE` is denied the sheet falls back to `ACTION_DIAL` and says so
 * once. Empty state: `No starred contacts` in `muted` — no instructions, no
 * link. The drive screen feeds [Favourites.starred] here on the Calls tile tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesSheet(
    contacts: List<Favourites.Contact>,
    onDismiss: () -> Unit,
) {
    val ink = LocalInk.current
    val context = LocalContext.current
    var dialFallback by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            if (contacts.isEmpty()) {
                Text(
                    text = "No starred contacts",
                    style = MaterialTheme.typography.titleMedium,
                    color = ink.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
            } else {
                contacts.take(8).forEach { contact ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .clickable {
                                dial(context, contact) { dialFallback = true }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ink.text,
                        )
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ink.muted,
                        )
                    }
                }
            }
            if (dialFallback) {
                Text(
                    text = "Call permission denied — dialing instead",
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.muted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** The dial action for a contact: `ACTION_CALL`, or `ACTION_DIAL` when
 *  `CALL_PHONE` is denied. Pure so the fallback is JVM-testable. */
fun dialAction(callPhoneDenied: Boolean): String =
    if (callPhoneDenied) Intent.ACTION_DIAL else Intent.ACTION_CALL

/** Fires `ACTION_CALL` with the contact's `tel:` URI; on a denied `CALL_PHONE`
 *  falls back to `ACTION_DIAL` and reports the fallback once. */
private fun dial(
    context: android.content.Context,
    contact: Favourites.Contact,
    onFallback: () -> Unit,
) {
    val tel = Uri.fromParts("tel", contact.number, null)
    try {
        context.startActivity(Intent(dialAction(callPhoneDenied = false), tel))
    } catch (e: SecurityException) {
        onFallback()
        context.startActivity(Intent(dialAction(callPhoneDenied = true), tel))
    }
}