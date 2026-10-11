package io.github.bahuauddimitri.bercail.data.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The latest release of Bercail as GitHub announces it, read from a server that plays GitHub's recorded answer. */
@RunWith(AndroidJUnit4::class)
class GitHubReleasesUiTest {
    private val gitHub = MockWebServer()
    private val recorded = checkNotNull(javaClass.getResource("/github/latest-release.json")).readText()

    @Before
    fun start() = gitHub.start()

    @After
    fun stop() = gitHub.close()

    private fun releases(packagesUnder: String = GitHubReleases.PACKAGES) =
        GitHubReleases(gitHub.url("/repos/bercail/releases/latest").toString(), packagesUnder)

    private fun gitHubAnswers(body: String, code: Int = 200) = gitHub.enqueue(MockResponse(code = code, body = body))

    private fun latest(body: String): Release? {
        gitHubAnswers(body)
        return runBlocking { releases().latest() }
    }

    @Test
    fun `la dernière version publiée est lue dans la réponse de GitHub, avec l'adresse de son paquet`() {
        val release = latest(recorded)

        assertThat(release).isNotNull()
        assertThat(release?.version).isEqualTo(Version(0, 8, 1))
        assertThat(release?.packageUrl).isEqualTo(
            "https://github.com/BahuaudDimitri/Bercail-launcher/releases/download/v0.8.1/bercail-0.8.1.apk"
        )
    }

    @Test
    fun `la taille et l'empreinte du paquet sont lues pour le vérifier après téléchargement`() {
        val release = latest(recorded)

        assertThat(release?.size).isEqualTo(2_419_727L)
        assertThat(release?.sha256).isEqualTo("388225494e48209b8b8f54b5bb2e0d89ffa59894278a9eeb25c2f1e718824969")
    }

    @Test
    fun `Bercail se présente à GitHub et ne lui envoie aucun identifiant`() {
        latest(recorded)

        val request = gitHub.takeRequest()
        assertThat(request.url.encodedPath).isEqualTo("/repos/bercail/releases/latest")
        assertThat(request.headers["User-Agent"]).isEqualTo("Bercail")
        assertThat(request.headers["Authorization"]).isNull()
        assertThat(request.headers["Cookie"]).isNull()
    }

    @Test
    fun `un paquet hébergé ailleurs que dans les releases de Bercail est ignoré`() {
        val elsewhere = recorded.replace(
            "https://github.com/BahuaudDimitri/Bercail-launcher/releases/download/",
            "https://example.test/download/"
        )

        assertThat(latest(elsewhere)).isNull()
    }

    @Test
    fun `une adresse de paquet en http simple est ignorée`() {
        val plain = recorded.replace("\"browser_download_url\": \"https://", "\"browser_download_url\": \"http://")

        assertThat(latest(plain)).isNull()
    }

    @Test
    fun `une version en préparation ou un brouillon ne compte pas`() {
        assertThat(latest(recorded.replace("\"prerelease\": false", "\"prerelease\": true"))).isNull()
        assertThat(latest(recorded.replace("\"draft\": false", "\"draft\": true"))).isNull()
    }

    @Test
    fun `une release sans paquet à installer ne compte pas`() {
        assertThat(latest(recorded.replace("bercail-0.8.1.apk\"", "bercail-0.8.1.zip\""))).isNull()
    }

    @Test
    fun `une étiquette qui n'est pas une version ne compte pas`() {
        assertThat(latest(recorded.replace("\"tag_name\": \"v0.8.1\"", "\"tag_name\": \"nightly\""))).isNull()
    }

    @Test
    fun `une empreinte absente ou mal formée est simplement inconnue`() {
        val without = recorded.replace(Regex("\"digest\": \"[^\"]*\""), "\"digest\": null")
        val odd = recorded.replace(Regex("\"digest\": \"[^\"]*\""), "\"digest\": \"md5:abc\"")

        assertThat(latest(without)?.sha256).isNull()
        assertThat(latest(odd)?.sha256).isNull()
    }

    @Test
    fun `une réponse illisible ne donne aucune version`() {
        assertThat(latest("<html>Trop de demandes</html>")).isNull()
        assertThat(latest("{}")).isNull()
    }

    @Test
    fun `une erreur de GitHub ne donne aucune version`() {
        gitHubAnswers(recorded, code = 403)
        assertThat(runBlocking { releases().latest() }).isNull()

        gitHubAnswers("", code = 500)
        assertThat(runBlocking { releases().latest() }).isNull()
    }

    @Test
    fun `sans réseau, aucune version, et aucune erreur`() {
        val releases = releases()
        gitHub.close()

        assertThat(runBlocking { releases.latest() }).isNull()
    }

    @Test
    fun `une réponse démesurée est abandonnée`() {
        val huge = "{\"body\": \"" + "x".repeat(2_000_000) + "\"}"

        assertThat(latest(huge)).isNull()
    }

    @Test
    fun `l'adresse lue par défaut est celle des releases de Bercail, en https`() {
        assertThat(GitHubReleases.LATEST)
            .isEqualTo("https://api.github.com/repos/BahuaudDimitri/Bercail-launcher/releases/latest")
        assertThat(GitHubReleases.PACKAGES)
            .isEqualTo("https://github.com/BahuaudDimitri/Bercail-launcher/releases/download/")
    }
}
