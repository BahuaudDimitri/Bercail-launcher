package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/** Playback progress. With [onSeek], touching or dragging anywhere on its 40 dp band moves the position. */
@Composable
fun BcProgress(progress: Float, onSeek: ((Float) -> Unit)?, modifier: Modifier = Modifier) {
    val value = progress.coerceIn(0f, 1f)
    var width by remember { mutableIntStateOf(0) }
    val seek by rememberUpdatedState(onSeek)
    fun fractionAt(x: Float) = if (width == 0) 0f else (x / width).coerceIn(0f, 1f)

    val touch = if (onSeek == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(Unit) { detectTapGestures { seek?.invoke(fractionAt(it.x)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> seek?.invoke(fractionAt(change.position.x)) }
            }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BcSizes.minTouch)
            .onSizeChanged { width = it.width }
            .then(touch)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                if (onSeek != null) {
                    setProgress { target ->
                        onSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(TRACK_HEIGHT).clip(BcShapes.pill).background(BcColors.inset)) {
            Box(Modifier.fillMaxWidth(value).fillMaxHeight().background(BcColors.text.copy(alpha = FILL_ALPHA)))
        }
    }
}

/** A choice between a few options: a sunken pill with a fine edge, the selected option lit inside it. */
@Composable
fun BcSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(BcShapes.pill)
            .background(BcColors.inset)
            .border(1.dp, BcColors.segmentEdge, BcShapes.pill)
            .padding(SEGMENT_INSET)
            .selectableGroup()
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = BcSizes.minTouch)
                    .clip(BcShapes.pill)
                    .background(if (isSelected) BcColors.text else BcColors.inset.copy(alpha = 0f))
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = SEGMENT_PADDING),
                contentAlignment = Alignment.Center
            ) {
                BcText(
                    text = label,
                    style = BcTextStyle.Button,
                    color = if (isSelected) BcTextColor.OnLight else BcTextColor.Muted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * An on/off switch drawn as a line: a 40 × 22 dp outline and its knob, grey when off, in the accent of the moment
 * when on. Its touch area is at least 48 × 40 dp.
 */
@Composable
fun BcSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) KNOB_TRAVEL else 0.dp,
        animationSpec = tween(BcTheme.motion.fadeMillis, easing = BcTheme.motion.easing),
        label = "switch knob"
    )
    val ink = if (checked) BcTheme.moment.accent else BcColors.off
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = SWITCH_TOUCH_WIDTH, minHeight = BcSizes.minTouch)
            .toggleable(checked, null, indication = null, enabled, Role.Switch, onCheckedChange)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(TRACK_WIDTH, TRACK_HEIGHT_SWITCH)
                .border(SWITCH_STROKE, ink, BcShapes.pill)
                .testTag(SWITCH_TRACK_TAG + contentDescription.orEmpty()),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .padding(start = KNOB_INSET)
                    .offset { IntOffset(knobOffset.roundToPx(), 0) }
                    .size(KNOB_SIZE)
                    .background(ink, BcShapes.pill)
            )
        }
    }
}

/** Tags the drawn outline of a switch, after its description: for the tests. */
internal const val SWITCH_TRACK_TAG = "switch-"

private const val FILL_ALPHA = 0.8f
private val TRACK_HEIGHT = 4.dp
private val SEGMENT_INSET = 3.dp
private val SEGMENT_PADDING = 14.dp
private val SWITCH_TOUCH_WIDTH = 48.dp
private val TRACK_WIDTH = 40.dp
private val TRACK_HEIGHT_SWITCH = 22.dp
private val SWITCH_STROKE = 1.5.dp

// From the outer edge: the 1.5 dp line, then 3 dp of room around the knob.
private val KNOB_INSET = 4.5.dp
private val KNOB_SIZE = 13.dp
private val KNOB_TRAVEL = TRACK_WIDTH - KNOB_INSET * 2 - KNOB_SIZE
