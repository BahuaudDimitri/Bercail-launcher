package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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

/** A choice between a few options, as a sunken pill with the selected option lit. */
@Composable
fun BcSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(BcShapes.pill)
            .background(BcColors.inset)
            .padding(SEGMENT_INSET)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_INSET)
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
                    style = BcTextStyle.Label,
                    color = if (isSelected) BcTextColor.OnLight else BcTextColor.Muted,
                    maxLines = 1
                )
            }
        }
    }
}

/** An on/off switch, mint when on. Its touch area is at least 48 × 40 dp. */
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
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = SWITCH_TOUCH_WIDTH, minHeight = BcSizes.minTouch)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        val track = Modifier.size(TRACK_WIDTH, TRACK_HEIGHT_SWITCH).clip(BcShapes.pill)
        Box(
            if (checked) {
                track.background(BcColors.pastelMint)
            } else {
                track.background(BcColors.inset).border(1.dp, BcColors.tileEdge, BcShapes.pill)
            }
        ) {
            Box(
                Modifier
                    .padding(KNOB_INSET)
                    .offset { IntOffset(knobOffset.roundToPx(), 0) }
                    .size(KNOB_SIZE)
                    .background(if (checked) BcColors.onLight else BcColors.textMuted, BcShapes.pill)
            )
        }
    }
}

private const val FILL_ALPHA = 0.8f
private val TRACK_HEIGHT = 4.dp
private val SEGMENT_INSET = 4.dp
private val SEGMENT_PADDING = 12.dp
private val SWITCH_TOUCH_WIDTH = 48.dp
private val TRACK_WIDTH = 46.dp
private val TRACK_HEIGHT_SWITCH = 28.dp
private val KNOB_INSET = 4.dp
private val KNOB_SIZE = 20.dp
private val KNOB_TRAVEL = 18.dp
