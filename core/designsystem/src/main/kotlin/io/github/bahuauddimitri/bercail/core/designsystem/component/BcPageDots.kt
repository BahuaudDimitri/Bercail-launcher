package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * The pagination dots between the screens of the home: the current one is a longer, brighter dash.
 * Each dot is announced "Écran <name>" and is a touch target at least 40 dp high.
 */
@Composable
fun BcPageDots(pages: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val fade = BcTheme.motion.fadeMillis
    Row(modifier.selectableGroup()) {
        pages.forEachIndexed { index, page ->
            val current = index == selected
            val width by animateDpAsState(if (current) CURRENT_WIDTH else DOT, tween(fade), label = "page dot")
            Box(
                modifier = Modifier
                    .size(TOUCH_WIDTH, BcSizes.minTouch)
                    .selectable(selected = current, role = Role.Tab, onClick = { onSelect(index) })
                    .semantics { contentDescription = "Écran $page" },
                contentAlignment = Alignment.Center
            ) {
                val alpha = if (current) CURRENT_ALPHA else OTHER_ALPHA
                Box(Modifier.size(width, DOT).background(BcColors.text.copy(alpha = alpha), BcShapes.pill))
            }
        }
    }
}

private const val CURRENT_ALPHA = 0.9f
private const val OTHER_ALPHA = 0.35f
private val TOUCH_WIDTH = 30.dp
private val DOT = 6.dp
private val CURRENT_WIDTH = 18.dp
