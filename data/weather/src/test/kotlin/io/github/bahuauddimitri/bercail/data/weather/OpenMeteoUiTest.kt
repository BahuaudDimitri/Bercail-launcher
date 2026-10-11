package io.github.bahuauddimitri.bercail.data.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Place
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The forecast as Open-Meteo gives it, read from a server that plays its recorded answer for Paris. */
@RunWith(AndroidJUnit4::class)
class OpenMeteoUiTest {
    private val openMeteo = MockWebServer()
    private val recorded = checkNotNull(javaClass.getResource("/openmeteo/forecast-paris.json")).readText()
    private val nantes = Place(47.21725, -1.55336)
    private val locale = Locale.getDefault()

    @Before
    fun start() = openMeteo.start()

    @After
    fun stop() {
        openMeteo.close()
        Locale.setDefault(locale)
    }

    private fun service(zone: String = "Europe/Paris") =
        OpenMeteo(openMeteo.url("/v1/forecast").toString(), zone = { ZoneId.of(zone) })

    private fun forecast(body: String, code: Int = 200, zone: String = "Europe/Paris"): Forecast? {
        openMeteo.enqueue(MockResponse(code = code, body = body))
        return runBlocking { service(zone).forecastAt(nantes) }
    }

    @Test
    fun `la prévision est lue heure par heure, sur deux jours`() {
        val forecast = forecast(recorded)

        assertThat(forecast).isNotNull()
        assertThat(forecast!!.hours).hasSize(48)
    }

    @Test
    fun `chaque heure donne la température, le ciel, le vent et ses rafales`() {
        val first = forecast(recorded)!!.hours.first()

        assertThat(first).isEqualTo(
            HourForecast(LocalDateTime.of(2026, 10, 11, 2, 0), temperature = 13, Sky.Showers, wind = 12, gusts = 25)
        )
    }

    @Test
    fun `les températures et les vitesses sont arrondies à l'unité`() {
        val second = forecast(recorded)!!.hours[1]

        // Open-Meteo says 12.6 °C, wind 11.7 km/h, gusts 25.2 km/h.
        assertThat(second.temperature).isEqualTo(13)
        assertThat(second.wind).isEqualTo(12)
        assertThat(second.gusts).isEqualTo(25)
    }

    @Test
    fun `les heures sont celles du téléphone, où qu'il soit`() {
        assertThat(forecast(recorded, zone = "Asia/Tokyo")!!.hours.first().time)
            .isEqualTo(LocalDateTime.of(2026, 10, 11, 9, 0))
        assertThat(forecast(recorded, zone = "America/New_York")!!.hours.first().time)
            .isEqualTo(LocalDateTime.of(2026, 10, 10, 20, 0))
    }

    @Test
    fun `Bercail demande deux jours, heure par heure, pour une position arrondie au centième de degré`() {
        forecast(recorded)

        val asked = openMeteo.takeRequest().url
        assertThat(asked.encodedPath).isEqualTo("/v1/forecast")
        assertThat(asked.queryParameter("latitude")).isEqualTo("47.22")
        assertThat(asked.queryParameter("longitude")).isEqualTo("-1.55")
        assertThat(asked.queryParameter("hourly"))
            .isEqualTo("temperature_2m,weather_code,wind_speed_10m,wind_gusts_10m")
        assertThat(asked.queryParameter("forecast_days")).isEqualTo("2")
        assertThat(asked.queryParameter("timeformat")).isEqualTo("unixtime")
        assertThat(asked.querySize).isEqualTo(5)
    }

    @Test
    fun `la position s'écrit avec un point, même sur un téléphone en français`() {
        Locale.setDefault(Locale.FRANCE)

        forecast(recorded)

        assertThat(openMeteo.takeRequest().url.queryParameter("latitude")).isEqualTo("47.22")
    }

    @Test
    fun `Bercail se présente à Open-Meteo et ne lui envoie aucun identifiant`() {
        forecast(recorded)

        val request = openMeteo.takeRequest()
        assertThat(request.headers["User-Agent"]).isEqualTo("Bercail")
        assertThat(request.headers["Authorization"]).isNull()
        assertThat(request.headers["Cookie"]).isNull()
    }

    @Test
    fun `quand Open-Meteo répond par une erreur, il n'y a pas de prévision`() {
        assertThat(forecast("{}", code = 500)).isNull()
        assertThat(forecast("{\"error\":true,\"reason\":\"Too many requests\"}", code = 429)).isNull()
    }

    @Test
    fun `sans réseau, il n'y a pas de prévision`() {
        val unreachable = service()
        openMeteo.close()

        assertThat(runBlocking { unreachable.forecastAt(nantes) }).isNull()
    }

    @Test
    fun `une réponse qui n'est pas une prévision est ignorée`() {
        assertThat(forecast("<html>Portail wifi</html>")).isNull()
        assertThat(forecast("{}")).isNull()
        assertThat(forecast("{\"hourly\":{\"time\":[]}}")).isNull()
    }

    @Test
    fun `une prévision à trous est ignorée plutôt que devinée`() {
        val shorter = recorded.replace("\"weather_code\":[80,", "\"weather_code\":[")
        val missing = recorded.replace("\"temperature_2m\":[13.0,", "\"temperature_2m\":[null,")

        assertThat(forecast(shorter)).isNull()
        assertThat(forecast(missing)).isNull()
    }

    @Test
    fun `une réponse démesurée est ignorée`() {
        assertThat(forecast(recorded + " ".repeat(300_000))).isNull()
    }
}
