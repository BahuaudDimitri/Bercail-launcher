package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPressVeil
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import kotlin.math.abs

/**
 * The glass goes down to the edge of the screen; its content stays above the phone's navigation bar, and above
 * the keyboard when it is out.
 */
@Composable
internal fun bottomPadding(): Dp {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val keyboard = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    return max(BcSpacing.l, max(navigationBar, keyboard) + BcSpacing.s)
}

/** The grab bar: a short light line inside a 120 × 40 dp touch area. */
@Composable
internal fun Handle(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(HANDLE_TOUCH_WIDTH, BcSizes.minTouch)
            .clickable(null, BcPressVeil.Pill, role = Role.Button, onClick = onClick)
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
internal fun Modifier.verticalSlide(onSlide: (up: Boolean) -> Unit): Modifier = pointerInput(Unit) {
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

private const val HANDLE_ALPHA = 0.4f
private val HANDLE_TOUCH_WIDTH = 120.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_HEIGHT = 4.dp
