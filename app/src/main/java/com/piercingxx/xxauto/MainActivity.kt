package com.piercingxx.xxauto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.piercingxx.xxauto.drive.DriveScreen
import com.piercingxx.xxauto.media.SessionHub
import com.piercingxx.xxauto.ui.theme.LocalInk
import com.piercingxx.xxauto.ui.theme.Type
import com.piercingxx.xxauto.ui.theme.inkColors

/**
 * The single activity: the drive screen. singleTask (manifest) so the
 * auto-launch receiver (AU2) reuses it rather than stacking a second.
 */
class MainActivity : ComponentActivity() {

    private var sessionHub: SessionHub? = null

    /**
     * The hub, exposed to the Compose tree as state so the drive screen can
     * resolve a connected [androidx.media3.session.MediaController] and build
     * its transport row. Null while the activity is stopped (nothing runs in
     * the background).
     */
    private var hubState by mutableStateOf<SessionHub?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalInk provides inkColors()) {
                MaterialTheme(typography = Type) {
                    DriveScreen(
                        modifier = Modifier.fillMaxSize(),
                        sessionHub = hubState,
                    )
                }
            }
        }
    }

    /**
     * Media controllers live only while the activity is visible (Phase 2 —
     * xx-auto has no service and never will): connect on start, release on
     * stop.
     */
    override fun onStart() {
        super.onStart()
        sessionHub = SessionHub(this).also { it.start() }
        hubState = sessionHub
    }

    override fun onStop() {
        sessionHub?.stop()
        sessionHub = null
        hubState = null
        super.onStop()
    }
}