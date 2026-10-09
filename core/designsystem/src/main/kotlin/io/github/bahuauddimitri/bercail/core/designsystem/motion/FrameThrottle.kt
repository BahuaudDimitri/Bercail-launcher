package io.github.bahuauddimitri.bercail.core.designsystem.motion

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlinx.coroutines.isActive

/** Battery rule: continuous animations draw at most 20 frames per second, whatever the screen's refresh rate. */
const val MAX_ANIMATION_FPS = 20

/** Lets through at most [fps] frames per second out of the display's frames. */
internal class FrameThrottle(fps: Int) {
    // A frame arriving slightly early still counts, so 60 Hz gives exactly every third frame.
    private val interval = NANOS_PER_SECOND / fps - VSYNC_TOLERANCE
    private var last = Long.MIN_VALUE

    fun shouldDraw(frameNanos: Long): Boolean {
        if (last != Long.MIN_VALUE && frameNanos - last < interval) return false
        last = frameNanos
        return true
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val VSYNC_TOLERANCE = 2_000_000L
    }
}

/**
 * Seconds elapsed since the animation started, updated at most 20 times per second while [running].
 * Stands still when Android removes animations. Stops by itself when it leaves the screen.
 */
@Composable
internal fun rememberAnimationSeconds(running: Boolean, start: Float = 0f): State<Float> {
    val seconds = remember { mutableFloatStateOf(start) }
    val animate = running && !BcTheme.motion.reduced
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val throttle = FrameThrottle(MAX_ANIMATION_FPS)
        val from = seconds.floatValue
        var origin = -1L
        while (isActive) {
            withInfiniteAnimationFrameNanos { now ->
                if (origin < 0) origin = now
                if (throttle.shouldDraw(now)) seconds.floatValue = from + (now - origin) / NANOS_PER_SECOND_F
            }
        }
    }
    return seconds
}

private const val NANOS_PER_SECOND_F = 1e9f
