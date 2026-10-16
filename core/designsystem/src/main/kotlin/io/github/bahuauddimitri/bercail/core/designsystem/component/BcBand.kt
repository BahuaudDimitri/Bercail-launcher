package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * The flat band at the bottom of the home screen: no background, no handle. Sliding more than 24 dp up anywhere on
 * it calls [onOpen], which opens the sheet over it; a slide never presses a button. Without [onOpen] (the band as a
 * remote on the Listen screen), it opens nothing. Its height follows its content smoothly.
 * While [searching], it rises to the whole screen, under the status bar, in the glass of the sheets cut to the
 * screen: no corner, no edge, darker (80 %), frosted over a still [frost]; its content fills it.
 */
@Composable
fun BcBand(
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
    searching: Boolean = false,
    frost: BcFrost? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = BcTheme.motion
    val open by rememberUpdatedState(onOpen)
    val shown by animateFloatAsState(
        targetValue = if (searching) 1f else 0f,
        animationSpec = tween(motion.fadeMillis),
        label = "band glass"
    )
    // Frosted only while searching: folded, the band has no glass at all.
    val glass = rememberGlass(frost?.takeIf { searching }, GlassCut.Full)
    // While searching, the band is the whole screen: nothing to slide.
    val opens = onOpen != null && !searching
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(glass) { shown }
            .then(if (opens) Modifier.opensOnSlideUp { open?.invoke() } else Modifier)
            .animateContentSize(tween(motion.drawerMillis, easing = motion.easing))
            .then(if (searching) Modifier.fillMaxHeight().statusBarsPadding() else Modifier)
            .padding(
                start = BcSpacing.l,
                top = if (searching) BcSpacing.l else BcSpacing.xs,
                end = BcSpacing.l,
                bottom = bottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(BcSpacing.m)
    ) {
        Column(
            modifier = if (searching) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BcSpacing.m),
            content = content
        )
    }
}

/** Sliding up on the band, and the matching screen-reader action, open the sheet. */
private fun Modifier.opensOnSlideUp(open: () -> Unit): Modifier = this
    .verticalSlide { up -> if (up) open() }
    .semantics {
        expand {
            open()
            true
        }
    }

/**
 * The sheet that rises over the band and covers it: the same glass as every sheet, with its handle. It takes
 * 62 % of the screen at most. Its handle, sliding it down or the system back gesture closes it; touching beside it
 * does not. It grows from the bottom, so that what stands above it makes room as it rises.
 */
@Composable
fun BcBandSheet(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    frost: BcFrost? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = BcTheme.motion
    val close by rememberUpdatedState(onClose)
    BackHandler(enabled = visible) { close() }
    BoxWithConstraints(modifier) {
        val room = maxHeight * SHEET_MAX_FRACTION
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically(tween(motion.sheetMillis, easing = motion.easing), expandFrom = Alignment.Top),
            exit = shrinkVertically(tween(motion.sheetMillis, easing = motion.easing), shrinkTowards = Alignment.Top)
        ) {
            SheetSurface({ close() }, Modifier.heightIn(max = room), frost, content)
        }
    }
}
