package com.piercingxx.xxauto.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.piercingxx.xxauto.R
import com.piercingxx.xxauto.trigger.CarConnectReceiver
import com.piercingxx.xxauto.ui.theme.LocalInk
import kotlinx.coroutines.launch

/**
 * The settings screen (design "Settings"): a plain list, one toggle per row,
 * the suite's calm product register. Reads the current [Settings] from
 * [AutoPrefs] and writes each toggle back through it. Reached from the `⚙`
 * glyph on the drive screen (Phase 3); back returns to Drive.
 */
@Composable
fun SettingsScreen(
    autoPrefs: AutoPrefs,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val ink = LocalInk.current
    val settings by autoPrefs.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    fun set(transform: (Settings) -> Settings) {
        scope.launch { autoPrefs.update(transform) }
    }

    // AU2: the auto-launch receiver is enabled only while the toggle is on.
    // Flip its manifest component state to match, so it never wakes for a
    // connection the user has not opted into.
    val context = LocalContext.current
    LaunchedEffect(settings.autoLaunch) {
        CarConnectReceiver.setEnabled(context, settings.autoLaunch)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ink.ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = ink.text,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 16.dp),
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = ink.text,
            )
        }

        ToggleRow(
            label = "Open when the car connects — pick a Bluetooth device",
            checked = settings.autoLaunch,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(autoLaunch = on) } },
        )
        ToggleRow(
            label = "Now playing",
            checked = settings.surfaceNowPlaying,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(surfaceNowPlaying = on) } },
        )
        ToggleRow(
            label = "Radio and audiobook tiles",
            checked = settings.surfaceQuickPick,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(surfaceQuickPick = on) } },
        )
        ToggleRow(
            label = "Maps tile",
            checked = settings.surfaceNav,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(surfaceNav = on) } },
        )
        ToggleRow(
            label = "Calls tile — asks for Contacts + Phone when switched on",
            checked = settings.surfaceCalls,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(surfaceCalls = on) } },
        )
        ToggleRow(
            label = "Keep the screen on",
            checked = settings.keepScreenOn,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(keepScreenOn = on) } },
        )
        ToggleRow(
            label = "Rotate with the phone (off = landscape)",
            checked = settings.followRotation,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(followRotation = on) } },
        )
        ToggleRow(
            label = "Always black (off = follow the suite theme)",
            checked = settings.alwaysInk,
            ink = ink,
            onCheckedChange = { on -> set { it.copy(alwaysInk = on) } },
        )

        HorizontalDivider(color = ink.line, modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "xx-auto",
            style = MaterialTheme.typography.labelMedium,
            color = ink.muted,
        )
        Text(
            text = stringResource(R.string.local_only_statement),
            style = MaterialTheme.typography.labelMedium,
            color = ink.muted,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    ink: com.piercingxx.xxauto.ui.theme.InkColors,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ink.text,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}