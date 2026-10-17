package io.github.bahuauddimitri.bercail.core.domain.time

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

/** The alarm of the phone at the end of the line of the day. */
class WakeUpTest {
    private val day = LocalDate.of(2026, 10, 2)
    private val evening = day.atTime(20, 40)
    private val tomorrowMorning = day.plusDays(1).atTime(7, 15)
    private val forecast = Forecast(
        listOf(
            HourForecast(day.plusDays(1).atTime(6, 0), temperature = 8, sky = Sky.Clear),
            HourForecast(day.plusDays(1).atTime(7, 0), temperature = 9, sky = Sky.Clear),
            HourForecast(day.plusDays(1).atTime(8, 0), temperature = 11, sky = Sky.Cloudy)
        )
    )

    @Test
    fun `le réveil des prochaines 24 heures finit la ligne avec son heure`() {
        val wakeUp = wakeUpAt(evening, tomorrowMorning, forecast = null)

        assertThat(wakeUp.time).isEqualTo("07:15")
        assertThat(wakeUp.caption).isEqualTo("réveil")
    }

    @Test
    fun `le réveil porte la température prévue à son heure`() {
        assertThat(wakeUpAt(evening, tomorrowMorning, forecast).caption).isEqualTo("réveil · 9°")
    }

    @Test
    fun `sans prévision à cette heure, le réveil dit seulement réveil`() {
        val later = day.plusDays(1).atTime(10, 30)

        assertThat(wakeUpAt(evening, later, forecast).caption).isEqualTo("réveil")
    }

    @Test
    fun `sans réveil, ou au-delà de 24 heures, la ligne finit sur l'anneau gris Aucun réveil`() {
        assertThat(wakeUpAt(evening, alarm = null, forecast)).isEqualTo(WakeUp.None)
        assertThat(wakeUpAt(evening, evening.plusHours(24).plusMinutes(1), forecast)).isEqualTo(WakeUp.None)
    }

    @Test
    fun `un réveil déjà passé laisse Aucun réveil au bout de la ligne`() {
        assertThat(wakeUpAt(evening, evening.minusMinutes(1), forecast)).isEqualTo(WakeUp.None)
    }

    @Test
    fun `sans réveil, le bout de la ligne écrit et annonce Aucun réveil, sans heure`() {
        assertThat(WakeUp.None.set).isFalse()
        assertThat(WakeUp.None.time).isEmpty()
        assertThat(WakeUp.None.caption).isEqualTo("Aucun réveil")
        assertThat(WakeUp.None.spoken).isEqualTo("Aucun réveil")
    }

    @Test
    fun `un réveil des prochaines 24 heures est bien un réveil réglé`() {
        assertThat(wakeUpAt(evening, tomorrowMorning, forecast).set).isTrue()
    }

    @Test
    fun `le lecteur d'écran annonce le réveil de demain, ou celui d'aujourd'hui après minuit`() {
        assertThat(wakeUpAt(evening, tomorrowMorning, forecast).spoken).isEqualTo("Réveil demain à 07:15")

        val afterMidnight: LocalDateTime = day.plusDays(1).atTime(1, 20)
        assertThat(wakeUpAt(afterMidnight, tomorrowMorning, forecast).spoken).isEqualTo("Réveil à 07:15")
    }
}
