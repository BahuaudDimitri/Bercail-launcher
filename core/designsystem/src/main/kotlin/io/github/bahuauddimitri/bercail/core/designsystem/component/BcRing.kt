package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor
import kotlin.math.roundToInt

internal const val UNREAD_BADGE = "unread-badge-"
internal const val RING_TAG = "ring-"
private const val MAX_BADGE = 99

/** What a ring holds: the initial of a person, an icon, or the glyph of an app. */
sealed interface BcRingMark {
    data class Initial(val letter: String) : BcRingMark

    data class Icon(val icon: BcIcons) : BcRingMark

    /**
     * The icon of an app in one colour: white, its shape in the alpha, framed as the icon is, so that it fills the
     * ring as the icon would. It takes the colour of the ring.
     */
    data class Glyph(val glyph: ImageBitmap) : BcRingMark
}

/** The color of a ring and of what it holds. */
sealed interface BcRingTone {
    /** A person, or a home control that is on. */
    data class Pastel(val pastel: BcPastel) : BcRingTone

    /** "Tous": light, on a fainter ring. */
    data object Neutral : BcRingTone

    /** A home control that is off, or a link that leaves Bercail ("Tout"). */
    data object Off : BcRingTone

    /** What stands out in a list: the first app found, the main media app. In the accent of the moment. */
    data object Accent : BcRingTone
}

/**
 * A ring of the open sheet that opens something: a favorite person (their initial in their pastel, the unread
 * count in the accent of the moment), "Tous" or "Tout". The label sits under the ring, on one line.
 */
@Composable
fun BcRing(
    label: String,
    mark: BcRingMark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: BcRingTone = BcRingTone.Neutral,
    unread: Int = 0,
    description: String = label
) {
    RingColumn(
        label = label,
        modifier = modifier
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Box {
            Ring(label, mark, tone)
            if (unread > 0) UnreadBadge(unread, Modifier.align(Alignment.TopEnd).testTag(UNREAD_BADGE + label))
        }
    }
}

/**
 * A ring of the open sheet that switches a home control: in its pastel when on, grey when off. On, its label says
 * the name and the state ("Salon · 60 %"); off, the name alone. A screen reader hears a switch with its state.
 */
@Composable
fun BcRing(
    name: String,
    state: String,
    mark: BcRingMark,
    pastel: BcPastel,
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    RingColumn(
        label = if (on) "$name · $state" else name,
        modifier = modifier
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .semantics {
                contentDescription = name
                stateDescription = state
            }
    ) {
        Ring(name, mark, if (on) BcRingTone.Pastel(pastel) else BcRingTone.Off)
    }
}

/**
 * A row of rings across the whole width: the first at the left edge, the last at the right edge, the room left
 * shared between them. On a narrow screen, each ring gives up some of its 64 dp column, never its 52 dp ring: five
 * rings fit on a 320 dp phone.
 */
@Composable
fun BcRingRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.fillMaxWidth()) { rings, constraints ->
        val share = if (rings.isEmpty()) 0 else constraints.maxWidth / rings.size
        val placed = rings.map { it.measure(Constraints(maxWidth = share, maxHeight = constraints.maxHeight)) }
        val height = placed.maxOfOrNull { it.height } ?: 0
        // As Arrangement.SpaceBetween: the same room between each two, each place rounded to the pixel.
        val room = (constraints.maxWidth - placed.sumOf { it.width }).toFloat() / (placed.size - 1).coerceAtLeast(1)
        layout(constraints.maxWidth, height) {
            var x = 0f
            placed.forEach {
                it.placeRelative(x.roundToInt(), 0)
                x += it.width + room
            }
        }
    }
}

@Composable
private fun RingColumn(label: String, modifier: Modifier, ring: @Composable () -> Unit) {
    Column(
        modifier = modifier.width(COLUMN_WIDTH).defaultMinSize(minHeight = BcSizes.minTouch),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LABEL_GAP)
    ) {
        ring()
        // One line, never cut: a long label may reach over the room between two rings, as on the reference.
        BcText(
            text = label,
            modifier = Modifier.wrapContentWidth(unbounded = true),
            style = BcTextStyle.Micro,
            color = BcTextColor.Muted,
            maxLines = 1
        )
    }
}

@Composable
internal fun Ring(
    key: String,
    mark: BcRingMark,
    tone: BcRingTone,
    size: Dp = BcSizes.ring,
    initialStyle: BcTextStyle = BcTextStyle.Initials,
    iconSize: Dp = ICON
) {
    val ink = tone.ink()
    val edge = if (tone == BcRingTone.Neutral) BcColors.strokeRing else ink
    Box(
        Modifier
            .size(size)
            .border(RING_STROKE, edge, BcShapes.pill)
            .testTag(RING_TAG + key),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalBcContentColor provides ink) {
            when (mark) {
                is BcRingMark.Initial -> BcText(mark.letter, style = initialStyle, maxLines = 1)

                is BcRingMark.Icon -> BcIcon(mark.icon, contentDescription = null, size = iconSize)

                is BcRingMark.Glyph -> Image(
                    bitmap = mark.glyph,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(ink)
                )
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun BcRingTone.ink(): Color = when (this) {
    is BcRingTone.Pastel -> pastel.color
    BcRingTone.Neutral -> BcColors.text
    BcRingTone.Off -> BcColors.off
    BcRingTone.Accent -> BcTheme.moment.accent
}

/** The unread count, in the accent of the moment. */
@Composable
private fun UnreadBadge(count: Int, modifier: Modifier) {
    Box(
        modifier = modifier
            .offset(x = BADGE_OFFSET_X, y = -BADGE_OFFSET_Y)
            .defaultMinSize(minWidth = BADGE_SIZE, minHeight = BADGE_SIZE)
            .background(BcTheme.moment.accent, BcShapes.pill)
            .padding(horizontal = BADGE_PADDING),
        contentAlignment = Alignment.Center
    ) {
        val label = if (count > MAX_BADGE) "$MAX_BADGE+" else "$count"
        BcText(label, style = BcTextStyle.Badge, color = BcTextColor.OnLight)
    }
}

private val COLUMN_WIDTH = 64.dp
private val LABEL_GAP = 6.dp
private val RING_STROKE = 1.5.dp
private val ICON = 20.dp
private val BADGE_SIZE = 18.dp
private val BADGE_OFFSET_X = 2.dp
private val BADGE_OFFSET_Y = 4.dp
private val BADGE_PADDING = 5.dp
