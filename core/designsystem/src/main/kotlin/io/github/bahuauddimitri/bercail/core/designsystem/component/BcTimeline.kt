package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.motion.WAVE_BULGE
import io.github.bahuauddimitri.bercail.core.designsystem.motion.WAVE_SPREAD
import io.github.bahuauddimitri.bercail.core.designsystem.motion.bell
import io.github.bahuauddimitri.bercail.core.designsystem.motion.followFinger
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val TIMELINE_BUBBLE = "timeline-bubble"

/**
 * An appointment on the timeline: where it falls in the day (0 = 7 am, 1 = midnight), its time, whether it is
 * next. Its [title] is what the bubble says under the finger; its [key] tells the caller which one was opened.
 */
data class BcTimelineMark(
    val position: Float,
    val label: String,
    val next: Boolean = false,
    val title: String = "",
    val key: String? = null
)

/**
 * The day from 7 am (top) to midnight (bottom), along the right edge: past as a solid line, future dotted, a
 * glowing dot for now, the time of each appointment beside it and the next one circled. Marks outside the day are
 * not drawn.
 *
 * With [onOpen], the line answers to the finger like the alphabet of the search: while it is touched, it bends
 * around the finger, a bubble names the appointment the finger is closest to (with a tick at each new one), and
 * lifting the finger calls [onOpen] with that appointment, or with nothing on a day without any.
 */
@Composable
fun BcTimeline(
    now: Float,
    marks: List<BcTimelineMark>,
    modifier: Modifier = Modifier,
    description: String? = null,
    onOpen: ((BcTimelineMark?) -> Unit)? = null
) {
    val visible = marks.filter { it.position in 0f..1f }
    val touch = remember { TimelineTouch() }
    val swell = animateFloatAsState(if (touch.down) 1f else 0f, tween(BcTheme.motion.fadeMillis), label = "day wave")
    // How far the line stands away from its place, this many pixels along it.
    val lean: Density.(Float) -> Float = { along ->
        WAVE_BULGE.toPx() * swell.value * bell(along - touch.finger, WAVE_SPREAD.toPx())
    }
    val bubbleShown by remember { derivedStateOf { swell.value > 0f && touch.last >= 0 } }
    Box(
        modifier
            .onSizeChanged { touch.length = it.height }
            .semantics {
                if (description != null) contentDescription = description
                if (onOpen != null) {
                    onClick(label = OPEN_DAY) {
                        onOpen(null)
                        true
                    }
                }
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawLine(now.coerceIn(0f, 1f), visible, bent = swell.value > 0f) { lean(it) }
        }
        visible.forEachIndexed { index, mark ->
            BcText(
                text = mark.label,
                style = BcTextStyle.Micro,
                color = if (mark.next || index == touch.reached) BcTextColor.Primary else BcTextColor.Muted,
                maxLines = 1,
                modifier = Modifier.matchParentSize().placeBeside(mark.position, lean)
            )
        }
        if (onOpen != null) FingerZone(visible, touch, onOpen)
        if (bubbleShown) Bubble(visible.getOrNull(touch.last)?.spoken.orEmpty(), swell, touch)
    }
}

/** Where the finger is on the line, and which appointment it is closest to. */
private class TimelineTouch {
    /** The length of the line, in pixels. */
    var length by mutableIntStateOf(0)

    /** Where the finger is along the line, in pixels; kept after it lifts, while the wave flattens. */
    var finger by mutableFloatStateOf(0f)
    var down by mutableStateOf(false)

    /** The appointment the finger is closest to, or nothing while the line is not touched. */
    var reached by mutableStateOf<Int?>(null)

    /** The last appointment reached: what the bubble still says while it fades. */
    var last by mutableIntStateOf(-1)
}

/**
 * The part of the timeline that takes the finger: the line itself and a little around it, not the whole width of
 * the times beside it, so that the rest of the screen keeps its own gestures.
 */
@Composable
private fun BoxScope.FingerZone(marks: List<BcTimelineMark>, touch: TimelineTouch, onOpen: (BcTimelineMark?) -> Unit) {
    val current by rememberUpdatedState(marks)
    val open by rememberUpdatedState(onOpen)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    Box(
        Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(FINGER_ZONE).followFinger(
            onMove = { along ->
                touch.finger = along.coerceIn(0f, touch.length.toFloat())
                touch.down = true
                val closest = current.indices.minByOrNull { abs(current[it].position * touch.length - touch.finger) }
                if (closest != touch.reached) {
                    touch.reached = closest
                    if (closest != null) {
                        touch.last = closest
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                }
            },
            onLift = {
                val chosen = touch.reached?.let(current::getOrNull)
                touch.down = false
                touch.reached = null
                open(chosen)
            },
            onCancel = {
                touch.down = false
                touch.reached = null
            }
        )
    )
}

/** What the bubble says of an appointment: "15:00 · Point design". */
private val BcTimelineMark.spoken: String get() = if (title.isBlank()) label else "$label · $title"

/** The appointment under the finger, in a light bubble that follows it on the far side of the wave. */
@Composable
private fun BoxScope.Bubble(text: String, swell: State<Float>, touch: TimelineTouch) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val bubble = measurable.measure(Constraints(maxWidth = BUBBLE_MAX.roundToPx()))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val away = (WAVE_BULGE.toPx() * swell.value).roundToInt()
                    val x = constraints.maxWidth - (LINE_INSET + BUBBLE_BESIDE).roundToPx() - away - bubble.width
                    val y = (touch.finger - bubble.height / 2f).roundToInt()
                    bubble.place(x, y.coerceIn(0, (constraints.maxHeight - bubble.height).coerceAtLeast(0)))
                }
            }
            .graphicsLayer { alpha = swell.value }
            .background(BcColors.text, BcShapes.pill)
            .padding(horizontal = BUBBLE_SIDE, vertical = BUBBLE_EDGE)
            .testTag(TIMELINE_BUBBLE)
    ) {
        BcText(text, style = BcTextStyle.LabelStrong, color = BcTextColor.OnLight, maxLines = 1)
    }
}

private fun DrawScope.drawLine(now: Float, marks: List<BcTimelineMark>, bent: Boolean, lean: (Float) -> Float) {
    val inset = LINE_INSET.toPx()
    val length = size.height

    /** The point of the line at this share of the day, pushed away from its place around the finger. */
    fun at(fraction: Float): Offset {
        val along = fraction * length
        val away = if (bent) lean(along) else 0f
        return Offset(size.width - inset - away, along)
    }

    fun stretch(from: Float, to: Float) = Path().apply {
        val start = at(from)
        moveTo(start.x, start.y)
        var fraction = from
        while (fraction < to) {
            fraction = (fraction + BEND_STEP.toPx() / length).coerceAtMost(to)
            val next = at(fraction)
            lineTo(next.x, next.y)
        }
    }

    val width = LINE_WIDTH.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), GAP.toPx()))
    val future = BcColors.text.copy(alpha = FUTURE_ALPHA)
    val past = BcColors.text.copy(alpha = PAST_ALPHA)
    if (bent && length > 0f) {
        drawPath(stretch(now, 1f), future, style = Stroke(width, cap = StrokeCap.Round, pathEffect = dash))
        drawPath(stretch(0f, now), past, style = Stroke(width, cap = StrokeCap.Round))
    } else {
        drawLine(future, at(now), at(1f), width, StrokeCap.Round, dash)
        drawLine(past, at(0f), at(now), width, StrokeCap.Round)
    }
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

/**
 * Places a label beside the line, centered on its fraction of the day and kept inside the timeline; it follows the
 * line when it [lean]s away from its place.
 */
private fun Modifier.placeBeside(fraction: Float, lean: Density.(Float) -> Float) = layout { measurable, constraints ->
    val label = measurable.measure(Constraints())
    layout(constraints.maxWidth, constraints.maxHeight) {
        val away = lean(fraction * constraints.maxHeight).roundToInt()
        val x = constraints.maxWidth - (LINE_INSET + LABEL_GAP).roundToPx() - label.width
        val y = (fraction * constraints.maxHeight - label.height / 2f).roundToInt()
        label.place(x - away, y.coerceIn(0, (constraints.maxHeight - label.height).coerceAtLeast(0)))
    }
}

private const val OPEN_DAY = "Voir la journée"
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

/** How finely the bent line is drawn. */
private val BEND_STEP = 4.dp

/** How wide the line takes the finger, from the edge it runs along. */
private val FINGER_ZONE = 48.dp

/** The bubble stands clear of the times written beside the line. */
private val BUBBLE_BESIDE = 56.dp
private val BUBBLE_MAX = 240.dp
private val BUBBLE_SIDE = 14.dp
private val BUBBLE_EDGE = 8.dp
