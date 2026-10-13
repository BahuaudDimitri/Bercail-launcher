package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * How Bercail shows that something is being touched: a veil with round corners, there at once and fading when the
 * finger lifts. It reaches a little beyond what is touched, so that a bare line of text is not framed to the
 * letter; what clips its content to its own shape (a button, a tile, a row) clips the veil too. [fitted], it is a
 * pill exactly as large as what is touched: for a pill that only paints its background.
 */
internal class BcPressVeil private constructor(private val color: Color, private val fitted: Boolean = false) :
    IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        VeilNode(interactionSource, color, fitted)

    override fun equals(other: Any?): Boolean = other is BcPressVeil && other.color == color && other.fitted == fitted

    override fun hashCode(): Int = color.hashCode() + if (fitted) 1 else 0

    companion object {
        /** On the dark surfaces of Bercail, that is nearly everywhere. */
        val OnDark = BcPressVeil(BcColors.pressVeil)

        /** On a light surface: the main button. */
        val OnLight = BcPressVeil(BcColors.pressVeilOnLight)

        /** On a pill: the music pill, « Annuler l'envoi », the handle of the drawer. */
        val Pill = BcPressVeil(BcColors.pressVeil, fitted = true)
    }
}

private class VeilNode(
    private val interactions: InteractionSource,
    private val color: Color,
    private val fitted: Boolean
) : Modifier.Node(),
    DrawModifierNode {
    private val shown = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            val pressed = mutableListOf<PressInteraction.Press>()
            interactions.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed += interaction
                    is PressInteraction.Release -> pressed -= interaction.press
                    is PressInteraction.Cancel -> pressed -= interaction.press
                }
                // There at once, so that even a quick touch shows; gone softly.
                launch { if (pressed.isEmpty()) shown.animateTo(0f, tween(FADE_MILLIS)) else shown.snapTo(1f) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val strength = shown.value
        if (strength <= 0f) return
        val side = if (fitted) 0f else SIDE_MARGIN.toPx()
        val edge = if (fitted) 0f else EDGE_MARGIN.toPx()
        drawRoundRect(
            color = color.copy(alpha = color.alpha * strength),
            topLeft = Offset(-side, -edge),
            size = Size(size.width + 2 * side, size.height + 2 * edge),
            cornerRadius = CornerRadius(if (fitted) size.minDimension / 2 else RADIUS.toPx())
        )
    }
}

private const val FADE_MILLIS = 220

/** How far the veil reaches beyond what is touched. */
private val SIDE_MARGIN = 10.dp
private val EDGE_MARGIN = 4.dp
private val RADIUS = 14.dp
