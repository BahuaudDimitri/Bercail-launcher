package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType
import kotlin.math.roundToInt

/** What opens a summary line: one dot per person who wrote, or an icon. */
sealed interface BcSummaryLeading {
    data class Dots(val pastels: List<BcPastel>) : BcSummaryLeading

    data class Icon(val icon: BcIcons) : BcSummaryLeading
}

/**
 * The summary line of the band: "2 non lus · Léa, Tom", then [aside] on the same line after a small dot
 * ("Salon 60 %, Volets ouverts"), which gives way first when the room runs out.
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
    aside: String? = null,
    onClickLabel: String? = null,
    swipe: BcRowSwipe? = null
) {
    val slide = rememberSlide()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .then(if (swipe != null) Modifier.slidesRight(slide, swipe) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        if (swipe != null) SlideHint(swipe, slide)
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
            if (aside != null) Aside(aside, Modifier.weight(1f, fill = false))
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

/** What follows the messages on the line, after a small dot. */
@Composable
private fun Aside(text: String, modifier: Modifier) {
    Box(Modifier.size(SEPARATOR).background(BcColors.textMuted.copy(alpha = SEPARATOR_ALPHA), BcShapes.pill))
    BasicText(
        text = text,
        modifier = modifier,
        style = BcType.bodySmall.copy(color = BcColors.textMuted),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private const val SEPARATOR_ALPHA = 0.5f
private val SEPARATOR = 3.dp
