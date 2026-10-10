package io.github.bahuauddimitri.bercail.core.domain.weather

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import org.junit.Test

/** The weather in one line: temperature, then a sentence that fits the hour. */
class WeatherLineTest {
    @Test
    fun `la ligne météo donne la température puis la phrase`() {
        val line = weatherLine(Weather(11, WeatherCondition.Clear), DayPart.Morning)

        assertThat(line).isEqualTo("11° · Ciel dégagé ce matin")
    }

    @Test
    fun `par ciel dégagé, on ne parle de nuit claire que la nuit`() {
        assertThat(weatherLine(Weather(9, WeatherCondition.Clear), DayPart.Night)).isEqualTo("9° · Nuit claire")
        DayPart.entries.filter { it != DayPart.Night }.forEach { part ->
            assertThat(weatherLine(Weather(9, WeatherCondition.Clear), part)).doesNotContain("Nuit claire")
        }
    }

    @Test
    fun `la phrase suit le moment de la journée`() {
        val rain = Weather(14, WeatherCondition.Rain)

        assertThat(weatherLine(rain, DayPart.Morning)).isEqualTo("14° · Pluie ce matin")
        assertThat(weatherLine(rain, DayPart.Afternoon)).isEqualTo("14° · Pluie cet après-midi")
        assertThat(weatherLine(rain, DayPart.Evening)).isEqualTo("14° · Pluie ce soir")
        assertThat(weatherLine(rain, DayPart.Night)).isEqualTo("14° · Pluie cette nuit")
    }

    @Test
    fun `chaque temps a sa phrase`() {
        assertThat(weatherLine(Weather(12, WeatherCondition.Wind), DayPart.Evening)).isEqualTo("12° · Vent ce soir")
        assertThat(weatherLine(Weather(0, WeatherCondition.Snow), DayPart.Morning)).isEqualTo("0° · Neige ce matin")
        assertThat(weatherLine(Weather(17, WeatherCondition.Clear), DayPart.Afternoon))
            .isEqualTo("17° · Ciel dégagé cet après-midi")
    }

    @Test
    fun `les températures négatives gardent leur signe`() {
        assertThat(weatherLine(Weather(-3, WeatherCondition.Snow), DayPart.Night)).isEqualTo("-3° · Neige cette nuit")
    }
}
