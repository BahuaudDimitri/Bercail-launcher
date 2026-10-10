package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The search bar is the field itself: touching it makes the drawer rise over the whole screen. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeSearchUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var open by mutableStateOf(false)
    private var query by mutableStateOf("")
    private var submitted = 0

    private fun show() {
        val home = FakeWorld(Moment.Matin).homeViewModel()
        compose.setContent {
            BcTheme {
                HomeRoute(
                    viewModel = home,
                    search = HomeSearch(
                        open = open,
                        query = query,
                        onQueryChange = { if (open) query = it },
                        onOpenChange = {
                            open = it
                            if (!it) query = ""
                        },
                        onSubmit = { submitted++ }
                    ) { BcText("Toutes les apps") }
                )
            }
        }
    }

    private fun drawerHeight() = compose.onNodeWithTag(DRAWER_TAG).getUnclippedBoundsInRoot().height

    private fun screenHeight() = compose.onRoot().getUnclippedBoundsInRoot().height

    private fun searchBarBottom() = compose.onNodeWithContentDescription("Chercher").getUnclippedBoundsInRoot().bottom

    @Test
    fun `toucher la barre Chercher ouvre la recherche`() {
        show()

        compose.onNodeWithContentDescription("Chercher").performClick()
        compose.waitForIdle()

        assertThat(open).isTrue()
        compose.onNodeWithContentDescription("Chercher").assertIsFocused()
    }

    @Test
    fun `en recherche, le tiroir monte jusqu'en haut, ses lignes se replient et la liste apparaît`() {
        show()

        compose.onNodeWithContentDescription("Chercher").performClick()

        assertThat(drawerHeight()).isEqualTo(screenHeight())
        compose.onNodeWithText("Toutes les apps").assertExists()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
        compose.onNodeWithContentDescription("Déplier le tiroir").assertDoesNotExist()
    }

    @Test
    fun `la barre Chercher reste à sa place quand la recherche s'ouvre`() {
        show()
        val before = searchBarBottom()

        compose.onNodeWithContentDescription("Chercher").performClick()

        assertThat(searchBarBottom()).isEqualTo(before)
    }

    @Test
    fun `une fois la recherche ouverte, ce qu'on tape lui arrive`() {
        show()

        compose.onNodeWithContentDescription("Chercher").performClick()
        compose.onNodeWithContentDescription("Chercher").performTextInput("spo")

        assertThat(query).isEqualTo("spo")
    }

    @Test
    fun `la touche rechercher du clavier est transmise à la recherche`() {
        show()
        compose.onNodeWithContentDescription("Chercher").performClick()
        compose.onNodeWithContentDescription("Chercher").performTextInput("spo")

        compose.onNodeWithContentDescription("Chercher").performImeAction()

        assertThat(submitted).isEqualTo(1)
    }

    @Test
    fun `le retour système ferme la recherche, vide le champ et rend au tiroir sa hauteur`() {
        show()
        val folded = drawerHeight()
        compose.onNodeWithContentDescription("Chercher").performClick()
        compose.onNodeWithContentDescription("Chercher").performTextInput("spo")

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertThat(open).isFalse()
        assertThat(query).isEqualTo("")
        assertThat(drawerHeight()).isEqualTo(folded)
        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
        compose.onNodeWithContentDescription("Chercher").assertIsNotFocused()
    }

    @Test
    fun `fermer la recherche rend le tiroir ouvert comme il était`() {
        show()
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        val expanded = drawerHeight()
        compose.onNodeWithContentDescription("Chercher").performClick()

        open = false
        compose.waitForIdle()

        assertThat(drawerHeight()).isEqualTo(expanded)
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
    }

    @Test
    fun `pendant la recherche, le haut de l'accueil s'efface sous le tiroir`() {
        show()

        compose.onNodeWithContentDescription("Chercher").performClick()

        compose.onNodeWithTag(TIMELINE_TAG).assertDoesNotExist()
        compose.onNodeWithText("Stand-up").assertDoesNotExist()
        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
    }

    @Test
    fun `la recherche fermée, le haut de l'accueil revient exactement à sa place`() {
        show()
        val before = compose.onNodeWithTag(TIMELINE_TAG).getUnclippedBoundsInRoot()
        compose.onNodeWithContentDescription("Chercher").performClick()

        open = false
        compose.waitForIdle()

        assertThat(compose.onNodeWithTag(TIMELINE_TAG).getUnclippedBoundsInRoot()).isEqualTo(before)
        compose.onNodeWithText("Stand-up").assertExists()
    }

    @Test
    fun `la recherche s'ouvre aussi depuis l'écran Écoute`() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithContentDescription("Chercher").performClick()

        assertThat(drawerHeight()).isEqualTo(screenHeight())
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }
}
