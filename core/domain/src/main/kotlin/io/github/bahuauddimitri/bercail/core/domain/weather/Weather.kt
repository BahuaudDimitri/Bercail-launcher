package io.github.bahuauddimitri.bercail.core.domain.weather

import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import kotlinx.coroutines.flow.Flow

enum class WeatherCondition(internal val wording: String) {
    Clear("Ciel dégagé"),
    Rain("Pluie"),
    Wind("Vent"),
    Snow("Neige")
}

/** The weather now: temperature in degrees Celsius and what the sky does. */
data class Weather(val temperature: Int, val condition: WeatherCondition)

interface WeatherSource {
    /** The current weather, or nothing while it is unknown. */
    val weather: Flow<Weather?>
}

/** The weather in one line, with a sentence that fits the hour: no "clear night" in the morning. */
fun weatherLine(weather: Weather, part: DayPart): String {
    val sentence = if (weather.condition == WeatherCondition.Clear && part == DayPart.Night) {
        "Nuit claire"
    } else {
        "${weather.condition.wording} ${part.wording}"
    }
    return "${weather.temperature}° · $sentence"
}

private val DayPart.wording: String
    get() = when (this) {
        DayPart.Morning -> "ce matin"
        DayPart.Afternoon -> "cet après-midi"
        DayPart.Evening -> "ce soir"
        DayPart.Night -> "cette nuit"
    }
