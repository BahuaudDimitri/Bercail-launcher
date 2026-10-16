package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * What the sheets frost: the background drawn under [frostSource] (Brume). It is recorded only while a sheet asks
 * for it, and only when the background draws anyway: a still Brume is recorded once, then costs nothing more.
 */
@Stable
class BcFrost internal constructor(internal val picture: GraphicsLayer) {
    /** Where the background lies on the screen. */
    internal var origin by mutableStateOf(Offset.Zero)

    /** How many open sheets want the frost: the background is recorded while one does. */
    internal var wanting by mutableIntStateOf(0)

    /** How many times the background was recorded for the glass. */
    internal var recordings = 0
}

/** The frost of a screen: one per screen, shared by the background and its sheets. */
@Composable
fun rememberBcFrost(): BcFrost {
    val picture = rememberGraphicsLayer()
    return remember(picture) { BcFrost(picture) }
}

/** Marks the background that the sheets of [frost] blur behind them. */
fun Modifier.frostSource(frost: BcFrost): Modifier = this
    .onGloballyPositioned { frost.origin = it.positionInRoot() }
    .drawWithContent {
        if (frost.wanting > 0) {
            frost.picture.record { this@drawWithContent.drawContent() }
            frost.recordings++
            drawLayer(frost.picture)
        } else {
            drawContent()
        }
    }

/** How the glass of a sheet is drawn, for the tests: frosted over a still Brume, tinted otherwise. */
internal enum class SheetGlass { Frosted, Tinted }

internal val SheetGlassKey = SemanticsPropertyKey<SheetGlass>("SheetGlass")

/**
 * Draws behind a sheet the background of [frost], blurred by 36 dp and saturated by 1.7, at the place it has on
 * the screen. [blurred] holds the blurred copy; [origin] is where the sheet lies on the screen.
 */
internal fun Modifier.frostedBehind(frost: BcFrost, blurred: GraphicsLayer, origin: () -> Offset): Modifier =
    drawBehind {
        blurred.renderEffect = frostEffect(this)
        blurred.record(frost.picture.size) { drawLayer(frost.picture) }
        val shift = frost.origin - origin()
        translate(shift.x, shift.y) { drawLayer(blurred) }
    }

private fun frostEffect(density: Density) = with(density) {
    val radius = FROST_BLUR.toPx()
    val saturation = ColorMatrix().apply { setSaturation(FROST_SATURATION) }
    RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(saturation),
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    ).asComposeRenderEffect()
}

private val FROST_BLUR = 36.dp
private const val FROST_SATURATION = 1.7f
