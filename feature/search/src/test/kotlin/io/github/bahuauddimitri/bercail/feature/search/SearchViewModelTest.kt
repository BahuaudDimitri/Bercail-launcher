package io.github.bahuauddimitri.bercail.feature.search

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** What the search shows and does, on a fake phone with the prototype's 27 apps. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val world = FakeWorld(Moment.Matin)
    private val search by lazy { world.searchViewModel() }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** The search is on screen: someone listens to its state. */
    private fun TestScope.show() = backgroundScope.launch(dispatcher) { search.state.collect {} }

    private val state get() = search.state.value

    // ---------- opening and closing ----------

    @Test
    fun `fermée, la recherche n'écoute ni les apps ni rien d'autre`() = runTest(dispatcher) {
        show()

        assertThat(state.open).isFalse()
        assertThat(world.apps.apps.subscriptionCount.value).isEqualTo(0)
        assertThat(world.messages.conversations.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `ouverte, la recherche liste toutes les apps de A à Z avec les lettres utilisées`() = runTest(dispatcher) {
        show()

        search.onOpenChange(true)

        assertThat(state.directory.sections.first().apps.map { it.name })
            .containsExactly("Agenda", "Appareil photo")
        assertThat(state.directory.letters.first()).isEqualTo("A")
        assertThat(state.directory.letters.last()).isEqualTo("Y")
        assertThat(state.results).isNull()
    }

    @Test
    fun `les apps favorites des réglages sont en tête`() = runTest(dispatcher) {
        show()

        search.onOpenChange(true)

        assertThat(state.directory.favorites.map { it.name }).containsExactly("Spotify", "YouTube", "Beeper", "Agenda")
    }

    @Test
    fun `une app installée pendant que la recherche est ouverte apparaît`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)

        world.apps.install(App("zoom", "Zoom"))

        assertThat(state.directory.letters.last()).isEqualTo("Z")
    }

    @Test
    fun `fermer la recherche vide le champ et n'écoute plus rien`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange("spo")

        search.onOpenChange(false)

        assertThat(state.open).isFalse()
        assertThat(state.query).isEqualTo("")
        assertThat(world.apps.apps.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `rouverte, la recherche montre tout de suite la liste qu'elle connaissait`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onOpenChange(false)

        search.onOpenChange(true)

        assertThat(state.directory.sections.size).isEqualTo(16)
    }

    @Test
    fun `fermée, ce qu'on tape est ignoré`() = runTest(dispatcher) {
        show()

        search.onQueryChange("spo")

        assertThat(state.query).isEqualTo("")
    }

    // ---------- typing ----------

    @Test
    fun `taper filtre les apps sans tenir compte des accents`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)

        search.onQueryChange("tele")

        assertThat(state.results).isNotNull()
        assertThat(state.results!!.apps.map { it.name }).containsExactly("Téléphone")
    }

    @Test
    fun `la recherche trouve aussi les gens, la maison et l'agenda`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)

        search.onQueryChange("lé")

        assertThat(state.results!!.people.map { it.name }).containsExactly("Léa")
        assertThat(state.results!!.events.map { it.title }).containsExactly("Déjeuner avec Léa")

        search.onQueryChange("vol")
        assertThat(state.results!!.controls.map { it.label }).containsExactly("Volets ouverts")
    }

    @Test
    fun `effacer ce qu'on a tapé revient à la liste de toutes les apps`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange("spo")

        search.onQueryChange("")

        assertThat(state.results).isNull()
    }

    @Test
    fun `sans rien de trouvé, le résultat est vide`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)

        search.onQueryChange("zzz")

        assertThat(state.results!!.isEmpty).isTrue()
    }

    // ---------- acting on a result ----------

    @Test
    fun `ouvrir une app la lance, ferme la recherche et vide le champ`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange("spo")

        search.onOpenApp("spotify")

        assertThat(world.apps.opened).containsExactly("spotify")
        assertThat(state.open).isFalse()
        assertThat(state.query).isEqualTo("")
    }

    @Test
    fun `toucher une commande maison dans les résultats la bascule sur place`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange("vol")

        search.onToggleControl("volets")

        assertThat(state.open).isTrue()
        assertThat(state.results!!.controls.map { it.label }).containsExactly("Volets fermés")
        assertThat(world.home.controls.first().first { it.id == "volets" }.isOn).isFalse()
    }

    @Test
    fun `chercher sur le web envoie ce qu'on a tapé et ferme la recherche`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange(" ramen maison ")

        search.onSearchWeb()

        assertThat(world.phone.visits).containsExactly("web: ramen maison")
        assertThat(state.open).isFalse()
    }

    @Test
    fun `chercher sur le Play Store envoie ce qu'on a tapé et ferme la recherche`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)
        search.onQueryChange("ramen")

        search.onSearchStore()

        assertThat(world.phone.visits).containsExactly("store: ramen")
        assertThat(state.open).isFalse()
    }

    @Test
    fun `sans rien de tapé, on n'envoie rien chercher`() = runTest(dispatcher) {
        show()
        search.onOpenChange(true)

        search.onSearchWeb()
        search.onSearchStore()

        assertThat(world.phone.visits).isEmpty()
    }
}
