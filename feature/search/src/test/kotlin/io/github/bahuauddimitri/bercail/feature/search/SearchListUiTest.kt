package io.github.bahuauddimitri.bercail.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The search list on a simulated phone: every app before typing, results by family while typing. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class SearchListUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld(Moment.Matin)
    private val search = world.searchViewModel()
    private val opened = mutableListOf<String>()
    private val links = object : SearchLinks {
        override fun onOpenSettings() = opened.add("réglages").let {}
        override fun onOpenConversation(id: String) = opened.add("conversation $id").let {}
        override fun onOpenDay() = opened.add("journée").let {}
    }

    private fun show(typed: String = "") {
        search.onOpenChange(true)
        search.onQueryChange(typed)
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.base)) { SearchRoute(search, links = links) } }
        }
    }

    private fun railLetter(letter: String) = compose.onNode(hasText(letter) and hasAnyAncestor(hasTestTag(RAIL_TAG)))

    // ---------- before typing ----------

    @Test
    fun `avant de taper, la liste s'appelle « Toutes les apps » et commence par les Réglages de Bercail`() {
        show()

        compose.onNodeWithText("Toutes les apps").assertExists()
        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed().performClick()

        assertThat(opened).containsExactly("réglages")
    }

    @Test
    fun `les apps favorites viennent ensuite, puis les apps de A à Z`() {
        show()

        compose.onNodeWithText("Apps favorites").assertIsDisplayed()
        compose.onNodeWithText("Appareil photo").assertExists()
    }

    @Test
    fun `toucher une app la lance et ferme la recherche`() {
        show()

        compose.onNodeWithText("Appareil photo").performClick()

        assertThat(world.apps.opened).containsExactly("appareil photo")
        assertThat(search.state.value.open).isFalse()
    }

    @Test
    fun `toucher une lettre de l'alphabet amène la liste à cette lettre`() {
        show()
        compose.onNodeWithText("Waze").assertDoesNotExist()

        railLetter("W").performClick()

        compose.onNodeWithText("Waze").assertIsDisplayed()
    }

    @Test
    fun `l'alphabet ne montre que les lettres qui ont une app`() {
        show()

        railLetter("S").assertExists()
        railLetter("E").assertDoesNotExist()
        railLetter("Z").assertDoesNotExist()
    }

    @Test
    fun `la flèche ferme la recherche`() {
        show()

        compose.onNodeWithContentDescription("Fermer la recherche").performClick()

        assertThat(search.state.value.open).isFalse()
    }

    // ---------- while typing ----------

    @Test
    fun `en tapant, la liste s'appelle « Résultats » et l'alphabet s'efface`() {
        show(typed = "spo")

        compose.onNodeWithText("Résultats").assertExists()
        compose.onNodeWithText("Spotify").assertIsDisplayed()
        railLetter("S").assertDoesNotExist()
    }

    @Test
    fun `les résultats sont rangés par famille`() {
        show(typed = "l")

        listOf("Apps", "Gens", "Maison", "Agenda").forEach {
            compose.onNodeWithTag(RESULTS_TAG).performScrollToNode(hasText(it))
            compose.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun `toucher une personne demande sa conversation`() {
        show(typed = "léa")

        compose.onNodeWithText("Léa").performClick()

        assertThat(opened).containsExactly("conversation lea")
    }

    @Test
    fun `toucher une commande maison la bascule sur place, la recherche reste ouverte`() {
        show(typed = "volets")

        compose.onNodeWithText("Volets").performClick()

        compose.onNodeWithText("fermés").assertExists()
        compose.onNodeWithText("Résultats").assertExists()
    }

    @Test
    fun `toucher un rendez-vous demande la journée`() {
        show(typed = "déjeuner")

        compose.onNodeWithText("Déjeuner avec Léa").performClick()

        assertThat(opened).containsExactly("journée")
    }

    @Test
    fun `taper « réglages » propose les Réglages de Bercail`() {
        show(typed = "réglages")

        compose.onNodeWithText("Réglages de Bercail").performClick()

        assertThat(opened).containsExactly("réglages")
    }

    @Test
    fun `sans rien sur le téléphone, la recherche propose le web et le Play Store`() {
        show(typed = "ramen")

        compose.onNodeWithText("Rien sur le téléphone pour « ramen »").assertExists()
        compose.onNodeWithText("Chercher « ramen » sur le web").performClick()

        assertThat(world.phone.visits).containsExactly("web: ramen")
    }

    @Test
    fun `la proposition Play Store envoie ce qu'on a tapé au Play Store`() {
        show(typed = "ramen")

        compose.onNodeWithText("Chercher « ramen » sur le Play Store").performClick()

        assertThat(world.phone.visits).containsExactly("store: ramen")
    }
}
