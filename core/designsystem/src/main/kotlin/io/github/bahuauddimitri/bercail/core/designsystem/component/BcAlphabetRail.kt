package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The alphabet along the edge of the search list, letters in use only. Touching or sliding along it reaches
 * a letter (once per letter); letters near the finger grow and lean out like a wave.
 */
@Composable
fun BcAlphabetRail(letters: List<String>, onLetter: (String) -> Unit, modifier: Modifier = Modifier) {
    var height by remember { mutableIntStateOf(0) }
    var finger by remember { mutableStateOf<Float?>(null) }
    val reach by rememberUpdatedState(onLetter)
    val current by rememberUpdatedState(letters)

    Column(
        modifier = modifier
            .width(RAIL_WIDTH)
            .onSizeChanged { height = it.height }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var last = -1
                    fun follow(y: Float) {
                        if (height == 0 || current.isEmpty()) return
                        finger = y / height * current.size - HALF
                        val index = (y / height * current.size).toInt().coerceIn(0, current.lastIndex)
                        if (index != last) {
                            last = index
                            reach(current[index])
                        }
                    }
                    val down = awaitFirstDown()
                    down.consume()
                    follow(down.position.y)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                follow(change.position.y)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    finger = null
                }
            }
            .padding(end = RAIL_END_PADDING),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        letters.forEachIndexed { index, letter ->
            val wave = finger?.let { (1f - abs(index - it) / WAVE_REACH).coerceIn(0f, 1f) } ?: 0f
            BcText(
                text = letter,
                style = BcTextStyle.LabelStrong,
                color = if (wave > HIT) BcTextColor.Primary else BcTextColor.Muted,
                modifier = Modifier
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(1f, HALF)
                        scaleX = 1f + GROWTH * wave
                        scaleY = 1f + GROWTH * wave
                        translationX = -LEAN.toPx() * wave
                    }
                    .semantics {
                        onClick(label = "Aller à $letter") {
                            reach(letter)
                            true
                        }
                    }
            )
        }
    }
}

private const val HALF = 0.5f
private const val WAVE_REACH = 3f
private const val GROWTH = 0.9f
private const val HIT = 0.8f
private val RAIL_WIDTH = 48.dp
private val RAIL_END_PADDING = 10.dp
private val LEAN = 18.dp
