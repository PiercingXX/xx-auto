package com.piercingxx.xxauto.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.piercingxx.xxauto.R

// xx-auto type scale. Space Mono light for the title line, JetBrains Mono light
// for everything else (design: "Space Mono for the title line, JetBrains Mono
// for everything else, weight light"). Text sizes start at 24sp body / 40sp
// title; minimum touch target 72dp is layout, not type.
private val SpaceMono = FontFamily(Font(R.font.spacemono, FontWeight.Light))
private val JetBrainsMono = FontFamily(Font(R.font.jetbrainsmono, FontWeight.Light))

val Type = Typography(
    displayLarge = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 48.sp),
    titleLarge = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 32.sp),
    bodyLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 20.sp, lineHeight = 28.sp),
    bodySmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 20.sp, lineHeight = 28.sp),
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 24.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Light, fontSize = 14.sp, lineHeight = 20.sp),
)