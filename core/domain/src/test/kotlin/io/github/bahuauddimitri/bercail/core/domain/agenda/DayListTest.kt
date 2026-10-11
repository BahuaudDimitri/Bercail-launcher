package io.github.bahuauddimitri.bercail.core.domain.agenda

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

/** The « Aujourd'hui » sheet: every appointment of the day, the past dimmed, the next one brought forward. */
class DayListTest {
    private val day = LocalDate.of(2026, 10, 2)
    private val events = listOf(
        event("Stand-up", 9, 30, 10, 0),
        event("Déjeuner avec Léa", 12, 30, 13, 30),
        event("Point design", 14, 0, 15, 0)
    )

    @Test
    fun `la feuille liste les rendez-vous du jour dans l'ordre, chacun avec son heure`() {
        val rows = dayRowsAt(at(8, 10), events.reversed())

        assertThat(rows.map { "${it.time} ${it.title}" })
            .containsExactly("09:30 Stand-up", "12:30 Déjeuner avec Léa", "14:00 Point design")
    }

    @Test
    fun `un rendez-vous terminé est atténué`() {
        val rows = dayRowsAt(at(10, 0), events)

        assertThat(rows.first().state).isEqualTo(DayRowState.Past)
    }

    @Test
    fun `un rendez-vous en cours n'est pas encore atténué`() {
        val rows = dayRowsAt(at(9, 45), events)

        assertThat(rows.first().state).isEqualTo(DayRowState.Coming)
    }

    @Test
    fun `le prochain rendez-vous est mis en avant, lui seul`() {
        val rows = dayRowsAt(at(10, 30), events)

        assertThat(rows.map { it.state })
            .containsExactly(DayRowState.Past, DayRowState.Highlighted, DayRowState.Coming)
    }

    @Test
    fun `venu de la recherche, c'est le rendez-vous choisi qui est mis en avant`() {
        val rows = dayRowsAt(at(8, 10), events, focus = "Point design")

        assertThat(rows.map { it.state })
            .containsExactly(DayRowState.Coming, DayRowState.Coming, DayRowState.Highlighted)
    }

    @Test
    fun `un rendez-vous choisi dans la recherche est mis en avant même s'il est passé`() {
        val rows = dayRowsAt(at(16, 0), events, focus = "Stand-up")

        assertThat(rows.first().state).isEqualTo(DayRowState.Highlighted)
    }

    @Test
    fun `un événement sur la journée entière vient en tête, marqué « Journée »`() {
        val rows = dayRowsAt(at(8, 10), events + wholeDay("Anniversaire de Léa"))

        assertThat(rows.first().time).isEqualTo("Journée")
        assertThat(rows.first().title).isEqualTo("Anniversaire de Léa")
    }

    @Test
    fun `une journée entière n'est ni atténuée ni mise en avant à la place du prochain rendez-vous`() {
        val rows = dayRowsAt(at(10, 30), events + wholeDay("Anniversaire de Léa"))

        assertThat(rows.first().state).isEqualTo(DayRowState.Coming)
        assertThat(rows.single { it.state == DayRowState.Highlighted }.title).isEqualTo("Déjeuner avec Léa")
    }

    @Test
    fun `la date s'écrit en toutes lettres`() {
        assertThat(day.inWords()).isEqualTo("vendredi 2 octobre")
        assertThat(LocalDate.of(2026, 12, 27).inWords()).isEqualTo("dimanche 27 décembre")
    }

    @Test
    fun `le premier du mois s'écrit « 1er »`() {
        assertThat(LocalDate.of(2026, 8, 1).inWords()).isEqualTo("samedi 1er août")
    }

    private fun at(hour: Int, minute: Int): LocalDateTime = day.atTime(hour, minute)

    private fun event(title: String, hour: Int, minute: Int, endHour: Int, endMinute: Int) =
        AgendaEvent(id = title, title = title, start = at(hour, minute), end = at(endHour, endMinute))

    private fun wholeDay(title: String) =
        AgendaEvent(title, title, start = day.atStartOfDay(), end = day.plusDays(1).atStartOfDay(), allDay = true)
}
