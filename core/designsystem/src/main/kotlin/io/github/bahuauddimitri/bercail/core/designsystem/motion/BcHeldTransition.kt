package io.github.bahuauddimitri.bercail.core.designsystem.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMotion
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A transition between two screens that the finger holds: [progress] goes from 0, the first screen, to 1, the
 * second. It follows the finger ([drag]); once the finger lets go ([release]) it settles on the nearer side, or on
 * the side it was flung to; it can also play the whole way alone ([playTo]), in [BcMotion.screenMillis], on the
 * prototype's curve, which lands softly, without a bounce.
 * Battery rule: [progress] changes at most 20 times per second, under the finger as when it settles, and once it
 * rests on a side nothing runs any more.
 */
@Stable
class BcHeldTransition internal constructor(
    resting: Float,
    private val scope: CoroutineScope,
    /** Let go past this share of the way, it goes on to the second screen. */
    private val tipsOver: Float = HALF_WAY
) {
    internal var motion: BcMotion = BcMotion.forAnimatorScale(1f)
    private val shown = mutableFloatStateOf(resting)
    private var unsettled by mutableStateOf(false)

    /** Where the finger holds it, while it does. */
    private var finger: Float? = null
    private var settle: Settle? = null
    private var frames: Job? = null
    private val throttle = FrameThrottle(MAX_ANIMATION_FPS)

    /** From 0, the first screen, to 1, the second, as last drawn. */
    val progress: Float get() = shown.floatValue

    /** True from the moment the finger takes it until it rests on a side again. */
    val isMoving: Boolean get() = unsettled

    /** The finger moved it by [delta], a share of the whole way: positive towards the second screen. */
    fun drag(delta: Float) {
        settle = null
        finger = ((finger ?: shown.floatValue) + delta).coerceIn(0f, 1f)
        unsettled = true
        drawFrames()
    }

    /**
     * The finger lets go, moving at [velocity] (shares of the whole way per second): it settles on the side it was
     * flung to, or else on the nearer one. Returns that side, 0 or 1.
     */
    fun release(velocity: Float = 0f): Float {
        val from = finger ?: return settle?.to ?: shown.floatValue
        val side = when {
            velocity >= FLING -> 1f
            velocity <= -FLING -> 0f
            from >= tipsOver -> 1f
            else -> 0f
        }
        settleTo(from, side)
        return side
    }

    /** Plays the rest of the way to [side], 0 or 1, alone: a touch, the back gesture, a screen reader's action. */
    fun playTo(side: Float) {
        val going = settle?.to ?: shown.floatValue.takeIf { finger == null }
        if (going != side) settleTo(finger ?: shown.floatValue, side)
    }

    private fun settleTo(from: Float, side: Float) {
        finger = null
        settle = Settle(from, side, (abs(side - from) * motion.screenMillis).toInt())
        unsettled = true
        drawFrames()
    }

    private fun drawFrames() {
        if (frames?.isActive == true) return
        frames = scope.launch {
            // Frames are only asked for while there is something to draw: a finger held still asks for none.
            do withFrameNanos(::frame) while (settle != null || (finger ?: shown.floatValue) != shown.floatValue)
            if (finger == null) unsettled = false
        }
    }

    /** Draws this frame if the battery rule lets it. */
    private fun frame(nanos: Long) {
        val going = settle
        val wanted = going?.at(nanos, motion) ?: finger ?: shown.floatValue
        if (wanted != shown.floatValue && throttle.shouldDraw(nanos)) shown.floatValue = wanted
        if (going != null && going.isOver && shown.floatValue == going.to) settle = null
    }

    /** On its way from [from] to [to], in [millis] from its first frame. */
    private class Settle(val from: Float, val to: Float, val millis: Int) {
        private var start = -1L
        var isOver = false
            private set

        fun at(nanos: Long, motion: BcMotion): Float {
            if (start < 0) start = nanos
            val elapsed = if (millis == 0) 1f else (nanos - start) / NANOS_PER_MILLI / millis
            isOver = elapsed >= 1f
            return if (isOver) to else from + (to - from) * motion.easing.transform(elapsed)
        }
    }

    internal companion object {
        /** A flick this fast (the whole way in a second) carries it over, even before half way. */
        private const val FLING = 1f
        const val HALF_WAY = 0.5f
        private const val NANOS_PER_MILLI = 1_000_000f
    }
}

/** A transition held by the finger, resting at first on [resting]: 0 for the first screen, 1 for the second. */
@Composable
fun rememberBcHeldTransition(resting: Float = 0f): BcHeldTransition = rememberHeldTransition(resting)

/** The same, going on to the second screen once let go past [tipsOver] of the way (a sheet: a third). */
@Composable
internal fun rememberHeldTransition(resting: Float, tipsOver: Float = BcHeldTransition.HALF_WAY): BcHeldTransition {
    val scope = rememberCoroutineScope()
    val transition = remember { BcHeldTransition(resting, scope, tipsOver) }
    val motion = BcTheme.motion
    SideEffect { transition.motion = motion }
    return transition
}
