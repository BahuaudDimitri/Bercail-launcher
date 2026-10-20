package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import kotlin.math.roundToInt

/**
 * A row of a list put in order by hand: its [key], and the row itself, which takes the modifier it is given (it
 * carries « Monter » and « Descendre » for screen readers).
 */
class BcReorderRow(val key: String, val row: @Composable (Modifier) -> Unit)

/**
 * A list put in order by hand. Each row has a handle on its right, three short lines: held and dragged, the row
 * follows the finger above the others, which make room; let go, it takes the place it is over, and [onMove] tells
 * where from and where to (from 0). Screen readers move a row one place with « Monter » and « Descendre ». Nothing
 * moves unless a finger does: no animation runs by itself.
 */
@Composable
fun BcReorderList(rows: List<BcReorderRow>, onMove: (from: Int, to: Int) -> Unit, modifier: Modifier = Modifier) {
    var dragged by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val latest by rememberUpdatedState(rows)
    val from = rows.indexOfFirst { it.key == dragged }
    val to = placeOf(from, offset, rowHeight, rows.size)
    Column(modifier) {
        rows.forEachIndexed { index, item ->
            key(item.key) {
                val shift = shiftOf(index, from, to, offset, rowHeight)
                Row(
                    Modifier
                        .onSizeChanged { rowHeight = it.height }
                        .zIndex(if (index == from) 1f else 0f)
                        .graphicsLayer { translationY = shift },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { item.row(Modifier.moveActions(index, rows.size, onMove)) }
                    Handle(
                        Modifier.testTag(BC_REORDER_HANDLE_TAG + item.key).pointerInput(item.key) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragged = item.key
                                    offset = 0f
                                },
                                onDragEnd = {
                                    val start = latest.indexOfFirst { it.key == item.key }
                                    val end = placeOf(start, offset, rowHeight, latest.size)
                                    dragged = null
                                    offset = 0f
                                    if (start >= 0 && end != start) onMove(start, end)
                                },
                                onDragCancel = {
                                    dragged = null
                                    offset = 0f
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    offset += amount
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

/** The place a row dragged from [from] by [offset] pixels is over: the nearest one, never beyond the list. */
private fun placeOf(from: Int, offset: Float, rowHeight: Int, size: Int): Int =
    if (from < 0 || rowHeight == 0) from else (from + (offset / rowHeight).roundToInt()).coerceIn(0, size - 1)

/** How far a row is drawn from its place: the dragged one under the finger, those it passes one row aside. */
private fun shiftOf(index: Int, from: Int, to: Int, offset: Float, rowHeight: Int): Float = when {
    from < 0 -> 0f
    index == from -> offset
    index in (from + 1)..to -> -rowHeight.toFloat()
    index in to until from -> rowHeight.toFloat()
    else -> 0f
}

/** « Monter » and « Descendre » for screen readers, except where the list ends. */
private fun Modifier.moveActions(index: Int, size: Int, onMove: (Int, Int) -> Unit): Modifier = semantics {
    customActions = buildList {
        if (index > 0) add(CustomAccessibilityAction(UP) { onMove(index, index - 1).let { true } })
        if (index < size - 1) add(CustomAccessibilityAction(DOWN) { onMove(index, index + 1).let { true } })
    }
}

/** The handle: three short lines in grey, in a target as large as a finger; screen readers use the actions. */
@Composable
private fun Handle(modifier: Modifier) {
    Box(modifier.size(BcSizes.minTouch).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(HANDLE_WIDTH, HANDLE_HEIGHT)) {
            val stroke = HANDLE_STROKE.toPx()
            repeat(HANDLE_LINES) { line ->
                val y = stroke / 2 + line * (size.height - stroke) / (HANDLE_LINES - 1)
                drawLine(BcColors.textMuted, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
            }
        }
    }
}

/** Tags the handle of a row, followed by the row's key: for the tests. */
const val BC_REORDER_HANDLE_TAG = "bc-reorder-handle-"

private const val UP = "Monter"
private const val DOWN = "Descendre"
private const val HANDLE_LINES = 3
private val HANDLE_WIDTH = 18.dp
private val HANDLE_HEIGHT = 10.dp
private val HANDLE_STROKE = 1.5.dp
