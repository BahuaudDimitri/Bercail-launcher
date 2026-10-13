package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor
import kotlin.math.exp
import kotlin.math.roundToInt

internal const val ALPHABET_BUBBLE = "alphabet-bubble"

/**
 * The alphabet along the edge of the search list, letters in use only. Touching or sliding along it reaches
 * a letter (once per letter, with a tick under the finger). While it is touched, the column of letters bends away
 * from the edge around the finger, like a rope pulled sideways, and a bubble next to it carries the letter reached;
 * the letters keep their size. When the finger lifts, the column straightens and the bubble fades.
 */
@Composable
fun BcAlphabetRail(letters: List<String>, onLetter: (String) -> Unit, modifier: Modifier = Modifier) {
    var height by remember { mutableIntStateOf(0) }

    /** Where the finger is along the rail, in pixels from its top; kept after it lifts, while the wave flattens. */
    var finger by remember { mutableFloatStateOf(0f) }

    /** The letter under the finger, or nothing while the rail is not touched. */
    var touched by remember { mutableStateOf<Int?>(null) }
    var lastTouched by remember { mutableIntStateOf(0) }
    val reach by rememberUpdatedState(onLetter)
    val current by rememberUpdatedState(letters)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val swell = animateFloatAsState(if (touched != null) 1f else 0f, tween(BcTheme.motion.fadeMillis), label = "wave")
    val bubbleShown by remember { derivedStateOf { swell.value > 0f } }

    Box(
        modifier = modifier
            .width(RAIL_WIDTH)
            .onSizeChanged { height = it.height }
            .followFinger(
                onMove = { y ->
                    if (height == 0 || current.isEmpty()) return@followFinger
                    finger = y.coerceIn(0f, height.toFloat())
                    val index = (y / height * current.size).toInt().coerceIn(0, current.lastIndex)
                    if (index != touched) {
                        touched = index
                        lastTouched = index
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        reach(current[index])
                    }
                },
                onLift = { touched = null }
            )
    ) {
        Letters(
            letters = letters,
            lit = touched,
            // How far the letter whose middle is at this height leans out, in pixels.
            lean = { middle -> BULGE.toPx() * swell.value * bell(middle * height - finger, SPREAD.toPx()) },
            onLetter = { reach(it) },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        if (bubbleShown) Bubble(letters.getOrNull(lastTouched).orEmpty(), swell, finger = { finger })
    }
}

/**
 * The column of letters, evenly spread over the height; when the room is short (the keyboard is out), they stand
 * closer together. [lean] says how far the letter standing at a share of the height (0 at the top, 1 at the
 * bottom) moves away from the edge; the one that is [lit] is the letter reached.
 */
@Composable
private fun Letters(
    letters: List<String>,
    lit: Int?,
    lean: Density.(Float) -> Float,
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier.fillMaxHeight().padding(end = RAIL_END_PADDING),
        content = {
            letters.forEachIndexed { index, letter ->
                BcText(
                    text = letter,
                    style = BcTextStyle.LabelStrong,
                    color = if (index == lit) BcTextColor.Primary else BcTextColor.Muted,
                    modifier = Modifier
                        .offset { IntOffset(-lean((index + HALF) / letters.size).roundToInt(), 0) }
                        .semantics {
                            onClick(label = "Aller à $letter") {
                                onLetter(letter)
                                true
                            }
                        }
                )
            }
        }
    ) { measurables, constraints ->
        // Each letter keeps its own height, whatever the room: a squeezed letter would show cut in half.
        val placed = measurables.map { it.measure(Constraints()) }
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placed.sumOf { it.height }
        val width = placed.maxOfOrNull { it.width } ?: 0
        layout(width, height) {
            placed.forEachIndexed { index, letter ->
                // Evenly spread, each in the middle of its share of the height: where the finger looks for it.
                val middle = (index + HALF) / placed.size * height
                letter.place(width - letter.width, (middle - letter.height * HALF).roundToInt())
            }
        }
    }
}

/** Tells where the finger is along the rail
, from the moment it touches it until it lifts, wherever it slides. */
private fun Modifier.followFinger(onMove: (Float) -> Unit, onLift: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onMove(down.position.y)
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    onMove(change.position.y)
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
        onLift()
    }
}

/** The letter reached, in a light bubble
 that follows the finger on the far side of the wave. */
@Composable
private fun Bubble(letter: String, swell: State<Float>, finger: () -> Float) {
    Box(
        modifier = Modifier
            .offset {
                val side = RAIL_WIDTH.toPx() - BUBBLE.toPx() - BUBBLE_GAP.toPx() - BULGE.toPx() * swell.value
                IntOffset(side.roundToInt(), (finger() - BUBBLE.toPx() * HALF).roundToInt())
            }
            .size(BUBBLE)
            .graphicsLayer {
                alpha = swell.value
                scaleX = BUBBLE_START + (1f - BUBBLE_START) * swell.value
                scaleY = scaleX
            }
            .background(BcColors.text, BcShapes.pill)
            .testTag(ALPHABET_BUBBLE),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalBcContentColor provides BcColors.onLight) {
            BcText(letter, style = BcTextStyle.Initials, color = BcTextColor.OnLight, maxLines = 1)
        }
    }
}

/** How much of the bulge a letter takes, [distance] pixels away from the finger: all of it under the finger. */
private fun bell(distance: Float, spread: Float): Float = exp(-(distance * distance) / (2f * spread * spread))

private const val HALF = 0.5f
private const val BUBBLE_START = 0.6f
private val RAIL_WIDTH = 48.dp
private val RAIL_END_PADDING = 10.dp

/** How far the letter under the finger moves away from the edge. */
private val BULGE = 72.dp

/** How wide the wave is: letters this far from the finger still move by more than half of the bulge. */
private val SPREAD = 56.dp
private val BUBBLE = 48.dp
private val BUBBLE_GAP = 40.dp
