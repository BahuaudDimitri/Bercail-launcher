package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * A block of lines that is one touch target, like the top of the home screen: touching it calls [onClick], which
 * [onClickLabel] names for a screen reader. It can also be slid to the right, like a line of the folded drawer: it
 * follows the finger, what [swipe] opens is written in the room it leaves, and lifting the finger opens it.
 */
@Composable
fun BcSwipeBlock(
    swipe: BcRowSwipe,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLabel: String? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    val slide = rememberSlide()
    Box(modifier.fillMaxWidth().slidesRight(slide, swipe), contentAlignment = Alignment.CenterStart) {
        SlideHint(swipe, slide)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(slide.value.roundToInt(), 0) }
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                .semantics { customActions = listOf(swipe.asAction()) },
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}
