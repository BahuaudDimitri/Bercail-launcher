package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes

/**
 * A message bubble. Theirs are in the person's pastel, mine are light. A bubble is always announced
 * with its true author: "Moi" for mine, never the contact's name.
 */
@Composable
fun BcBubble(
    text: String,
    author: String,
    mine: Boolean,
    pastel: BcPastel,
    modifier: Modifier = Modifier,
    header: String? = null
) {
    val speaker = if (mine) ME else author
    Column(
        modifier = modifier
            .maxWidthFraction(MAX_WIDTH_FRACTION)
            .background(
                color = if (mine) BcColors.text else pastel.color,
                shape = if (mine) BcShapes.bubbleMine else BcShapes.bubbleTheirs
            )
            .padding(horizontal = PADDING_H, vertical = PADDING_V)
            .clearAndSetSemantics { contentDescription = "$speaker : $text" }
    ) {
        val ink = if (mine) BcTextColor.OnLight else BcTextColor.OnPastel
        if (header != null) BcText(header, style = BcTextStyle.Micro, color = ink)
        BcText(text, style = BcTextStyle.Message, color = ink)
    }
}

/** "Annuler l'envoi", under a message just sent, with a ring counting down the time left ([remaining] from 1 to 0). */
@Composable
fun BcUndoButton(remaining: Float, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .background(BcColors.tile, BcShapes.pill)
            .clickable(role = Role.Button, onClick = onUndo)
            .padding(start = UNDO_START, end = UNDO_END),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UNDO_GAP)
    ) {
        Canvas(Modifier.size(RING)) {
            val stroke = RING_STROKE.toPx()
            val inset = stroke / 2
            drawArc(
                color = BcColors.text,
                startAngle = START_ANGLE,
                sweepAngle = FULL_TURN * remaining.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        BcText("Annuler l'envoi", style = BcTextStyle.LabelStrong)
    }
}

/** Caps the width at a fraction of the space the parent offers; shorter content stays short. */
private fun Modifier.maxWidthFraction(fraction: Float) = layout { measurable, constraints ->
    val max = (constraints.maxWidth * fraction).toInt()
    val placeable = measurable.measure(constraints.copy(minWidth = minOf(constraints.minWidth, max), maxWidth = max))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private const val ME = "Moi"
private const val MAX_WIDTH_FRACTION = 0.82f
private const val START_ANGLE = -90f
private const val FULL_TURN = 360f
private val PADDING_H = 13.dp
private val PADDING_V = 9.dp
private val UNDO_START = 10.dp
private val UNDO_END = 14.dp
private val UNDO_GAP = 8.dp
private val RING = 18.dp
private val RING_STROKE = 2.dp
