package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/** Glass: translucent tile with a light edge. Light: the primary action, light on dark. Ghost: no background. */
enum class BcButtonVariant(internal val background: Color, internal val content: Color, internal val edge: Boolean) {
    Glass(BcColors.tile, BcColors.text, edge = true),
    Light(BcColors.text, BcColors.onLight, edge = false),
    Ghost(Color.Transparent, BcColors.text, edge = false)
}

private const val DISABLED_ALPHA = 0.3f

/** A pill-shaped button with a label and an optional leading icon. At least 40 dp high. */
@Composable
fun BcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BcButtonVariant = BcButtonVariant.Glass,
    enabled: Boolean = true,
    icon: BcIcons? = null
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .surface(variant, BcShapes.pill, enabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = BcSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.s)
    ) {
        CompositionLocalProvider(LocalBcContentColor provides variant.content) {
            if (icon != null) BcIcon(icon, contentDescription = null, size = SMALL_ICON)
            BcText(text, style = BcTextStyle.Label)
        }
    }
}

/**
 * A round button holding a single icon, announced by [contentDescription].
 * A larger [size] (the remote's buttons) also gets a larger icon.
 */
@Composable
fun BcIconButton(
    icon: BcIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BcButtonVariant = BcButtonVariant.Glass,
    enabled: Boolean = true,
    size: Dp = BcSizes.iconButton
) {
    Box(
        modifier = modifier
            .size(size)
            .surface(variant, BcShapes.pill, enabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalBcContentColor provides variant.content) {
            val iconSize = if (size > BcSizes.iconButton) LARGE_ICON else SMALL_ICON
            BcIcon(icon, contentDescription = contentDescription, size = iconSize)
        }
    }
}

private val SMALL_ICON = 18.dp
private val LARGE_ICON = 22.dp

internal fun Modifier.surface(variant: BcButtonVariant, shape: Shape, enabled: Boolean = true): Modifier = this
    .alpha(if (enabled) 1f else DISABLED_ALPHA)
    .clip(shape)
    .background(variant.background)
    .then(if (variant.edge) Modifier.border(1.dp, BcColors.tileEdge, shape) else Modifier)
