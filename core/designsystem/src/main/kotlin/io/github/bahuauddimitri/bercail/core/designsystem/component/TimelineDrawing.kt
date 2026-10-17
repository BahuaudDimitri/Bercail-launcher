package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment

// How the line of the day (BcTimeline) is laid out and drawn: its scale, its strokes and its rings.

/**
 * Where each moment of the day falls: 7 am [headroom] pixels down, midnight [foot] pixels above the bottom of
 * [length], and the alarm under it.
 */
internal class DayScale(
    private val headroom: Float,
    private val length: Float,
    private val foot: Float,
    private val lift: Float
) {
    fun yOf(fraction: Float): Float = headroom + fraction * (length - headroom - foot).coerceAtLeast(0f)

    /** Where the ring of the alarm stands, at the bottom of the line. */
    val alarm: Float get() = length - lift
}

internal fun Density.dayScale(headroom: Dp, length: Float) =
    DayScale(headroom.toPx(), length, END_ROOM.toPx(), ALARM_LIFT.toPx())

/**
 * The line itself: from the top edge down to now, thick, in the accent and with its glow; from now to midnight, a
 * hairline. Then the dots of the appointments and the ring of now. Everything is still: nothing here animates.
 */
internal fun DrawScope.drawLine(
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
        val color = if (mark.position <= now) moment.accent else FUTURE_DOT
        drawAppointment(center, MARK_RADIUS.toPx(), color, next = mark.next, accent = moment.accent)
    }
    drawNow(at(nowY), moment)
}

/**
 * The dot of an appointment, of [radius], in [color]. The [next] one is circled in the [accent], a dark gap between
 * the dot and its ring, so that the ring stands apart from the line it sits on. Shared with the list of the day.
 */
internal fun DrawScope.drawAppointment(center: Offset, radius: Float, color: Color, next: Boolean, accent: Color) {
    if (next) {
        val gap = radius + NEXT_GAP.toPx()
        val ring = RING_WIDTH.toPx()
        drawCircle(BcColors.nightTop, gap, center)
        drawCircle(accent, gap + ring / 2, center, style = Stroke(ring))
    }
    drawCircle(color, radius, center)
}

/** A coming appointment: a white dot at 90 %. */
internal val FUTURE_DOT = BcColors.text.copy(alpha = 0.9f)

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

/**
 * The ring of the alarm, hollow, in the accent, with a soft glow. Without an alarm ([set] false), the same ring in
 * grey, without its glow.
 */
internal fun DrawScope.drawEnd(day: DayScale, moment: BcMoment, set: Boolean, bent: Boolean, lean: (Float) -> Float) {
    val y = day.alarm
    val center = Offset(size.width - LINE_INSET.toPx() - (if (bent) lean(y) else 0f), y)
    if (set) {
        val glow = ALARM_GLOW.toPx()
        drawCircle(Brush.radialGradient(listOf(moment.halo, moment.halo.copy(alpha = 0f)), center, glow), glow, center)
    }
    drawCircle(BcColors.nightTop, ALARM_RADIUS.toPx(), center)
    val ring = if (set) moment.accent else BcColors.textMuted
    drawCircle(ring, ALARM_RADIUS.toPx() - RING_WIDTH.toPx() / 2, center, style = Stroke(RING_WIDTH.toPx()))
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

private const val NOW_OUTLINE_ALPHA = 0.7f
internal val LINE_INSET = 12.dp
private val PAST_WIDTH = 3.dp
private val FUTURE_WIDTH = 1.dp

/** The glow of the past: a blurred stroke under it, drawn once, never animated. */
private val GLOW_WIDTH = 6.dp
private val GLOW_BLUR = 7.dp
private val MARK_RADIUS = 3.dp

/** The dark gap between the dot of the next appointment and its ring. */
private val NEXT_GAP = 3.dp
private val RING_WIDTH = 1.5.dp
private val NOW_RADIUS = 8.dp
private val NOW_BORDER = 2.5.dp
private val NOW_OUTLINE = 4.dp
private val NOW_GLOW = 18.dp

/** Room under midnight for the alarm: its ring stands [ALARM_LIFT] above the bottom, 10 dp under midnight. */
internal val END_ROOM = 34.dp
internal val ALARM_LIFT = 24.dp
private val ALARM_RADIUS = 6.dp
private val ALARM_GLOW = 12.dp

/** How finely the bent line is drawn. */
private val BEND_STEP = 4.dp
