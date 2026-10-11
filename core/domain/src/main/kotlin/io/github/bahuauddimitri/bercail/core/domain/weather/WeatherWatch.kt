package io.github.bahuauddimitri.bercail.core.domain.weather

import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/** A point on the map, as the forecasts need it. */
data class Place(val latitude: Double, val longitude: Double)

/** A city the user chose for the weather, instead of the place where the phone is. [region] tells homonyms apart. */
data class City(val name: String, val region: String, val latitude: Double, val longitude: Double) {
    val place: Place get() = Place(latitude, longitude)
}

/** Finds cities by their name. */
interface CitySearch {
    /** The cities of that name, the best known first, or nothing when the search cannot be made. */
    suspend fun find(name: String): List<City>?
}

/** Where the phone is, roughly. */
interface PlaceSource {
    /** The approximate position of the phone, or nothing when it is not allowed or not known. */
    suspend fun current(): Place?
}

/** Where the forecasts come from. */
interface ForecastService {
    /** The forecast of the hours to come at [place], or nothing when it cannot be read. */
    suspend fun forecastAt(place: Place): Forecast?
}

/** What Bercail remembers of the weather between two looks. */
data class ForecastNotes(
    val forecast: Forecast? = null,
    /** When [forecast] was received. */
    val fetchedAt: LocalDateTime? = null,
    /** When the last request failed, if it is more recent than [fetchedAt]. */
    val failedAt: LocalDateTime? = null,
    /** The chosen city these notes are about, or nothing when they are about the place where the phone is. */
    val city: Place? = null
)

/** Keeps the [ForecastNotes] on the phone. */
interface ForecastJournal {
    suspend fun read(): ForecastNotes

    suspend fun write(notes: ForecastNotes)
}

/**
 * Bercail asks for a forecast once an hour at most. After a request that failed, it waits a quarter of an hour.
 * A clock that went back never blocks the next request.
 */
fun isForecastDue(notes: ForecastNotes, now: LocalDateTime): Boolean {
    val failed = notes.failedAt
    val last = failed ?: notes.fetchedAt
    val wait = if (failed != null) AFTER_A_FAILED_FORECAST else BETWEEN_FORECASTS
    return last == null || now < last || Duration.between(last, now) >= wait
}

/** A forecast is shown for six hours: after that, saying nothing is better than saying something stale. */
fun ForecastNotes.usableAt(now: LocalDateTime): Forecast? {
    val age = fetchedAt?.let { Duration.between(it, now) } ?: return null
    return forecast.takeIf { !age.isNegative && age < FORECAST_LIFE }
}

internal val BETWEEN_FORECASTS: Duration = Duration.ofHours(1)
internal val AFTER_A_FAILED_FORECAST: Duration = Duration.ofMinutes(15)
internal val FORECAST_LIFE: Duration = Duration.ofHours(6)

/**
 * The weather of the city the user chose, or of the place where the phone is. Everything happens while someone
 * listens, that is while the home screen is shown: the forecast kept on the phone comes at once, then a new one
 * is asked for if the last is an hour old. The clock's minutes carry it along: no timer, no background work.
 * Without a chosen city, nothing is asked before the user has agreed to share the approximate position.
 */
class WeatherWatch(
    clock: Clock,
    access: AccessSource,
    settings: SettingsSource,
    private val position: PlaceSource,
    private val service: ForecastService,
    private val journal: ForecastJournal,
    background: CoroutineContext = EmptyCoroutineContext,
    private val onFailure: (Exception) -> Unit = {}
) : WeatherSource {
    override val forecast: Flow<Forecast?> = combine(
        access.granted.map { Access.Location in it }.distinctUntilChanged(),
        settings.settings.map { it.weatherCity?.place }.distinctUntilChanged(),
        clock.minutes,
        ::Moment
    )
        .transform { (allowed, city, now) ->
            if (city == null && !allowed) return@transform emit(null)
            // Notes about another place say nothing of this one.
            val known = journal.read().takeIf { it.city == city } ?: ForecastNotes(city = city)
            emit(known.usableAt(now))
            if (isForecastDue(known, now)) {
                val fresh = asked(known, now)
                journal.write(fresh)
                emit(fresh.usableAt(now))
            }
        }
        // Whatever breaks unexpectedly, the home screen goes on without the weather.
        .catch { failure -> if (failure is Exception) emit(null).also { onFailure(failure) } else throw failure }
        .distinctUntilChanged()
        .flowOn(background)

    /** The notes after one request: a new forecast, or the old one with the time of the failure. */
    private suspend fun asked(known: ForecastNotes, now: LocalDateTime): ForecastNotes {
        val received = (known.city ?: position.current())?.let { service.forecastAt(it) }
        return if (received != null) {
            ForecastNotes(received, fetchedAt = now, city = known.city)
        } else {
            known.copy(failedAt = now)
        }
    }

    /** What decides of the weather at one moment: the user's agreement, the chosen city, the time. */
    private data class Moment(val allowed: Boolean, val city: Place?, val now: LocalDateTime)
}
