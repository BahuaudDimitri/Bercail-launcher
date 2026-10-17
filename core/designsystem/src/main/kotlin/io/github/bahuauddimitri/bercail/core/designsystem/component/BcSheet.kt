package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberPowerSaveMode
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * A sheet of glass opened over the screen, for a conversation, the day or the media apps. It takes 62 % of
 * the screen at most, and more while the keyboard is out, which hides the bottom of it.
 * No veil darkens the screen behind it. Pulling it down (from where [grip] allows), touching the screen above it or
 * the system back gesture closes it: pulled, the glass follows the finger, and closes once let go past a third of
 * its height or flicked down; let go before, it goes back up.
 */
@Composable
fun BcSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    frost: BcFrost? = null,
    grip: BcSheetGrip = BcSheetGrip.Anywhere,
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = BcTheme.motion
    val dismiss by rememberUpdatedState(onDismiss)
    BackHandler(enabled = visible) { dismiss() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The keyboard takes the bottom of the screen: the sheet may then rise higher, to keep room for its content.
        val keyboardOut = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val maxSheet by animateDpAsState(
            targetValue = maxHeight * if (keyboardOut) SHEET_MAX_WITH_KEYBOARD else SHEET_MAX_FRACTION,
            animationSpec = tween(motion.sheetMillis, easing = motion.easing),
            label = "sheet room"
        )
        // No veil: the screen stays as it is above the sheet, but touching it closes the sheet.
        if (visible) {
            Box(
                Modifier
                    .fillMaxSize()
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
            SheetSurface({ dismiss() }, modifier.heightIn(max = maxSheet), frost, grip, content)
        }
    }
}

/**
 * The glass of every sheet, from the bottom of the screen: its handle on top, which closes it. Pulled down from
 * where [grip] allows, it follows the finger, and closes once it has gone the whole way down. Its content stays
 * above the phone's navigation bar and above the keyboard. Each opening starts with the sheet in its place.
 */
@Composable
internal fun SheetSurface(
    onClose: () -> Unit,
    modifier: Modifier,
    frost: BcFrost?,
    grip: BcSheetGrip,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = rememberGlass(frost)
    val pull = rememberSheetPull()
    ClosesWhenGone(pull, onClose)
    val anywhere = grip == BcSheetGrip.Anywhere
    Column(
        // The slide is read outside the sheet, which moves: what the sheet is called (its test tag) moves with it.
        modifier = Modifier
            .pullsDown(pull, anywhere)
            .followsPull(pull)
            .then(modifier)
            .fillMaxWidth()
            .glass(glass)
            .then(if (anywhere) Modifier.pullsWithLists(pull) else Modifier)
            .padding(start = BcSpacing.l, end = BcSpacing.l, bottom = bottomPadding()),
        verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Handle(description = "Fermer la feuille", onClick = onClose, modifier = Modifier.gripsSheet(pull))
        CompositionLocalProvider(LocalSheetGrip provides pull.takeIf { !anywhere }) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
                content = content
            )
        }
    }
}

internal const val SHEET_MAX_FRACTION = 0.62f
private const val SHEET_MAX_WITH_KEYBOARD = 0.94f
private val SHEET_GAP = 10.dp
