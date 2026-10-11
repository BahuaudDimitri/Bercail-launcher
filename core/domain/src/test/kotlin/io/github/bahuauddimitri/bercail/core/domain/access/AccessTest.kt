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
    fun `sans autorisation, la ligne Météo dit ce qui sera envoyé, et à qui`() {
        val line = weatherSourceLine(allowed = false)

        assertThat(line.title).isEqualTo("Lieu des prévisions")
        assertThat(line.detail)
            .isEqualTo("Toucher pour autoriser ta position approximative, envoyée arrondie à Open-Meteo")
        assertThat(line.touchable).isTrue()
    }

    @Test
    fun `une fois la position autorisée, la ligne Météo dit d'où viennent les prévisions et à quel rythme`() {
        val line = weatherSourceLine(allowed = true)

        assertThat(line.detail)
            .isEqualTo("Ta position approximative · Open-Meteo, une fois par heure au plus")
        assertThat(line.touchable).isFalse()
    }
}
