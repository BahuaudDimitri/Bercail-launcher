package io.github.bahuauddimitri.bercail.core.domain.apps

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import org.junit.Test

/** The dock of the band: the apps chosen in the settings, five at most, in the order they were touched. */
class DockTest {
    private val installed = listOf("Chrome", "Téléphone", "Beeper", "Appareil photo", "Agenda", "Spotify", "Waze")
        .map { App(id = it.lowercase(), name = it) }
    private val ids = installed.map { it.id }

    private fun Settings.touching(vararg apps: String) =
        apps.fold(this) { settings, app -> settings.withDockAppToggled(app, ids) }

    @Test
    fun `le dock garde les apps choisies dans l'ordre où on les a touchées`() {
        val dock = dockOf(installed, listOf("beeper", "chrome", "agenda"))

        assertThat(dock.map { it.name }).containsExactly("Beeper", "Chrome", "Agenda")
    }

    @Test
    fun `le dock ne tient que cinq apps`() {
        assertThat(MAX_DOCK_APPS).isEqualTo(5)
        val dock = dockOf(installed, ids)

        assertThat(dock.map { it.name }).containsExactly("Chrome", "Téléphone", "Beeper", "Appareil photo", "Agenda")
    }

    @Test
    fun `une app désinstallée sort du dock`() {
        val dock = dockOf(installed, listOf("désinstallée", "chrome"))

        assertThat(dock.map { it.name }).containsExactly("Chrome")
    }

    @Test
    fun `toucher une app l'ajoute au bout du dock, la retoucher l'enlève`() {
        val settings = Settings().touching("chrome", "beeper")
        assertThat(settings.dockApps).containsExactly("chrome", "beeper")

        assertThat(settings.touching("chrome").dockApps).containsExactly("beeper")
    }

    @Test
    fun `une sixième app ne rentre pas dans le dock`() {
        val full = Settings().touching("chrome", "téléphone", "beeper", "appareil photo", "agenda")

        assertThat(full.touching("spotify").dockApps).containsExactly(
            "chrome",
            "téléphone",
            "beeper",
            "appareil photo",
            "agenda"
        )
    }

    @Test
    fun `retirer une app du dock libère sa place pour une autre`() {
        val full = Settings().touching("chrome", "téléphone", "beeper", "appareil photo", "agenda")

        assertThat(full.touching("beeper", "spotify").dockApps)
            .containsExactly("chrome", "téléphone", "appareil photo", "agenda", "spotify")
    }

    @Test
    fun `une app désinstallée ne compte plus dans les cinq places`() {
        val full = Settings(dockApps = listOf("désinstallée", "chrome", "téléphone", "beeper", "agenda"))

        assertThat(full.touching("spotify").dockApps)
            .containsExactly("chrome", "téléphone", "beeper", "agenda", "spotify")
    }
}
