package com.piercingxx.xxauto.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.piercingxx.xxauto.R

/**
 * xx-auto's Ink palette, read from the vendored brand tokens
 * (`res/values/colors_brand.xml`). No hexes are retyped in the live palette —
 * [inkColors] reads each colour straight from the token resource, so a brand
 * change is a file swap in `colors_brand.xml`, not a hunt through Compose code.
 *
 * Big targets. Black ground. Nothing else. The ramp is the white-on-ink
 * opacity stops from the brand tokens; the accent is Signal white (no product
 * accent). Warn / Error appear only on the "not installed" tile state and a
 * failed call.
 */
data class InkColors(
    val ink: Color,
    val inkRaised: Color,
    val line: Color,
    val muted: Color,
    val text: Color,
    val signal: Color,
    val warn: Color,
    val error: Color,
)

/** The default Ink palette — the drive screen's ground regardless of theme. */
val InkPalette = InkColors(
    ink = Color(0xFF000000),
    inkRaised = Color(0xFF09090B),
    line = Color(0x1AFFFFFF),
    muted = Color(0x80FFFFFF),
    text = Color(0xE6FFFFFF),
    signal = Color(0xFFFFFFFF),
    warn = Color(0xFFFDBA74),
    error = Color(0xFFFF6767),
)

/**
 * The live Ink palette, read from the vendored brand tokens
 * (`res/values/colors_brand.xml`) so a brand change is a file swap, not a
 * retype. `MainActivity` provides it through [LocalInk] at the composition
 * root; [InkPalette] remains the fallback default for anything composed
 * outside that root (unit tests, previews).
 */
@Composable
fun inkColors(): InkColors = InkColors(
    ink = colorResource(R.color.pxx_ink),
    inkRaised = colorResource(R.color.pxx_ink_raised),
    line = colorResource(R.color.pxx_white_10),
    muted = colorResource(R.color.pxx_white_50),
    text = colorResource(R.color.pxx_white_90),
    signal = colorResource(R.color.pxx_signal),
    warn = colorResource(R.color.pxx_warn),
    error = colorResource(R.color.pxx_error),
)

/**
 * Composition local for the current ground. `MainActivity` provides it with
 * the resource-backed [inkColors] palette at the composition root; [InkPalette]
 * is the fallback default for anything composed outside that root. Phase 3
 * (`always_ink` off) swaps the provided palette for the ThemeStore ground,
 * light presets inverting the ramp like xx-camera.
 */
val LocalInk = staticCompositionLocalOf { InkPalette }
