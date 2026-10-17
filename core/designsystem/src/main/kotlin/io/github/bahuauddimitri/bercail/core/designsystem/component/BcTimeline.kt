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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
 * What ends the line, just under midnight: the alarm of the phone, its time over its [caption] ("réveil · 9°"), and
 * what a screen reader says of it ([description]). Without an alarm ([set] false), the ring is grey and only the
 * [caption] is written ("Aucun réveil"): it still opens the alarms.
 */
data class BcTimelineEnd(val time: String, val caption: String, val description: String, val set: Boolean = true)

/**
 * The day from 7 am to midnight, along the right edge, in the accent of the moment: the past as a thick line with
 * a glow, rising to the top edge, the future as a hairline, a ring for now, "Title 09:30" beside each appointment,
 * the past ones in the accent and the next one circled. [headroom] is left free above 7 am (for the status bar): the
 * line still rises through it to the edge. Above [titlesBelow] (the top of the screen, where its own title stands),
 * appointments only write their time. Midnight keeps its place, with room under it for the [end] of the line: the
 * alarm, a hollow ring in the accent (in grey, "Aucun réveil", when none is set); touching it, or lifting the finger
 * on it, calls [onOpenEnd]. Marks outside the day are not drawn.
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
    end: BcTimelineEnd? = null,
    description: String? = null,
    onOpenDay: ((BcTimelineMark?) -> Unit)? = null,
    onOpenMark: (BcTimelineMark) -> Unit = {},
    onOpenEnd: () -> Unit = {}
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
            .daySemantics(description, onOpenDay)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val day = dayScale(headroom, size.height)
            drawLine(now.coerceIn(0f, 1f), visible, day, moment, bent = swell.value > 0f) { lean(it) }
            if (end != null) drawEnd(day, moment, end.set, bent = swell.value > 0f) { lean(it) }
        }
        val scale = with(LocalDensity.current) { dayScale(headroom, maxHeight.toPx()) }
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
        if (end != null) EndOfLine(end, onOpenEnd, lean)
        if (onOpenDay != null) {
            FingerZone(visible, end != null, headroom, touch, TimelineOpens(onOpenDay, onOpenMark, onOpenEnd))
        }
        if (bubbleShown) Bubble(spokenAt(touch.last, visible, end), swell, touch)
    }
}

/** The line is read as its [description]; with [onOpenDay], a screen reader can open the day from it. */
private fun Modifier.daySemantics(description: String?, onOpenDay: ((BcTimelineMark?) -> Unit)?) = semantics {
    if (description != null) contentDescription = description
    if (onOpenDay != null) {
        onClick(label = OPEN_DAY) {
            onOpenDay(null)
            true
        }
    }
}

/**
 * What the bubble says of what the finger reached: an appointment, or the alarm one place after the last one
 * ("07:15 · Réveil", or "Aucun réveil").
 */
private fun spokenAt(reached: Int, marks: List<BcTimelineMark>, end: BcTimelineEnd?): String =
    if (reached == marks.size) {
        end?.let { if (it.set) "${it.time} · $ALARM" else it.caption }.orEmpty()
    } else {
        marks.getOrNull(reached)?.spoken.orEmpty()
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

    /** The finger is [position] on the zone of the line, [zone] pixels wide; [slop] pixels make a slide. */
    fun move(position: Offset, slop: Float, zone: Float) {
        if (!down) start = position.y
        down = true
        slid = slid || abs(position.y - start) > slop
        cancelled = width - zone + position.x < bubbleLeft
        if (!cancelled) finger = position.y.coerceIn(0f, length.toFloat())
    }

    /** The finger is closest to [closest]: true when it is a new one, which deserves a tick. */
    fun reach(closest: Int?): Boolean {
        if (closest == reached) return false
        reached = closest
        if (closest != null) last = closest
        return closest != null
    }

    fun release() {
        down = false
        cancelled = false
        slid = false
        reached = null
    }
}

/** What the line opens: the day, an appointment, the alarms. */
private class TimelineOpens(
    val day: (BcTimelineMark?) -> Unit,
    val mark: (BcTimelineMark) -> Unit,
    val alarms: () -> Unit
)

/**
 * The part of the timeline that takes the finger: the line itself and a little around it, not the whole width of
 * the times beside it, so that the rest of the screen keeps its own gestures. With an [alarm] at the end of the
 * line, the finger can reach it too: it then stands one place after the last appointment.
 */
@Composable
private fun BoxScope.FingerZone(
    marks: List<BcTimelineMark>,
    alarm: Boolean,
    headroom: Dp,
    touch: TimelineTouch,
    opens: TimelineOpens
) {
    val current by rememberUpdatedState(marks)
    val withAlarm by rememberUpdatedState(alarm)
    val open by rememberUpdatedState(opens)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val density = LocalDensity.current
    val top by rememberUpdatedState(headroom)
    val zone = with(density) { FINGER_ZONE.toPx() }
    val reach = with(density) { ALARM_REACH.toPx() }
    val slop = LocalViewConfiguration.current.touchSlop
    fun scale() = density.dayScale(top, touch.length.toFloat())
    Box(
        Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(FINGER_ZONE).followFinger(
            onMove = { position ->
                touch.move(position, slop, zone)
                if (!touch.cancelled) {
                    val day = scale()
                    val places = current.map { day.yOf(it.position) } + listOfNotNull(day.alarm.takeIf { withAlarm })
                    val closest = places.indices.minByOrNull { abs(places[it] - touch.finger) }
                    if (touch.reach(closest)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            },
            onLift = {
                val day = scale()
                val lift = Lift(
                    touch.reached,
                    touch.cancelled,
                    touch.slid,
                    withAlarm && abs(touch.finger - day.alarm) <= reach
                )
                touch.release()
                lift.open(current, open) { current.minByOrNull { abs(day.yOf(it.position) - touch.finger) } }
            },
            onCancel = touch::release
        )
    )
}

/** How the finger left the line: what it reached, whether it was cancelled, whether it slid, whether on the alarm. */
private class Lift(val reached: Int?, val cancelled: Boolean, val slid: Boolean, val onAlarm: Boolean) {
    fun open(marks: List<BcTimelineMark>, opens: TimelineOpens, closest: () -> BcTimelineMark?) {
        when {
            cancelled -> Unit

            !slid && onAlarm -> opens.alarms()

            // A touch opens the day, on the appointment closest to it.
            !slid -> opens.day(closest())

            reached == marks.size -> opens.alarms()

            reached != null -> opens.mark(marks[reached])

            else -> opens.day(null)
        }
    }
}

/**
 * The alarm at the end of the line: its time over its caption, beside a ring drawn with the line. The ring answers
 * to a screen reader; the finger reaches it through the line itself.
 */
@Composable
private fun BoxScope.EndOfLine(end: BcTimelineEnd, onOpen: () -> Unit, lean: Density.(Float) -> Float) {
    Column(
        Modifier.matchParentSize().layout { measurable, constraints ->
            val label = measurable.measure(Constraints())
            layout(constraints.maxWidth, constraints.maxHeight) {
                val y = dayScale(0.dp, constraints.maxHeight.toFloat()).alarm
                val x = constraints.maxWidth - (LINE_INSET + LABEL_GAP).roundToPx() - label.width
                label.place(x - lean(y).roundToInt(), (y - label.height / 2f).roundToInt())
            }
        },
        horizontalAlignment = Alignment.End
    ) {
        if (end.set) {
            BcText(end.time, style = BcTextStyle.BodySmall, color = BcTextColor.Primary, maxLines = 1)
            BcText(end.caption, style = BcTextStyle.Micro, color = BcTextColor.Muted, maxLines = 1)
        } else {
            BcText(end.caption, style = BcTextStyle.BodySmall, color = BcTextColor.Muted, maxLines = 1)
        }
    }
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .offset(x = ALARM_TARGET / 2 - LINE_INSET, y = ALARM_TARGET / 2 - ALARM_LIFT)
            .size(ALARM_TARGET)
            .semantics {
                contentDescription = end.description
                role = Role.Button
                onClick(label = OPEN_ALARMS) {
                    onOpen()
                    true
                }
            }
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
 * Places a label beside the line, centered on its fraction of the day and kept inside the timeline; it follows the
 * line when it [lean]s away from its place.
 */
private fun Modifier.placeBeside(fraction: Float, headroom: Dp, lean: Density.(Float) -> Float) =
    layout { measurable, constraints ->
        val label = measurable.measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            val y = dayScale(headroom, constraints.maxHeight.toFloat()).yOf(fraction)
            val away = lean(y).roundToInt()
            val x = constraints.maxWidth - (LINE_INSET + LABEL_GAP).roundToPx() - label.width
            val top = (y - label.height / 2f).roundToInt()
            label.place(x - away, top.coerceIn(0, (constraints.maxHeight - label.height).coerceAtLeast(0)))
        }
    }

private const val OPEN_DAY = "Voir la journée"
private const val OPEN_ALARMS = "Ouvrir les alarmes"
private const val ALARM = "Réveil"
private const val PAST_LABEL_ALPHA = 0.5f
private const val TIME_ALPHA = 0.7f

private val LABEL_GAP = 18.dp
private val TITLE_GAP = 5.dp
private val TITLE_MAX = 150.dp

/** A title starts this far below the height given to the titles: half its own height. */
private val TITLE_CLEARANCE = 8.dp

/** The ring of the alarm takes the finger this far around it, and screen readers find it in this square. */
private val ALARM_REACH = 24.dp
private val ALARM_TARGET = 48.dp

/** How wide the line takes the finger, from the edge it runs along. */
private val FINGER_ZONE = 48.dp

/** The bubble stands clear of the times written beside the line. */
private val BUBBLE_BESIDE = 56.dp
private val BUBBLE_MAX = 240.dp
private val BUBBLE_SIDE = 14.dp
private val BUBBLE_EDGE = 8.dp
