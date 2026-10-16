package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcButtonUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val moment = mutableStateOf(BcMoment.Day)
    private val content = mutableStateOf<@Composable () -> Unit>({})

    @Test
    fun `le bouton est une pilule dessinée à 36 dp de haut dans une cible de 40 dp`() {
        show { BcButton("Autoriser", onClick = {}) }

        compose.onNodeWithText("Autoriser").assertHeightIsEqualTo(40.dp)
        compose.onNodeWithTag(BUTTON_SURFACE_TAG, useUnmergedTree = true).assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun `toucher juste au-dessus de la pilule dessinée déclenche quand même le bouton`() {
        var clicks = 0
        show { BcButton("Autoriser", onClick = { clicks++ }) }

        compose.onNodeWithText("Autoriser").performTouchInput { click(Offset(centerX, 1f)) }

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `le bouton bordé n'a qu'un léger fond blanc, sans aplat`() {
        show { BcButton("Autoriser", onClick = {}) }

        val expected = BcColors.buttonFill.compositeOver(BcColors.base)
        assertThat(pixelOfSurface(at = SURFACE_SIDE).distanceTo(expected)).isLessThan(TOLERANCE)
    }

    @Test
    fun `le bouton plein d'accent prend l'accent du moment`() {
        show { BcButton("Installer", onClick = {}, variant = BcButtonVariant.Accent) }

        BcMoment.entries.forEach { each ->
            moment.value = each
            compose.waitForIdle()

            assertThat(pixelOfSurface(at = SURFACE_SIDE).distanceTo(each.accent)).isLessThan(TOLERANCE)
        }
    }

    @Test
    fun `le bouton Envoyer d'une réponse est un rond de 48 dp plein de l'accent du moment`() {
        show { BcReplyField(value = "J'arrive", onValueChange = {}, onSend = {}) }

        val send = compose.onNodeWithContentDescription("Envoyer")
        send.assertHeightIsEqualTo(48.dp).assertWidthIsEqualTo(48.dp)
        val pixels = send.captureToImage().toPixelMap()
        val inside = with(compose.density) { SEND_INSIDE.roundToPx() }
        assertThat(pixels[inside, pixels.height / 2].distanceTo(BcMoment.Day.accent)).isLessThan(TOLERANCE)
    }

    @Test
    fun `le champ Répondre est une pilule d'au moins 48 dp`() {
        show { BcReplyField(value = "", onValueChange = {}, onSend = {}) }

        compose.onNodeWithTag(REPLY_FIELD_TAG).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `annuler l'envoi n'a pas de fond, son anneau se vide dans l'accent sur un reste blanc`() {
        show { BcUndoButton(remaining = 0.5f, onUndo = {}) }

        val pixels = compose.onNodeWithText("Annuler l'envoi").captureToImage().toPixelMap()
        fun at(x: Dp, y: Dp) = with(compose.density) { pixels[x.roundToPx(), y.roundToPx()] }
        // The ring, 16 dp wide, is centered on the 40 dp target; half of it is left, on the right.
        assertThat(at(UNDO_RING_RIGHT, UNDO_MIDDLE).distanceTo(BcMoment.Day.accent)).isLessThan(TOLERANCE)
        val rest = BcColors.strokeButton.compositeOver(BcColors.base)
        assertThat(at(UNDO_RING_LEFT, UNDO_MIDDLE).distanceTo(rest)).isLessThan(TOLERANCE)
        assertThat(at(UNDO_BETWEEN, UNDO_TOP).distanceTo(BcColors.base)).isLessThan(TOLERANCE)
    }

    @Test
    fun `le texte posé sur l'accent est sombre`() {
        assertThat(BcColors.onAccent).isEqualTo(Color(0xFF1B1B24))
    }

    /** Shows [shown] on the night background, in the accent of [moment]. */
    private fun show(shown: @Composable () -> Unit) {
        content.value = shown
        compose.setContent {
            BcTheme {
                BcMomentScope(moment.value) {
                    Box(Modifier.background(BcColors.base)) { content.value() }
                }
            }
        }
        compose.waitForIdle()
    }

    /** The color of the drawn pill [at] that distance from its left edge, halfway up, away from the label. */
    private fun pixelOfSurface(at: Dp): Color {
        val pixels = compose.onNodeWithTag(BUTTON_SURFACE_TAG, useUnmergedTree = true).captureToImage().toPixelMap()
        return pixels[with(compose.density) { at.roundToPx() }, pixels.height / 2]
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        /** Inside the round end of the pill, before the 16 dp padding where the label starts. */
        val SURFACE_SIDE = 10.dp

        /** Inside the round send button, left of its arrow. */
        val SEND_INSIDE = 8.dp

        /** The middle of the stroke of the undo ring, on its right and on its left, and a point beside it. */
        val UNDO_RING_RIGHT = 15.25.dp
        val UNDO_RING_LEFT = 0.75.dp
        val UNDO_MIDDLE = 20.dp
        val UNDO_BETWEEN = 20.dp
        val UNDO_TOP = 2.dp
        const val TOLERANCE = 0.03f
    }
}
