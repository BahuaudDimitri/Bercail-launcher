package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.motion.WAVE_BULGE
import io.github.bahuauddimitri.bercail.core.designsystem.motion.WAVE_SPREAD
import io.github.bahuauddimitri.bercail.core.designsystem.motion.bell
import io.github.bahuauddimitri.bercail.core.designsystem.motion.followFinger
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val TIMELINE_BUBBLE = "timeline-bubble"

/**
 * An appointment on the timeline: where it falls in the day (0 = 7 am, 1 = midnight), its time, whether it is
 * next. Its [title] is written beside the line, before the time, and said by the bubble under the finger; a [brief]
 * mark (too close to the one before) only writes its time. Its [key] tells the caller which one was opened.
 */
data class BcTimelineMark(
    val position: Float,
    val label: String,
    val next: Boolean = false,
    val title: String = "",
    val key: String? = null,
    val brief: Boolean = false
)

/**
 * The day from 7 am to midnight, along the right edge, in the accent of the moment: the past as a thick line with
 * a glow, rising to the top edge, the future as a hairline, a ring for now, "Title 09:30" beside each appointment,
 * the past ones in the accent and the next one circled. [headroom] is left free above 7 am (for the status bar): the
 * line still rises through it to the edge. Above [titlesBelow] (the top of the screen, where its own title stands),
 * appointments only write their time. Marks outside the day are not drawn.
 *
 * With [onOpenDay], the line answers to the finger like the alphabet of the search. A touch without sliding calls
 * [onOpenDay] with the appointment closest to the finger (or nothing on a day without any). While the finger slides
 * along it, the line bends around the finger and a bubble names the closest appointment (with a tick at each new
 * one); lifting the finger then calls [onOpenMark] with it. Moving the finger away to the left, past the bubble,
 * cancels: the bubble fades, the line straightens, and lifting opens nothing; coming back to the line resumes.
 */
@Composable
fun BcTimeline(
    now: Float,
    marks: List<BcTimelineMark>,
    modifier: Modifier = Modifier,
    headroom: Dp = 0.dp,
    titlesBelow: Dp = 0.dp,
    description: String? = null,
    onOpenDay: ((BcTimelineMark?) -> Unit)? = null,
    onOpenMark: (BcTimelineMark) -> Unit = {}
) {
    val visible = marks.filter { it.position in 0f..1f }
    val moment = BcTheme.moment
    val touch = remember { TimelineTouch() }
    val swell = animateFloatAsState(if (touch.engaged) 1f else 0f, tween(BcTheme.motion.fadeMillis), label = "day wave")
    // How far the line stands away from its place, this many pixels down.
    val lean: Density.(Float) -> Float = { y ->
        WAVE_BULGE.toPx() * swell.value * bell(y - touch.finger, WAVE_SPREAD.toPx())
    }
    val bubbleShown by remember { derivedStateOf { swell.value > 0f && touch.last >= 0 } }
    BoxWithConstraints(
        modifier
            .onSizeChanged {
                touch.length = it.height
                touch.width = it.width
            }
            .semantics {
                if (description != null) contentDescription = description
                if (onOpenDay != null) {
                    onClick(label = OPEN_DAY) {
                        onOpenDay(null)
                        true
                    }
                }
            }
    ) {
        Canvas(Modifier.matchParentSize()) {
            val day = DayScale(headroom.toPx(), size.height)
            drawLine(now.coerceIn(0f, 1f), visible, day, moment, bent = swell.value > 0f) { lean(it) }
        }
        val scale = with(LocalDensity.current) { DayScale(headroom.toPx(), maxHeight.toPx()) }
        val titlesFrom = with(LocalDensity.current) { (titlesBelow + TITLE_CLEARANCE).toPx() }
        visible.forEachIndexed { index, mark ->
            MarkLabel(
                mark = mark,
                // The bubble names the appointment under the finger: it is not written twice.
                named = touch.engaged && index == touch.reached,
                titled = !mark.brief && mark.title.isNotBlank() && scale.yOf(mark.position) >= titlesFrom,
                past = mark.position <= now,
                modifier = Modifier.matchParentSize().placeBeside(mark.position, headroom, lean)
            )
        }
        if (onOpenDay != null) FingerZone(visible, headroom, touch, onOpenDay, onOpenMark)
        if (bubbleShown) Bubble(visible.getOrNull(touch.last)?.spoken.orEmpty(), swell, touch)
    }
}

/**
 * What is written beside an appointment: "Title 09:30", or only its time. The next one stands out, the past ones
 * fade.
 */
@Composable
private fun MarkLabel(mark: BcTimelineMark, named: Boolean, titled: Boolean, past: Boolean, modifier: Modifier) {
    Row(
        modifier.graphicsLayer {
            alpha = when {
                named -> 0f
                past && !mark.next -> PAST_LABEL_ALPHA
                else -> 1f
            }
        },
        horizontalArrangement = Arrangement.spacedBy(TITLE_GAP)
    ) {
        if (titled) {
            BcText(
                text = mark.title,
                style = if (mark.next) BcTextStyle.Label else BcTextStyle.LabelSmall,
                color = if (mark.next) BcTextColor.Primary else BcTextColor.Muted,
                maxLines = 1,
                modifier = Modifier.alignByBaseline().widthIn(max = TITLE_MAX)
            )
        }
        BcText(
            text = mark.label,
            style = BcTextStyle.Micro,
            color = BcTextColor.Muted,
            maxLines = 1,
            modifier = Modifier.alignByBaseline().graphicsLayer { alpha = TIME_ALPHA }
        )
    }
}

/** Where each moment of the day falls: 7 am [headroom] pixels down, midnight at the bottom of [length]. */
private class DayScale(private val headroom: Float, private val length: Float) {
    fun yOf(fraction: Float): Float = headroom + fraction * (length - headroom).coerceAtLeast(0f)
}

/** Where the finger is on the line, and which appointment it is closest to. */
private class TimelineTouch {
    /** The length and the width of the timeline, in pixels. */
    var length by mutableIntStateOf(0)
    var width = 0

    /** Where the finger is down the line, in pixels; kept after it lifts, while the wave flattens. */
    var finger by mutableFloatStateOf(0f)
    var down by mutableStateOf(false)

    /** Where the finger first touched, and whether it has slid since: a touch without sliding opens the day. */
    var start = 0f
    var slid = false

    /** The finger went away to the left, past the bubble: lifting it opens nothing. */
    var cancelled by mutableStateOf(false)

    /** Where the left edge of the bubble last stood, in pixels from the left of the timeline. */
    var bubbleLeft = Float.NEGATIVE_INFINITY

    /** The appointment the finger is closest to, or nothing while the line is not touched. */
    var reached by mutableStateOf<Int?>(null)

    /** The last appointment reached: what the bubble still says while it fades. */
    var last by mutableIntStateOf(-1)

    /** The line answers to the finger: it bends, and the bubble shows. */
    val engaged: Boolean get() = down && !cancelled

    fun release() {
        down = false
        cancelled = false
        reached = null
    }
}

/**
 * The part of the timeline that takes the finger: the line itself and a little around it, not the whole width of
 * the times beside it, so that the rest of the screen keeps its own gestures.
 */
@Composable
private fun BoxScope.FingerZone(
    marks: List<BcTimelineMark>,
    headroom: Dp,
    touch: TimelineTouch,
    onOpenDay: (BcTimelineMark?) -> Unit,
    onOpenMark: (BcTimelineMark) -> Unit
) {
    val current by rememberUpdatedState(marks)
    val openDay by rememberUpdatedState(onOpenDay)
    val openMark by rememberUpdatedState(onOpenMark)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val density = LocalDensity.current
    val top by rememberUpdatedState(with(density) { headroom.toPx() })
    val zone = with(density) { FINGER_ZONE.toPx() }
    val slop = LocalViewConfiguration.current.touchSlop
    Box(
        Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(FINGER_ZONE).followFinger(
            onMove = { position ->
                if (!touch.down) touch.start = position.y
                touch.down = true
                touch.slid = touch.slid || abs(position.y - touch.start) > slop
                touch.cancelled = touch.width - zone + position.x < touch.bubbleLeft
                if (touch.cancelled) return@followFinger
                touch.finger = position.y.coerceIn(0f, touch.length.toFloat())
                val day = DayScale(top, touch.length.toFloat())
                val closest = current.indices.minByOrNull { abs(day.yOf(current[it].position) - touch.finger) }
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
                val (cancelled, slid) = touch.cancelled to touch.slid
                touch.release()
                when {
                    cancelled -> Unit
                    slid && chosen != null -> openMark(chosen)
                    else -> openDay(chosen)
                }
            },
            onCancel = touch::release
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
                    // Past this edge, to the left, the finger cancels. It is noted while the bubble stands in full.
                    if (touch.engaged && swell.value == 1f) touch.bubbleLeft = x.toFloat()
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

/**
 * The line itself: from the top edge down to now, thick, in the accent and with its glow; from now to midnight, a
 * hairline. Then the dots of the appointments and the ring of now. Everything is still: nothing here animates.
 */
private fun DrawScope.drawLine(
    now: Float,
    marks: List<BcTimelineMark>,
    day: DayScale,
    moment: BcMoment,
    bent: Boolean,
    lean: (Float) -> Float
) {
    val inset = LINE_INSET.toPx()
    fun at(y: Float) = Offset(size.width - inset - (if (bent) lean(y) else 0f), y)

    fun stretch(from: Float, to: Float) = Path().apply {
        val start = at(from)
        moveTo(start.x, start.y)
        var y = from
        while (y < to) {
            y = if (bent) (y + BEND_STEP.toPx()).coerceAtMost(to) else to
            val next = at(y)
            lineTo(next.x, next.y)
        }
    }

    val nowY = day.yOf(now)
    drawPath(stretch(nowY, day.yOf(1f)), BcColors.stroke, style = Stroke(FUTURE_WIDTH.toPx(), cap = StrokeCap.Round))
    val past = stretch(0f, nowY)
    drawGlow(past, moment)
    drawPath(past, moment.accent, style = Stroke(PAST_WIDTH.toPx(), cap = StrokeCap.Round))
    marks.forEach { mark ->
        val center = at(day.yOf(mark.position))
        if (mark.next) {
            drawCircle(BcColors.nightTop, NEXT_GAP.toPx(), center)
            drawCircle(moment.accent, NEXT_RING.toPx(), center, style = Stroke(RING_WIDTH.toPx()))
        }
        val color = if (mark.position <= now) moment.accent else BcColors.text.copy(alpha = FUTURE_DOT_ALPHA)
        drawCircle(color, MARK_RADIUS.toPx(), center)
    }
    drawNow(at(nowY), moment)
}

/** The soft glow of the accent under [path]. */
private fun DrawScope.drawGlow(path: Path, moment: BcMoment) = drawIntoCanvas { canvas ->
    val paint = Paint().apply {
        color = moment.halo
        style = PaintingStyle.Stroke
        strokeWidth = GLOW_WIDTH.toPx()
        strokeCap = StrokeCap.Round
    }
    paint.asFrameworkPaint().maskFilter = BlurMaskFilter(GLOW_BLUR.toPx(), BlurMaskFilter.Blur.NORMAL)
    canvas.drawPath(path, paint)
}

/** The ring of now: dark inside, circled in the accent, with its glow. */
private fun DrawScope.drawNow(center: Offset, moment: BcMoment) {
    val glow = NOW_GLOW.toPx()
    drawCircle(Brush.radialGradient(listOf(moment.halo, moment.halo.copy(alpha = 0f)), center, glow), glow, center)
    drawCircle(BcColors.nightTop.copy(alpha = NOW_OUTLINE_ALPHA), (NOW_RADIUS + NOW_OUTLINE).toPx(), center)
    drawCircle(BcColors.nightTop, NOW_RADIUS.toPx(), center)
    val border = NOW_BORDER.toPx()
    drawCircle(moment.accent, NOW_RADIUS.toPx() - border / 2, center, style = Stroke(border))
}

/**
 * Places a label beside the line, centered on its fraction of the day and kept inside the timeline; it follows the
 * line when it [lean]s away from its place.
 */
private fun Modifier.placeBeside(fraction: Float, headroom: Dp, lean: Density.(Float) -> Float) =
    layout { measurable, constraints ->
        val label = measurable.measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = DayScale(headroom.toPx(), constraints.maxHeight.toFloat()).yOf(fraction)
            val away = lean(y).roundToInt()
            val x = constraints.maxWidth - (LINE_INSET + LABEL_GAP).roundToPx() - label.width
            val top = (y - label.height / 2f).roundToInt()
            label.place(x - away, top.coerceIn(0, (constraints.maxHeight - label.height).coerceAtLeast(0)))
        }
    }

private const val OPEN_DAY = "Voir la journée"
private const val PAST_LABEL_ALPHA = 0.5f
private const val TIME_ALPHA = 0.7f
private const val FUTURE_DOT_ALPHA = 0.9f
private const val NOW_OUTLINE_ALPHA = 0.7f
private val LINE_INSET = 12.dp
private val PAST_WIDTH = 3.dp
private val FUTURE_WIDTH = 1.dp

/** The glow of the past: a blurred stroke under it, drawn once, never animated. */
private val GLOW_WIDTH = 6.dp
private val GLOW_BLUR = 7.dp
private val MARK_RADIUS = 3.dp
private val NEXT_GAP = 6.dp
private val NEXT_RING = 6.75.dp
private val RING_WIDTH = 1.5.dp
private val NOW_RADIUS = 8.dp
private val NOW_BORDER = 2.5.dp
private val NOW_OUTLINE = 4.dp
private val NOW_GLOW = 18.dp
private val LABEL_GAP = 18.dp
private val TITLE_GAP = 5.dp
private val TITLE_MAX = 150.dp

/** A title starts this far below the height given to the titles: half its own height. */
private val TITLE_CLEARANCE = 8.dp

/** How finely the bent line is drawn. */
private val BEND_STEP = 4.dp

/** How wide the line takes the finger, from the edge it runs along. */
private val FINGER_ZONE = 48.dp

/** The bubble stands clear of the times written beside the line. */
private val BUBBLE_BESIDE = 56.dp
private val BUBBLE_MAX = 240.dp
private val BUBBLE_SIDE = 14.dp
private val BUBBLE_EDGE = 8.dp
