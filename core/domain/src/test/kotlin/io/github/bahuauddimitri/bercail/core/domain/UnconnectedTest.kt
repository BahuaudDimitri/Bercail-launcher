package io.github.bahuauddimitri.bercail.core.domain

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.agenda.glanceAt
import io.github.bahuauddimitri.bercail.core.domain.home.homeSummary
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.messages.messagesSummary
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** A source that is not plugged in yet shows an honest empty state, never made-up data. */
class UnconnectedTest {
    private val today = LocalDate.of(2026, 10, 10)

    @Test
    fun `sans agenda branché, l'accueil dit qu'il n'y a plus rien aujourd'hui`() = runBlocking {
        val events = Unconnected.agenda.eventsOn(today).first()

        assertThat(events).isEmpty()
        assertThat(glanceAt(today.atTime(9, 0), events).countdown).isEqualTo("Plus rien aujourd'hui")
    }

    @Test
    fun `sans météo branchée, il n'y a pas de ligne météo`() = runBlocking {
        assertThat(Unconnected.weather.weather.first()).isNull()
    }

    @Test
    fun `sans messages branchés, le résumé dit « Aucun message »`() = runBlocking {
        assertThat(messagesSummary(Unconnected.messages.conversations.first()).detail).isEqualTo("Aucun message")
    }

    @Test
    fun `sans maison branchée, la maison est au repos et rien ne se bascule`() = runBlocking {
        Unconnected.home.toggle("salon")

        assertThat(homeSummary(Unconnected.home.controls.first())).isEqualTo("Maison au repos")
    }

    @Test
    fun `sans lecteur branché, rien n'est chargé et les boutons ne font rien`() = runBlocking {
        with(Unconnected.media) {
            play()
            pause()
            next()
            previous()
            seekTo(10.seconds)
        }

        assertThat(Unconnected.media.state.first()).isEqualTo(MediaState.None)
        assertThat(Unconnected.media.position.first()).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `toucher une app favorite l'ajoute aux favorites, la retoucher l'enlève`() {
        val added = Settings().withFavoriteAppToggled("spotify").withFavoriteAppToggled("agenda")
        assertThat(added.favoriteApps).isEqualTo(listOf("spotify", "agenda"))

        assertThat(added.withFavoriteAppToggled("spotify").favoriteApps).isEqualTo(listOf("agenda"))
    }
}
