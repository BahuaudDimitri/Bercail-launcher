package io.github.bahuauddimitri.bercail.data.weather

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastJournal
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastNotes
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Opens the file of the weather notes. One store per file and per process; [scope] lives as long as the app.
 * A damaged file starts again from nothing: the only loss is one more request.
 */
fun forecastStore(file: File, scope: CoroutineScope): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope
) { file }

/** The last forecast and when it was received, in a file: read when the home screen shows, written once an hour. */
class StoredForecast(private val store: DataStore<Preferences>) : ForecastJournal {
    override suspend fun read(): ForecastNotes = store.data.first().let { saved ->
        val forecast = saved[HOURS]?.let(::forecastIn)
        ForecastNotes(
            forecast = forecast,
            // A forecast that cannot be read anymore was never received.
            fetchedAt = saved[FETCHED_AT]?.let(::dateOrNull).takeIf { forecast != null },
            failedAt = saved[FAILED_AT]?.let(::dateOrNull),
            city = saved[CITY_LATITUDE]?.let { latitude -> saved[CITY_LONGITUDE]?.let { Place(latitude, it) } }
        )
    }

    override suspend fun write(notes: ForecastNotes) {
        store.edit { saved ->
            saved.clear()
            notes.forecast?.let { saved[HOURS] = written(it) }
            notes.fetchedAt?.let { saved[FETCHED_AT] = it.toString() }
            notes.failedAt?.let { saved[FAILED_AT] = it.toString() }
            notes.city?.let {
                saved[CITY_LATITUDE] = it.latitude
                saved[CITY_LONGITUDE] = it.longitude
            }
        }
    }

    internal companion object {
        private val HOURS = stringPreferencesKey("hours")
        private val FETCHED_AT = stringPreferencesKey("fetched_at")
        private val FAILED_AT = stringPreferencesKey("failed_at")
        private val CITY_LATITUDE = doublePreferencesKey("city_latitude")
        private val CITY_LONGITUDE = doublePreferencesKey("city_longitude")
        private const val FIELDS = 5

        /** One line per hour: its time, the temperature, the sky, the wind and its gusts. */
        fun written(forecast: Forecast): String = forecast.hours.joinToString("\n") {
            listOf(it.time, it.temperature, it.sky.name, it.wind, it.gusts).joinToString(";")
        }

        /** The forecast written by [written], or nothing if a single line cannot be read. */
        fun forecastIn(text: String): Forecast? {
            val hours = text.lines().map { line -> hourIn(line) ?: return null }
            return Forecast(hours).takeIf { hours.isNotEmpty() }
        }

        private fun hourIn(line: String): HourForecast? {
            val fields = line.split(";").takeIf { it.size == FIELDS } ?: return null
            val time = dateOrNull(fields[0])
            val sky = Sky.entries.firstOrNull { it.name == fields[2] }
            val numbers = listOf(fields[1], fields[3], fields[4]).mapNotNull(String::toIntOrNull)
            return if (time != null && sky != null && numbers.size == FIELDS - 2) {
                HourForecast(time, temperature = numbers[0], sky = sky, wind = numbers[1], gusts = numbers[2])
            } else {
                null
            }
        }

        private fun dateOrNull(text: String): LocalDateTime? = try {
            LocalDateTime.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
