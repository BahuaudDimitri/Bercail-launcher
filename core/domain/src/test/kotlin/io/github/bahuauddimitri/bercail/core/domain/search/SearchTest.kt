package io.github.bahuauddimitri.bercail.core.domain.search

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.FoundMessage
import java.time.LocalDateTime
import org.junit.Test

/** The search of the drawer: what the list shows before typing, and what a few letters find. */
class SearchTest {
    private val apps = listOf("Spotify", "Agenda", "Écouteurs", "Appareil photo", "Téléphone", "YouTube", "1Password")
        .map { App(id = it.lowercase(), name = it) }
    private val people = listOf(Conversation("lea", "Léa", "WhatsApp"), Conversation("tom", "Tom", "Signal"))
    private val house = listOf(
        HomeControl.Light("salon", "Salon", brightness = 60, on = true),
        HomeControl.Shutters("volets", "Volets", open = true)
    )
    private val day = listOf(
        AgendaEvent(
            "1",
            "Déjeuner avec Léa",
            LocalDateTime.of(2026, 10, 2, 12, 30),
            LocalDateTime.of(2026, 10, 2, 13, 30)
        )
    )

    private fun find(query: String) = search(query, SearchScope(apps, people, house, day))

    // ---------- before typing: every app, from A to Z ----------

    @Test
    fun `les apps sont rangées de A à Z, sans tenir compte des accents ni des majuscules`() {
        val directory = appDirectory(apps, favoriteIds = emptyList())

        assertThat(directory.sections.flatMap { it.apps }.map { it.name })
            .containsExactly("1Password", "Agenda", "Appareil photo", "Écouteurs", "Spotify", "Téléphone", "YouTube")
    }

    @Test
    fun `chaque lettre a sa section, une app accentuée va sous sa lettre sans accent`() {
        val directory = appDirectory(apps, favoriteIds = emptyList())

        assertThat(directory.sections.map { it.letter }).containsExactly("#", "A", "E", "S", "T", "Y")
        assertThat(directory.sections.first { it.letter == "E" }.apps.map { it.name }).containsExactly("Écouteurs")
    }

    @Test
    fun `l'alphabet ne propose que les lettres qui ont au moins une app`() {
        val directory = appDirectory(apps, favoriteIds = emptyList())

        assertThat(directory.letters).containsExactly("#", "A", "E", "S", "T", "Y")
    }

    @Test
    fun `les apps favorites viennent en tête, dans l'ordre choisi`() {
        val directory = appDirectory(apps, favoriteIds = listOf("youtube", "spotify", "désinstallée"))

        assertThat(directory.favorites.map { it.name }).containsExactly("YouTube", "Spotify")
    }

    // ---------- while typing ----------

    @Test
    fun `la recherche ignore les accents et les majuscules`() {
        assertThat(find("ecou").apps.map { it.name }).containsExactly("Écouteurs")
        assertThat(find("TÉLÉ").apps.map { it.name }).containsExactly("Téléphone")
        assertThat(find("lea").people.map { it.name }).containsExactly("Léa")
    }

    @Test
    fun `la recherche trouve au milieu d'un nom`() {
        assertThat(find("photo").apps.map { it.name }).containsExactly("Appareil photo")
    }

    @Test
    fun `les apps dont le nom commence par ce qu'on tape passent devant`() {
        assertThat(find("p").apps.map { it.name })
            .containsExactly("1Password", "Appareil photo", "Spotify", "Téléphone")
        assertThat(find("t").apps.map { it.name }.first()).isEqualTo("Téléphone")
    }

    @Test
    fun `une recherche range ses résultats par famille, apps, gens, maison, agenda`() {
        val results = find("l")

        assertThat(results.apps.map { it.name }).containsExactly("Appareil photo", "Téléphone")
        assertThat(results.people.map { it.name }).containsExactly("Léa")
        assertThat(results.controls.map { it.name }).containsExactly("Salon", "Volets")
        assertThat(results.events.map { it.title }).containsExactly("Déjeuner avec Léa")
    }

    @Test
    fun `les espaces autour de ce qu'on tape ne comptent pas`() {
        assertThat(find("  spot ").apps.map { it.name }).containsExactly("Spotify")
    }

    @Test
    fun `taper « réglages » propose les Réglages de Bercail`() {
        assertThat(find("régl").settings).isTrue()
        assertThat(find("reglages de bercail").settings).isTrue()
        assertThat(find("tiroir").settings).isTrue()
        assertThat(find("fond").settings).isTrue()
    }

    @Test
    fun `deux lettres ne suffisent pas à proposer les réglages`() {
        assertThat(find("re").settings).isFalse()
        assertThat(find("spotify").settings).isFalse()
    }

    @Test
    fun `sans rien de trouvé, la recherche le dit, pour proposer le web et le Play Store`() {
        val results = find("zzz")

        assertThat(results.isEmpty).isTrue()
        assertThat(results.apps).isEmpty()
    }

    @Test
    fun `dès qu'une famille a un résultat, la recherche n'est pas vide`() {
        assertThat(find("salon").isEmpty).isFalse()
        assertThat(find("réglages").isEmpty).isFalse()
    }

    // ---------- messages found by the messaging app ----------

    private val terrace = FoundMessage("lea", "Je suis en terrasse", LocalDateTime.of(2026, 10, 2, 12, 58))
    private val match = FoundMessage("tom", "Tu as vu le match en terrasse ?", LocalDateTime.of(2026, 10, 1, 7, 20))

    private fun findWith(found: List<FoundMessage>, query: String = "terrasse") =
        search(query, SearchScope(apps, people, house, day, found))

    @Test
    fun `les messages trouvés sont rangés sous leur personne, le plus récent d'abord`() {
        val lines = findWith(listOf(match, terrace)).messages

        assertThat(lines.map { it.person.name }).containsExactly("Léa", "Tom")
        assertThat(lines.map { it.text }).containsExactly("Je suis en terrasse", "Tu as vu le match en terrasse ?")
    }

    @Test
    fun `un message que j'ai écrit est annoncé « Toi », jamais comme écrit par le contact`() {
        val mine = terrace.copy(text = "En terrasse à 12h30 ?", mine = true)

        assertThat(findWith(listOf(mine)).messages.single().text).isEqualTo("Toi : En terrasse à 12h30 ?")
    }

    @Test
    fun `un message d'une conversation que l'accueil ne connaît pas n'est pas proposé`() {
        val elsewhere = FoundMessage("archive", "Terrasse refaite", LocalDateTime.of(2024, 5, 1, 9, 0))

        assertThat(findWith(listOf(elsewhere)).messages).isEmpty()
    }

    @Test
    fun `cinq messages au plus sont proposés`() {
        val many = (1..8).map { terrace.copy(at = terrace.at.plusMinutes(it.toLong()), text = "Terrasse $it") }

        val lines = findWith(many).messages

        assertThat(lines.map { it.text })
            .containsExactly("Terrasse 8", "Terrasse 7", "Terrasse 6", "Terrasse 5", "Terrasse 4")
    }

    @Test
    fun `un message trouvé suffit pour que la recherche ne soit pas vide`() {
        assertThat(findWith(listOf(terrace), query = "terrasse").isEmpty).isFalse()
        assertThat(findWith(emptyList(), query = "terrasse").isEmpty).isTrue()
    }

    @Test
    fun `les messages ne sont cherchés qu'à partir de trois lettres`() {
        assertThat(searchesMessages("te")).isFalse()
        assertThat(searchesMessages("  te  ")).isFalse()
        assertThat(searchesMessages("ter")).isTrue()
        assertThat(searchesMessages("Été")).isTrue()
    }
}
