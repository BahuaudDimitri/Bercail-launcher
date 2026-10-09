package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs

/** Distance a finger must travel up or down for the drawer to change state. */
val DRAWER_DRAG_THRESHOLD = 24.dp

/**
 * The glass drawer at the bottom of the home screen. Touching the handle, or sliding more than 24 dp up or down
 * anywhere on it, expands or collapses it; a slide never presses a button. Its height follows its content smoothly.
 */
@Composable
fun BcDrawer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    searching: Boolean = false,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BcShapes.drawer)
            .background(glass)
            .verticalSlide { up -> if (up != expanded) change(up) }
            .animateContentSize(tween(motion.drawerMillis, easing = motion.easing))
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
            .padding(start = BcSpacing.l, end = BcSpacing.l, bottom = BcSpacing.l),
        verticalArrangement = Arrangement.spacedBy(BcSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Handle(
            description = if (expanded) "Replier le tiroir" else "Déplier le tiroir",
            onClick = { change(!expanded) }
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BcSpacing.m), content = content)
    }
}

/**
 * The same glass drawer, opened over the screen for a conversation, the day or the media apps.
 * Sliding it down, touching the veil behind it or the system back gesture closes it.
 */
@Composable
fun BcSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = BcTheme.motion
    val dismiss by rememberUpdatedState(onDismiss)
    BackHandler(enabled = visible) { dismiss() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxSheet = maxHeight * SHEET_MAX_FRACTION
        AnimatedVisibility(
            visible,
            enter = fadeIn(tween(motion.fadeMillis)),
            exit = fadeOut(tween(motion.fadeMillis))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BcColors.scrim)
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null
                    ) { dismiss() }
                    .semantics { contentDescription = "Fermer" }
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(motion.sheetMillis, easing = motion.easing)) { it },
            exit = slideOutVertically(tween(motion.sheetMillis, easing = motion.easing)) { it }
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheet)
                    .clip(BcShapes.drawer)
                    .background(BcColors.glassSheet)
                    .verticalSlide { up -> if (!up) dismiss() }
                    .padding(start = BcSpacing.l, end = BcSpacing.l, bottom = BcSpacing.l),
                verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Handle(description = "Fermer la feuille", onClick = { dismiss() })
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
                    content = content
                )
            }
        }
    }
}

/** The grab bar: a short light line inside a 120 × 40 dp touch area. */
@Composable
private fun Handle(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HANDLE_TOUCH_WIDTH, BcSizes.minTouch)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(
                HANDLE_WIDTH,
                HANDLE_HEIGHT
            ).background(BcColors.text.copy(alpha = HANDLE_ALPHA), BcShapes.pill)
        )
    }
}

/**
 * Reports one vertical slide per gesture once the finger has travelled [DRAWER_DRAG_THRESHOLD], even outside the
 * element. The slide consumes the touch, so a button under the finger is not pressed.
 */
private fun Modifier.verticalSlide(onSlide: (up: Boolean) -> Unit): Modifier = pointerInput(Unit) {
    val threshold = DRAWER_DRAG_THRESHOLD.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var reported = false
        fun check(y: Float) {
            val travel = y - down.position.y
            if (!reported && abs(travel) >= threshold) {
                reported = true
                onSlide(travel < 0)
            }
        }
        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            ?: return@awaitEachGesture
        start.consume()
        check(start.position.y)
        verticalDrag(start.id) { change ->
            change.consume()
            check(change.position.y)
        }
    }
}

private const val SHEET_MAX_FRACTION = 0.62f
private const val HANDLE_ALPHA = 0.4f
private val SHEET_GAP = 10.dp
private val HANDLE_TOUCH_WIDTH = 120.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_HEIGHT = 4.dp
