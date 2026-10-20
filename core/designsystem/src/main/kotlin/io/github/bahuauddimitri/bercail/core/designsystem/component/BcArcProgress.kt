package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The progress of the Listen screen: the half circle under the arch ([BcArchCover]), 164 dp around the centre of its
 * curve, from its left end (the start) through the bottom to its right end (the end). The part played is a 3 dp
 * stroke in the accent of the moment, with its glow, the rest a fine grey stroke, and a white 14 dp ring marks the
 * position. Under it, centred, [elapsed] (15 sp) then [duration] (12 sp, grey), as the caller writes them.
 *
 * With [onSeek], touching or dragging within the 40 dp band along the arc moves the position, by the angle of the
 * finger around the centre, held at the ends of the half circle; a gesture started elsewhere is left to the screen.
 * A screen reader reads and sets the position as on [BcProgress].
 *
 * A [live] stream keeps the half circle in grey, without ring nor seeking, and says « En direct » in the accent
 * instead of the two times; nothing else moves. Drawn once per change: nothing runs at rest.
 *
 * The centre of the curve lies [BcArcProgressCurveCentre] below the top of the component, halfway across: place it on
 * the centre of the arch's curve ([BcArchCoverCurveCentre] below the arch's top). Around a smaller arch, a smaller
 * [radius] keeps the arc concentric with its curve; the band, the strokes and the ring keep their sizes, and the
 * times stay 16 dp under the bottom of the half circle.
 */
@Composable
fun BcArcProgress(
    progress: Float,
    elapsed: String,
    duration: String,
    onSeek: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    radius: Dp = BcArcProgressRadius
) {
    val value = progress.coerceIn(0f, 1f)
    val seekable = onSeek != null && !live
    val seek by rememberUpdatedState(onSeek)
    val moment = BcTheme.moment
    Box(
        modifier
            .width((radius + BAND / 2) * 2)
            .then(
                if (seekable) {
                    Modifier.pointerInput(radius) {
                        seekAlongArc(radius) { seek?.invoke(it) }
                    }
                } else {
                    Modifier
                }
            )
            .semantics {
                if (!live) progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                if (seekable) {
                    setProgress { target ->
                        seek?.invoke(target.coerceIn(0f, 1f))
                        true
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BcArcProgressCurveCentre + radius + BAND / 2)
                .drawBehind { drawHalfCircle(if (live) null else value, moment, radius) }
        )
        Times(elapsed, duration, live, Modifier.padding(top = BcArcProgressCurveCentre + radius + TIMES_UNDER))
    }
}

/** How far below the top of [BcArcProgress] the centre of its half circle lies, halfway across. */
val BcArcProgressCurveCentre = 20.dp

/** The radius of the half circle of [BcArcProgress] on the Pixel 9, 164 dp: a little beyond the arch's curve. */
val BcArcProgressRadius = 164.dp

/** The two times under the arc, or « En direct » in the accent of the moment. */
@Composable
private fun Times(elapsed: String, duration: String, live: Boolean, modifier: Modifier) {
    if (live) {
        CompositionLocalProvider(LocalBcContentColor provides BcTheme.moment.accent) {
            BcText(LIVE, modifier, style = BcTextStyle.TrackTime, maxLines = 1)
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(TIMES_GAP)) {
            BcText(
                elapsed,
                Modifier.alignByBaseline(),
                style = BcTextStyle.TrackTime,
                color = BcTextColor.Primary,
                maxLines = 1
            )
            BcText(
                duration,
                Modifier.alignByBaseline(),
                style = BcTextStyle.ListSubtitle,
                color = BcTextColor.Muted,
                maxLines = 1
            )
        }
    }
}

/** The half circle; with a [played] fraction, the part played in the accent and the white ring at the position. */
private fun DrawScope.drawHalfCircle(played: Float?, moment: BcMoment, arcRadius: Dp) {
    val radius = arcRadius.toPx()
    val centre = Offset(size.width / 2, BcArcProgressCurveCentre.toPx())
    val square = Rect(centre - Offset(radius, radius), Size(radius * 2, radius * 2))
    drawArc(
        BcColors.stroke,
        startAngle = 0f,
        sweepAngle = HALF_TURN,
        useCenter = false,
        topLeft = square.topLeft,
        size = square.size,
        style = Stroke(REST_STROKE.toPx(), cap = StrokeCap.Round)
    )
    if (played == null) return
    if (played > 0f) {
        val path = Path().apply { arcTo(square, HALF_TURN, -HALF_TURN * played, forceMoveTo = true) }
        drawGlow(path, moment)
        drawPath(path, moment.accent, style = Stroke(PLAYED_STROKE.toPx(), cap = StrokeCap.Round))
    }
    drawCircle(BcColors.text, RING.toPx() / 2, centre + arcPoint(played, radius))
}

/**
 * Follows one finger put down within the band along the arc, and hands each of its positions to [onSeek], as a
 * fraction of the half circle. A finger put down elsewhere is left untouched, for the screen.
 */
private suspend fun PointerInputScope.seekAlongArc(radius: Dp, onSeek: (Float) -> Unit) = awaitEachGesture {
    val down = awaitFirstDown()
    val centre = Offset(size.width / 2f, BcArcProgressCurveCentre.toPx())
    if (inArcBand(down.position - centre, radius.toPx(), BAND.toPx() / 2)) {
        down.consume()
        onSeek(arcFraction(down.position - centre))
        var change = down
        while (change.pressed) {
            change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (change.pressed) {
                change.consume()
                onSeek(arcFraction(change.position - centre))
            }
        }
    }
}

/**
 * The fraction of the half circle the point [from] the centre stands at, by its angle alone: 0 at the left end, ½ at
 * the bottom, 1 at the right end. Above the ends, it is held at the nearer end.
 */
internal fun arcFraction(from: Offset): Float {
    val angle = atan2(from.y, from.x)
    return when {
        angle >= 0f -> 1f - angle / PI.toFloat()
        from.x >= 0f -> 1f
        else -> 0f
    }
}

/** The point of the half circle of [radius] at [fraction], from its centre. */
internal fun arcPoint(fraction: Float, radius: Float): Offset {
    val angle = PI * (1 - fraction)
    return Offset((radius * cos(angle)).toFloat(), (radius * sin(angle)).toFloat())
}

/** Whether the point [from] the centre lies within [halfBand] of the half circle of [radius] (its ends included). */
internal fun inArcBand(from: Offset, radius: Float, halfBand: Float): Boolean =
    from.y >= -halfBand && abs(from.getDistance() - radius) <= halfBand

private const val LIVE = "En direct"
private const val HALF_TURN = 180f
private val BAND = 40.dp
private val REST_STROKE = 1.5.dp
private val PLAYED_STROKE = 3.dp
private val RING = 14.dp

// The times sit 16 dp under the bottom of the arc: 180 dp below the centre of the curve on the reference.
private val TIMES_UNDER = 16.dp
private val TIMES_GAP = 8.dp
