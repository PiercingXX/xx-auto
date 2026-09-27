package com.piercingxx.xxauto.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.piercingxx.xxauto.R

// xx-auto type scale. Space Mono light for the title line, JetBrains Mono light
// for everything else (design: "Space Mono for the title line, JetBrains Mono
// for everything else, weight light"). Text sizes start at 24sp body / 40sp
// title; minimum touch target 72dp is layout, not type.
//
// Every Material slot is set, so no stock component falls back to Roboto.
//
// Where each one is used:
// - displayLarge   40  Space Mono   the now-playing title, "Nothing playing"
// - titleLarge     24  Space Mono   a screen or sheet title ("Settings", "Radio")
// - bodyLarge      24  JetBrains    drive-screen body: subtitle, tile labels, sheet rows
// - bodyMedium     20  JetBrains    settings rows, the time row, one-line notes
// - bodySmall      16  JetBrains    supporting lines under a row
// - labelLarge     20  JetBrains    a custom button's text fallback
// - labelMedium    16  JetBrains    a tile's second line ("not installed")
// - labelSmall     14  JetBrains    section labels, a glyph's seconds badge
private val SpaceMono = FontFamily(Font(R.font.spacemono, FontWeight.Light))
private val JetBrainsMono = FontFamily(Font(R.font.jetbrainsmono, FontWeight.Light))

private fun space(size: TextUnit, line: TextUnit) =
    TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Light, fontSize = size, lineHeight = line)

private fun mono(size: TextUnit, line: TextUnit, tracking: TextUnit = 0.sp) =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Light,
        fontSize = size,
        lineHeight = line,
        letterSpacing = tracking,
    )

val Type = Typography(
    displayLarge = space(40.sp, 48.sp),
    displayMedium = space(36.sp, 44.sp),
    displaySmall = space(32.sp, 40.sp),
    headlineLarge = space(32.sp, 40.sp),
    headlineMedium = space(28.sp, 36.sp),
    headlineSmall = space(24.sp, 32.sp),
    titleLarge = space(24.sp, 32.sp),
    titleMedium = mono(20.sp, 28.sp),
    titleSmall = mono(16.sp, 24.sp),
    bodyLarge = mono(24.sp, 32.sp),
    bodyMedium = mono(20.sp, 28.sp),
    bodySmall = mono(16.sp, 24.sp),
    labelLarge = mono(20.sp, 28.sp),
    labelMedium = mono(16.sp, 24.sp),
    labelSmall = mono(14.sp, 20.sp, tracking = 0.5.sp),
)
