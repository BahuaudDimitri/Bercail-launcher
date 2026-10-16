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
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
        const val TOLERANCE = 0.03f
    }
}
