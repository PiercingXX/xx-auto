package com.piercingxx.xxauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.piercingxx.xxauto.display.ExternalDisplay
import com.piercingxx.xxauto.drive.DriveController
import com.piercingxx.xxauto.drive.DriveScreen
import com.piercingxx.xxauto.drive.DriveUiState
import com.piercingxx.xxauto.log.AppLog
import com.piercingxx.xxauto.media.PositionTicker
import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.nav.MapsHandoff
import com.piercingxx.xxauto.nav.MapsLaunch
import com.piercingxx.xxauto.settings.AutoPrefs
import com.piercingxx.xxauto.settings.Settings
import com.piercingxx.xxauto.settings.SettingsScreen
import com.piercingxx.xxauto.trigger.AutoSession
import com.piercingxx.xxauto.ui.theme.ThemeStore
import com.piercingxx.xxauto.ui.theme.XxAutoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The single activity: the drive screen and its settings. singleTask
 * (manifest) so the auto-launch receiver (AU2) reuses it rather than
 * stacking a second.
 *
 * Lifecycle, and nothing outside it (xx-auto has no service and never will):
 * - onStart / onStop: the [SessionHub] controllers and the external display.
 * - onResume / onPause: the 1s position ticker, `keep_screen_on` and
 *   `follow_rotation` (applied from prefs while resumed, cleared on pause).
 * - onCreate / onDestroy: the `CAR_GONE` receiver (AU2 close path) and the
 *   launcher theme listener (`always_ink` off).
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: AutoPrefs
    private lateinit var hub: SessionHub
    private lateinit var controller: DriveController
    private lateinit var externalDisplay: ExternalDisplay

    private var ground by mutableIntStateOf(ThemeStore.DEFAULT_BACKGROUND)
    private var maps by mutableStateOf(MapsLaunch.Target.NOT_INSTALLED)
    private var note by mutableStateOf<String?>(null)
    private var stopThemeObserver: (() -> Unit)? = null

    private val ticker = PositionTicker(
        isPlaying = { hub.state.value.anyPlaying },
        onTick = { hub.refresh() },
    )

    private val carGone = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // AU2: only an auto-opened screen closes itself.
            if (AutoSession.autoOpened) {
                AppLog.i(TAG, "car gone — closing the auto-opened drive screen")
                AutoSession.autoOpened = false
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = AutoPrefs(this)
        hub = SessionHub(this)
        if (savedInstanceState == null) readLaunch(intent)

        val themeStore = ThemeStore(this)
        ground = themeStore.backgroundArgb
        stopThemeObserver = themeStore.observeBackground { ground = it }

        ContextCompat.registerReceiver(
            this, carGone, IntentFilter(AutoSession.ACTION_CAR_GONE), ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        externalDisplay = ExternalDisplay(this) { Root(onExternalDisplay = true) }
        controller = DriveController(
            context = this,
            hub = hub,
            scope = lifecycleScope,
            state = { currentState },
            onNote = ::showNote,
            beforeMaps = externalDisplay::releaseForMaps,
            onOpenSettings = { openSettings = true },
        )

        // AU9: keep_screen_on and follow_rotation, live while resumed.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                prefs.settings.collect(::applyWindow)
            }
        }

        setContent { Root(onExternalDisplay = false) }
    }

    /** Settings navigation, hoisted so the ⚙ action can reach it. */
    private var openSettings by mutableStateOf(false)

    private var currentState = DriveUiState()

    @Composable
    private fun Root(onExternalDisplay: Boolean) {
        val settings by prefs.settings.collectAsStateWithLifecycle(initialValue = null)
        val players by hub.state.collectAsStateWithLifecycle()
        val loaded = settings ?: return // first DataStore read is a few ms; avoid a default-flash
        val state = DriveUiState(settings = loaded, players = players, maps = maps, note = note)
        currentState = state
        XxAutoTheme(alwaysInk = loaded.alwaysInk, groundArgb = ground) {
            var showSettings by rememberSaveable { mutableStateOf(false) }
            if (!onExternalDisplay) {
                LaunchedEffect(openSettings) {
                    if (openSettings) {
                        showSettings = true
                        openSettings = false
                    }
                }
            }
            if (showSettings && !onExternalDisplay) {
                SettingsScreen(
                    autoPrefs = prefs,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { showSettings = false },
                )
            } else {
                DriveScreen(state = state, actions = controller, modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readLaunch(intent)
    }

    override fun onStart() {
        super.onStart()
        hub.start()
        externalDisplay.start()
    }

    override fun onResume() {
        super.onResume()
        maps = MapsHandoff.target(this)
        ticker.start()
        // Apply the window settings immediately, not a frame later.
        lifecycleScope.launch { applyWindow(prefs.settings.first()) }
    }

    override fun onPause() {
        ticker.stop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onStop() {
        externalDisplay.stop()
        hub.stop()
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(carGone) }
        stopThemeObserver?.invoke()
        super.onDestroy()
    }

    /**
     * AU2: remember whether the car-connect trigger opened this screen. Any
     * other open — the launcher icon, recents — is manual, and a manual open
     * never self-closes, even on top of an auto-opened one.
     */
    private fun readLaunch(intent: Intent?) {
        val fromHistory = (intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        AutoSession.autoOpened = !fromHistory && intent?.getBooleanExtra(AutoSession.EXTRA_AUTO_OPENED, false) == true
        if (::externalDisplay.isInitialized) externalDisplay.onFreshOpen()
    }

    private fun applyWindow(settings: Settings) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val orientation = if (settings.followRotation) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        if (requestedOrientation != orientation) requestedOrientation = orientation
    }

    private fun showNote(text: String) {
        note = text
        window.decorView.removeCallbacks(clearNote)
        window.decorView.postDelayed(clearNote, 4_000L)
    }

    private val clearNote = Runnable { note = null }

    private companion object {
        const val TAG = "main"
    }
}
