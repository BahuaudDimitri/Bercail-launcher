package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcRingUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `un anneau de personne montre son initiale et s'annonce comme un bouton`() {
        var opened = 0
        compose.setContent {
            BcTheme {
                BcRing(
                    label = "Léa",
                    mark = BcRingMark.Initial("L"),
                    onClick = { opened++ },
                    tone = BcRingTone.Pastel(BcPastel.Coral),
                    description = "Léa, 2 non lus"
                )
            }
        }

        compose.onNodeWithText("L", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Léa, 2 non lus")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `un anneau fait 52 dp et se touche sur au moins 40 dp`() {
        compose.setContent {
            BcTheme { BcRing("Tom", BcRingMark.Initial("T"), onClick = {}, tone = BcRingTone.Pastel(BcPastel.Blue)) }
        }

        assertThat(BcSizes.ring).isEqualTo(52.dp)
        compose.onNodeWithTag(RING_TAG + "Tom", useUnmergedTree = true)
            .assertWidthIsAtLeast(BcSizes.ring)
            .assertHeightIsAtLeast(BcSizes.ring)
        compose.onNodeWithContentDescription("Tom").assertHeightIsAtLeast(BcSizes.minTouch)
    }

    @Test
    fun `sans non-lus l'anneau n'a pas de pastille`() {
        compose.setContent {
            BcTheme {
                Row {
                    BcRing("Tom", BcRingMark.Initial("T"), onClick = {}, tone = BcRingTone.Pastel(BcPastel.Blue))
                    BcRing("Léa", BcRingMark.Initial("L"), onClick = {}, unread = 3)
                }
            }
        }

        compose.onNodeWithTag(UNREAD_BADGE + "Tom", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(UNREAD_BADGE + "Léa", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `au-delà de 99 non-lus la pastille affiche 99+`() {
        compose.setContent {
            BcTheme { BcRing("Tous", BcRingMark.Icon(BcIcons.Chat), onClick = {}, unread = 140) }
        }

        compose.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `toucher une commande la bascule et Android l'annonce comme un interrupteur`() {
        var on by mutableStateOf(false)
        compose.setContent {
            BcTheme {
                BcRing(
                    name = "Salon",
                    state = if (on) "60 %" else "éteint",
                    mark = BcRingMark.Icon(BcIcons.Lamp),
                    pastel = BcPastel.Apricot,
                    on = on,
                    onToggle = { on = !on }
                )
            }
        }

        compose.onNodeWithContentDescription("Salon")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "éteint"))
            .assertIsOff()
            .performClick()

        assertThat(on).isEqualTo(true)
        compose.onNodeWithContentDescription("Salon").assertIsOn()
    }

    @Test
    fun `une commande allumée écrit son nom et son état sur une ligne, éteinte son nom seul`() {
        compose.setContent {
            BcTheme {
                Row {
                    BcRing("Salon", "60 %", BcRingMark.Icon(BcIcons.Lamp), BcPastel.Apricot, on = true, onToggle = {})
                    BcRing("Cinéma", "prête", BcRingMark.Icon(BcIcons.Film), BcPastel.Lavender, on = false, onToggle = {
                    })
                }
            }
        }

        compose.onNodeWithText("Salon · 60 %", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Cinéma", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("prête", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun showRow(width: Int) {
        compose.setContent {
            BcTheme {
                Box(Modifier.requiredWidth(width.dp).testTag(ROW)) {
                    BcRingRow {
                        NAMES.forEach { BcRing(it, BcRingMark.Initial(it.take(1)), onClick = {}) }
                    }
                }
            }
        }
    }

    private fun boundsOf(name: String) = compose.onNodeWithContentDescription(name).getUnclippedBoundsInRoot()

    @Test
    fun `sur un écran étroit, cinq anneaux se partagent la largeur et gardent chacun leur rond entier`() {
        showRow(width = NARROW)

        val boxes = NAMES.map(::boundsOf)
        boxes.forEach { assertThat(it.width).isGreaterThanOrEqualTo(BcSizes.ring) }
        boxes.zipWithNext().forEach { (left, right) -> assertThat(left.right).isLessThanOrEqualTo(right.left) }
        assertThat(boxes.last().right - boxes.first().left).isLessThanOrEqualTo(NARROW.dp)
    }

    @Test
    fun `sur un écran large, les anneaux gardent 64 dp, le premier au bord gauche et le dernier au bord droit`() {
        showRow(width = WIDE)

        NAMES.forEach { assertThat(boundsOf(it).width).isEqualTo(COLUMN) }
        val row = compose.onNodeWithTag(ROW).getUnclippedBoundsInRoot()
        assertThat(boundsOf(NAMES.first()).left).isEqualTo(row.left)
        assertThat(boundsOf(NAMES.last()).right).isEqualTo(row.right)
    }

    private companion object {
        val NAMES = listOf("Léa", "Tom", "Équipe", "Maman", "Tous")
        const val NARROW = 288
        const val WIDE = 379
        val COLUMN = 64.dp
        const val ROW = "row"
    }
}
