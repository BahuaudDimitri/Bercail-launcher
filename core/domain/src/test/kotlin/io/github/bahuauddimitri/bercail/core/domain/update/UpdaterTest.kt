package io.github.bahuauddimitri.bercail.core.domain.update

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.testing.FakeClock
import io.github.bahuauddimitri.bercail.core.testing.FakeReleaseFeed
import io.github.bahuauddimitri.bercail.core.testing.FakeUpdateJournal
import io.github.bahuauddimitri.bercail.core.testing.FakeUpdatePackages
import io.github.bahuauddimitri.bercail.core.testing.release
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test

/**
 * Bercail updates itself: one look a day at its releases when the home screen shows, the package fetched while
 * it is visible, the installation once it is hidden.
 */
class UpdaterTest {
    private val clock = FakeClock(LocalDateTime.of(2026, 10, 12, 8, 0))
    private val feed = FakeReleaseFeed(published = release("0.8.1"))
    private val packages = FakeUpdatePackages()
    private val journal = FakeUpdateJournal()
    private var updater = updaterAt("0.8.1")

    private fun updaterAt(version: String) = Updater(Version.parse(version), feed, packages, journal, clock)

    private val status get() = updater.status.value

    private fun publish(version: String) {
        feed.published = release(version)
    }

    @Test
    fun `quand l'accueil s'affiche pour la première fois, Bercail lit la dernière version publiée`() = runBlocking {
        updater.onHomeShown()

        assertThat(feed.reads).isEqualTo(1)
        assertThat(journal.notes.checkedAt).isEqualTo(clock.now())
    }

    @Test
    fun `la lecture n'a lieu qu'une fois par jour, même si l'accueil s'affiche cent fois`() = runBlocking {
        repeat(100) {
            updater.onHomeShown()
            clock.advanceMinutes(10)
        }

        assertThat(feed.reads).isEqualTo(1)
    }

    @Test
    fun `le lendemain, la lecture a lieu de nouveau`() = runBlocking {
        updater.onHomeShown()
        clock.advanceMinutes(24 * 60)

        updater.onHomeShown()

        assertThat(feed.reads).isEqualTo(2)
    }

    @Test
    fun `si la lecture échoue, Bercail réessaie une heure plus tard, pas avant`() = runBlocking {
        feed.reachable = false
        updater.onHomeShown()
        assertThat(status).isEqualTo(UpdateStatus.UpToDate(UpdateTrouble.Unreachable))

        clock.advanceMinutes(30)
        updater.onHomeShown()
        assertThat(feed.reads).isEqualTo(1)

        clock.advanceMinutes(30)
        feed.reachable = true
        updater.onHomeShown()
        assertThat(feed.reads).isEqualTo(2)
        assertThat(status).isEqualTo(UpdateStatus.UpToDate())
    }

    @Test
    fun `quand Bercail est à jour, rien n'est téléchargé`() = runBlocking {
        updater.onHomeShown()

        assertThat(status).isEqualTo(UpdateStatus.UpToDate())
        assertThat(packages.downloads).isEmpty()
    }

    @Test
    fun `une version publiée plus ancienne que celle du téléphone est ignorée`() = runBlocking {
        publish("0.7.0")

        updater.onHomeShown()

        assertThat(status).isEqualTo(UpdateStatus.UpToDate())
        assertThat(packages.downloads).isEmpty()
        assertThat(journal.notes.found).isNull()
    }

    @Test
    fun `une version plus récente est téléchargée, puis prête`() = runBlocking {
        publish("0.9.0")

        updater.onHomeShown()

        assertThat(packages.downloads).containsExactly(Version(0, 9, 0))
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
    }

    @Test
    fun `un paquet déjà sur le téléphone n'est pas téléchargé une seconde fois`() = runBlocking {
        publish("0.9.0")
        updater.onHomeShown()

        clock.advanceMinutes(5)
        updater.onHomeShown()
        clock.advanceMinutes(24 * 60)
        updater.onHomeShown()

        assertThat(packages.downloads).containsExactly(Version(0, 9, 0))
    }

    @Test
    fun `un téléchargement raté est retenté une heure plus tard, pas à chaque retour à l'accueil`() = runBlocking {
        publish("0.9.0")
        packages.reachable = false
        updater.onHomeShown()
        assertThat(status).isEqualTo(UpdateStatus.Waiting(Version(0, 9, 0)))

        clock.advanceMinutes(20)
        updater.onHomeShown()
        assertThat(packages.downloads.size).isEqualTo(1)

        clock.advanceMinutes(40)
        packages.reachable = true
        updater.onHomeShown()
        assertThat(packages.downloads.size).isEqualTo(2)
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
        // The releases themselves were only read once that day.
        assertThat(feed.reads).isEqualTo(1)
    }

    @Test
    fun `rien ne s'installe tant que l'accueil est affiché`() = runBlocking {
        publish("0.9.0")

        updater.onHomeShown()

        assertThat(packages.installs).isEmpty()
    }

    @Test
    fun `la version prête s'installe quand l'accueil est caché, sans rien demander`() = runBlocking {
        publish("0.9.0")
        updater.onHomeShown()

        updater.onHomeHidden()

        assertThat(packages.installs).containsExactly("0.9.0 without asking")
    }

    @Test
    fun `quitter l'accueil sans version prête n'installe rien`() = runBlocking {
        updater.onHomeShown()

        updater.onHomeHidden()

        assertThat(packages.installs).isEmpty()
    }

    @Test
    fun `tant qu'Android n'autorise pas Bercail à installer, la version prête attend un toucher`() = runBlocking {
        publish("0.9.0")
        packages.allowedToInstall = false

        updater.onHomeShown()
        updater.onHomeHidden()

        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0), needsUser = true))
        assertThat(packages.installs).isEmpty()
    }

    @Test
    fun `une fois l'autorisation donnée, la version prête s'installe seule quand l'accueil est caché`() = runBlocking {
        publish("0.9.0")
        packages.allowedToInstall = false
        updater.onHomeShown()
        updater.onHomeHidden()

        packages.allowedToInstall = true
        updater.onHomeShown()
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
        updater.onHomeHidden()

        assertThat(packages.installs).containsExactly("0.9.0 without asking")
    }

    @Test
    fun `si Android veut un accord, Bercail n'insiste pas et attend qu'on touche la ligne`() = runBlocking {
        publish("0.9.0")
        packages.answer = InstallAnswer.NeedsUser
        updater.onHomeShown()

        updater.onHomeHidden()
        updater.onHomeShown()
        updater.onHomeHidden()

        assertThat(packages.installs).containsExactly("0.9.0 without asking")
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0), needsUser = true))
    }

    @Test
    fun `toucher la ligne d'une version prête l'installe tout de suite, en demandant l'accord`() = runBlocking {
        publish("0.9.0")
        updater.onHomeShown()

        updater.proceed()

        assertThat(packages.installs).containsExactly("0.9.0 asking the user")
    }

    @Test
    fun `si l'accord est refusé, la version reste prête et attend un nouveau toucher`() = runBlocking {
        publish("0.9.0")
        packages.answer = InstallAnswer.Declined
        updater.onHomeShown()

        updater.proceed()
        updater.onHomeHidden()

        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0), needsUser = true))
        assertThat(packages.installs).containsExactly("0.9.0 asking the user")
    }

    @Test
    fun `si Android refuse le paquet, il est supprimé et oublié jusqu'à la prochaine lecture`() = runBlocking {
        publish("0.9.0")
        packages.answer = InstallAnswer.Refused
        updater.onHomeShown()

        updater.onHomeHidden()
        clock.advanceMinutes(5)
        updater.onHomeShown()
        updater.onHomeHidden()

        assertThat(packages.installs).containsExactly("0.9.0 without asking")
        assertThat(packages.has(release("0.9.0"))).isEqualTo(false)
        assertThat(journal.notes.found).isNull()
        assertThat(status).isEqualTo(UpdateStatus.UpToDate(UpdateTrouble.Refused))
    }

    @Test
    fun `après un refus d'Android, toucher la ligne relit les versions et retélécharge`() = runBlocking {
        publish("0.9.0")
        packages.answer = InstallAnswer.Refused
        updater.onHomeShown()
        updater.onHomeHidden()

        updater.proceed()

        assertThat(feed.reads).isEqualTo(2)
        assertThat(packages.downloads.size).isEqualTo(2)
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
    }

    @Test
    fun `toucher la ligne quand tout est à jour relit les versions, même le même jour`() = runBlocking {
        updater.onHomeShown()
        publish("0.9.0")

        updater.proceed()

        assertThat(feed.reads).isEqualTo(2)
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
    }

    @Test
    fun `toucher la ligne d'une version disponible la télécharge sans attendre une heure`() = runBlocking {
        publish("0.9.0")
        packages.reachable = false
        updater.onHomeShown()

        packages.reachable = true
        updater.proceed()

        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
    }

    @Test
    fun `une fois la nouvelle version installée, son paquet est supprimé du téléphone`() = runBlocking {
        publish("0.9.0")
        updater.onHomeShown()
        updater.onHomeHidden()

        // Android restarts Bercail in its new version.
        updater = updaterAt("0.9.0")
        updater.onHomeShown()

        assertThat(packages.cleared).isEqualTo(1)
        assertThat(journal.notes.found).isNull()
        assertThat(status).isEqualTo(UpdateStatus.UpToDate())
    }

    @Test
    fun `après un redémarrage, la version déjà téléchargée est toujours prête`() = runBlocking {
        publish("0.9.0")
        updater.onHomeShown()

        updater = updaterAt("0.8.1")
        updater.onHomeShown()

        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
        assertThat(feed.reads).isEqualTo(1)
        assertThat(packages.downloads.size).isEqualTo(1)
    }

    @Test
    fun `quitter l'accueil pendant le téléchargement l'interrompt, il reprend au retour`() = runBlocking {
        publish("0.9.0")
        packages.slowNetwork = CompletableDeferred()
        val shown = launch { updater.onHomeShown() }
        yield()
        assertThat(status).isEqualTo(UpdateStatus.Downloading(Version(0, 9, 0)))

        shown.cancelAndJoin()
        updater.onHomeHidden()
        assertThat(status).isEqualTo(UpdateStatus.Waiting(Version(0, 9, 0)))
        assertThat(packages.installs).isEmpty()

        packages.slowNetwork = null
        updater.onHomeShown()
        assertThat(status).isEqualTo(UpdateStatus.Ready(Version(0, 9, 0)))
    }

    @Test
    fun `deux demandes en même temps ne lancent qu'une lecture`() = runBlocking {
        publish("0.9.0")
        packages.slowNetwork = CompletableDeferred()
        val shown = launch { updater.onHomeShown() }
        yield()

        updater.proceed()
        packages.slowNetwork?.complete(Unit)
        shown.join()

        assertThat(feed.reads).isEqualTo(1)
        assertThat(packages.downloads.size).isEqualTo(1)
    }

    @Test
    fun `la version de mise au point ne cherche jamais de mise à jour`() = runBlocking {
        updater = updaterAt("0.0.0-dev")
        publish("0.9.0")

        updater.onHomeShown()
        updater.proceed()
        updater.onHomeHidden()

        assertThat(status).isEqualTo(UpdateStatus.Off)
        assertThat(feed.reads).isEqualTo(0)
        assertThat(packages.installs).isEmpty()
    }
}
