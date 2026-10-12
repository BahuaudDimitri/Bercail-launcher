package io.github.bahuauddimitri.bercail.data.weather

import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastService
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONException
import org.json.JSONObject

/**
 * The forecasts of Open-Meteo: one small request, no account, no key. Only a position rounded to a hundredth of
 * a degree (about one kilometre) is sent, and nothing that names the phone or its user.
 */
class OpenMeteo(
    private val forecastUrl: String = FORECAST,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val background: CoroutineDispatcher = Dispatchers.IO,
    private val onFailure: (Exception) -> Unit = {}
) : ForecastService {
    override suspend fun forecastAt(place: Place): Forecast? = runInterruptible(background) {
        try {
            forecastIn(read(place))
        } catch (failure: IOException) {
            unread(failure)
        } catch (failure: JSONException) {
            unread(failure)
        }
    }

    /** No forecast this time. Why is told to [onFailure]: otherwise a missing weather line explains nothing. */
    private fun unread(failure: Exception): Forecast? {
        onFailure(failure)
        return null
    }

    private fun read(place: Place): String {
        // Always a point for the decimals, whatever the language of the phone.
        val where = "latitude=%.2f&longitude=%.2f".format(Locale.ROOT, place.latitude, place.longitude)
        val connection = URL("$forecastUrl?$where&$WANTED").openHttp()
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Open-Meteo answered $code")
            val answer = connection.inputStream.use { it.readNBytes(LONGEST_ANSWER + 1) }
            if (answer.size > LONGEST_ANSWER) throw IOException("Open-Meteo's answer is too long")
            return answer.decodeToString()
        } finally {
            connection.disconnect()
        }
    }

    /** The hours of the answer, at the time of the phone. A list shorter than the others means no forecast. */
    private fun forecastIn(answer: String): Forecast {
        val hourly = JSONObject(answer).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val lists = listOf("temperature_2m", "weather_code", "wind_speed_10m", "wind_gusts_10m")
            .map(hourly::getJSONArray)
        if (times.length() == 0 || lists.any { it.length() != times.length() }) {
            throw JSONException("The hours and what is forecast for them do not match")
        }
        val (temperatures, codes, winds) = lists
        val gusts = lists.last()
        val zone = zone()
        return Forecast(
            (0 until times.length()).map { hour ->
                HourForecast(
                    time = Instant.ofEpochSecond(times.getLong(hour)).atZone(zone).toLocalDateTime(),
                    temperature = temperatures.getDouble(hour).roundToInt(),
                    sky = Sky.ofCode(codes.getInt(hour)),
                    wind = winds.getDouble(hour).roundToInt(),
                    gusts = gusts.getDouble(hour).roundToInt()
                )
            }
        )
    }

    companion object {
        const val FORECAST = "https://api.open-meteo.com/v1/forecast"

        /** Two days, hour by hour, with the hours as instants: the phone turns them into its own time. */
        private const val WANTED =
            "hourly=temperature_2m,weather_code,wind_speed_10m,wind_gusts_10m&forecast_days=2&timeformat=unixtime"

        /** Open-Meteo's answer weighs about 2 KB; anything much longer is not what was asked for. */
        private const val LONGEST_ANSWER = 64 * 1024
    }
}

/** A plain request that gives up after [TIMEOUT_MS] and names Bercail. */
internal fun URL.openHttp(): HttpURLConnection = (openConnection() as HttpURLConnection).apply {
    connectTimeout = TIMEOUT_MS
    readTimeout = TIMEOUT_MS
    setRequestProperty("User-Agent", "Bercail")
}

private const val TIMEOUT_MS = 15_000
