package io.github.bahuauddimitri.bercail.core.designsystem.motion

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.exp

/**
 * The wave a finger raises along a rail: the alphabet of the search, the line of the day. What stands under the
 * finger moves away from its place by [WAVE_BULGE], and its neighbours follow, less and less, like a rope pulled
 * sideways. It only moves while the finger does: nothing here runs by itself.
 */
internal val WAVE_BULGE = 72.dp

/** How wide the wave is: what stands this far from the finger still moves by more than half of the bulge. */
internal val WAVE_SPREAD = 56.dp

/** How much of the bulge is taken [distance] pixels away from the finger: all of it under the finger. */
internal fun bell(distance: Float, spread: Float): Float = exp(-(distance * distance) / (2f * spread * spread))

/**
 * Tells where the finger is on the rail (how far down, and how far aside), from the moment it touches it until it
 * lifts, wherever it slides meanwhile. The touch is taken: nothing behind the rail slides with it. [onLift] is the
 * finger leaving the screen; [onCancel], the touch taken away by something else.
 */
internal fun Modifier.followFinger(
    onMove: (Offset) -> Unit,
    onLift: () -> Unit,
    onCancel: () -> Unit = onLift
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var lifted = false
        try {
            val down = awaitFirstDown()
            down.consume()
            onMove(down.position)
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { change ->
                    if (change.pressed) {
                        onMove(change.position)
                        change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
            lifted = true
            onLift()
        } finally {
            if (!lifted) onCancel()
        }
    }
}
