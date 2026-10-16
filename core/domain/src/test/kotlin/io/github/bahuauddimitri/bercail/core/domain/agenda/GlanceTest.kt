package io.github.bahuauddimitri.bercail.core.domain.agenda

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
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
    fun `sans rendez-vous à venir, la journée est libre avant 18 h, la soirée ensuite`() {
        assertThat(glanceAt(at(9, 0), emptyList()).title).isEqualTo("Journée libre")
        assertThat(glanceAt(at(15, 0), emptyList()).title).isEqualTo("Journée libre")
        assertThat(glanceAt(at(18, 0), emptyList()).title).isEqualTo("Soirée libre")
        assertThat(glanceAt(at(23, 0), emptyList()).title).isEqualTo("Soirée libre")
    }

    @Test
    fun `les rendez-vous sont lus dans l'ordre de leur heure, pas de leur arrivée`() {
        val glance = glanceAt(at(8, 10), events.reversed())

        assertThat(glance.title).isEqualTo("Stand-up")
    }

    @Test
    fun `un événement sur la journée entière n'est jamais annoncé comme le prochain rendez-vous`() {
        val birthday =
            AgendaEvent("b", "Anniversaire", day.atStartOfDay(), day.plusDays(1).atStartOfDay(), allDay = true)

        assertThat(glanceAt(at(8, 10), events + birthday).title).isEqualTo("Stand-up")
        assertThat(glanceAt(at(20, 40), listOf(birthday)).countdown).isEqualTo("Plus rien aujourd'hui")
    }

    @Test
    fun `tant que Bercail n'a pas le droit de lire l'agenda, l'en-tête propose de le relier`() {
        assertThat(closedGlance.countdown).isEqualTo("Agenda pas encore relié")
        assertThat(closedGlance.title).isEqualTo("Relier l'agenda")
        assertThat(closedGlance.after).isEqualTo("Toucher pour autoriser sa lecture")
    }

    @Test
    fun `quand plus rien n'est à venir, l'en-tête mène à l'app agenda du téléphone`() {
        assertThat(glanceAt(at(20, 40), events).leadsToCalendar).isTrue()
        assertThat(glanceAt(at(9, 0), emptyList()).leadsToCalendar).isTrue()
    }

    @Test
    fun `tant qu'un rendez-vous est à venir, l'en-tête mène à la journée`() {
        assertThat(glanceAt(at(13, 40), events).leadsToCalendar).isFalse()
    }

    @Test
    fun `une journée entière ne compte pas comme à venir`() {
        val off = AgendaEvent("off", "Congé", day.atStartOfDay(), day.plusDays(1).atStartOfDay(), allDay = true)

        assertThat(glanceAt(at(8, 10), listOf(off)).leadsToCalendar).isTrue()
    }

    @Test
    fun `tant que Bercail n'a pas le droit de lire l'agenda, l'en-tête mène à la journée qui propose de le relier`() {
        assertThat(closedGlance.leadsToCalendar).isFalse()
    }

    private fun at(hour: Int, minute: Int): LocalDateTime = day.atTime(hour, minute)

    private fun event(title: String, hour: Int, minute: Int, endHour: Int, endMinute: Int) =
        AgendaEvent(id = title, title = title, start = at(hour, minute), end = at(endHour, endMinute))
}
