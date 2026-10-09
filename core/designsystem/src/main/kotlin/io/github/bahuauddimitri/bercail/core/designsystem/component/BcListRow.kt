package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** What sits at the start of a list row. */
sealed interface BcLeading {
    /** Initials on a pastel, or on the neutral app tile without [pastel]. */
    data class Initials(val text: String, val pastel: BcPastel? = null) : BcLeading

    data class Icon(val icon: BcIcons) : BcLeading

    /** An app icon or a photo. */
    data class Picture(val bitmap: ImageBitmap) : BcLeading

    /** A time of day, for agenda rows. */
    data class Time(val text: String) : BcLeading
}

/** Dimmed: in the past. Highlighted: the next one. */
enum class BcRowEmphasis { Normal, Dimmed, Highlighted }

/** One row of a list: apps, search results, events of the day. At least 48 dp high. */
@Composable
fun BcListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: BcLeading? = null,
    emphasis: BcRowEmphasis = BcRowEmphasis.Normal,
    titleStyle: BcTextStyle = BcTextStyle.TitleLight,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.listRow)
            .clip(ROW_SHAPE)
            .background(if (emphasis == BcRowEmphasis.Highlighted) BcColors.tile else BcColors.tile.copy(alpha = 0f))
            .clickable(role = Role.Button, onClick = onClick)
            .alpha(if (emphasis == BcRowEmphasis.Dimmed) DIMMED_ALPHA else 1f)
            .padding(horizontal = BcSpacing.m, vertical = ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        if (leading != null) Leading(leading)
        Column(Modifier.weight(1f)) {
            BcText(title, style = titleStyle, maxLines = 1)
            if (subtitle != null) BcText(subtitle, style = BcTextStyle.Label, color = BcTextColor.Muted, maxLines = 1)
        }
        trailing?.invoke()
    }
}

@Composable
private fun Leading(leading: BcLeading) {
    when (leading) {
        is BcLeading.Initials -> Box(
            Modifier.size(LEADING_SIZE).background(leading.pastel?.color ?: BcColors.appTile, LEADING_SHAPE),
            contentAlignment = Alignment.Center
        ) { BcText(leading.text, style = BcTextStyle.LabelStrong, color = BcTextColor.OnPastel, maxLines = 1) }

        is BcLeading.Icon -> Box(
            Modifier.size(LEADING_SIZE).border(1.dp, BcColors.tileEdge, LEADING_SHAPE),
            contentAlignment = Alignment.Center
        ) { BcIcon(leading.icon, contentDescription = null, size = LEADING_ICON) }

        is BcLeading.Picture -> Image(
            leading.bitmap,
            contentDescription = null,
            modifier = Modifier.size(LEADING_SIZE).clip(LEADING_SHAPE),
            contentScale = ContentScale.Crop
        )

        is BcLeading.Time -> BcText(
            leading.text,
            modifier = Modifier.width(TIME_WIDTH),
            style = BcTextStyle.Caption,
            color = BcTextColor.Muted,
            maxLines = 1
        )
    }
}

private const val DIMMED_ALPHA = 0.45f
private val ROW_SHAPE = RoundedCornerShape(16.dp)
private val ROW_VERTICAL_PADDING = 7.dp
private val ROW_GAP = 14.dp
private val LEADING_SIZE = 34.dp
private val LEADING_SHAPE = RoundedCornerShape(12.dp)
private val LEADING_ICON = 18.dp
private val TIME_WIDTH = 46.dp
