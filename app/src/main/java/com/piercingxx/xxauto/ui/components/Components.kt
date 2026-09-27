package com.piercingxx.xxauto.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.piercingxx.xxauto.ui.icons.LineGlyphs
import com.piercingxx.xxauto.ui.theme.LocalInk

/** Design: the minimum touch target, on the drive screen and in its sheets. */
val Target = 72.dp

/** A line glyph at [size], drawn in [tint]. Decorative: the caller carries the label. */
@Composable
fun Glyph(vector: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Icon(imageVector = vector, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/**
 * An icon-only button: a [Target]-sized hit area around a line glyph.
 * [description] is what TalkBack (and the smoke walker) reads.
 */
@Composable
fun GlyphButton(
    vector: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalInk.current.text,
    glyphSize: Dp = 28.dp,
    contentAlignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = modifier
            .size(Target)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = contentAlignment,
    ) {
        Glyph(vector, tint, glyphSize)
    }
}

/**
 * The one bottom-sheet look (quick-pick, favourites, the Settings car picker):
 * ink-raised ground with a hairline top edge so it separates from a black
 * screen, a short shade handle, a Space Mono title, and a [leading] slot for a
 * back glyph when the sheet drills down. Opens fully: these are lists, and a
 * half-open list is one more thing to aim at while driving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkSheet(
    title: String,
    onDismiss: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = ink.inkRaised,
        contentColor = ink.text,
        tonalElevation = 0.dp,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        dragHandle = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // The hairline top edge, following the corners. Drawn inside
                // the sheet (not as a border modifier) so it rides the sheet's
                // own offset while it slides.
                Canvas(modifier = Modifier.matchParentSize()) {
                    val radius = SheetCorner.toPx()
                    val stroke = 1.dp.toPx()
                    clipRect(bottom = radius) {
                        drawRoundRect(
                            color = ink.line,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, radius * 4),
                            cornerRadius = CornerRadius(radius - stroke / 2),
                            style = Stroke(width = stroke),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(ink.shade, RoundedCornerShape(2.dp)),
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(start = if (leading == null) 24.dp else 0.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ink.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}

private val SheetCorner = 16.dp

/** The sheet's back glyph, for [InkSheet]'s leading slot. */
@Composable
fun SheetBack(onClick: () -> Unit) {
    GlyphButton(LineGlyphs.Back, description = "Back", onClick = onClick, glyphSize = 24.dp)
}

/**
 * One sheet row: at least [Target] tall, a 24sp primary line, an optional
 * muted second line, and an optional trailing glyph (a chevron for "opens
 * more"). The whole row is the target.
 */
@Composable
fun SheetRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: ImageVector? = null,
) {
    val ink = LocalInk.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Target)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ink.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            Glyph(trailing, ink.muted, 24.dp)
        }
    }
}

/** A sheet's one-line state ("Loading…", "Nothing here"), held at row height so the sheet doesn't jump. */
@Composable
fun SheetStatus(text: String, color: Color = LocalInk.current.muted) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Target)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
