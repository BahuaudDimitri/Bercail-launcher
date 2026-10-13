package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** What opens a summary line: one dot per person who wrote, or an icon. */
sealed interface BcSummaryLeading {
    data class Dots(val pastels: List<BcPastel>) : BcSummaryLeading

    data class Icon(val icon: BcIcons) : BcSummaryLeading
}

/**
 * One line of the folded drawer: "2 non lus · Léa, Tom", "Salon 60 %, Volets ouverts".
 * [strong] is written first, brighter; the whole line is one touch target, at least 40 dp high.
 * [onClickLabel] tells a screen reader what touching the line does.
 *
 * With [swipe], the line can also be slid to the right: it follows the finger, what it opens is written in the
 * room it leaves (brighter, with a tick, once the slide is long enough), and lifting the finger then opens it.
 * A slide that is too short, or to the left, does nothing: the line goes back to its place.
 */
@Composable
fun BcSummaryRow(
    text: String,
    leading: BcSummaryLeading,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strong: String? = null,
    onClickLabel: String? = null,
    swipe: BcRowSwipe? = null
) {
    val slide = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val sliding by remember { derivedStateOf { slide.value > 0f } }
    val armed by remember(threshold) { derivedStateOf { slide.value >= threshold } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .then(if (swipe != null) Modifier.slidesRight(slide, swipe) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        if (swipe != null && sliding) {
            BcText(
                text = swipe.label,
                style = BcTextStyle.Label,
                color = if (armed) BcTextColor.Primary else BcTextColor.Muted,
                maxLines = 1,
                modifier = Modifier.followsTheLine(slide, threshold)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = BcSizes.minTouch)
                .offset { IntOffset(slide.value.roundToInt(), 0) }
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                .semantics { if (swipe != null) customActions = listOf(swipe.asAction()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)
        ) {
            Box(Modifier.widthIn(min = LEADING_WIDTH)) {
                when (leading) {
                    is BcSummaryLeading.Dots -> Dots(leading.pastels)
                    is BcSummaryLeading.Icon -> BcIcon(leading.icon, null, size = ICON, tint = BcTextColor.Muted)
                }
            }
            SummaryText(text, strong)
        }
    }
}

@Composable
private fun SummaryText(text: String, strong: String?) {
    BasicText(
        text = buildAnnotatedString {
            if (strong != null) {
                withStyle(SpanStyle(color = BcColors.text, fontWeight = FontWeight.Medium)) { append(strong) }
                append(" · ")
            }
            append(text)
        },
        style = BcType.bodySmall.copy(color = BcColors.textMuted),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Lets a finger slide the line to the right, up to [SWIPE_LIMIT]. A slide up or down is left to whatever is
 * behind: the drawer still unfolds from a line. Lifting the finger past [SWIPE_THRESHOLD] does what the line says.
 */
@Composable
private fun Modifier.slidesRight(slide: Animatable<Float, *>, swipe: BcRowSwipe): Modifier {
    val scope = rememberCoroutineScope()
    val action by rememberUpdatedState(swipe)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val back = tween<Float>(BcTheme.motion.fadeMillis)
    return pointerInput(Unit) {
        val threshold = SWIPE_THRESHOLD.toPx()
        val limit = SWIPE_LIMIT.toPx()
        var travelled = 0f
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f },
            onDragEnd = {
                if (slide.value >= threshold) action.onSwipe()
                scope.launch { slide.animateTo(0f, back) }
            },
            onDragCancel = { scope.launch { slide.animateTo(0f, back) } },
            onHorizontalDrag = { change, amount ->
                travelled += amount
                val wanted = travelled.coerceIn(0f, limit)
                if (wanted > 0f || slide.value > 0f) change.consume()
                if (slide.value < threshold && wanted >= threshold) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                scope.launch(start = CoroutineStart.UNDISPATCHED) { slide.snapTo(wanted) }
            }
        )
    }
}

/** Places what the slide opens just before the line, coming in from the edge with it and growing clearer. */
private fun Modifier.followsTheLine(slide: Animatable<Float, *>, threshold: Float): Modifier = this
    .layout { measurable, _ ->
        val hint = measurable.measure(Constraints())
        // It takes no room of its own: it stands where the line was.
        layout(0, hint.height) { hint.place((slide.value - HINT_GAP.toPx() - hint.width).roundToInt(), 0) }
    }
    .graphicsLayer { alpha = (slide.value / threshold).coerceIn(0f, 1f) }

/** What sliding a line to the right does; [label] says it while the line slides: "Ouvrir Beeper". */
class BcRowSwipe(val label: String, val onSwipe: () -> Unit) {
    /** The same thing, offered to a screen reader, which cannot slide a line. */
    internal fun asAction() = CustomAccessibilityAction(label) {
        onSwipe()
        true
    }
}

/** One dot per person, in their pastel; a single dim dot when nobody wrote. */
@Composable
private fun Dots(pastels: List<BcPastel>) {
    Row(horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
        if (pastels.isEmpty()) {
            Box(Modifier.size(DOT).background(BcColors.textMuted.copy(alpha = EMPTY_ALPHA), BcShapes.pill))
        }
        pastels.forEach { Box(Modifier.size(DOT).background(it.color, BcShapes.pill)) }
    }
}

private const val EMPTY_ALPHA = 0.4f
private val TEXT_GAP = 10.dp
private val LEADING_WIDTH = 28.dp
private val ICON = 16.dp
private val DOT = 9.dp
private val DOT_GAP = 4.dp

/** How far a line must be slid for the slide to count, and how far it can go. */
private val SWIPE_THRESHOLD = 64.dp
private val SWIPE_LIMIT = 112.dp
private val HINT_GAP = 12.dp
