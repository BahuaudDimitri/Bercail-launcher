package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/** Distance a finger must travel up or down for the drawer to change state. */
val DRAWER_DRAG_THRESHOLD = 24.dp

/**
 * The glass drawer at the bottom of the home screen. Touching the handle, or sliding more than 24 dp up or down
 * anywhere on it, expands or collapses it; a slide never presses a button. Its height follows its content smoothly.
 * Without [handle] (the drawer as a remote on the Listen screen), it keeps its shape but no longer opens or closes.
 * While [searching], it grows to the whole screen, under the status bar; its content fills it.
 */
@Composable
fun BcDrawer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    searching: Boolean = false,
    handle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = BcTheme.motion
    val change by rememberUpdatedState(onExpandedChange)
    val glass by animateColorAsState(
        targetValue = when {
            searching -> BcColors.glassSearch
            expanded -> BcColors.glassOpen
            else -> BcColors.glass
        },
        animationSpec = tween(motion.fadeMillis),
        label = "drawer glass"
    )
    // While searching, the drawer is the whole screen: nothing to grab, nothing to slide.
    val grips = handle && !searching
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BcShapes.drawer)
            .background(glass)
            .then(if (grips) Modifier.opens(expanded) { change(it) } else Modifier)
            .animateContentSize(tween(motion.drawerMillis, easing = motion.easing))
            .then(if (searching) Modifier.fillMaxHeight().statusBarsPadding() else Modifier)
            .padding(
                start = BcSpacing.l,
                top = if (grips) 0.dp else BcSpacing.l,
                end = BcSpacing.l,
                bottom = bottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(BcSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (grips) {
            Handle(
                description = if (expanded) "Replier le tiroir" else "Déplier le tiroir",
                onClick = { change(!expanded) }
            )
        }
        Column(
            modifier = if (searching) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BcSpacing.m),
            content = content
        )
    }
}

/** Sliding up or down on the drawer, and the matching screen-reader actions, open or close it. */
private fun Modifier.opens(expanded: Boolean, change: (Boolean) -> Unit): Modifier = this
    .verticalSlide { up -> change(up) }
    .semantics {
        if (expanded) {
            collapse {
                change(false)
                true
            }
        } else {
            expand {
                change(true)
                true
            }
        }
    }
