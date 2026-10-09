package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

/** A round home control (lamp, room, scene): lit in its pastel when on, glass when off, with its name and state. */
@Composable
fun BcHomeToggle(
    name: String,
    state: String,
    icon: BcIcons,
    pastel: BcPastel,
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(COLUMN_WIDTH)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .semantics {
                contentDescription = name
                stateDescription = state
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GAP)
    ) {
        val circle = Modifier.size(BcSizes.homeToggle)
        val lit = if (on) {
            circle
                .shadow(GLOW, BcShapes.pill, ambientColor = pastel.color, spotColor = pastel.color)
                .background(pastel.color, BcShapes.pill)
        } else {
            circle.background(BcColors.tile, BcShapes.pill).border(1.dp, BcColors.tileEdge, BcShapes.pill)
        }
        Box(lit, contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalBcContentColor provides if (on) BcColors.onPastel else BcColors.textMuted) {
                BcIcon(icon, contentDescription = null)
            }
        }
        val nameColor = if (on) BcTextColor.Primary else BcTextColor.Muted
        BcText(name, style = BcTextStyle.LabelSmall, color = nameColor, maxLines = 1)
        BcText(state, style = BcTextStyle.Micro, color = BcTextColor.Muted, maxLines = 1)
    }
}

private val COLUMN_WIDTH = 58.dp
private val GAP = 4.dp
private val GLOW = 10.dp
