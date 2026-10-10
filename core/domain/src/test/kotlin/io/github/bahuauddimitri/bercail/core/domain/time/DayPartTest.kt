package io.github.bahuauddimitri.bercail.core.domain.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.time.LocalTime
import org.junit.Test

/** The moments of the day: they choose the weather sentence and the colors of Brume. */
class DayPartTest {
    @Test
    fun `le matin va de 5 h à midi`() {
        assertThat(DayPart.at(LocalTime.of(5, 0))).isEqualTo(DayPart.Morning)
        assertThat(DayPart.at(LocalTime.of(11, 59))).isEqualTo(DayPart.Morning)
    }

    @Test
    fun `l'après-midi va de midi à 18 h`() {
        assertThat(DayPart.at(LocalTime.of(12, 0))).isEqualTo(DayPart.Afternoon)
        assertThat(DayPart.at(LocalTime.of(17, 59))).isEqualTo(DayPart.Afternoon)
    }

    @Test
    fun `le soir va de 18 h à 20 h`() {
        assertThat(DayPart.at(LocalTime.of(18, 0))).isEqualTo(DayPart.Evening)
        assertThat(DayPart.at(LocalTime.of(19, 59))).isEqualTo(DayPart.Evening)
    }

    @Test
    fun `la nuit va de 20 h à 5 h`() {
        assertThat(DayPart.at(LocalTime.of(20, 0))).isEqualTo(DayPart.Night)
        assertThat(DayPart.at(LocalTime.of(0, 30))).isEqualTo(DayPart.Night)
        assertThat(DayPart.at(LocalTime.of(4, 59))).isEqualTo(DayPart.Night)
    }

    @Test
    fun `seule la nuit est sombre`() {
        assertThat(DayPart.Night.isDark).isTrue()
        assertThat(DayPart.Evening.isDark).isFalse()
        assertThat(DayPart.Morning.isDark).isFalse()
    }
}
