package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcType

/** What opens a summary line: one dot per person who wrote, or an icon. */
sealed interface BcSummaryLeading {
    data class Dots(val pastels: List<BcPastel>) : BcSummaryLeading

    data class Icon(val icon: BcIcons) : BcSummaryLeading
}

/**
 * One line of the folded drawer: "2 non lus · Léa, Tom", "Salon 60 %, Volets ouverts".
 * [strong] is written first, brighter; the whole line is one touch target, at least 40 dp high.
 * [onClickLabel] tells a screen reader what touching the line does.
 */
@Composable
fun BcSummaryRow(
    text: String,
    leading: BcSummaryLeading,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strong: String? = null,
    onClickLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TEXT_GAP)
    ) {
        Box(Modifier.widthIn(min = LEADING_WIDTH)) {
            when (leading) {
                is BcSummaryLeading.Dots -> Dots(leading.pastels)
                is BcSummaryLeading.Icon -> BcIcon(leading.icon, null, size = ICON, tint = BcTextColor.Muted)
            }
        }
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
