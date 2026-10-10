package io.github.bahuauddimitri.bercail.core.testing

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.search.appDirectory
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The fake phone: its apps, and the places it can be sent to. */
class FakePhoneTest {
    @Test
    fun `le téléphone factice a les 27 apps du prototype, de Agenda à YouTube`() = runTest {
        val apps = FakeWorld().apps.apps.first()

        assertThat(apps).hasSize(27)
        assertThat(appDirectory(apps, emptyList()).letters.first()).isEqualTo("A")
        assertThat(appDirectory(apps, emptyList()).letters.last()).isEqualTo("Y")
    }

    @Test
    fun `les apps favorites factices sont Spotify, YouTube, Beeper et Agenda`() = runTest {
        val world = FakeWorld()

        val directory = appDirectory(world.apps.apps.first(), world.settings.settings.first().favoriteApps)

        assertThat(directory.favorites.map { it.name }).containsExactly("Spotify", "YouTube", "Beeper", "Agenda")
    }

    @Test
    fun `le téléphone factice retient les apps ouvertes`() {
        val apps = FakeAppsSource(Samples.apps)

        apps.open("spotify")
        apps.open("agenda")

        assertThat(apps.opened).containsExactly("spotify", "agenda")
    }

    @Test
    fun `une app installée ou désinstallée change la liste`() = runTest {
        val apps = FakeAppsSource(listOf(App("a", "Agenda")))

        apps.install(App("s", "Signal"))
        assertThat(apps.apps.first().map { it.name }).containsExactly("Agenda", "Signal")

        apps.uninstall("a")
        assertThat(apps.apps.first().map { it.name }).containsExactly("Signal")
    }

    @Test
    fun `les apps factices n'ont pas d'icône, sauf celles qu'on peint`() = runTest {
        val painted = FakeAppsSource(Samples.apps).apply { paintIcon("agenda", color = 7, size = 2) }

        assertThat(painted.icon("agenda")!!.pixels.toList()).containsExactly(7, 7, 7, 7)
        assertThat(painted.iconRequests).containsExactly("agenda")
        assertThat(FakeAppsSource(Samples.apps).icon("spotify")).isNull()
    }

    @Test
    fun `le téléphone factice retient où on l'envoie`() {
        val phone = FakePhone()

        phone.searchWeb("ramen")
        phone.searchStore("ramen")
        phone.openSettings()
        phone.openHomeScreenSettings()

        assertThat(phone.visits).containsExactly("web: ramen", "store: ramen", "settings", "home screen settings")
    }

    @Test
    fun `un réglage changé reste changé`() = runTest {
        val settings = FakeSettingsSource()

        settings.update { it.copy(timeline = TimelineOrientation.Horizontal) }

        assertThat(settings.settings.first().timeline).isEqualTo(TimelineOrientation.Horizontal)
    }
}
