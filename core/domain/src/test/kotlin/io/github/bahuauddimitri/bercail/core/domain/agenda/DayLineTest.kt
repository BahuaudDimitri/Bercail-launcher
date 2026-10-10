package io.github.bahuauddimitri.bercail.core.domain.agenda

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

/** The day drawn as a line, from 7 am to midnight. */
class DayLineTest {
    private val day = LocalDate.of(2026, 10, 2)
    private val events = listOf(event("Stand-up", 9, 30), event("Déjeuner", 12, 30), event("Ciné", 21, 0))

    @Test
    fun `la ligne commence à 7 h et finit à minuit`() {
        assertThat(dayLineAt(at(7, 0), events).now).isEqualTo(0f)
        assertThat(dayLineAt(at(15, 30), events).now).isCloseTo(0.5f, TOLERANCE)
        assertThat(dayLineAt(at(23, 59), events).now).isCloseTo(1f, TOLERANCE)
    }

    @Test
    fun `avant 7 h, le point de maintenant reste avant le début de la ligne`() {
        assertThat(dayLineAt(at(6, 0), events).now).isLessThan(0f)
    }

    @Test
    fun `chaque rendez-vous a son heure écrite sur la ligne`() {
        val line = dayLineAt(at(8, 10), events)

        assertThat(line.marks.map { it.time }).containsExactly("09:30", "12:30", "21:00")
    }

    @Test
    fun `chaque rendez-vous est placé à son heure`() {
        val line = dayLineAt(at(8, 10), events)

        assertThat(line.marks.first().position).isCloseTo(2.5f / 17f, TOLERANCE)
    }

    @Test
    fun `seul le prochain rendez-vous est entouré`() {
        val line = dayLineAt(at(10, 0), events)

        assertThat(line.marks.map { it.next }).containsExactly(false, true, false)
    }

    @Test
    fun `après le dernier rendez-vous, plus aucun n'est entouré`() {
        val line = dayLineAt(at(22, 0), events)

        assertThat(line.marks.map { it.next }).containsExactly(false, false, false)
    }

    private fun at(hour: Int, minute: Int): LocalDateTime = day.atTime(hour, minute)

    private fun event(title: String, hour: Int, minute: Int) =
        AgendaEvent(id = title, title = title, start = at(hour, minute), end = at(hour, minute).plusMinutes(30))

    private companion object {
        const val TOLERANCE = 0.002f
    }
}
