package com.piercingxx.xxauto.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.piercingxx.xxauto.R
import com.piercingxx.xxauto.trigger.BondedDevices
import com.piercingxx.xxauto.ui.components.Glyph
import com.piercingxx.xxauto.ui.components.GlyphButton
import com.piercingxx.xxauto.ui.components.InkSheet
import com.piercingxx.xxauto.ui.components.SheetRow
import com.piercingxx.xxauto.ui.icons.LineGlyphs
import com.piercingxx.xxauto.ui.theme.InkColors
import com.piercingxx.xxauto.ui.theme.LocalInk
import kotlinx.coroutines.launch

/**
 * The settings screen (design "Settings"): a plain list, one toggle per row,
 * the suite's calm product register, then the static version + local-only
 * block. Back (arrow or system back) returns to Drive.
 *
 * Permission flows live here because this is where the user switches the
 * surfaces that need them (AU2, AU3, AU8):
 * - `auto_launch` on → BLUETOOTH_CONNECT → bonded-device picker → the overlay
 *   grant (one-line explanation); refused overlay → POST_NOTIFICATIONS once.
 * - `surface_calls` on → READ_CONTACTS + CALL_PHONE; no Contacts → the toggle
 *   snaps back off with a one-line reason. CALL_PHONE refused is fine: calls
 *   fall back to the dialer.
 */
@Composable
fun SettingsScreen(
    autoPrefs: AutoPrefs,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val settings by autoPrefs.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    fun set(transform: (Settings) -> Settings) {
        scope.launch { autoPrefs.update(transform) }
    }

    BackHandler(onBack = onBack)

    var devicePickerOpen by remember { mutableStateOf(false) }
    var autoLaunchNote by remember { mutableStateOf<String?>(null) }
    var callsNote by remember { mutableStateOf<String?>(null) }
    var overlayGranted by remember { mutableStateOf(canDrawOverlays(context)) }
    var notificationsAsked by remember { mutableStateOf(false) }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // Returning from "Display over other apps": refused → the notification
    // path, which needs POST_NOTIFICATIONS on 33+ (asked once, only here).
    LifecycleResumeEffect(Unit) {
        overlayGranted = canDrawOverlays(context)
        onPauseOrDispose { }
    }
    val overlaySettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = canDrawOverlays(context)
        if (!overlayGranted && !notificationsAsked && needsNotificationGrant(context)) {
            notificationsAsked = true
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val requestBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            devicePickerOpen = true
        } else {
            autoLaunchNote = "Needs Nearby devices permission to see the car."
        }
    }

    val requestCalls = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val contacts = result[Manifest.permission.READ_CONTACTS] == true || hasPermission(context, Manifest.permission.READ_CONTACTS)
        if (contacts) {
            callsNote = null
            set { it.copy(surfaceCalls = true) }
        } else {
            callsNote = "Calls needs Contacts access to show your starred contacts."
        }
    }

    fun startAutoLaunch() {
        autoLaunchNote = null
        if (Build.VERSION.SDK_INT >= 31 && !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            requestBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            devicePickerOpen = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ink.ink)
            .safeDrawingPadding(),
    ) {
        // Fixed header: the back glyph and the screen's title line.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphButton(LineGlyphs.Back, description = "Back", onClick = onBack, glyphSize = 26.dp)
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = ink.text,
                modifier = Modifier.semantics { heading() },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                SectionLabel("Car", ink, first = true)
                ToggleRow(
                    label = "Open when the car connects",
                    body = "Pick a Bluetooth device",
                    checked = settings.autoLaunch,
                    ink = ink,
                    onCheckedChange = { on ->
                        if (on) startAutoLaunch() else set { it.copy(autoLaunch = false) }
                    },
                )
                if (settings.autoLaunch) {
                    val device = settings.autoLaunchDevice
                    DetailRow(
                        label = "Car",
                        value = device?.let { BondedDevices.nameOf(context, it) } ?: "Pick a device",
                        ink = ink,
                        onClick = ::startAutoLaunch,
                    )
                    DetailRow(
                        label = if (overlayGranted) "Opens by itself" else "Opens from a notification",
                        value = if (overlayGranted) {
                            "Display over other apps is on. xx-auto never draws over anything; it only lets the screen open from the background."
                        } else {
                            "Allow Display over other apps to open with no tap. xx-auto never draws over anything."
                        },
                        ink = ink,
                        onClick = if (overlayGranted) null else {
                            { overlaySettings.launch(overlayIntent(context)) }
                        },
                    )
                }
                autoLaunchNote?.let { Note(it, ink) }

                SectionLabel("On the drive screen", ink)
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
                    label = "Calls tile",
                    body = "Asks for Contacts + Phone when switched on",
                    checked = settings.surfaceCalls,
                    ink = ink,
                    onCheckedChange = { on ->
                        if (on) {
                            requestCalls.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE))
                        } else {
                            callsNote = null
                            set { it.copy(surfaceCalls = false) }
                        }
                    },
                )
                callsNote?.let { Note(it, ink) }

                SectionLabel("Screen", ink)
                ToggleRow(
                    label = "Keep the screen on",
                    checked = settings.keepScreenOn,
                    ink = ink,
                    onCheckedChange = { on -> set { it.copy(keepScreenOn = on) } },
                )
                ToggleRow(
                    label = "Rotate with the phone",
                    body = "Off = landscape",
                    checked = settings.followRotation,
                    ink = ink,
                    onCheckedChange = { on -> set { it.copy(followRotation = on) } },
                )
                ToggleRow(
                    label = "Always black",
                    body = "Off = follow the suite theme",
                    checked = settings.alwaysInk,
                    ink = ink,
                    onCheckedChange = { on -> set { it.copy(alwaysInk = on) } },
                )

                // The static block: version, then the local-only statement.
                HorizontalDivider(color = ink.line, modifier = Modifier.padding(top = 24.dp, bottom = 20.dp))
                Text(
                    text = AboutVersion.line(versionName(context), versionCode(context)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.text,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.local_only_statement),
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.muted,
                )
            }
        }
    }

    if (devicePickerOpen) {
        DevicePicker(
            ink = ink,
            devices = remember { BondedDevices.list(context) },
            onPick = { device ->
                devicePickerOpen = false
                set { it.copy(autoLaunch = true, autoLaunchDevice = device.address) }
                // Then the overlay flow: explain in one line (the row above),
                // and go straight to the system page. Refusal is fine.
                if (!canDrawOverlays(context)) overlaySettings.launch(overlayIntent(context))
            },
            onDismiss = { devicePickerOpen = false },
        )
    }
}

@Composable
private fun DevicePicker(
    ink: InkColors,
    devices: List<BondedDevices.Device>,
    onPick: (BondedDevices.Device) -> Unit,
    onDismiss: () -> Unit,
) {
    InkSheet(title = "Which car?", onDismiss = onDismiss) {
        if (devices.isEmpty()) {
            Text(
                text = "No paired devices. Pair the car in Bluetooth settings first.",
                style = MaterialTheme.typography.bodyMedium,
                color = ink.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            )
        }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            devices.forEach { device ->
                SheetRow(title = device.name, subtitle = device.address, onClick = { onPick(device) })
            }
        }
    }
}

/** A quiet group label over a run of rows; a hairline above every group but the first. */
@Composable
private fun SectionLabel(text: String, ink: InkColors, first: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!first) HorizontalDivider(color = ink.line, modifier = Modifier.padding(top = 12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = ink.muted,
            modifier = Modifier
                .padding(top = if (first) 8.dp else 20.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
}

/**
 * One switch row: the label, an optional muted line under it, the switch at
 * the end. The whole row toggles. The switch is Signal when on (Ink thumb,
 * i.e. inverted like every active state) and a muted outline when off, on
 * either ground: on Paper / Mist the ramp has already flipped to near-black.
 */
@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    ink: InkColors,
    onCheckedChange: (Boolean) -> Unit,
    body: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onCheckedChange(!checked) })
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ink.text)
            body?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = ink.muted) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ink.ink,
                checkedTrackColor = ink.signal,
                checkedBorderColor = ink.signal,
                checkedIconColor = ink.signal,
                uncheckedThumbColor = ink.muted,
                uncheckedTrackColor = ink.ink,
                uncheckedBorderColor = ink.muted,
                uncheckedIconColor = ink.ink,
            ),
        )
    }
}

/** A sub-row under a toggle (indented), optionally tappable, with a chevron when it is. */
@Composable
private fun DetailRow(label: String, value: String, ink: InkColors, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.Center) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ink.text)
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = ink.muted)
        }
        if (onClick != null) Glyph(LineGlyphs.Chevron, ink.muted, 22.dp)
    }
}

@Composable
private fun Note(text: String, ink: InkColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = ink.warnText,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
    )
}

private fun hasPermission(context: Context, permission: String): Boolean =
    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun canDrawOverlays(context: Context): Boolean = android.provider.Settings.canDrawOverlays(context)

private fun needsNotificationGrant(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 && !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)

private fun overlayIntent(context: Context): Intent =
    Intent(
        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )

private fun versionName(context: Context): String? =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()

@Suppress("DEPRECATION")
private fun versionCode(context: Context): Long =
    runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }.getOrDefault(0L)
