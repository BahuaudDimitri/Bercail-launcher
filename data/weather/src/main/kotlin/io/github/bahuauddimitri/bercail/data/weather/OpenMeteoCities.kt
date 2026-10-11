package io.github.bahuauddimitri.bercail.data.weather

import io.github.bahuauddimitri.bercail.core.domain.weather.City
import io.github.bahuauddimitri.bercail.core.domain.weather.CitySearch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONException
import org.json.JSONObject

/**
 * Cities found by their name with Open-Meteo's public search: no account, no key. Only the typed name is sent,
 * and only when the user asks for the search.
 */
class OpenMeteoCities(
    private val searchUrl: String = SEARCH,
    private val background: CoroutineDispatcher = Dispatchers.IO
) : CitySearch {
    override suspend fun find(name: String): List<City>? {
        val typed = name.trim()
        if (typed.isEmpty()) return emptyList()
        return runInterruptible(background) {
            try {
                read(typed)?.let(::citiesIn)
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            }
        }
    }

    private fun read(name: String): String? {
        val asked = "name=${URLEncoder.encode(name, Charsets.UTF_8)}&count=$MOST_CITIES&language=fr&format=json"
        val connection = URL("$searchUrl?$asked").openHttp()
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val answer = connection.inputStream.use { it.readNBytes(LONGEST_ANSWER + 1) }
            return answer.takeIf { it.size <= LONGEST_ANSWER }?.decodeToString()
        } finally {
            connection.disconnect()
        }
    }

    /** The cities of the answer, in its order. An answer without the list means that no city has that name. */
    private fun citiesIn(answer: String): List<City> {
        val results = JSONObject(answer).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map(results::getJSONObject).map { city ->
            val region = listOf("admin1", "country").map { city.optString(it) }.filter { it.isNotEmpty() }
            City(
                name = city.getString("name"),
                region = region.joinToString(", "),
                latitude = city.getDouble("latitude"),
                longitude = city.getDouble("longitude")
            )
        }
    }

    companion object {
        const val SEARCH = "https://geocoding-api.open-meteo.com/v1/search"
        private const val MOST_CITIES = 5

        /** Five cities weigh about 5 KB; anything much longer is not what was asked for. */
        private const val LONGEST_ANSWER = 128 * 1024
    }
}
