package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The flat band at the bottom of the home screen, and the sheet that rises over it. */
@RunWith(AndroidJUnit4::class)
class BcBandUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var open by mutableStateOf(false)
    private var opens by mutableStateOf(true)
    private var buttonClicks = 0

    private fun showBand() = compose.setContent {
        BcTheme {
            Box(Modifier.fillMaxSize()) {
                BcBand(
                    onOpen = if (opens) ({ open = true }) else null,
                    modifier = Modifier.align(Alignment.BottomCenter).testTag("band")
                ) {
                    BcButton("Salon", onClick = { buttonClicks++ })
                }
            }
        }
    }

    private fun slideUpOnBand(distance: Float) = compose.onNodeWithTag("band").performTouchInput {
        swipe(start = Offset(centerX, bottom - 4f), end = Offset(centerX, bottom - 4f - distance), durationMillis = 300)
    }

    @Test
    fun `le bandeau n'a pas de poignée`() {
        showBand()

        compose.onNodeWithContentDescription("Ouvrir la feuille").assertDoesNotExist()
        compose.onNodeWithContentDescription("Fermer la feuille").assertDoesNotExist()
    }

    @Test
    fun `glisser de plus de 24 dp vers le haut sur le bandeau ouvre la feuille`() {
        showBand()

        compose.onNodeWithTag("band").performTouchInput {
            swipe(start = Offset(centerX, bottom - 4f), end = Offset(centerX, bottom - 4f - 40.dp.toPx()), 300)
        }

        assertThat(open).isEqualTo(true)
    }

    @Test
    fun `glisser de moins de 24 dp ne change rien`() {
        showBand()

        compose.onNodeWithTag("band").performTouchInput {
            swipe(start = Offset(centerX, bottom - 4f), end = Offset(centerX, bottom - 4f - 16.dp.toPx()), 300)
        }

        assertThat(open).isEqualTo(false)
    }

    @Test
    fun `le glissement compte même si le doigt sort du bandeau`() {
        showBand()

        compose.onNodeWithTag("band").performTouchInput {
            swipe(start = Offset(centerX, bottom - 4f), end = Offset(centerX, top - 200.dp.toPx()), 300)
        }

        assertThat(open).isEqualTo(true)
    }

    @Test
    fun `un glissement qui commence sur un bouton du bandeau n'active pas le bouton`() {
        showBand()

        compose.onNodeWithText("Salon").performTouchInput {
            swipe(start = center, end = Offset(centerX, centerY - 60.dp.toPx()), durationMillis = 300)
        }

        assertThat(buttonClicks).isEqualTo(0)
        assertThat(open).isEqualTo(true)
    }

    @Test
    fun `un bouton du bandeau réagit toujours au toucher`() {
        showBand()

        compose.onNodeWithText("Salon").performClick()

        assertThat(buttonClicks).isEqualTo(1)
    }

    @Test
    fun `un lecteur d'écran ouvre la feuille depuis le bandeau`() {
        showBand()

        compose.onNodeWithTag("band").performSemanticsAction(SemanticsActions.Expand)

        assertThat(open).isEqualTo(true)
    }

    @Test
    fun `un bandeau qui n'ouvre rien ignore les glissements`() {
        opens = false
        showBand()

        slideUpOnBand(80f)

        assertThat(open).isEqualTo(false)
    }

    // ---------- the sheet over the band ----------

    private var sheetOpen by mutableStateOf(true)

    private fun showBandSheet(contentHeight: Int = 200) = compose.setContent {
        BcTheme {
            Box(Modifier.fillMaxSize()) {
                BcBandSheet(
                    visible = sheetOpen,
                    onClose = { sheetOpen = false },
                    modifier = Modifier.align(Alignment.BottomCenter).testTag("sheet")
                ) {
                    Column { Box(Modifier.height(contentHeight.dp)) }
                }
            }
        }
    }

    @Test
    fun `la feuille posée sur le bandeau se ferme par sa poignée`() {
        showBandSheet()

        compose.onNodeWithContentDescription("Fermer la feuille").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(sheetOpen).isEqualTo(false)
    }

    @Test
    fun `glisser la feuille posée sur le bandeau vers le bas la ferme`() {
        showBandSheet()

        compose.onNodeWithTag("sheet").performTouchInput {
            swipe(start = Offset(centerX, top + 4f), end = Offset(centerX, top + 4f + 60.dp.toPx()), 300)
        }

        assertThat(sheetOpen).isEqualTo(false)
    }

    @Test
    fun `le geste retour d'Android ferme la feuille posée sur le bandeau`() {
        showBandSheet()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertThat(sheetOpen).isEqualTo(false)
    }

    @Test
    fun `la feuille posée sur le bandeau ne dépasse pas 62 % de l'écran`() {
        showBandSheet(contentHeight = 2_000)

        val screen = compose.onRoot().getUnclippedBoundsInRoot().height
        val sheet = compose.onNodeWithTag("sheet").getUnclippedBoundsInRoot().height

        assertThat(sheet.value).isCloseTo(screen.value * 0.62f, delta = 1f)
    }
}
