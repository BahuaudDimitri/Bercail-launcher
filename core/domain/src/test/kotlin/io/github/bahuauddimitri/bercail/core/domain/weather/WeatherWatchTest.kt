package io.github.bahuauddimitri.bercail.core.domain.weather

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.testing.FakeAccess
import io.github.bahuauddimitri.bercail.core.testing.FakeClock
import io.github.bahuauddimitri.bercail.core.testing.FakeForecastJournal
import io.github.bahuauddimitri.bercail.core.testing.FakeForecastService
import io.github.bahuauddimitri.bercail.core.testing.FakePlaceSource
import io.github.bahuauddimitri.bercail.core.testing.FakeSettingsSource
import io.github.bahuauddimitri.bercail.core.testing.forecastOn
import io.github.bahuauddimitri.bercail.core.testing.hourly
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * When Bercail asks for the weather: only while the home screen is shown, once an hour at most, and never before
 * the user has agreed to share the approximate position of the phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherWatchTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val day = LocalDate.of(2026, 10, 2)
    private val morning: LocalDateTime = day.atTime(8, 10)
    private val sunny = forecastOn(day) { hourly(11, Sky.Clear) }
    private val rainy = forecastOn(day) { hourly(9, Sky.Rain) }

    private val clock = FakeClock(morning)
    private val access = FakeAccess()
    private val position = FakePlaceSource(Place(47.22, -1.55))
    private val service = FakeForecastService(sunny)
    private val journal = FakeForecastJournal()
    private val failures = mutableListOf<Exception>()
    private val settings = FakeSettingsSource()
    private val nantes = City("Nantes", "Pays de la Loire, France", 47.21725, -1.55336)
    private val lyon = City("Lyon", "Auvergne-Rhône-Alpes, France", 45.74846, 4.84671)
    private val watch =
        WeatherWatch(clock, access, settings, position, service, journal, onFailure = { failures += it })

    /** What the home screen was told so far, in order. */
    private val seen = mutableListOf<Forecast?>()

    /** The home screen is shown: someone listens to the weather. */
    private fun TestScope.showHome(): Job = backgroundScope.launch(dispatcher) { watch.forecast.collect { seen += it } }

    // ---------- the user's agreement ----------

    @Test
    fun `sans le droit de connaître la position, rien n'est demandé et aucune météo n'est montrée`() =
        runTest(dispatcher) {
            access.refuse(Access.Location)

            showHome()

            assertThat(seen).containsExactly(null)
            assertThat(position.reads).isEqualTo(0)
            assertThat(service.asked).isEmpty()
        }

    @Test
    fun `même une prévision gardée n'est pas montrée si l'utilisateur a repris son accord`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(sunny, fetchedAt = morning.minusMinutes(10))
        access.refuse(Access.Location)

        showHome()

        assertThat(seen).containsExactly(null)
    }

    @Test
    fun `dès que la position est autorisée, la prévision est demandée`() = runTest(dispatcher) {
        access.refuse(Access.Location)
        showHome()

        access.grant(Access.Location)

        assertThat(seen.last()).isEqualTo(sunny)
        assertThat(service.asked).containsExactly(Place(47.22, -1.55))
    }

    // ---------- when the forecast is asked for ----------

    @Test
    fun `à l'affichage de l'accueil, sans prévision connue, Bercail en demande une là où est le téléphone`() =
        runTest(dispatcher) {
            showHome()

            assertThat(service.asked).containsExactly(Place(47.22, -1.55))
            assertThat(seen).containsExactly(null, sunny)
        }

    @Test
    fun `la prévision reçue est gardée sur le téléphone, avec son heure`() = runTest(dispatcher) {
        showHome()

        assertThat(journal.notes).isEqualTo(ForecastNotes(sunny, fetchedAt = morning))
    }

    @Test
    fun `une prévision de moins d'une heure n'est pas redemandée quand l'accueil revient`() = runTest(dispatcher) {
        showHome().cancel()
        clock.advanceMinutes(59)

        showHome()

        assertThat(service.asked).hasSize(1)
        assertThat(seen.last()).isEqualTo(sunny)
    }

    @Test
    fun `une prévision d'une heure est redemandée quand l'accueil revient`() = runTest(dispatcher) {
        showHome().cancel()
        clock.advanceMinutes(60)
        service.forecast = rainy

        showHome()

        assertThat(service.asked).hasSize(2)
        assertThat(seen.last()).isEqualTo(rainy)
    }

    @Test
    fun `accueil affiché sans interruption, la prévision est redemandée au bout d'une heure, pas avant`() =
        runTest(dispatcher) {
            showHome()

            repeat(59) { clock.advanceMinutes(1) }
            assertThat(service.asked).hasSize(1)

            clock.advanceMinutes(1)
            assertThat(service.asked).hasSize(2)
        }

    @Test
    fun `accueil caché, rien n'est demandé, même après des heures`() = runTest(dispatcher) {
        showHome().cancel()

        repeat(5) { clock.advanceMinutes(60) }

        assertThat(service.asked).hasSize(1)
    }

    @Test
    fun `la prévision gardée s'affiche tout de suite, la nouvelle la remplace à son arrivée`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(sunny, fetchedAt = morning.minusHours(2))
        service.forecast = rainy

        showHome()

        assertThat(seen).containsExactly(sunny, rainy)
    }

    @Test
    fun `si l'horloge a reculé, la prévision est redemandée`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(sunny, fetchedAt = morning.plusDays(1))

        showHome()

        assertThat(service.asked).hasSize(1)
        assertThat(journal.notes.fetchedAt).isEqualTo(morning)
    }

    // ---------- without network, without position ----------

    @Test
    fun `sans réseau, la dernière prévision reste montrée`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(sunny, fetchedAt = morning.minusHours(2))
        service.reachable = false

        showHome()

        assertThat(seen.last()).isEqualTo(sunny)
        assertThat(journal.notes.failedAt).isEqualTo(morning)
    }

    @Test
    fun `après un échec, Bercail attend un quart d'heure avant de redemander`() = runTest(dispatcher) {
        service.reachable = false
        showHome()
        assertThat(service.asked).hasSize(1)

        repeat(14) { clock.advanceMinutes(1) }
        assertThat(service.asked).hasSize(1)

        service.reachable = true
        clock.advanceMinutes(1)
        assertThat(service.asked).hasSize(2)
        assertThat(seen.last()).isEqualTo(sunny)
        assertThat(journal.notes.failedAt).isNull()
    }

    @Test
    fun `une prévision de plus de six heures n'est plus montrée`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(sunny, fetchedAt = morning.minusHours(6))
        service.reachable = false

        showHome()

        assertThat(seen.last()).isNull()
    }

    @Test
    fun `si le téléphone ne sait pas où il est, rien n'est demandé au service météo`() = runTest(dispatcher) {
        position.place = null

        showHome()

        assertThat(service.asked).isEmpty()
        assertThat(seen.last()).isNull()
        assertThat(journal.notes.failedAt).isEqualTo(morning)
    }

    @Test
    fun `une panne imprévue de la météo ne fait pas tomber l'accueil, elle laisse une trace`() = runTest(dispatcher) {
        service.breakdown = IllegalStateException("imprévu")

        showHome()

        assertThat(seen.last()).isNull()
        assertThat(failures).hasSize(1)
    }

    // ---------- the rules alone ----------

    @Test
    fun `une prévision est due après une heure, ou un quart d'heure après un échec`() {
        val fetched = ForecastNotes(sunny, fetchedAt = morning)
        val failed = fetched.copy(failedAt = morning.plusMinutes(70))

        assertThat(isForecastDue(ForecastNotes(), morning)).isTrue()
        assertThat(isForecastDue(fetched, morning.plusMinutes(59))).isFalse()
        assertThat(isForecastDue(fetched, morning.plusMinutes(60))).isTrue()
        assertThat(isForecastDue(failed, morning.plusMinutes(84))).isFalse()
        assertThat(isForecastDue(failed, morning.plusMinutes(85))).isTrue()
    }

    @Test
    fun `une prévision se montre pendant six heures, jamais si elle vient du futur`() {
        val notes = ForecastNotes(sunny, fetchedAt = morning)

        assertThat(notes.usableAt(morning.plusHours(5))).isNotNull()
        assertThat(notes.usableAt(morning.plusHours(6))).isNull()
        assertThat(notes.usableAt(morning.minusMinutes(1))).isNull()
        assertThat(ForecastNotes().usableAt(morning)).isNull()
    }

    // ---------- a city instead of the position ----------

    @Test
    fun `avec une ville choisie, la prévision est celle de la ville, et la position n'est pas lue`() =
        runTest(dispatcher) {
            settings.change { it.copy(weatherCity = nantes) }

            showHome()

            assertThat(service.asked).containsExactly(nantes.place)
            assertThat(position.reads).isEqualTo(0)
            assertThat(journal.notes.city).isEqualTo(nantes.place)
        }

    @Test
    fun `une ville choisie se passe de l'autorisation de position`() = runTest(dispatcher) {
        access.refuse(Access.Location)
        settings.change { it.copy(weatherCity = nantes) }

        showHome()

        assertThat(seen.last()).isEqualTo(sunny)
    }

    @Test
    fun `changer de ville redemande la prévision tout de suite, sans montrer celle de l'ancienne`() =
        runTest(dispatcher) {
            settings.change { it.copy(weatherCity = nantes) }
            showHome()
            service.forecast = rainy

            settings.change { it.copy(weatherCity = lyon) }

            assertThat(service.asked).containsExactly(nantes.place, lyon.place)
            assertThat(seen).containsExactly(null, sunny, null, rainy)
        }

    @Test
    fun `revenir à la position du téléphone redemande la prévision, là où est le téléphone`() = runTest(dispatcher) {
        settings.change { it.copy(weatherCity = lyon) }
        showHome()

        settings.change { it.copy(weatherCity = null) }

        assertThat(service.asked).containsExactly(lyon.place, Place(47.22, -1.55))
        assertThat(journal.notes.city).isNull()
    }

    @Test
    fun `la prévision d'une ville gardée sur le téléphone n'est montrée que pour cette ville`() = runTest(dispatcher) {
        journal.notes = ForecastNotes(rainy, fetchedAt = morning.minusMinutes(10), city = lyon.place)
        settings.change { it.copy(weatherCity = nantes) }

        showHome()

        assertThat(seen).containsExactly(null, sunny)
    }
}
