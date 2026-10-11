package io.github.bahuauddimitri.bercail.core.domain.weather

import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow

/** How the sky looks: it chooses the dot of the weather line and what is drawn over Brume. */
enum class WeatherCondition { Clear, Rain, Wind, Snow }

/**
 * What the sky does during one hour, as the forecasts name it. [expected] is how "expected" agrees with it in
 * French; something [falls] when it rains or snows.
 */
enum class Sky(
    internal val wording: String,
    internal val expected: String = "",
    internal val falls: Boolean = false,
    internal val snows: Boolean = false
) {
    Clear("Ciel dégagé"),
    Cloudy("Quelques nuages"),
    Overcast("Ciel couvert"),
    Fog("Brouillard"),
    Drizzle("Bruine", "attendue", falls = true),
    LightRain("Pluie fine", "attendue", falls = true),
    Rain("Pluie", "attendue", falls = true),
    HeavyRain("Forte pluie", "attendue", falls = true),
    FreezingRain("Pluie verglaçante", "attendue", falls = true),
    Showers("Averses", "attendues", falls = true),
    HeavyShowers("Fortes averses", "attendues", falls = true),
    LightSnow("Neige légère", "attendue", falls = true, snows = true),
    Snow("Neige", "attendue", falls = true, snows = true),
    HeavySnow("Forte neige", "attendue", falls = true, snows = true),
    SnowShowers("Averses de neige", "attendues", falls = true, snows = true),
    Storm("Orage", "attendu", falls = true),
    Hail("Orage de grêle", "attendu", falls = true);

    companion object {
        /** Reads the weather code of the forecasts (the WMO table). A code nobody knows is a grey sky. */
        @Suppress("MagicNumber", "CyclomaticComplexMethod") // The table itself: one line per group of codes.
        fun ofCode(code: Int): Sky = when (code) {
            0, 1 -> Clear
            2 -> Cloudy
            45, 48 -> Fog
            51, 53, 55 -> Drizzle
            61 -> LightRain
            63 -> Rain
            65 -> HeavyRain
            56, 57, 66, 67 -> FreezingRain
            71 -> LightSnow
            73, 77 -> Snow
            75 -> HeavySnow
            80, 81 -> Showers
            82 -> HeavyShowers
            85, 86 -> SnowShowers
            95 -> Storm
            96, 99 -> Hail
            else -> Overcast
        }
    }
}

/** One hour of forecast, starting at [time]: degrees Celsius, the sky, the wind and its gusts in km/h. */
data class HourForecast(
    val time: LocalDateTime,
    val temperature: Int,
    val sky: Sky,
    val wind: Int = 0,
    val gusts: Int = 0
)

/** The hours to come, in order. */
data class Forecast(val hours: List<HourForecast>)

/** The weather now, as the home screen says it. */
data class Weather(val temperature: Int, val condition: WeatherCondition, val sentence: String) {
    /** The weather in one line: "14° · Pluie fine jusqu'à 16 h". */
    val line: String get() = "$temperature° · $sentence"
}

interface WeatherSource {
    /** The forecast of the hours to come, or nothing while it is unknown. Nothing is fetched while nobody listens. */
    val forecast: Flow<Forecast?>
}

/**
 * The weather at [now], read in the [forecast]: the temperature of the current hour and one sentence that fits
 * the hour. Rain or snow comes first (until when, or when it is expected), then a strong wind, then the sky.
 * Nothing when the forecast does not cover the current hour.
 */
fun weatherAt(now: LocalDateTime, forecast: Forecast): Weather? {
    val hour = now.truncatedTo(ChronoUnit.HOURS)
    val index = forecast.hours.indexOfFirst { it.time == hour }
    if (index < 0) return null
    val current = forecast.hours[index]
    val ahead = forecast.hours.drop(index + 1)
    val part = DayPart.at(now.toLocalTime())
    val coming = ahead.take(SOON_HOURS).firstOrNull { it.sky.falls }
    return when {
        current.sky.falls -> Weather(current.temperature, current.sky.look, falling(current, ahead, part))

        coming != null -> Weather(
            current.temperature,
            current.look,
            "${coming.sky.wording} ${coming.sky.expected} vers ${coming.time.hour.asHour()}"
        )

        current.isWindy -> Weather(current.temperature, WeatherCondition.Wind, windy(current, part))

        else -> Weather(current.temperature, WeatherCondition.Clear, dry(current, ahead, part))
    }
}

/** "Pluie fine jusqu'à 16 h", or "toute la journée" when it does not stop within twelve hours. */
private fun falling(current: HourForecast, ahead: List<HourForecast>, part: DayPart): String {
    val stop = ahead.take(LONG_HOURS).firstOrNull { !it.sky.falls }
    val until = if (stop != null) {
        "jusqu'à ${stop.time.hour.asHour()}"
    } else {
        when (part) {
            DayPart.Morning -> "toute la journée"
            DayPart.Afternoon -> "jusqu'à la nuit"
            DayPart.Evening -> "toute la soirée"
            DayPart.Night -> "toute la nuit"
        }
    }
    return "${current.sky.wording} $until"
}

/** "Vent 35 km/h ce soir", or "Vent 35 km/h, rafales à 50" when the gusts are strong. */
private fun windy(current: HourForecast, part: DayPart): String {
    val wind = "Vent ${current.wind.rounded()} km/h"
    return if (current.gusts >= STRONG_GUSTS) "$wind, rafales à ${current.gusts.rounded()}" else "$wind ${part.wording}"
}

/** "Ciel dégagé, 17° cet après-midi" in the morning, "Nuit claire" at night, the sky and the moment otherwise. */
private fun dry(current: HourForecast, ahead: List<HourForecast>, part: DayPart): String {
    val afternoon = ahead
        .filter { it.time.toLocalDate() == current.time.toLocalDate() && it.time.hour in AFTERNOON }
        .maxOfOrNull { it.temperature }
    return when {
        current.sky == Sky.Clear && part == DayPart.Night -> "Nuit claire"
        part == DayPart.Morning && afternoon != null -> "${current.sky.wording}, $afternoon° cet après-midi"
        else -> "${current.sky.wording} ${part.wording}"
    }
}

private val Sky.look: WeatherCondition
    get() = if (snows) WeatherCondition.Snow else WeatherCondition.Rain

/** The look of an hour when nothing falls: the wind if it is strong, a plain sky otherwise. */
private val HourForecast.look: WeatherCondition
    get() = if (isWindy) WeatherCondition.Wind else WeatherCondition.Clear

private val HourForecast.isWindy: Boolean get() = wind >= STRONG_WIND || gusts >= STRONG_GUSTS

private val DayPart.wording: String
    get() = when (this) {
        DayPart.Morning -> "ce matin"
        DayPart.Afternoon -> "cet après-midi"
        DayPart.Evening -> "ce soir"
        DayPart.Night -> "cette nuit"
    }

/** An hour as the sentence writes it: "16 h", "midi", "minuit". */
private fun Int.asHour(): String = when (this) {
    0 -> "minuit"
    NOON -> "midi"
    else -> "$this h"
}

/** A speed to the nearest 5 km/h: nobody feels the difference between 34 and 37. */
private fun Int.rounded(): Int = (this / SPEED_STEP.toFloat()).roundToInt() * SPEED_STEP

private const val SOON_HOURS = 6
private const val LONG_HOURS = 12
private const val STRONG_WIND = 30
private const val STRONG_GUSTS = 50
private const val SPEED_STEP = 5
private const val NOON = 12
private val AFTERNOON = 12..17
