package io.github.bahuauddimitri.bercail.core.domain.agenda

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

/** The top of the home screen: how long until the next appointment, its title, and what comes after. */
class GlanceTest {
    private val day = LocalDate.of(2026, 10, 2)
    private val events = listOf(
        event("Stand-up", 9, 30, 10, 0),
        event("Déjeuner avec Léa", 12, 30, 13, 30),
        event("Point design", 14, 0, 15, 0)
    )

    @Test
    fun `à 8 h 10, le prochain rendez-vous est annoncé avec son délai et son heure`() {
        val glance = glanceAt(at(8, 10), events)

        assertThat(glance.countdown).isEqualTo("Dans 1 h 20 · 09:30")
        assertThat(glance.title).isEqualTo("Stand-up")
    }

    @Test
    fun `le rendez-vous d'après est annoncé avec son heure`() {
        val glance = glanceAt(at(8, 10), events)

        assertThat(glance.after).isEqualTo("Ensuite Déjeuner avec Léa à 12:30")
    }

    @Test
    fun `sous une heure, le délai s'écrit en minutes`() {
        assertThat(glanceAt(at(8, 45), events).countdown).isEqualTo("Dans 45 min · 09:30")
    }

    @Test
    fun `à une heure pile, le délai s'écrit en heures`() {
        assertThat(glanceAt(at(8, 30), events).countdown).isEqualTo("Dans 1 h 00 · 09:30")
    }

    @Test
    fun `au-delà d'une heure, les minutes s'écrivent sur deux chiffres`() {
        assertThat(glanceAt(at(8, 25), events).countdown).isEqualTo("Dans 1 h 05 · 09:30")
    }

    @Test
    fun `une minute entamée compte encore, on n'annonce jamais « Dans 0 min »`() {
        val now = at(9, 29).plusSeconds(40)

        assertThat(glanceAt(now, events).countdown).isEqualTo("Dans 1 min · 09:30")
    }

    @Test
    fun `un rendez-vous déjà commencé n'est plus le prochain`() {
        val glance = glanceAt(at(9, 45), events)

        assertThat(glance.title).isEqualTo("Déjeuner avec Léa")
    }

    @Test
    fun `avant le dernier rendez-vous, il n'y a rien d'autre aujourd'hui`() {
        val glance = glanceAt(at(13, 40), events)

        assertThat(glance.title).isEqualTo("Point design")
        assertThat(glance.after).isEqualTo("Rien d'autre aujourd'hui")
    }

    @Test
    fun `après le dernier rendez-vous, plus rien aujourd'hui`() {
        val glance = glanceAt(at(20, 40), events)

        assertThat(glance.countdown).isEqualTo("Plus rien aujourd'hui")
        assertThat(glance.title).isEqualTo("Soirée libre")
        assertThat(glance.after).isNull()
    }

    @Test
    fun `les rendez-vous sont lus dans l'ordre de leur heure, pas de leur arrivée`() {
        val glance = glanceAt(at(8, 10), events.reversed())

        assertThat(glance.title).isEqualTo("Stand-up")
    }

    private fun at(hour: Int, minute: Int): LocalDateTime = day.atTime(hour, minute)

    private fun event(title: String, hour: Int, minute: Int, endHour: Int, endMinute: Int) =
        AgendaEvent(id = title, title = title, start = at(hour, minute), end = at(endHour, endMinute))
}
