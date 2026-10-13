package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * What sliding a line or a pill to the right does; [label] says it while it slides: "Ouvrir Beeper". The thing
 * follows the finger, [label] is written in the room it leaves (brighter, with a tick, once the slide is long
 * enough), and lifting the finger then calls [onSwipe]. Too short, it goes back to its place and nothing happens.
 */
class BcRowSwipe(val label: String, val onSwipe: () -> Unit) {
    /** The same thing, offered to a screen reader, which cannot slide anything. */
    internal fun asAction() = CustomAccessibilityAction(label) {
        onSwipe()
        true
    }
}

/** How far the thing has been slid, in pixels. */
internal typealias Slide = Animatable<Float, *>

@Composable
internal fun rememberSlide(): Slide = remember { Animatable(0f) }

/**
 * Lets a finger slide the thing to the right, up to [SWIPE_LIMIT]. Only a slide that starts to the right is taken:
 * to the left, up or down, the finger is left to whatever is behind (the drawer that unfolds, the screen that
 * changes). Lifting the finger past [SWIPE_THRESHOLD] does what [swipe] says.
 */
@Composable
internal fun Modifier.slidesRight(slide: Slide, swipe: BcRowSwipe): Modifier {
    val scope = rememberCoroutineScope()
    val action by rememberUpdatedState(swipe)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val back = tween<Float>(BcTheme.motion.fadeMillis)
    return pointerInput(Unit) {
        val threshold = SWIPE_THRESHOLD.toPx()
        val limit = SWIPE_LIMIT.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val first = awaitSlideToTheRight(down) ?: return@awaitEachGesture
            fun follow(change: PointerInputChange) {
                change.consume()
                val wanted = (change.position.x - down.position.x - viewConfiguration.touchSlop).coerceIn(0f, limit)
                if (slide.value < threshold && wanted >= threshold) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                scope.launch(start = CoroutineStart.UNDISPATCHED) { slide.snapTo(wanted) }
            }
            follow(first)
            val lifted = drag(first.id) { follow(it) }
            if (lifted && slide.value >= threshold) action.onSwipe()
            scope.launch { slide.animateTo(0f, back) }
        }
    }
}

/** What a finger is doing, once it has moved since it touched the thing. */
private enum class Way { Undecided, ToTheRight, Elsewhere }

private fun wayOf(moved: Offset, slop: Float): Way = when {
    abs(moved.x) < slop && abs(moved.y) < slop -> Way.Undecided
    moved.x > 0f && abs(moved.y) <= abs(moved.x) -> Way.ToTheRight
    else -> Way.Elsewhere
}

/**
 * Waits until the finger that came [down] has chosen its way, and gives it back once it slides to the right.
 * Nothing if it goes another way, lifts, or is taken by something else meanwhile.
 */
private suspend fun AwaitPointerEventScope.awaitSlideToTheRight(down: PointerInputChange): PointerInputChange? {
    var sliding: PointerInputChange? = null
    var way = Way.Undecided
    while (way == Way.Undecided) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
        way = if (change == null || !change.pressed || change.isConsumed) {
            Way.Elsewhere
        } else {
            wayOf(change.position - down.position, viewConfiguration.touchSlop)
        }
        if (way == Way.ToTheRight) sliding = change
    }
    return sliding
}

/**
 * What the slide opens, written just before the thing that slides: it comes in from the edge with it and grows
 * clearer, then brighter once the slide is long enough. Nothing is there while nothing slides.
 */
@Composable
internal fun SlideHint(swipe: BcRowSwipe, slide: Slide) {
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val sliding by remember { derivedStateOf { slide.value > 0f } }
    val armed by remember(threshold) { derivedStateOf { slide.value >= threshold } }
    if (!sliding) return
    BcText(
        text = swipe.label,
        style = BcTextStyle.Label,
        color = if (armed) BcTextColor.Primary else BcTextColor.Muted,
        maxLines = 1,
        modifier = Modifier
            .layout { measurable, _ ->
                val hint = measurable.measure(Constraints())
                // It takes no room of its own: it stands where the thing was.
                layout(0, hint.height) { hint.place((slide.value - HINT_GAP.toPx() - hint.width).roundToInt(), 0) }
            }
            .graphicsLayer { alpha = (slide.value / threshold).coerceIn(0f, 1f) }
    )
}

/** How far the thing must be slid for the slide to count, and how far it can go. */
private val SWIPE_THRESHOLD = 64.dp
private val SWIPE_LIMIT = 112.dp
private val HINT_GAP = 12.dp
