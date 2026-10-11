package io.github.bahuauddimitri.bercail.data.weather

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.ForecastNotes
import io.github.bahuauddimitri.bercail.core.domain.weather.HourForecast
import io.github.bahuauddimitri.bercail.core.domain.weather.Sky
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The last forecast is kept in a file: the weather is there at once when the home screen comes back. */
class StoredForecastTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "weather.preferences_pb") }
    private val morning = LocalDateTime.of(2026, 10, 2, 8, 10)
    private val forecast = Forecast(
        listOf(
            HourForecast(LocalDateTime.of(2026, 10, 2, 8, 0), temperature = 11, Sky.Clear, wind = 8, gusts = 15),
            HourForecast(LocalDateTime.of(2026, 10, 2, 9, 0), temperature = -2, Sky.LightSnow, wind = 35, gusts = 60)
        )
    )

    /** Opens the file as the app does at start, runs [block], then closes it as when the app stops. */
    private fun <T> withNotes(block: suspend (StoredForecast) -> T): T = runBlocking {
        val job = Job()
        val notes = StoredForecast(forecastStore(file, CoroutineScope(Dispatchers.IO + job)))
        try {
            block(notes)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `au premier lancement, Bercail n'a aucune prévision`() {
        assertThat(withNotes { it.read() }).isEqualTo(ForecastNotes())
    }

    @Test
    fun `une prévision notée se relit telle quelle, heure par heure`() {
        val read = withNotes {
            it.write(ForecastNotes(forecast, fetchedAt = morning))
            it.read()
        }

        assertThat(read).isEqualTo(ForecastNotes(forecast, fetchedAt = morning))
    }

    @Test
    fun `la prévision survit à un redémarrage de Bercail`() {
        withNotes { it.write(ForecastNotes(forecast, fetchedAt = morning)) }

        assertThat(withNotes { it.read() }.forecast).isEqualTo(forecast)
    }

    @Test
    fun `l'heure d'un échec est notée à côté de la prévision gardée`() {
        val failed = ForecastNotes(forecast, fetchedAt = morning, failedAt = morning.plusHours(1))
        withNotes { it.write(failed) }

        assertThat(withNotes { it.read() }).isEqualTo(failed)
    }

    @Test
    fun `un échec sans aucune prévision se note aussi`() {
        withNotes { it.write(ForecastNotes(failedAt = morning)) }

        val read = withNotes { it.read() }

        assertThat(read.forecast).isNull()
        assertThat(read.failedAt).isEqualTo(morning)
    }

    @Test
    fun `une nouvelle prévision remplace l'ancienne et efface l'échec`() {
        val newer = Forecast(forecast.hours.drop(1))
        withNotes { it.write(ForecastNotes(forecast, fetchedAt = morning, failedAt = morning.plusHours(1))) }

        withNotes { it.write(ForecastNotes(newer, fetchedAt = morning.plusHours(2))) }

        assertThat(withNotes { it.read() }).isEqualTo(ForecastNotes(newer, fetchedAt = morning.plusHours(2)))
    }

    @Test
    fun `un fichier abîmé est repris de zéro`() {
        file.writeText("ceci n'est pas un fichier de notes")

        assertThat(withNotes { it.read() }).isEqualTo(ForecastNotes())
    }

    @Test
    fun `une prévision écrite par une version qui connaissait d'autres ciels est oubliée, sans planter`() {
        withNotes { it.write(ForecastNotes(forecast, fetchedAt = morning)) }
        val text = StoredForecast.written(forecast).replace("Clear", "Aurora")

        assertThat(StoredForecast.forecastIn(text)).isNull()
        assertThat(StoredForecast.forecastIn("n'importe quoi")).isNull()
        assertThat(StoredForecast.forecastIn("")).isNull()
    }
}
