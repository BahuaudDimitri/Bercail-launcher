package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/** What sits at the start of a list row: an initial, an icon or an app in a 36 dp ring, an app as it is, or a time. */
sealed interface BcLeading {
    /** An initial in a white ring, or a person's initial in a ring of their [pastel]. */
    data class Initials(val text: String, val pastel: BcPastel? = null) : BcLeading

    data class Icon(val icon: BcIcons) : BcLeading

    /** The icon of an app in one colour ([BcRingMark.Glyph]), in the colour of the ring. */
    data class Glyph(val glyph: ImageBitmap) : BcLeading

    /** The real icon of an app, in its own colours, 36 dp round and without a ring, even when the row stands out. */
    data class Picture(val bitmap: ImageBitmap) : BcLeading

    /** A time of day, for agenda rows. */
    data class Time(val text: String) : BcLeading
}

/** Dimmed: in the past. Accent: what stands out (the next appointment, the chosen app), its ring in the accent. */
enum class BcRowEmphasis { Normal, Dimmed, Accent }

/**
 * One row of a list: apps, search results, events of the day. 50 dp high, with no background, even when it stands
 * out. The letters of the title in [found] (what was typed in the search) are drawn in the accent of the moment. The
 * subtitle holds on [subtitleLines] lines, then is cut; the row grows with it.
 */
@Composable
fun BcListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleLines: Int = 1,
    leading: BcLeading? = null,
    emphasis: BcRowEmphasis = BcRowEmphasis.Normal,
    titleStyle: BcTextStyle = BcTextStyle.ListTitle,
    found: IntRange? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clip(ROW_SHAPE)
            .clickable(role = Role.Button, onClick = onClick)
            .alpha(if (emphasis == BcRowEmphasis.Dimmed) DIMMED_ALPHA else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        if (leading != null) Leading(title, leading, accent = emphasis == BcRowEmphasis.Accent)
        Column(Modifier.weight(1f)) {
            Title(title, titleStyle, found)
            if (subtitle != null) {
                BcText(subtitle, style = BcTextStyle.ListSubtitle, color = BcTextColor.Muted, maxLines = subtitleLines)
            }
        }
        trailing?.invoke()
    }
}

/** The title, the letters [found] in the accent of the moment. */
@Composable
private fun Title(title: String, style: BcTextStyle, found: IntRange?) {
    val color = LocalBcContentColor.current
    val shown = found?.takeIf { it.first >= 0 && it.last < title.length && !it.isEmpty() }
    BasicText(
        text = if (shown == null) AnnotatedString(title) else title.withAccent(shown, BcTheme.moment.accent),
        style = style.style.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun String.withAccent(range: IntRange, accent: Color): AnnotatedString = buildAnnotatedString {
    append(substring(0, range.first))
    withStyle(SpanStyle(color = accent)) { append(substring(range)) }
    append(substring(range.last + 1))
}

@Composable
private fun Leading(key: String, leading: BcLeading, accent: Boolean) {
    val neutral = if (accent) BcRingTone.Accent else BcRingTone.Neutral
    when (leading) {
        is BcLeading.Initials -> RowRing(
            key,
            BcRingMark.Initial(leading.text),
            leading.pastel?.takeUnless { accent }?.let(BcRingTone::Pastel) ?: neutral
        )

        is BcLeading.Icon -> RowRing(key, BcRingMark.Icon(leading.icon), neutral)

        is BcLeading.Glyph -> RowRing(key, BcRingMark.Glyph(leading.glyph), neutral)

        is BcLeading.Picture -> Image(
            leading.bitmap,
            contentDescription = null,
            modifier = Modifier.size(RING_SIZE).clip(BcShapes.pill).testTag(PICTURE_TAG + key),
            contentScale = ContentScale.Crop
        )

        is BcLeading.Time -> CompositionLocalProvider(
            LocalBcContentColor provides if (accent) BcTheme.moment.accent else BcTextColor.Muted.color
        ) {
            BcText(leading.text, modifier = Modifier.width(TIME_WIDTH), style = BcTextStyle.Caption, maxLines = 1)
        }
    }
}

@Composable
private fun RowRing(key: String, mark: BcRingMark, tone: BcRingTone) {
    Ring(key, mark, tone, size = RING_SIZE, initialStyle = BcTextStyle.ListInitial, iconSize = RING_ICON)
}

/** Tags the picture of an app at the start of its row, after the row's title: for the tests. */
internal const val PICTURE_TAG = "picture-"

private const val DIMMED_ALPHA = 0.45f
private val ROW_HEIGHT = 50.dp
private val ROW_SHAPE = BcShapes.tile
private val ROW_GAP = 14.dp
private val RING_SIZE = 36.dp
private val RING_ICON = 17.dp
private val TIME_WIDTH = 48.dp
