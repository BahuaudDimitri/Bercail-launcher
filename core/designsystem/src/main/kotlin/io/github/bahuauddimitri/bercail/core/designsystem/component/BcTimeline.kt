package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import kotlin.math.roundToInt

/** An appointment on the timeline: where it falls in the day (0 = 7 am, 1 = midnight), its time, whether it is next. */
data class BcTimelineMark(val position: Float, val label: String, val next: Boolean = false)

enum class BcTimelineOrientation { Vertical, Horizontal }

/**
 * The day from 7 am to midnight: past as a solid line, future dotted, a glowing dot for now,
 * the time of each appointment beside it and the next one circled. Marks outside the day are not drawn.
 */
@Composable
fun BcTimeline(
    now: Float,
    marks: List<BcTimelineMark>,
    modifier: Modifier = Modifier,
    orientation: BcTimelineOrientation = BcTimelineOrientation.Vertical,
    description: String? = null
) {
    val vertical = orientation == BcTimelineOrientation.Vertical
    val visible = marks.filter { it.position in 0f..1f }
    val position = now.coerceIn(0f, 1f)
    Box(modifier.semantics { if (description != null) contentDescription = description }) {
        Canvas(Modifier.matchParentSize()) { drawLine(vertical, position, visible) }
        visible.forEach { mark ->
            BcText(
                text = mark.label,
                style = BcTextStyle.Micro,
                color = if (mark.next) BcTextColor.Primary else BcTextColor.Muted,
                maxLines = 1,
                modifier = Modifier.matchParentSize().placeBeside(mark.position, vertical)
            )
        }
    }
}

private fun DrawScope.drawLine(vertical: Boolean, now: Float, marks: List<BcTimelineMark>) {
    val inset = LINE_INSET.toPx()
    val start = if (vertical) Offset(size.width - inset, 0f) else Offset(0f, inset)
    val end = if (vertical) Offset(size.width - inset, size.height) else Offset(size.width, inset)
    fun at(fraction: Float) = start + (end - start) * fraction
    val width = LINE_WIDTH.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), GAP.toPx()))

    drawLine(BcColors.text.copy(alpha = FUTURE_ALPHA), at(now), end, width, StrokeCap.Round, dash)
    drawLine(BcColors.text.copy(alpha = PAST_ALPHA), start, at(now), width, StrokeCap.Round)
    marks.forEach { mark ->
        val center = at(mark.position)
        drawCircle(BcColors.text, MARK_RADIUS.toPx(), center)
        if (mark.next) drawCircle(BcColors.text, NEXT_RADIUS.toPx(), center, style = Stroke(width))
    }
    val dot = at(now)
    drawCircle(
        Brush.radialGradient(
            listOf(BcColors.text.copy(alpha = GLOW_ALPHA), BcColors.text.copy(alpha = 0f)),
            dot,
            GLOW.toPx()
        ),
        GLOW.toPx(),
        dot
    )
    drawCircle(BcColors.text, NOW_RADIUS.toPx(), dot)
}

/** Places a label beside the line, centered on its fraction of the day and kept inside the timeline. */
private fun Modifier.placeBeside(fraction: Float, vertical: Boolean) = layout { measurable, constraints ->
    val label = measurable.measure(Constraints())
    layout(constraints.maxWidth, constraints.maxHeight) {
        if (vertical) {
            val x = constraints.maxWidth - (LINE_INSET + LABEL_GAP).roundToPx() - label.width
            val y = (fraction * constraints.maxHeight - label.height / 2f).roundToInt()
            label.place(x, y.coerceIn(0, (constraints.maxHeight - label.height).coerceAtLeast(0)))
        } else {
            val x = (fraction * constraints.maxWidth - label.width / 2f).roundToInt()
            val y = (LINE_INSET + LABEL_GAP).roundToPx()
            label.place(x.coerceIn(0, (constraints.maxWidth - label.width).coerceAtLeast(0)), y)
        }
    }
}

private const val PAST_ALPHA = 0.55f
private const val FUTURE_ALPHA = 0.35f
private const val GLOW_ALPHA = 0.6f
private val LINE_INSET = 12.dp
private val LINE_WIDTH = 1.5.dp
private val DASH = 3.dp
private val GAP = 5.dp
private val MARK_RADIUS = 3.dp
private val NEXT_RADIUS = 7.dp
private val NOW_RADIUS = 3.5.dp
private val GLOW = 11.dp
private val LABEL_GAP = 12.dp
