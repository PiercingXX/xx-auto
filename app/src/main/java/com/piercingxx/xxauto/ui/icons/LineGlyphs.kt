package com.piercingxx.xxauto.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The drive screen's glyphs, drawn in the house line language: one stroke
 * weight, round caps and joins, no fill (BRAND-GUIDE §5, the same rules the
 * app mark follows). Unicode ⏮ ▶ ⏸ ⏭ ⚙ render differently in every font and
 * sometimes as colour emoji; these don't.
 *
 * 24-unit viewport, content inside 3..21. The paint colour here is a
 * placeholder: every glyph is drawn through `Icon(tint = …)`, which replaces
 * it with the current ink.
 */
object LineGlyphs {

    /** Stroke on the 24 grid: about a tenth of the glyph, like the mark's 4 on 40. */
    private const val STROKE = 1.75f

    val Play = glyph("play", "M8.5,5.5 L19,12 L8.5,18.5 Z")
    val Pause = glyph("pause", "M9,5.5 V18.5 M15,5.5 V18.5")
    val Next = glyph("next", "M6,5.5 L14.5,12 L6,18.5 Z M18,5.5 V18.5")
    val Previous = glyph("previous", "M18,5.5 L9.5,12 L18,18.5 Z M6,5.5 V18.5")
    val Stop = glyph("stop", "M7,7 H17 V17 H7 Z")
    val FastForward = glyph("fast_forward", "M3.5,6.5 L11,12 L3.5,17.5 Z M12.5,6.5 L20,12 L12.5,17.5 Z")
    val Rewind = glyph("rewind", "M20.5,6.5 L13,12 L20.5,17.5 Z M11.5,6.5 L4,12 L11.5,17.5 Z")

    /** A circular arrow turning clockwise, open at the upper right; seconds go inside. */
    val SkipForward = glyph("skip_forward", "M19.5,13 A7.5,7.5 0 1,1 12,5.5 M9.5,3 L12,5.5 L9.5,8")
    val SkipBack = glyph("skip_back", "M4.5,13 A7.5,7.5 0 1,0 12,5.5 M14.5,3 L12,5.5 L14.5,8")

    val ThumbUp = glyph("thumb_up", THUMB_CUFF, THUMB_HAND)
    val ThumbDown = glyph("thumb_down", THUMB_CUFF, THUMB_HAND, rotate = 180f)
    val Minus = glyph("minus", "M5,12 H19")
    val Plus = glyph("plus", "M12,5 V19 M5,12 H19")
    val Heart = glyph(
        "heart",
        "M12,19.5 L4.9,12.6 C2.9,10.6 3.2,7.3 5.6,5.9 C7.7,4.7 10.3,5.3 12,7.4 " +
            "C13.7,5.3 16.3,4.7 18.4,5.9 C20.8,7.3 21.1,10.6 19.1,12.6 Z",
    )
    val Star = glyph(
        "star",
        "M12,3.8 L14.29,9.64 L20.56,10.02 L15.71,14.01 L17.29,20.08 L12,16.7 " +
            "L6.71,20.08 L8.29,14.01 L3.44,10.02 L9.71,9.64 Z",
    )
    val Bookmark = glyph("bookmark", "M6.5,3.5 H17.5 V20.5 L12,16 L6.5,20.5 Z")
    val Flag = glyph("flag", "M5.5,20.5 V3.5 M5.5,4.5 H18 L15.5,8.75 L18,13 H5.5")

    val Shuffle = glyph("shuffle", SHUFFLE)
    val ShuffleOff = glyph("shuffle_off", SHUFFLE, STRIKE)
    val Repeat = glyph("repeat", REPEAT)
    val RepeatOne = glyph("repeat_one", REPEAT, "M11,10.6 L12.2,10 V14")
    val RepeatOff = glyph("repeat_off", REPEAT, STRIKE)

    val CheckCircle = glyph("check_circle", CIRCLE, "M8.3,12.3 L10.9,14.9 L15.9,9.6")
    val PlusCircle = glyph("plus_circle", CIRCLE, "M12,8.5 V15.5 M8.5,12 H15.5")
    val MinusCircle = glyph("minus_circle", CIRCLE, "M8.5,12 H15.5")
    val Block = glyph("block", CIRCLE, "M6,18 L18,6")

    val ListAdd = glyph("list_add", LIST, "M17.5,13 V20 M14,16.5 H21")
    val ListRemove = glyph("list_remove", LIST, "M15,14 L20,19 M20,14 L15,19")
    val ListNext = glyph("list_next", LIST, "M15,13.5 L18.5,16.5 L15,19.5")

    val Speed = glyph("speed", "M4.64,17.25 A8.5,8.5 0 1,1 19.36,17.25 M12,13 L16,9")
    val Share = glyph(
        "share",
        "M18,3.3 a2.2,2.2 0 1,1 0,4.4 a2.2,2.2 0 1,1 0,-4.4 " +
            "M6,9.8 a2.2,2.2 0 1,1 0,4.4 a2.2,2.2 0 1,1 0,-4.4 " +
            "M18,16.3 a2.2,2.2 0 1,1 0,4.4 a2.2,2.2 0 1,1 0,-4.4 " +
            "M8,11 L16,6.6 M8,13 L16,17.4",
    )

    /** The drive screen's one piece of chrome (design: "a ⚙ glyph"). */
    val Settings = glyph(
        "settings",
        "M19.05,10.11 L21.4,10.6 L21.4,13.4 L19.05,13.89 L18.32,15.65 L19.64,17.65 L17.65,19.64 " +
            "L15.65,18.32 L13.89,19.05 L13.4,21.4 L10.6,21.4 L10.11,19.05 L8.35,18.32 L6.35,19.64 " +
            "L4.36,17.65 L5.68,15.65 L4.95,13.89 L2.6,13.4 L2.6,10.6 L4.95,10.11 L5.68,8.35 " +
            "L4.36,6.35 L6.35,4.36 L8.35,5.68 L10.11,4.95 L10.6,2.6 L13.4,2.6 L13.89,4.95 " +
            "L15.65,5.68 L17.65,4.36 L19.64,6.35 L18.32,8.35 Z " +
            "M12,9 a3,3 0 1,1 0,6 a3,3 0 1,1 0,-6",
        stroke = 1.5f,
    )
    val Back = glyph("back", "M19,12 H5 M11,6 L5,12 L11,18")
    val Chevron = glyph("chevron", "M9.5,6 L15.5,12 L9.5,18")

    private fun glyph(
        name: String,
        vararg paths: String,
        rotate: Float = 0f,
        stroke: Float = STROKE,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = "xxauto.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        builder.addGroup(rotate = rotate, pivotX = 12f, pivotY = 12f)
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        builder.clearGroup()
        return builder.build()
    }
}

private const val THUMB_CUFF = "M3.5,10.5 H7 V19.5 H3.5 Z"
private const val THUMB_HAND =
    "M7,10.5 L10.6,3.8 C11.9,3.8 12.9,5 12.6,6.3 L12,9.5 H18.2 C19.5,9.5 20.5,10.8 20.1,12.1 " +
        "L18.3,18.3 C18.1,19 17.4,19.5 16.7,19.5 H7"
private const val SHUFFLE =
    "M3.5,7 H6.5 C10,7 11,17 14.5,17 H20 M3.5,17 H6.5 C10,17 11,7 14.5,7 H20 " +
        "M17,4 L20,7 L17,10 M17,14 L20,17 L17,20"
private const val REPEAT =
    "M4,12 V9.5 C4,8.1 5.1,7 6.5,7 H19.5 M16.5,4 L19.5,7 L16.5,10 " +
        "M20,12 V14.5 C20,15.9 18.9,17 17.5,17 H4.5 M7.5,14 L4.5,17 L7.5,20"
private const val STRIKE = "M3.5,3.5 L20.5,20.5"
private const val CIRCLE = "M12,3.5 a8.5,8.5 0 1,1 0,17 a8.5,8.5 0 1,1 0,-17"
private const val LIST = "M4,6.5 H16 M4,11.5 H16 M4,16.5 H11"
