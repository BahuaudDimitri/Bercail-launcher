package io.github.bahuauddimitri.bercail.core.domain.access

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/** What the settings say about each thing the user lets Bercail read on the phone. */
class AccessTest {
    @Test
    fun `sans autorisation, la ligne de l'agenda dans les réglages invite à la donner d'un toucher`() {
        val line = agendaSourceLine(allowed = false)

        assertThat(line.title).isEqualTo("Agenda du téléphone")
        assertThat(line.detail).isEqualTo("Toucher pour autoriser Bercail à lire tes rendez-vous")
        assertThat(line.touchable).isTrue()
    }

    @Test
    fun `une fois l'autorisation donnée, la ligne de l'agenda le dit et ne demande plus rien`() {
        val line = agendaSourceLine(allowed = true)

        assertThat(line.title).isEqualTo("Agenda du téléphone")
        assertThat(line.detail).isEqualTo("Bercail lit tes rendez-vous, sans rien y changer")
        assertThat(line.touchable).isFalse()
    }

    @Test
    fun `sans position autorisée ni ville choisie, la ligne Météo invite à choisir un lieu`() {
        val line = weatherSourceLine(allowed = false)

        assertThat(line.title).isEqualTo("Lieu des prévisions")
        assertThat(line.detail).isEqualTo("Toucher pour choisir : ta position approximative ou une ville")
    }

    @Test
    fun `une fois la position autorisée, la ligne Météo dit d'où viennent les prévisions et à quel rythme`() {
        val line = weatherSourceLine(allowed = true)

        assertThat(line.detail)
            .isEqualTo("Ta position approximative · Open-Meteo, une fois par heure au plus")
        assertThat(line.touchable).isTrue()
    }

    @Test
    fun `avec une ville choisie, la ligne Météo la nomme, que la position soit autorisée ou non`() {
        assertThat(weatherSourceLine(allowed = false, city = "Nantes").detail)
            .isEqualTo("Nantes · Open-Meteo, une fois par heure au plus")
        assertThat(weatherSourceLine(allowed = true, city = "Nantes").detail)
            .isEqualTo("Nantes · Open-Meteo, une fois par heure au plus")
    }

    @Test
    fun `la ligne Météo se touche toujours, pour changer de lieu`() {
        assertThat(weatherSourceLine(allowed = true).touchable).isTrue()
        assertThat(weatherSourceLine(allowed = false).touchable).isTrue()
        assertThat(weatherSourceLine(allowed = false).detail)
            .isEqualTo("Toucher pour choisir : ta position approximative ou une ville")
    }
}
