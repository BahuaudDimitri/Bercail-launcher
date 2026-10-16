package io.github.bahuauddimitri.bercail.feature.search

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.search.MessageLine
import io.github.bahuauddimitri.bercail.core.domain.search.SearchResults
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
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
        override fun onOpenDay(eventId: String) = opened.add("journée, $eventId").let {}
    }

    private fun show(typed: String = "") {
        search.onOpenChange(true)
        search.onQueryChange(typed)
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.base)) { SearchRoute(search, links = links) } }
        }
    }

    /** Shows these results as they are, whatever was typed. */
    private fun showResults(results: SearchResults) {
        val state = SearchUiState(open = true, query = "x", results = results)
        compose.setContent {
            BcTheme {
                Box(Modifier.fillMaxSize().background(BcColors.base)) {
                    SearchList(state, search, search.icons, links = links)
                }
            }
        }
    }

    /** How far from the top of the screen this text starts. */
    private fun topOf(text: String) = compose.onNodeWithText(text).getBoundsInRoot().top

    /** The texts of the lowest line of the results: the closest to the search bar, which sits under the list. */
    private fun lowestResult(): List<String> = compose
        .onAllNodes(hasAnyAncestor(hasTestTag(RESULTS_TAG)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        .fetchSemanticsNodes()
        .maxBy { it.boundsInRoot.top }
        .config[SemanticsProperties.Text]
        .map { it.text }

    /** The letters of this title drawn apart from the others, as found: nothing when none is. */
    private fun foundLetters(title: String): String {
        val text = compose.onAllNodes(hasText(title), useUnmergedTree = true)
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .first()
        return text.spanStyles.joinToString("") { text.text.substring(it.start, it.end) }
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
    fun `le dock vient ensuite, sous son titre, puis les apps de A à Z`() {
        show()

        compose.onNodeWithText("Dock").assertIsDisplayed()
        compose.onNodeWithText("Banque").assertExists()
    }

    @Test
    fun `les apps sont dessinées d'une seule couleur, seulement celles que la liste montre`() {
        show()
        compose.waitForIdle()

        assertThat(world.apps.glyphRequests).contains("chrome")
        assertThat(world.apps.glyphRequests).doesNotContain("youtube")
        assertThat(world.apps.iconRequests).isEmpty()
    }

    @Test
    fun `toucher une app la lance et ferme la recherche`() {
        show()

        compose.onNodeWithText("Banque").performClick()

        assertThat(world.apps.opened).containsExactly("banque")
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
    fun `en partant de la barre, les familles se suivent ainsi, Apps, Gens, Messages, Agenda, Maison, puis Réglages`() {
        val tom = Samples.people.first { it.id == "tom" }
        showResults(
            SearchResults(
                apps = Samples.apps.filter { it.id == "spotify" },
                people = listOf(tom),
                messages = listOf(MessageLine(tom, "Tu as vu le match hier ?")),
                controls = Samples.home.filter { it.id == "volets" },
                events = Samples.eventsOn(Samples.day).filter { it.title == "Courses" },
                settings = true
            )
        )

        val fromTheBarUp = listOf("Apps", "Gens", "Messages", "Agenda", "Maison", "Réglages").map { topOf(it) }

        assertThat(fromTheBarUp).isEqualTo(fromTheBarUp.sortedDescending())
        assertThat(lowestResult()).contains("Spotify")
    }

    @Test
    fun `dans chaque famille, le titre puis le premier trouvé, de haut en bas`() {
        showResults(SearchResults(people = Samples.people.filter { it.id == "lea" || it.id == "tom" }))

        assertThat(topOf("Gens")).isLessThan(topOf("Léa"))
        assertThat(topOf("Léa")).isLessThan(topOf("Tom"))
        assertThat(lowestResult()).contains("Tom")
    }

    @Test
    fun `la touche Entrée ouvre la première app trouvée, en haut du groupe Apps sous son titre`() {
        show(typed = "s")

        assertThat(topOf("Apps")).isLessThan(topOf("Spotify"))
        assertThat(topOf("Spotify")).isLessThan(topOf("Strava"))

        search.onSubmit()
        assertThat(world.apps.opened).containsExactly("spotify")
    }

    @Test
    fun `en tapant, les lettres trouvées sont mises en avant dans le nom des apps, des gens et des rendez-vous`() {
        show(typed = "LÉ")

        assertThat(foundLetters("Léa")).isEqualTo("Lé")
        assertThat(foundLetters("Téléphone")).isEqualTo("lé")
        assertThat(foundLetters("Déjeuner avec Léa")).isEqualTo("Lé")
    }

    @Test
    fun `avant de taper, aucune lettre n'est mise en avant`() {
        show()

        assertThat(foundLetters("Chrome")).isEqualTo("")
    }

    @Test
    fun `la première app trouvée, celle qu'ouvre Entrée, l'annonce sur sa ligne, et elle seule`() {
        show(typed = "s")

        compose.onNode(hasText("Spotify") and hasText("Entrée")).assertExists()
        compose.onAllNodesWithText("Entrée").assertCountEquals(1)
    }

    @Test
    fun `sans app trouvée, ni avant de taper, rien n'annonce Entrée`() {
        show()
        compose.onAllNodesWithText("Entrée").assertCountEquals(0)

        search.onQueryChange("léa")
        compose.waitForIdle()
        compose.onAllNodesWithText("Entrée").assertCountEquals(0)
    }

    @Test
    fun `toucher une personne demande sa conversation`() {
        show(typed = "léa")

        compose.onNodeWithText("Léa").performClick()

        assertThat(opened).containsExactly("conversation lea")
    }

    @Test
    fun `un message trouvé s'affiche sous « Messages », avec sa personne, et ouvre sa conversation`() {
        show(typed = "match")
        // The messaging app is asked once the typing has paused.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
        compose.waitForIdle()

        compose.onNodeWithText("Messages").assertExists()
        compose.onNodeWithText("Tom").assertExists()
        compose.onNodeWithText("Tu as vu le match hier ?").performClick()

        assertThat(opened).containsExactly("conversation tom")
    }

    @Test
    fun `toucher une commande maison la bascule sur place, la recherche reste ouverte`() {
        show(typed = "volets")

        compose.onNodeWithText("Volets").performClick()

        compose.onNodeWithText("fermés").assertExists()
        compose.onNodeWithText("Résultats").assertExists()
    }

    @Test
    fun `toucher un rendez-vous demande la journée, ce rendez-vous mis en avant`() {
        show(typed = "déjeuner")

        compose.onNodeWithText("Déjeuner avec Léa").performClick()

        assertThat(opened).containsExactly("journée, Déjeuner avec Léa")
    }

    @Test
    fun `quand une version attend un accord, la ligne des Réglages de Bercail l'annonce`() {
        world.updates.status.value = UpdateStatus.Ready(Version(0, 9, 0), needsUser = true)
        show()

        compose.onNodeWithText("Version 0.9.0 prête à installer").assertIsDisplayed()
    }

    @Test
    fun `quand aucune version n'attend, la ligne des Réglages de Bercail n'annonce rien`() {
        show()

        compose.onNodeWithText("Version 0.9.0 prête à installer").assertDoesNotExist()
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
