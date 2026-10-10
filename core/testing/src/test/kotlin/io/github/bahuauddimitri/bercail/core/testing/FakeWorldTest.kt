package io.github.bahuauddimitri.bercail.core.testing

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.agenda.glanceAt
import io.github.bahuauddimitri.bercail.core.domain.messages.messagesSummary
import io.github.bahuauddimitri.bercail.core.domain.weather.Weather
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The three moments of the prototype (morning, commute, evening) are the data sets of the tests. */
class FakeWorldTest {
    @Test
    fun `le matin, il est 8 h 10, ciel dégagé, Tom et Léa ont écrit`() = runTest {
        val world = FakeWorld(Moment.Matin)

        assertThat(world.clock.now().toLocalTime().toString()).isEqualTo("08:10")
        assertThat(world.weather.weather.first()).isEqualTo(Weather(11, WeatherCondition.Clear))
        assertThat(messagesSummary(world.messages.conversations.first()).detail).isEqualTo("Léa, Tom")
    }

    @Test
    fun `pendant le trajet, il est 13 h 35 et il pleut`() = runTest {
        val world = FakeWorld(Moment.Trajet)

        assertThat(world.clock.now().toLocalTime().toString()).isEqualTo("13:35")
        assertThat(world.weather.weather.first()).isEqualTo(Weather(14, WeatherCondition.Rain))
        assertThat(messagesSummary(world.messages.conversations.first()).detail).isEqualTo("Léa, Équipe")
    }

    @Test
    fun `le soir, il est 20 h 40, il y a du vent, Maman et Julien ont écrit`() = runTest {
        val world = FakeWorld(Moment.Soir)

        assertThat(world.clock.now().toLocalTime().toString()).isEqualTo("20:40")
        assertThat(world.weather.weather.first()).isEqualTo(Weather(12, WeatherCondition.Wind))
        assertThat(messagesSummary(world.messages.conversations.first()).headline).isEqualTo("3 non lus")
    }

    @Test
    fun `la journée factice a cinq rendez-vous, le premier est le Stand-up`() = runTest {
        val world = FakeWorld(Moment.Matin)
        val today = world.agenda.eventsOn(world.clock.now().toLocalDate()).first()

        assertThat(today).hasSize(5)
        assertThat(glanceAt(world.clock.now(), today).title).isEqualTo("Stand-up")
    }

    @Test
    fun `les rendez-vous d'un autre jour ne sont pas ceux d'aujourd'hui`() = runTest {
        val world = FakeWorld(Moment.Matin)

        assertThat(world.agenda.eventsOn(LocalDate.of(2026, 10, 3)).first()).isEmpty()
    }

    @Test
    fun `un agenda qui se répète propose la même journée chaque jour`() = runTest {
        val agenda = FakeAgendaSource(Samples.eventsOn(Samples.day), everyDay = true)

        val tomorrow = agenda.eventsOn(LocalDate.of(2026, 10, 3)).first()

        assertThat(tomorrow.map { it.start.toString() }.first()).isEqualTo("2026-10-03T09:30")
    }

    @Test
    fun `les favoris factices sont Léa, Tom, Équipe et Maman`() = runTest {
        val world = FakeWorld(Moment.Matin)

        assertThat(world.settings.settings.first().favoritePeople).containsExactly("lea", "tom", "team", "mum")
    }

    @Test
    fun `l'heure factice avance quand on le demande`() = runTest {
        val world = FakeWorld(Moment.Matin)

        world.clock.advanceMinutes(20)

        assertThat(world.clock.minutes.first().toLocalTime().toString()).isEqualTo("08:30")
    }

    @Test
    fun `un message reçu s'ajoute aux non-lus, une conversation ouverte les efface`() = runTest {
        val world = FakeWorld(Moment.Matin)

        world.messages.receive("mum")
        assertThat(messagesSummary(world.messages.conversations.first()).headline).isEqualTo("3 non lus")

        world.messages.markRead("mum")
        world.messages.markRead("lea")
        assertThat(messagesSummary(world.messages.conversations.first()).headline).isEqualTo("1 non lu")
    }
}
