package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcControlsUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `toucher une personne ouvre sa conversation`() {
        var opened = 0
        compose.setContent {
            BcTheme {
                BcPersonTile("Léa", "L", BcPastel.Coral, unread = 2, onClick = {
                    opened++
                }, description = "Léa, 2 non lus")
            }
        }

        compose.onNodeWithContentDescription("Léa, 2 non lus").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `la pastille de non-lus n'apparaît que s'il y en a`() {
        compose.setContent {
            BcTheme {
                BcPersonTile("Tom", "T", BcPastel.Blue, unread = 0, onClick = {})
                BcPersonTile("Léa", "L", BcPastel.Coral, unread = 3, onClick = {})
            }
        }

        compose.onNodeWithTag(UNREAD_BADGE + "Tom", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(UNREAD_BADGE + "Léa", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `au-delà de 99 non-lus la pastille affiche 99+`() {
        compose.setContent { BcTheme { BcPersonTile("Tous", "", pastel = null, unread = 140, onClick = {}) } }

        compose.onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `toucher une commande maison la bascule`() {
        var on by mutableStateOf(false)
        compose.setContent {
            BcTheme {
                BcHomeToggle("Salon", "éteint", BcIcons.Lamp, BcPastel.Apricot, on = on, onToggle = { on = !on })
            }
        }

        compose.onNodeWithContentDescription("Salon").assertIsOff().performClick()

        assertThat(on).isEqualTo(true)
        compose.onNodeWithContentDescription("Salon").assertIsOn()
    }

    @Test
    fun `toucher une ligne de liste l'ouvre`() {
        var opened = 0
        compose.setContent {
            BcTheme { BcListRow("Spotify", onClick = { opened++ }, leading = BcLeading.Initials("S")) }
        }

        compose.onNodeWithText("Spotify").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `toucher la barre de progression à un quart amène la lecture à un quart`() {
        var seek: Float? = null
        compose.setContent {
            BcTheme { BcProgress(0.5f, onSeek = { seek = it }, modifier = Modifier.width(200.dp).testTag("progress")) }
        }

        compose.onNodeWithTag("progress").assertHeightIsAtLeast(40.dp).performTouchInput {
            click(Offset(width * 0.25f, centerY))
        }

        assertThat(seek).isNotNull().isCloseTo(0.25f, 0.02f)
    }

    @Test
    fun `glisser au-delà du bout de la barre s'arrête à la fin`() {
        var seek: Float? = null
        compose.setContent {
            BcTheme { BcProgress(0.1f, onSeek = { seek = it }, modifier = Modifier.width(200.dp).testTag("progress")) }
        }

        compose.onNodeWithTag("progress").performTouchInput {
            swipe(start = Offset(width * 0.1f, centerY), end = Offset(width * 1.5f, centerY))
        }

        assertThat(seek).isNotNull().isCloseTo(1f, 0.001f)
    }

    @Test
    fun `choisir une option du sélecteur la sélectionne`() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            BcTheme {
                BcSegmented(listOf("Verticale", "Horizontale"), selected = selected, onSelect = { selected = it })
            }
        }

        compose.onNodeWithText("Verticale").assertIsSelected()
        compose.onNodeWithText("Horizontale").assertIsNotSelected().assertHeightIsAtLeast(40.dp).performClick()

        assertThat(selected).isEqualTo(1)
        compose.onNodeWithText("Horizontale").assertIsSelected()
    }

    @Test
    fun `toucher un interrupteur l'inverse`() {
        var checked by mutableStateOf(true)
        compose.setContent {
            BcTheme { BcSwitch(checked, onCheckedChange = { checked = it }, contentDescription = "Écoute automatique") }
        }

        compose.onNodeWithContentDescription("Écoute automatique")
            .assertIsOn()
            .assertHeightIsAtLeast(40.dp)
            .performClick()

        assertThat(checked).isEqualTo(false)
        compose.onNodeWithContentDescription("Écoute automatique").assertIsOff()
    }
}
