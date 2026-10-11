package io.github.bahuauddimitri.bercail.data.weather

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Cities found by their name, read from a server that plays Open-Meteo's recorded answer for « Nantes ». */
@RunWith(AndroidJUnit4::class)
class OpenMeteoCitiesUiTest {
    private val openMeteo = MockWebServer()
    private val recorded = checkNotNull(javaClass.getResource("/openmeteo/cities-nantes.json")).readText()

    @Before
    fun start() = openMeteo.start()

    @After
    fun stop() = openMeteo.close()

    private fun cities() = OpenMeteoCities(openMeteo.url("/v1/search").toString())

    private fun find(name: String, body: String, code: Int = 200): List<City>? {
        openMeteo.enqueue(MockResponse(code = code, body = body))
        return runBlocking { cities().find(name) }
    }

    @Test
    fun `les villes trouvées sont lues avec leur région et leur pays, la plus connue en tête`() {
        val found = find("Nantes", recorded)!!

        assertThat(found.first()).isEqualTo(City("Nantes", "Pays de la Loire, France", 47.21725, -1.55336))
        assertThat(found.map { "${it.name} (${it.region})" }).containsExactly(
            "Nantes (Pays de la Loire, France)",
            "Gilcrest (Colorado, États-Unis)",
            "Nantes-en-Ratier (Rhône-Alpes, France)",
            "Nantes (São Paulo, Brésil)",
            "Nantes (KwaZulu-Natal, Afrique du Sud)"
        )
    }

    @Test
    fun `Bercail demande cinq villes au plus, en français, et n'envoie que le nom tapé`() {
        find("Saint-Étienne", recorded)

        val request = openMeteo.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/v1/search")
        assertThat(request.url.queryParameter("name")).isEqualTo("Saint-Étienne")
        assertThat(request.url.queryParameter("count")).isEqualTo("5")
        assertThat(request.url.queryParameter("language")).isEqualTo("fr")
        assertThat(request.url.queryParameter("format")).isEqualTo("json")
        assertThat(request.url.querySize).isEqualTo(4)
        assertThat(request.headers["User-Agent"]).isEqualTo("Bercail")
        assertThat(request.headers["Cookie"]).isNull()
    }

    @Test
    fun `un nom que personne ne porte ne trouve aucune ville`() {
        assertThat(find("zzzzqqq", "{\"generationtime_ms\":0.2}")!!).isEmpty()
    }

    @Test
    fun `un nom vide ne part pas sur le réseau`() {
        assertThat(runBlocking { cities().find("   ") }!!).isEmpty()
        assertThat(openMeteo.requestCount).isEqualTo(0)
    }

    @Test
    fun `une ville sans région connue garde son pays`() {
        val bare = "{\"results\":[{\"name\":\"Monaco\",\"latitude\":43.73,\"longitude\":7.42,\"country\":\"Monaco\"}]}"

        assertThat(find("Monaco", bare)!!).containsExactly(City("Monaco", "Monaco", 43.73, 7.42))
    }

    @Test
    fun `sans réseau ou sur une erreur, la recherche dit qu'elle n'a pas pu se faire`() {
        assertThat(find("Nantes", "{}", code = 500)).isNull()
        assertThat(find("Nantes", "<html>Portail wifi</html>")).isNull()

        val unreachable = cities()
        openMeteo.close()
        assertThat(runBlocking { unreachable.find("Nantes") }).isNull()
    }
}
