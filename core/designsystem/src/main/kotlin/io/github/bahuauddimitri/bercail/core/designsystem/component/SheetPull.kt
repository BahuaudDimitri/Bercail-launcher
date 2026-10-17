package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import io.github.bahuauddimitri.bercail.core.designsystem.motion.BcHeldTransition
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberHeldTransition
import kotlin.math.roundToInt

/** Where a finger may take a sheet to pull it down and close it. */
enum class BcSheetGrip {
    /** Anywhere on its glass; a list inside it scrolls first, and pulls the sheet once it is at its top. */
    Anywhere,

    /** Only its handle and its header ([BcSheetHeader]): sliding on the rest (a conversation) never closes it. */
    Header
}

/**
 * A sheet that the finger pulls down: [transition] goes from 0, the sheet in its place, to 1, the sheet gone below
 * its own height. Let go past a third of its height, or flung down faster than its height in a second, it goes
 * the rest of the way down; let go before, it goes back up, without a bounce. It moves at most 20 times per second,
 * and nothing runs once it rests. A list inside it hands the gesture over through [connection] once at its top.
 */
@Stable
internal class SheetPull(private val transition: BcHeldTransition) {
    /** The sheet's height, in pixels. */
    var height = 0

    /** Called as the sheet, let go, starts to go the rest of the way down. */
    var onLeaving: () -> Unit = {}

    /** True when the finger now down came down on a part that may pull the sheet (its handle, its header). */
    var gripped = false

    /** How far down the finger holds it, in pixels, while it does. */
    private var held: Float? = null

    /** How far down the sheet is drawn, in pixels. */
    val offset: Float get() = transition.progress * height

    /** True while it is not resting in its place: held, pulled down, or on its way. */
    val isAway: Boolean get() = transition.progress > 0f || transition.isMoving

    /** True once it has gone the whole way down and rests there. */
    val isGone: Boolean get() = transition.progress == 1f && !transition.isMoving

    /** The finger moved it by [pixels], positive downwards: returns how much of it the sheet took. */
    fun drag(pixels: Float): Float {
        val from = held ?: offset
        val to = (from + pixels).coerceIn(0f, height.toFloat())
        // A slide up on a sheet in its place does not take it.
        if (height == 0 || (held == null && to == from)) return 0f
        held = to
        transition.drag((to - from) / height)
        return to - from
    }

    /** The finger lets go, moving at [velocity] pixels per second, positive downwards. */
    fun release(velocity: Float) {
        if (held == null || height == 0) return
        held = null
        if (transition.release(velocity / height) == 1f) onLeaving()
    }

    /** Puts the sheet back in its place, by itself: after it closed, for the band that comes back up. */
    fun comeBack() = transition.playTo(0f)

    /** A list inside the sheet scrolls first; at its top, the rest of the slide pulls the sheet. */
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Pulled down, the sheet goes back up before the list scrolls again.
            val pulled = held?.let { it > 0f } == true
            if (source != NestedScrollSource.UserInput || available.y >= 0f || !pulled) return Offset.Zero
            return Offset(0f, drag(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            return Offset(0f, drag(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // A list flung up to its top never closes the sheet: only a finger that pulled it does.
            val pulled = held?.let { it > 0f } == true
            release(available.y)
            return if (pulled) available else Velocity.Zero
        }
    }
}

/** A sheet pulled by the finger, with a third of its height to go before it closes. */
@Composable
internal fun rememberSheetPull(): SheetPull {
    val transition = rememberHeldTransition(resting = 0f, tipsOver = CLOSES_PAST)
    return remember(transition) { SheetPull(transition) }
}

/** Once [pull] has gone the whole way down, [onGone] is called, once. */
@Composable
internal fun ClosesWhenGone(pull: SheetPull, onGone: () -> Unit) {
    val gone by remember(pull) { derivedStateOf { pull.isGone } }
    val latest by rememberUpdatedState(onGone)
    LaunchedEffect(gone) { if (gone) latest() }
}

/** Moves the sheet down by as much as [pull] says, as read at each placement, and measures it for [pull]. */
internal fun Modifier.followsPull(pull: SheetPull): Modifier = this
    .offset { IntOffset(0, pull.offset.roundToInt()) }
    .onSizeChanged { pull.height = it.height }

/**
 * Sliding vertically here pulls the sheet: from anywhere if [anywhere], else only from a part marked by [gripsSheet].
 * It must be read on a part that does not move with the sheet: measured on the sheet, under the finger, the slide
 * would lose every other step. Once the finger has moved by the touch slop, it takes the touch, so that a button
 * under the finger is not pressed; a list under the finger that takes the slide first keeps it.
 */
internal fun Modifier.pullsDown(pull: SheetPull, anywhere: Boolean): Modifier = pointerInput(pull, anywhere) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val gripped = anywhere || pull.gripped
        pull.gripped = false
        if (!gripped) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        var slop = 0f
        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            slop = over
        } ?: return@awaitEachGesture
        tracker.addPointerInputChange(start)
        pull.drag(slop)
        val lifted = verticalDrag(start.id) { change ->
            tracker.addPointerInputChange(change)
            pull.drag(change.positionChange().y)
            change.consume()
        }
        pull.release(if (lifted) tracker.calculateVelocity().y else 0f)
    }
}

/** A finger coming down here may pull the sheet ([pullsDown], on a part outside, does the pulling). */
internal fun Modifier.gripsSheet(pull: SheetPull): Modifier = pointerInput(pull) {
    awaitEachGesture {
        // The first pass reaches this part before the sheet's own detector sees the finger.
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        pull.gripped = true
    }
}

/** A list inside hands the rest of its slide to the sheet, once at its top. */
internal fun Modifier.pullsWithLists(pull: SheetPull): Modifier = nestedScroll(pull.connection)

/** The sheet a [BcSheetHeader] grips, when only its header may pull it: none otherwise. */
internal val LocalSheetGrip = staticCompositionLocalOf<SheetPull?> { null }

/** Let go past this share of its height, a sheet closes. */
private const val CLOSES_PAST = 1f / 3
