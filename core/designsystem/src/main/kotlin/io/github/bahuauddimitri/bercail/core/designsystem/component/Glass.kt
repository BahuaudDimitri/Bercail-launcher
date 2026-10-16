package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberPowerSaveMode
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * The two cuts of the glass. A sheet rises from the bottom: rounded top corners, a light edge, a sheen over the top
 * 60 %. The search fills the screen: no corner, no edge, a fainter sheen over the top 30 %, and a darker glass.
 */
internal enum class GlassCut { Sheet, Full }

/** The glass of a sheet: what it frosts, if anything, and where it lies on the screen. */
@Stable
internal class Glass(val frosting: BcFrost?, val blurred: GraphicsLayer, val cut: GlassCut) {
    var origin by mutableStateOf(Offset.Zero)
}

/**
 * The glass of a sheet over [frost]. Frosted only over a still Brume, and never in battery saver or without
 * animations: tinted glass otherwise. While it is frosted, the background is recorded for it.
 */
@Composable
internal fun rememberGlass(frost: BcFrost?, cut: GlassCut = GlassCut.Sheet): Glass {
    val reduced = BcTheme.motion.reduced
    val powerSave by rememberPowerSaveMode()
    val frosting = frost?.takeIf { !reduced && !powerSave }
    DisposableEffect(frosting) {
        frosting?.let { it.wanting++ }
        onDispose { frosting?.let { it.wanting-- } }
    }
    val blurred = rememberGraphicsLayer()
    return remember(frosting, blurred, cut) { Glass(frosting, blurred, cut) }
}

/**
 * Draws [glass] behind the content: the frosted background if any, the bluish black tint, the sheen at the top
 * and, for a sheet, its edge and its corners. [shown] fades the whole glass in and out.
 */
internal fun Modifier.glass(glass: Glass, shown: () -> Float = { 1f }): Modifier {
    val sheet = glass.cut == GlassCut.Sheet
    val frosting = glass.frosting
    val tint = when {
        !sheet -> BcColors.glassSearch
        frosting != null -> BcColors.glassFrosted
        else -> BcColors.glassSheet
    }
    val sheen = if (sheet) SHEET_SHEEN else FULL_SHEEN
    return this
        .onGloballyPositioned { glass.origin = it.positionInRoot() }
        .then(if (sheet) Modifier.clip(BcShapes.sheet) else Modifier)
        .then(
            if (frosting !=
                null
            ) {
                Modifier.frostedBehind(frosting, glass.blurred, shown) { glass.origin }
            } else {
                Modifier
            }
        )
        .drawBehind {
            val opacity = shown()
            if (opacity > 0f) {
                drawRect(tint, alpha = opacity)
                drawRect(sheen, alpha = opacity)
            }
        }
        .semantics { this[SheetGlassKey] = if (frosting != null) SheetGlass.Frosted else SheetGlass.Tinted }
        .then(if (sheet) Modifier.border(EDGE, BcColors.sheetEdge, BcShapes.sheet) else Modifier)
}

private val EDGE = 1.dp
private const val SHEEN_STEP = 0.22f
private const val SHEEN_STEP_ALPHA = 0.035f
private const val SHEEN_END = 0.6f
private const val FULL_SHEEN_END = 0.3f

/** The sheen at the top of a sheet: white 11 %, fading out over the first 60 % of it. */
private val SHEET_SHEEN = Brush.verticalGradient(
    0f to BcColors.sheetSheen,
    SHEEN_STEP to BcColors.sheetSheen.copy(alpha = SHEEN_STEP_ALPHA),
    SHEEN_END to Color.Transparent
)

/** The sheen at the top of the search: white 5 %, fading out over the first 30 % of the screen. */
private val FULL_SHEEN = Brush.verticalGradient(0f to BcColors.searchSheen, FULL_SHEEN_END to Color.Transparent)
