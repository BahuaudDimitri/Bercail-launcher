package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The drawer grown to the whole screen for the search, and the panel of the settings. */
@RunWith(AndroidJUnit4::class)
class BcPanelUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var searching by mutableStateOf(false)
    private var expanded by mutableStateOf(false)
    private var closed = 0

    private fun showDrawer() = compose.setContent {
        BcTheme {
            Box(Modifier.fillMaxSize()) {
                BcDrawer(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    searching = searching,
                    modifier = Modifier.align(Alignment.BottomCenter).testTag("drawer")
                ) {
                    BcText("Liste", modifier = Modifier.weight(1f, fill = searching))
                    BcSearchField(value = "", onValueChange = {}, modifier = Modifier.testTag("field"))
                }
            }
        }
    }

    private fun gapUnderField(): Float {
        val drawer = compose.onNodeWithTag("drawer").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("field").getUnclippedBoundsInRoot()
        return (drawer.bottom - field.bottom).value
    }

    @Test
    fun `en recherche, le tiroir monte jusqu'en haut de l'écran`() {
        showDrawer()
        val resting = compose.onNodeWithTag("drawer").getUnclippedBoundsInRoot().height

        searching = true
        compose.waitForIdle()

        val screen = compose.onRoot().getUnclippedBoundsInRoot().height
        assertThat(compose.onNodeWithTag("drawer").getUnclippedBoundsInRoot().height).isEqualTo(screen)
        assertThat(screen).isGreaterThan(resting)
    }

    @Test
    fun `en recherche, le tiroir n'a plus de poignée et ignore les glissements`() {
        searching = true
        showDrawer()

        compose.onNodeWithContentDescription("Déplier le tiroir").assertDoesNotExist()
        compose.onNodeWithTag("drawer").performTouchInput {
            swipe(start = Offset(centerX, centerY), end = Offset(centerX, centerY - 80.dp.toPx()), 300)
        }

        assertThat(expanded).isEqualTo(false)
    }

    @Test
    fun `la barre Chercher ne bouge pas quand le tiroir monte`() {
        showDrawer()
        val before = compose.onNodeWithTag("field").getUnclippedBoundsInRoot().bottom

        searching = true
        compose.waitForIdle()

        assertThat(compose.onNodeWithTag("field").getUnclippedBoundsInRoot().bottom).isEqualTo(before)
    }

    @Test
    fun `quand le clavier sort, la barre Chercher passe au-dessus de lui`() {
        searching = true
        showDrawer()
        val before = gapUnderField()

        compose.activityRule.scenario.onActivity { activity ->
            val composeView = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val keyboard = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, KEYBOARD_PX))
                .build()
            ViewCompat.dispatchApplyWindowInsets(composeView, keyboard)
        }
        compose.waitForIdle()

        assertThat(gapUnderField()).isGreaterThan(before)
    }

    @Test
    fun `l'en-tête d'un panneau porte son titre et un retour d'au moins 40 dp`() {
        compose.setContent {
            BcTheme { BcPanelHeader("Toutes les apps", onBack = { closed++ }, backDescription = "Fermer la recherche") }
        }

        compose.onNodeWithText("Toutes les apps").assertExists()
        compose.onNodeWithContentDescription("Fermer la recherche").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `un panneau se ferme par sa flèche ou par le geste retour`() {
        compose.setContent {
            BcTheme {
                BcPanel("Réglages de Bercail", onBack = { closed++ }, backDescription = "Retour à la recherche") {
                    BcSectionLabel("Agenda")
                    BcSettingRow("Ligne du temps", subtitle = "Verticale ou horizontale")
                }
            }
        }
        compose.onNodeWithText("Réglages de Bercail").assertExists()
        compose.onNodeWithText("Agenda").assertExists()
        compose.onNodeWithText("Ligne du temps").assertExists()

        compose.onNodeWithContentDescription("Retour à la recherche").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        assertThat(closed).isEqualTo(2)
    }

    @Test
    fun `une ligne de réglage montre son titre, son explication et sa commande`() {
        var choice by mutableStateOf(0)
        compose.setContent {
            BcTheme {
                BcSettingRow("Tiroir", subtitle = "État de départ") {
                    BcSegmented(listOf("Replié", "Ouvert"), selected = choice, onSelect = { choice = it })
                }
            }
        }

        compose.onNodeWithText("Tiroir").assertExists()
        compose.onNodeWithText("État de départ").assertExists()
        compose.onNodeWithText("Ouvert").performClick()

        assertThat(choice).isEqualTo(1)
    }

    @Test
    fun `une ligne de réglage peut être un lien qu'on touche`() {
        compose.setContent {
            BcTheme { BcSettingRow("Apps favorites", subtitle = "Spotify, Beeper", onClick = { closed++ }) }
        }

        compose.onNodeWithText("Apps favorites").assertHeightIsAtLeast(40.dp).performClick()

        assertThat(closed).isEqualTo(1)
    }

    private companion object {
        const val KEYBOARD_PX = 400
    }
}
