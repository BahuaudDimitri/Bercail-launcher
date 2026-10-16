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
 * Sliding it down, touching the veil behind it or the system back gesture closes it.
 */
@Composable
fun BcSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    frost: BcFrost? = null,
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
            SheetSurface({ dismiss() }, modifier.heightIn(max = maxSheet), frost, content)
        }
    }
}

/**
 * The glass of every sheet, from the bottom of the screen: its handle on top, which closes it, as does sliding it
 * down. Its content stays above the phone's navigation bar and above the keyboard.
 */
@Composable
internal fun SheetSurface(
    onClose: () -> Unit,
    modifier: Modifier,
    frost: BcFrost?,
    content: @Composable ColumnScope.() -> Unit
) {
    val reduced = BcTheme.motion.reduced
    val powerSave by rememberPowerSaveMode()
    // Frosted only over a still Brume, and never in battery saver or without animations: tinted glass otherwise.
    val frosting = frost?.takeIf { !reduced && !powerSave }
    DisposableEffect(frosting) {
        frosting?.let { it.wanting++ }
        onDispose { frosting?.let { it.wanting-- } }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val blurred = rememberGraphicsLayer()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(BcShapes.sheet)
            .then(if (frosting != null) Modifier.frostedBehind(frosting, blurred) { origin } else Modifier)
            .background(if (frosting != null) BcColors.glassFrosted else BcColors.glassSheet)
            .background(SHEEN)
            .semantics { this[SheetGlassKey] = if (frosting != null) SheetGlass.Frosted else SheetGlass.Tinted }
            .border(1.dp, BcColors.sheetEdge, BcShapes.sheet)
            .verticalSlide { up -> if (!up) onClose() }
            .padding(start = BcSpacing.l, end = BcSpacing.l, bottom = bottomPadding()),
        verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Handle(description = "Fermer la feuille", onClick = onClose)
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SHEET_GAP),
            content = content
        )
    }
}

internal const val SHEET_MAX_FRACTION = 0.62f
private const val SHEET_MAX_WITH_KEYBOARD = 0.94f
private val SHEET_GAP = 10.dp
private const val SHEEN_STEP = 0.22f
private const val SHEEN_STEP_ALPHA = 0.035f
private const val SHEEN_END = 0.6f

/** The sheen at the top of the glass: white 11 %, fading out over the first 60 % of the sheet. */
private val SHEEN = Brush.verticalGradient(
    0f to BcColors.sheetSheen,
    SHEEN_STEP to BcColors.sheetSheen.copy(alpha = SHEEN_STEP_ALPHA),
    SHEEN_END to Color.Transparent
)
