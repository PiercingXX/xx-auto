package com.piercingxx.xxauto.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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
) {
    /** True on a light suite ground (Paper / Mist): the ramp is inverted. */
    val isLight: Boolean get() = ink.luminance() > 0.5f

    /** The 25% stop (BRAND-GUIDE `shade`): disabled glyphs, placeholders, handles. */
    val shade: Color get() = signal.copy(alpha = 0.25f)

    /**
     * Warn as text on this ground. The brand value is tuned for Ink; on Paper
     * or Mist it sinks to ~1.6:1, so a light ground pulls it halfway to the
     * (near-black) signal. Derived, not a new hex.
     */
    val warnText: Color get() = if (isLight) lerp(warn, signal, 0.5f) else warn

    /** Error as text on this ground; same rule as [warnText]. */
    val errorText: Color get() = if (isLight) lerp(error, signal, 0.35f) else error
}

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

/**
 * The palette on the launcher's synced ground (`always_ink` off, AU9). Same
 * rule as xx-camera's `Palette.forGround`: a dark ground keeps the white ramp;
 * a light preset (Paper, Mist) flips it — the ground becomes the "ink" and
 * near-black becomes the signal, on the same opacity stops. Warn / Error keep
 * their brand values. Alpha on the synced colour is dropped (grounds are
 * opaque).
 */
fun inkForGround(argb: Int, base: InkColors = InkPalette): InkColors {
    val ground = Color(argb).copy(alpha = 1f)
    val signal = if (ground.luminance() > 0.5f) Color.Black else Color.White
    return InkColors(
        ink = ground,
        inkRaised = signal.copy(alpha = 0.05f).compositeOver(ground),
        line = signal.copy(alpha = 0.10f),
        muted = signal.copy(alpha = 0.50f),
        text = signal.copy(alpha = 0.90f),
        signal = signal,
        warn = base.warn,
        error = base.error,
    )
}

/**
 * The app's theme root. `always_ink` on (the default): the resource-backed Ink
 * palette regardless of the launcher. Off: the stored suite ground
 * ([groundArgb], from [ThemeStore]) with light presets inverted.
 *
 * Material components (sheets, switches, ripples) read a colour scheme mapped
 * from the same ink, so nothing stock-purple leaks in, and the content colour
 * is the ink's text so press ripples show on a black ground. When the host is
 * an Activity, the status / navigation bar icons follow the ground: a light
 * ground (Paper, Mist) gets dark icons. The external display's Presentation
 * has no system bars and is skipped.
 */
@Composable
fun XxAutoTheme(alwaysInk: Boolean, groundArgb: Int, content: @Composable () -> Unit) {
    val base = inkColors()
    val ink = if (alwaysInk) base else inkForGround(groundArgb, base)

    val view = LocalView.current
    if (!view.isInEditMode) {
        val light = ink.isLight
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
        }
    }

    CompositionLocalProvider(LocalInk provides ink) {
        MaterialTheme(colorScheme = schemeFor(ink, scrim = base.ink), typography = Type, shapes = InkShapes) {
            CompositionLocalProvider(LocalContentColor provides ink.text, content = content)
        }
    }
}

/** Flat, small radii: blocks, not bubbles. */
private val InkShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Material's slots on the ink ramp (same mapping as xx-camera's `schemeFor`):
 * Signal is the only accent, surfaces are the ground or ink-raised, and there
 * is no tonal tint. [scrim] is the brand Ink on every ground.
 */
private fun schemeFor(ink: InkColors, scrim: Color): ColorScheme =
    (if (ink.isLight) lightColorScheme() else darkColorScheme()).copy(
        primary = ink.signal,
        onPrimary = ink.ink,
        primaryContainer = ink.inkRaised,
        onPrimaryContainer = ink.text,
        inversePrimary = ink.ink,
        secondary = ink.text,
        onSecondary = ink.ink,
        secondaryContainer = ink.inkRaised,
        onSecondaryContainer = ink.text,
        tertiary = ink.signal,
        onTertiary = ink.ink,
        tertiaryContainer = ink.inkRaised,
        onTertiaryContainer = ink.text,
        background = ink.ink,
        onBackground = ink.text,
        surface = ink.ink,
        onSurface = ink.text,
        surfaceVariant = ink.inkRaised,
        onSurfaceVariant = ink.muted,
        surfaceTint = ink.ink,
        inverseSurface = ink.signal,
        inverseOnSurface = ink.ink,
        error = ink.error,
        onError = ink.ink,
        errorContainer = ink.inkRaised,
        onErrorContainer = ink.errorText,
        outline = ink.muted,
        outlineVariant = ink.line,
        scrim = scrim,
        surfaceBright = ink.inkRaised,
        surfaceDim = ink.ink,
        surfaceContainerLowest = ink.ink,
        surfaceContainerLow = ink.inkRaised,
        surfaceContainer = ink.inkRaised,
        surfaceContainerHigh = ink.inkRaised,
        surfaceContainerHighest = ink.inkRaised,
    )

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
