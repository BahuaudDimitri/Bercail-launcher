package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPressVeil
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/**
 * The two looks of a button with a label. Bordered: the pill of every button, a faint white fill and a white edge.
 * Accent: filled with the accent of the moment, dark text; only for Send, Install and the play button of the main
 * media app.
 */
enum class BcButtonVariant {
    Bordered,
    Accent
}

/**
 * The looks of a round button holding an icon: [Bordered] and [Accent] as for a button with a label; [Light], filled
 * light, only for play and pause on Écoute; [Ghost], a bare icon (back arrow, clearing the search, the remote's
 * previous and next).
 */
enum class BcIconButtonVariant {
    Bordered,
    Accent,
    Light,
    Ghost
}

private const val DISABLED_ALPHA = 0.3f

/**
 * A pill-shaped button with a label and an optional leading icon: drawn 36 dp high, in a touch target of at least
 * 40 dp (a touch just above or below the drawn pill still counts).
 */
@Composable
fun BcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BcButtonVariant = BcButtonVariant.Bordered,
    enabled: Boolean = true,
    icon: BcIcons? = null
) {
    val look = if (variant == BcButtonVariant.Accent) Look.Accent else Look.Bordered
    val interactions = remember { MutableInteractionSource() }
    // The whole target takes the touch; the pill drawn inside it keeps the width the caller gives the button.
    Box(
        modifier = modifier
            .clickable(interactions, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .touchTarget(BcSizes.minTouch),
        propagateMinConstraints = true
    ) {
        Row(
            modifier = Modifier
                .testTag(BUTTON_SURFACE_TAG)
                .defaultMinSize(minHeight = BUTTON_HEIGHT)
                .surface(look, look.fill(), enabled)
                .indication(interactions, look.veil)
                .padding(horizontal = BcSpacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BcSpacing.s, Alignment.CenterHorizontally)
        ) {
            CompositionLocalProvider(LocalBcContentColor provides look.content) {
                if (icon != null) BcIcon(icon, contentDescription = null, size = SMALL_ICON)
                BcText(text, style = BcTextStyle.Button)
            }
        }
    }
}

/**
 * A round button holding a single icon, announced by [contentDescription].
 * A larger [size] (the remote's buttons, Send) also gets a larger icon.
 */
@Composable
fun BcIconButton(
    icon: BcIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BcIconButtonVariant = BcIconButtonVariant.Bordered,
    enabled: Boolean = true,
    size: Dp = BcSizes.iconButton
) {
    val look = when (variant) {
        BcIconButtonVariant.Bordered -> Look.Bordered
        BcIconButtonVariant.Accent -> Look.Accent
        BcIconButtonVariant.Light -> Look.Light
        BcIconButtonVariant.Ghost -> Look.Ghost
    }
    Box(
        modifier = modifier
            .size(size)
            .surface(look, look.fill(), enabled)
            .clickable(null, look.veil, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalBcContentColor provides look.content) {
            val iconSize = if (size > BcSizes.iconButton) LARGE_ICON else SMALL_ICON
            BcIcon(icon, contentDescription = contentDescription, size = iconSize)
        }
    }
}

/** What a button looks like: its content color and the veil shown over it while touched. */
private enum class Look(val content: Color, val veil: BcPressVeil) {
    Bordered(BcColors.text, BcPressVeil.OnDark),
    Accent(BcColors.onAccent, BcPressVeil.OnLight),
    Light(BcColors.onLight, BcPressVeil.OnLight),
    Ghost(BcColors.text, BcPressVeil.OnDark)
}

/** The drawn height of a button with a label. */
internal val BUTTON_HEIGHT = 36.dp
internal const val BUTTON_SURFACE_TAG = "button-surface"
private val SMALL_ICON = 18.dp
private val LARGE_ICON = 22.dp

/** The fill of a button: the accent follows the moment of the screen. */
@Composable
@ReadOnlyComposable
private fun Look.fill(): Color = when (this) {
    Look.Bordered -> BcColors.buttonFill
    Look.Accent -> BcTheme.moment.accent
    Look.Light -> BcColors.text
    Look.Ghost -> Color.Transparent
}

/** A pill of [fill], with a white edge for the bordered look, faded while disabled. */
private fun Modifier.surface(look: Look, fill: Color, enabled: Boolean): Modifier = this
    .alpha(if (enabled) 1f else DISABLED_ALPHA)
    .clip(BcShapes.pill)
    .background(fill)
    .then(if (look == Look.Bordered) Modifier.border(1.dp, BcColors.strokeButton, BcShapes.pill) else Modifier)

/**
 * Makes room for a touch target at least [min] high around what is drawn, which stays centered in it: what comes
 * before this modifier (the click) covers the whole target, what comes after only the drawing.
 */
internal fun Modifier.touchTarget(min: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = maxOf(placeable.height, min.roundToPx()).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}
