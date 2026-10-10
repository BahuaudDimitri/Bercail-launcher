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
}
