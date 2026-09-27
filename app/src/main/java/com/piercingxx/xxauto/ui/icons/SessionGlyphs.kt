package com.piercingxx.xxauto.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.media3.session.CommandButton

/**
 * What the card draws for a session's `CommandButton.icon` constant when the
 * owning package's own drawable can't be loaded (AU5: the radio's thumbs must
 * render even then). The order on the card is: the session's drawable, then
 * this glyph, then the button's display name.
 *
 * A glyph is either a line [vector] (optionally with a short [badge] inside,
 * e.g. the seconds of a skip) or, for the fixed playback speeds, plain
 * [text]. [active] marks a "filled" icon, which on this brand is not a fill:
 * the button inverts (Signal ground, Ink glyph, BRAND-GUIDE §3.1). [name] is
 * the spoken fallback when the session gave no display name.
 */
data class SessionGlyph(
    val name: String,
    val vector: ImageVector? = null,
    val badge: String? = null,
    val text: String? = null,
    val active: Boolean = false,
)

object SessionGlyphs {

    /** The glyph for [icon], or null when it is `ICON_UNDEFINED` or one this app doesn't draw. */
    fun forIcon(icon: Int): SessionGlyph? = when (icon) {
        CommandButton.ICON_THUMB_UP_UNFILLED -> SessionGlyph("Like", LineGlyphs.ThumbUp)
        CommandButton.ICON_THUMB_UP_FILLED -> SessionGlyph("Liked", LineGlyphs.ThumbUp, active = true)
        CommandButton.ICON_THUMB_DOWN_UNFILLED -> SessionGlyph("Dislike", LineGlyphs.ThumbDown)
        CommandButton.ICON_THUMB_DOWN_FILLED -> SessionGlyph("Disliked", LineGlyphs.ThumbDown, active = true)
        CommandButton.ICON_MINUS -> SessionGlyph("Neutral", LineGlyphs.Minus)
        CommandButton.ICON_PLUS -> SessionGlyph("Add", LineGlyphs.Plus)

        CommandButton.ICON_HEART_UNFILLED -> SessionGlyph("Favourite", LineGlyphs.Heart)
        CommandButton.ICON_HEART_FILLED -> SessionGlyph("Favourited", LineGlyphs.Heart, active = true)
        CommandButton.ICON_STAR_UNFILLED -> SessionGlyph("Star", LineGlyphs.Star)
        CommandButton.ICON_STAR_FILLED -> SessionGlyph("Starred", LineGlyphs.Star, active = true)
        CommandButton.ICON_BOOKMARK_UNFILLED -> SessionGlyph("Bookmark", LineGlyphs.Bookmark)
        CommandButton.ICON_BOOKMARK_FILLED -> SessionGlyph("Bookmarked", LineGlyphs.Bookmark, active = true)
        CommandButton.ICON_FLAG_UNFILLED -> SessionGlyph("Flag", LineGlyphs.Flag)
        CommandButton.ICON_FLAG_FILLED -> SessionGlyph("Flagged", LineGlyphs.Flag, active = true)

        CommandButton.ICON_SHUFFLE_ON, CommandButton.ICON_SHUFFLE_STAR -> SessionGlyph("Shuffle", LineGlyphs.Shuffle)
        CommandButton.ICON_SHUFFLE_OFF -> SessionGlyph("Shuffle off", LineGlyphs.ShuffleOff)
        CommandButton.ICON_REPEAT_ALL -> SessionGlyph("Repeat", LineGlyphs.Repeat)
        CommandButton.ICON_REPEAT_ONE -> SessionGlyph("Repeat one", LineGlyphs.RepeatOne)
        CommandButton.ICON_REPEAT_OFF -> SessionGlyph("Repeat off", LineGlyphs.RepeatOff)

        CommandButton.ICON_SKIP_FORWARD -> SessionGlyph("Skip forward", LineGlyphs.SkipForward)
        CommandButton.ICON_SKIP_FORWARD_5 -> SessionGlyph("Forward 5 seconds", LineGlyphs.SkipForward, badge = "5")
        CommandButton.ICON_SKIP_FORWARD_10 -> SessionGlyph("Forward 10 seconds", LineGlyphs.SkipForward, badge = "10")
        CommandButton.ICON_SKIP_FORWARD_15 -> SessionGlyph("Forward 15 seconds", LineGlyphs.SkipForward, badge = "15")
        CommandButton.ICON_SKIP_FORWARD_30 -> SessionGlyph("Forward 30 seconds", LineGlyphs.SkipForward, badge = "30")
        CommandButton.ICON_SKIP_BACK -> SessionGlyph("Skip back", LineGlyphs.SkipBack)
        CommandButton.ICON_SKIP_BACK_5 -> SessionGlyph("Back 5 seconds", LineGlyphs.SkipBack, badge = "5")
        CommandButton.ICON_SKIP_BACK_10 -> SessionGlyph("Back 10 seconds", LineGlyphs.SkipBack, badge = "10")
        CommandButton.ICON_SKIP_BACK_15 -> SessionGlyph("Back 15 seconds", LineGlyphs.SkipBack, badge = "15")
        CommandButton.ICON_SKIP_BACK_30 -> SessionGlyph("Back 30 seconds", LineGlyphs.SkipBack, badge = "30")
        CommandButton.ICON_FAST_FORWARD -> SessionGlyph("Fast forward", LineGlyphs.FastForward)
        CommandButton.ICON_REWIND -> SessionGlyph("Rewind", LineGlyphs.Rewind)

        CommandButton.ICON_PLAY -> SessionGlyph("Play", LineGlyphs.Play)
        CommandButton.ICON_PAUSE -> SessionGlyph("Pause", LineGlyphs.Pause)
        CommandButton.ICON_STOP -> SessionGlyph("Stop", LineGlyphs.Stop)
        CommandButton.ICON_NEXT -> SessionGlyph("Next", LineGlyphs.Next)
        CommandButton.ICON_PREVIOUS -> SessionGlyph("Previous", LineGlyphs.Previous)

        CommandButton.ICON_PLUS_CIRCLE_UNFILLED, CommandButton.ICON_PLUS_CIRCLE_FILLED ->
            SessionGlyph("Add", LineGlyphs.PlusCircle)
        CommandButton.ICON_MINUS_CIRCLE_UNFILLED, CommandButton.ICON_MINUS_CIRCLE_FILLED ->
            SessionGlyph("Remove", LineGlyphs.MinusCircle)
        CommandButton.ICON_CHECK_CIRCLE_UNFILLED -> SessionGlyph("Done", LineGlyphs.CheckCircle)
        CommandButton.ICON_CHECK_CIRCLE_FILLED -> SessionGlyph("Done", LineGlyphs.CheckCircle, active = true)
        CommandButton.ICON_BLOCK -> SessionGlyph("Block", LineGlyphs.Block)

        CommandButton.ICON_PLAYLIST_ADD, CommandButton.ICON_QUEUE_ADD -> SessionGlyph("Add to queue", LineGlyphs.ListAdd)
        CommandButton.ICON_PLAYLIST_REMOVE, CommandButton.ICON_QUEUE_REMOVE ->
            SessionGlyph("Remove from queue", LineGlyphs.ListRemove)
        CommandButton.ICON_QUEUE_NEXT -> SessionGlyph("Play next", LineGlyphs.ListNext)

        CommandButton.ICON_PLAYBACK_SPEED -> SessionGlyph("Playback speed", LineGlyphs.Speed)
        CommandButton.ICON_PLAYBACK_SPEED_0_5 -> speed("0.5")
        CommandButton.ICON_PLAYBACK_SPEED_0_8 -> speed("0.8")
        CommandButton.ICON_PLAYBACK_SPEED_1_0 -> speed("1")
        CommandButton.ICON_PLAYBACK_SPEED_1_2 -> speed("1.2")
        CommandButton.ICON_PLAYBACK_SPEED_1_5 -> speed("1.5")
        CommandButton.ICON_PLAYBACK_SPEED_1_8 -> speed("1.8")
        CommandButton.ICON_PLAYBACK_SPEED_2_0 -> speed("2")

        CommandButton.ICON_SHARE -> SessionGlyph("Share", LineGlyphs.Share)
        CommandButton.ICON_SETTINGS -> SessionGlyph("Settings", LineGlyphs.Settings)
        else -> null
    }

    private fun speed(value: String) = SessionGlyph("Speed $value×", text = "$value×")
}
