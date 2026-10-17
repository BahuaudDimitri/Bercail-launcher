package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
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

/** The flat rows of the settings, their section titles and their controls, as drawn. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcSettingRowUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val content = mutableStateOf<@Composable () -> Unit>({})

    /** Shows [shown] 360 dp wide on the night background, in the accent of the day. */
    private fun show(shown: @Composable () -> Unit) {
        content.value = shown
        compose.setContent {
            BcTheme {
                BcMomentScope(BcMoment.Day) {
                    Box(Modifier.width(WIDTH).background(BcColors.base)) { content.value() }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(text: String): DpRect = compose.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot()

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    @Test
    fun `une ligne de réglage est à plat, sans tuile, avec seulement un filet blanc à 10 % en bas`() {
        show { BcSettingRow("Musique en cours", Modifier.testTag(ROW), subtitle = "Bercail voit ce qui joue") }
        compose.redraw()

        val pixels = compose.onNodeWithTag(ROW).captureToImage().toPixelMap()
        val bottom = (0 until pixels.height).minOf {
            pixels[pixels.width / 2, it].distanceTo(BcColors.hairline.compositeOver(BcColors.base))
        }
        assertThat(pixels[1, pixels.height / 2].distanceTo(BcColors.base)).isLessThan(TOLERANCE)
        assertThat(bottom).isLessThan(TOLERANCE)
    }

    @Test
    fun `le titre d'une ligne de réglage est à 12 dp du haut, contre le bord gauche`() {
        show { BcSettingRow("Dock", Modifier.testTag(ROW), subtitle = "Chrome, Téléphone") }

        val row = compose.onNodeWithTag(ROW).getBoundsInRoot()
        val title = bounds("Dock")
        assertThat((title.top - row.top).value).isCloseTo(ROW_PADDING.value, delta = 0.5f)
        assertThat((title.left - row.left).value).isCloseTo(0f, delta = 0.5f)
    }

    @Test
    fun `la commande d'une ligne de réglage se pose à droite, à hauteur du titre`() {
        show {
            BcSettingRow(
                "Agenda du téléphone",
                Modifier.testTag(ROW),
                subtitle = "Pour lire tes rendez-vous",
                trailing = { BcButton("Autoriser", onClick = {}) }
            )
        }

        val row = compose.onNodeWithTag(ROW).getBoundsInRoot()
        val button = compose.onNodeWithText("Autoriser").getBoundsInRoot()
        assertThat(button.left).isGreaterThan(bounds("Agenda du téléphone").right)
        assertThat((row.right - button.right).value).isLessThan(BUTTON_SIDE.value)
        assertThat(button.top).isLessThan(bounds("Pour lire tes rendez-vous").bottom)
    }

    @Test
    fun `un long choix se pose sous le titre et son explication`() {
        show {
            BcSettingRow("Sans musique", subtitle = "La brume du moment, ou ton image") {
                BcSegmented(listOf("Brume du moment", "Mon image"), selected = 0, onSelect = {})
            }
        }

        assertThat(bounds("Brume du moment").top).isGreaterThan(bounds("La brume du moment, ou ton image").bottom)
    }

    @Test
    fun `dans un panneau, un titre de section a 18 dp au-dessus et 6 dp au-dessous`() {
        show {
            BcPanel("Réglages de Bercail", onBack = {}, backDescription = "Retour") {
                BcSettingRow("Avant", Modifier.testTag("before"))
                BcSectionLabel("Agenda")
                BcSettingRow("Après", Modifier.testTag("after"))
            }
        }

        val label = bounds("Agenda")
        val before = compose.onNodeWithTag("before").getBoundsInRoot()
        val after = compose.onNodeWithTag("after").getBoundsInRoot()
        assertThat((label.top - before.bottom).value).isCloseTo(LABEL_ABOVE.value, delta = 0.5f)
        assertThat((after.top - label.bottom).value).isCloseTo(LABEL_BELOW.value, delta = 0.5f)
    }

    @Test
    fun `un interrupteur se dessine au trait sur 40 × 22 dp, gris éteint, dans l'accent allumé`() {
        show {
            Column {
                BcSwitch(checked = false, onCheckedChange = {}, contentDescription = "Éteint")
                BcSwitch(checked = true, onCheckedChange = {}, contentDescription = "Allumé")
            }
        }

        compose.onNodeWithTag(SWITCH_TRACK_TAG + "Éteint", useUnmergedTree = true)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(22.dp)
        assertThat(edgeDistance("Éteint", BcColors.off)).isLessThan(TOLERANCE)
        assertThat(edgeDistance("Allumé", BcMoment.Day.accent)).isLessThan(TOLERANCE)
        assertThat(middleOfTrack("Éteint").distanceTo(BcColors.base)).isLessThan(TOLERANCE)
    }

    @Test
    fun `le sélecteur est une pilule bordée de blanc à 12 %`() {
        show { BcSegmented(listOf("Fermée", "Ouverte"), selected = 0, onSelect = {}, Modifier.testTag(ROW)) }
        compose.redraw()

        val pixels = compose.onNodeWithTag(ROW).captureToImage().toPixelMap()
        val edge = BcColors.segmentEdge.compositeOver(BcColors.inset.compositeOver(BcColors.base))
        val closest = (0..2).minOf { pixels[pixels.width / 2, it].distanceTo(edge) }
        assertThat(closest).isLessThan(TOLERANCE)
    }

    /** How close the left edge of the switch named [name] comes to [color], across its 1.5 dp stroke. */
    private fun edgeDistance(name: String, color: Color): Float {
        compose.redraw()
        val pixels = compose.onNodeWithTag(SWITCH_TRACK_TAG + name, useUnmergedTree = true).captureToImage()
            .toPixelMap()
        val stroke = with(compose.density) { STROKE.roundToPx() }
        return (0..stroke).minOf { pixels[it, pixels.height / 2].distanceTo(color) }
    }

    private fun middleOfTrack(name: String): Color {
        compose.onNodeWithContentDescription(name).assertExists()
        val pixels = compose.onNodeWithTag(SWITCH_TRACK_TAG + name, useUnmergedTree = true).captureToImage()
            .toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private companion object {
        const val ROW = "row"
        val WIDTH = 360.dp
        val ROW_PADDING = 12.dp
        val LABEL_ABOVE = 18.dp
        val LABEL_BELOW = 6.dp

        /** A button drawn at the right edge leaves at most this much beside it. */
        val BUTTON_SIDE: Dp = 4.dp
        val STROKE = 1.5.dp
        const val TOLERANCE = 0.04f
    }
}
