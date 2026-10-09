package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** A small glass chip: quick replies, the "Écouter…" pill. At least 40 dp high. */
@Composable
fun BcPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: BcIcons? = null) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .surface(BcButtonVariant.Glass, BcShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = PILL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.xs)
    ) {
        if (icon != null) BcIcon(icon, contentDescription = null, size = PILL_ICON, tint = BcTextColor.Muted)
        BcText(text, style = BcTextStyle.Label)
    }
}

private val PILL_PADDING = 13.dp
private val PILL_ICON = 16.dp
