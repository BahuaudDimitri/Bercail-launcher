@file:Suppress("MagicNumber") // Sample data copied from the prototype: hours, temperatures, wind speeds.

package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.weather.City
import io.github.bahuauddimitri.bercail.core.domain.weather.CitySearch
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastJournal
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastNotes
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastService
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import io.github.bahuauddimitri.bercail.core.domain.weather.PlaceSource
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/** A forecast the tests set by hand. */
class FakeWeatherSource(forecast: Forecast? = null) : WeatherSource {
    override val forecast = MutableStateFlow(forecast)

    fun set(forecast: Forecast?) {
        this.forecast.value = forecast
    }
}

/** Two days of forecast from midnight on [day], each hour of the day described by [hour]. */
fun forecastOn(day: LocalDate, hour: (Int) -> HourForecast): Forecast = Forecast(
    (0 until 48).map { index -> hour(index % 24).copy(time = day.atStartOfDay().plusHours(index.toLong())) }
)

/** One hour of a forecast made with [forecastOn], which sets its time. */
fun hourly(temperature: Int, sky: Sky, wind: Int = 8, gusts: Int = 15) =
    HourForecast(LocalDate.EPOCH.atStartOfDay(), temperature, sky, wind, gusts)

/** The prototype's three skies, as forecasts of its day. */
internal object Skies {
    /** A clear morning at 11°, 17° in the afternoon. */
    val clear = forecastOn(Samples.day) { h -> hourly(if (h in 12..17) 17 else 11, Sky.Clear) }

    /** A light rain around lunch, until 4 pm. */
    val rain = forecastOn(Samples.day) { h -> hourly(14, if (h in 11..15) Sky.LightRain else Sky.Overcast) }

    /** A windy evening. */
    val wind = forecastOn(Samples.day) { hourly(12, Sky.Clear, wind = 35, gusts = 45) }
}

/** A weather service that answers [forecast], and remembers the places it was asked about. */
class FakeForecastService(var forecast: Forecast? = null) : ForecastService {
    /** False when the network is down: the service then answers nothing. */
    var reachable = true

    /** Set to make the service break in a way nobody planned. */
    var breakdown: Exception? = null
    val asked = mutableListOf<Place>()

    override suspend fun forecastAt(place: Place): Forecast? {
        breakdown?.let { throw it }
        asked += place
        return forecast.takeIf { reachable }
    }
}

/** A phone that is at [place], or does not know where it is. */
class FakePlaceSource(var place: Place? = Place(47.22, -1.55)) : PlaceSource {
    /** How many times the position was read. */
    var reads = 0

    override suspend fun current(): Place? {
        reads++
        return place
    }
}

/** Notes kept in memory. */
class FakeForecastJournal(var notes: ForecastNotes = ForecastNotes()) : ForecastJournal {
    override suspend fun read(): ForecastNotes = notes

    override suspend fun write(notes: ForecastNotes) {
        this.notes = notes
    }
}

/** A search that knows [cities] and finds those whose name starts with what was typed, whatever the capitals. */
class FakeCitySearch(var cities: List<City> = emptyList()) : CitySearch {
    /** False when the network is down: the search then cannot be made. */
    var reachable = true

    /** Every name searched so far, in order. */
    val asked = mutableListOf<String>()

    override suspend fun find(name: String): List<City>? {
        asked += name
        return cities.filter { it.name.startsWith(name, ignoreCase = true) }.takeIf { reachable }
    }
}
