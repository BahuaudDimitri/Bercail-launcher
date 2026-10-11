package io.github.bahuauddimitri.bercail.core.domain.weather

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

/** The weather in one line: the temperature now, then one sentence drawn from the hours to come. */
class WeatherLineTest {
    private val day = LocalDate.of(2026, 10, 2)

    private fun at(hour: Int, minute: Int = 0): LocalDateTime = day.atTime(hour, minute)

    /** Two days of forecast, hour by hour, starting at midnight. The second day repeats the first. */
    private fun forecast(hour: (Int) -> HourForecast): Forecast = Forecast(
        (0 until 48).map { index ->
            val model = hour(index % 24)
            model.copy(time = day.atStartOfDay().plusHours(index.toLong()))
        }
    )

    private fun hour(temperature: Int, sky: Sky, wind: Int = 8, gusts: Int = 15) =
        HourForecast(day.atStartOfDay(), temperature, sky, wind, gusts)

    private fun lineAt(now: LocalDateTime, forecast: Forecast): String = weatherAt(now, forecast)!!.line

    // ---------- the line ----------

    @Test
    fun `la ligne météo donne la température de l'heure en cours, puis la phrase`() {
        val sunny = forecast { h -> hour(temperature = if (h < 12) 11 else 17, Sky.Clear) }

        assertThat(lineAt(at(8, 10), sunny)).isEqualTo("11° · Ciel dégagé, 17° cet après-midi")
    }

    @Test
    fun `les températures négatives gardent leur signe`() {
        val frost = forecast { hour(temperature = -3, Sky.Clear) }

        assertThat(lineAt(at(23, 0), frost)).isEqualTo("-3° · Nuit claire")
    }

    @Test
    fun `une prévision qui ne couvre pas l'heure qu'il est ne dit rien`() {
        val yesterday = Forecast(listOf(HourForecast(day.minusDays(1).atTime(8, 0), 11, Sky.Clear)))

        assertThat(weatherAt(at(8, 10), yesterday)).isNull()
        assertThat(weatherAt(at(8, 10), Forecast(emptyList()))).isNull()
    }

    // ---------- dry weather ----------

    @Test
    fun `le matin, par temps sec, la phrase annonce la température de l'après-midi`() {
        val cloudy = forecast { h -> hour(temperature = if (h in 12..17) 9 + h - 12 else 8, Sky.Cloudy) }

        assertThat(lineAt(at(7, 30), cloudy)).isEqualTo("8° · Quelques nuages, 14° cet après-midi")
    }

    @Test
    fun `l'après-midi et le soir, par temps sec, la phrase suit le moment de la journée`() {
        val grey = forecast { hour(temperature = 14, Sky.Overcast) }

        assertThat(lineAt(at(15, 0), grey)).isEqualTo("14° · Ciel couvert cet après-midi")
        assertThat(lineAt(at(19, 0), grey)).isEqualTo("14° · Ciel couvert ce soir")
        assertThat(lineAt(at(22, 0), grey)).isEqualTo("14° · Ciel couvert cette nuit")
    }

    @Test
    fun `par ciel dégagé, on ne parle de nuit claire que la nuit`() {
        val clear = forecast { hour(temperature = 9, Sky.Clear) }

        assertThat(lineAt(at(2, 0), clear)).isEqualTo("9° · Nuit claire")
        listOf(8, 15, 19).forEach { h -> assertThat(lineAt(at(h), clear)).doesNotContain("Nuit claire") }
    }

    @Test
    fun `le brouillard a sa phrase`() {
        val fog = forecast { hour(temperature = 6, Sky.Fog) }

        assertThat(lineAt(at(19, 0), fog)).isEqualTo("6° · Brouillard ce soir")
    }

    // ---------- rain and snow ----------

    @Test
    fun `quand il pleut, la phrase dit jusqu'à quelle heure`() {
        val shower = forecast { h -> hour(temperature = 14, if (h in 11..15) Sky.LightRain else Sky.Overcast) }

        assertThat(lineAt(at(13, 35), shower)).isEqualTo("14° · Pluie fine jusqu'à 16 h")
    }

    @Test
    fun `une pluie qui change de force reste une seule pluie`() {
        val rain = forecast { h ->
            hour(
                temperature = 12,
                if (h in 9..10) {
                    Sky.Rain
                } else if (h in 11..13) {
                    Sky.Drizzle
                } else {
                    Sky.Cloudy
                }
            )
        }

        assertThat(lineAt(at(9, 20), rain)).isEqualTo("12° · Pluie jusqu'à 14 h")
    }

    @Test
    fun `midi et minuit s'écrivent en toutes lettres`() {
        val morning = forecast { h -> hour(temperature = 10, if (h in 8..11) Sky.Showers else Sky.Cloudy) }
        val evening = forecast { h -> hour(temperature = 10, if (h in 20..23) Sky.Rain else Sky.Cloudy) }

        assertThat(lineAt(at(9, 0), morning)).isEqualTo("10° · Averses jusqu'à midi")
        assertThat(lineAt(at(21, 0), evening)).isEqualTo("10° · Pluie jusqu'à minuit")
    }

    @Test
    fun `une pluie qui ne s'arrête pas dans les douze heures dure tout le moment`() {
        val soaked = forecast { hour(temperature = 13, Sky.LightRain) }

        assertThat(lineAt(at(8, 0), soaked)).isEqualTo("13° · Pluie fine toute la journée")
        assertThat(lineAt(at(14, 0), soaked)).isEqualTo("13° · Pluie fine jusqu'à la nuit")
        assertThat(lineAt(at(19, 0), soaked)).isEqualTo("13° · Pluie fine toute la soirée")
        assertThat(lineAt(at(23, 0), soaked)).isEqualTo("13° · Pluie fine toute la nuit")
    }

    @Test
    fun `par temps sec, une pluie attendue dans les six heures est annoncée avec son heure`() {
        val later = forecast { h -> hour(temperature = 11, if (h in 11..13) Sky.LightRain else Sky.Cloudy) }

        assertThat(lineAt(at(8, 10), later)).isEqualTo("11° · Pluie fine attendue vers 11 h")
    }

    @Test
    fun `une pluie attendue plus tard que six heures n'est pas encore annoncée`() {
        val muchLater = forecast { h -> hour(temperature = 11, if (h in 16..18) Sky.Rain else Sky.Clear) }

        assertThat(lineAt(at(8, 10), muchLater)).isEqualTo("11° · Ciel dégagé, 11° cet après-midi")
    }

    @Test
    fun `l'annonce s'accorde avec ce qui est attendu`() {
        fun expecting(sky: Sky) = lineAt(at(14, 0), forecast { h -> hour(9, if (h == 17) sky else Sky.Overcast) })

        assertThat(expecting(Sky.Showers)).isEqualTo("9° · Averses attendues vers 17 h")
        assertThat(expecting(Sky.Storm)).isEqualTo("9° · Orage attendu vers 17 h")
        assertThat(expecting(Sky.Snow)).isEqualTo("9° · Neige attendue vers 17 h")
    }

    @Test
    fun `la neige se dit comme la pluie, avec ses mots`() {
        val snow = forecast { h -> hour(temperature = 0, if (h in 6..14) Sky.LightSnow else Sky.Overcast) }

        assertThat(lineAt(at(10, 0), snow)).isEqualTo("0° · Neige légère jusqu'à 15 h")
    }

    // ---------- wind ----------

    @Test
    fun `par vent fort, la phrase donne sa vitesse, arrondie à 5 km à l'heure`() {
        val windy = forecast { hour(temperature = 12, Sky.Clear, wind = 37, gusts = 44) }

        assertThat(lineAt(at(20, 40), windy)).isEqualTo("12° · Vent 35 km/h cette nuit")
        assertThat(lineAt(at(9, 0), windy)).isEqualTo("12° · Vent 35 km/h ce matin")
    }

    @Test
    fun `avec de fortes rafales, la phrase les annonce`() {
        val gusty = forecast { hour(temperature = 12, Sky.Cloudy, wind = 34, gusts = 52) }

        assertThat(lineAt(at(15, 0), gusty)).isEqualTo("12° · Vent 35 km/h, rafales à 50")
    }

    @Test
    fun `un vent léger ne fait pas une phrase`() {
        val breeze = forecast { hour(temperature = 12, Sky.Overcast, wind = 22, gusts = 35) }

        assertThat(lineAt(at(15, 0), breeze)).isEqualTo("12° · Ciel couvert cet après-midi")
    }

    @Test
    fun `sous la pluie, on parle de la pluie plutôt que du vent`() {
        val storm = forecast { hour(temperature = 12, Sky.Rain, wind = 45, gusts = 70) }

        assertThat(lineAt(at(15, 0), storm)).isEqualTo("12° · Pluie jusqu'à la nuit")
    }

    // ---------- how the sky looks ----------

    @Test
    fun `l'allure du ciel suit ce qui tombe, puis le vent`() {
        fun lookOf(sky: Sky, wind: Int = 8) =
            weatherAt(at(15, 0), forecast { hour(temperature = 5, sky, wind = wind) })!!.condition

        assertThat(lookOf(Sky.Clear)).isEqualTo(WeatherCondition.Clear)
        assertThat(lookOf(Sky.Overcast)).isEqualTo(WeatherCondition.Clear)
        assertThat(lookOf(Sky.Drizzle)).isEqualTo(WeatherCondition.Rain)
        assertThat(lookOf(Sky.Storm)).isEqualTo(WeatherCondition.Rain)
        assertThat(lookOf(Sky.SnowShowers)).isEqualTo(WeatherCondition.Snow)
        assertThat(lookOf(Sky.Clear, wind = 40)).isEqualTo(WeatherCondition.Wind)
        assertThat(lookOf(Sky.HeavySnow, wind = 40)).isEqualTo(WeatherCondition.Snow)
    }

    // ---------- the codes of the forecasts ----------

    @Test
    fun `chaque code des prévisions a son ciel`() {
        val read = mapOf(
            0 to Sky.Clear, 1 to Sky.Clear, 2 to Sky.Cloudy, 3 to Sky.Overcast, 45 to Sky.Fog, 48 to Sky.Fog,
            51 to Sky.Drizzle, 53 to Sky.Drizzle, 55 to Sky.Drizzle, 56 to Sky.FreezingRain, 57 to Sky.FreezingRain,
            61 to Sky.LightRain, 63 to Sky.Rain, 65 to Sky.HeavyRain, 66 to Sky.FreezingRain, 67 to Sky.FreezingRain,
            71 to Sky.LightSnow, 73 to Sky.Snow, 75 to Sky.HeavySnow, 77 to Sky.Snow,
            80 to Sky.Showers, 81 to Sky.Showers, 82 to Sky.HeavyShowers, 85 to Sky.SnowShowers, 86 to Sky.SnowShowers,
            95 to Sky.Storm, 96 to Sky.Hail, 99 to Sky.Hail
        )

        read.forEach { (code, sky) -> assertThat(Sky.ofCode(code)).isEqualTo(sky) }
    }

    @Test
    fun `un code inconnu ne casse rien, le ciel est dit couvert`() {
        assertThat(Sky.ofCode(42)).isEqualTo(Sky.Overcast)
        assertThat(Sky.ofCode(-1)).isEqualTo(Sky.Overcast)
    }

    @Test
    fun `chaque ciel a ses mots`() {
        fun wordsOf(sky: Sky) = lineAt(at(15, 0), forecast { hour(temperature = 5, sky) }).substringAfter("· ")

        assertThat(wordsOf(Sky.HeavyRain)).isEqualTo("Forte pluie jusqu'à la nuit")
        assertThat(wordsOf(Sky.FreezingRain)).isEqualTo("Pluie verglaçante jusqu'à la nuit")
        assertThat(wordsOf(Sky.HeavyShowers)).isEqualTo("Fortes averses jusqu'à la nuit")
        assertThat(wordsOf(Sky.Hail)).isEqualTo("Orage de grêle jusqu'à la nuit")
        assertThat(wordsOf(Sky.HeavySnow)).isEqualTo("Forte neige jusqu'à la nuit")
        assertThat(wordsOf(Sky.Drizzle)).isEqualTo("Bruine jusqu'à la nuit")
        assertThat(weatherAt(at(15, 0), forecast { hour(5, Sky.Clear) })).isNotNull()
    }
}
