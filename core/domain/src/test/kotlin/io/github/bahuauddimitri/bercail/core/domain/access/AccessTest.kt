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

    @Test
    fun `sans l'accès aux notifications, la ligne Musique invite à le donner et nomme le droit comme Android`() {
        val line = mediaSourceLine(allowed = false)

        assertThat(line.title).isEqualTo("Musique en cours")
        assertThat(line.detail)
            .isEqualTo("Toucher pour autoriser · Android range ce droit sous « Accès aux notifications »")
        assertThat(line.touchable).isTrue()
    }

    @Test
    fun `une fois l'accès donné, la ligne Musique dit ce que Bercail en fait et ne demande plus rien`() {
        val line = mediaSourceLine(allowed = true)

        assertThat(
            line.detail
        ).isEqualTo("Bercail voit ce qui joue et sait qu'un message arrive, sans lire aucune notification")
        assertThat(line.touchable).isFalse()
    }

    @Test
    fun `sans l'accès à Beeper, la ligne Messages invite à le donner et dit ce que Bercail en fera`() {
        val line = messagesSourceLine(allowed = false)

        assertThat(line.title).isEqualTo("Messages de Beeper")
        assertThat(line.detail).isEqualTo("Toucher pour autoriser Bercail à lire tes conversations et à y répondre")
        assertThat(line.touchable).isTrue()
    }

    @Test
    fun `une fois Beeper relié, la ligne Messages dit que rien ne quitte le téléphone`() {
        val line = messagesSourceLine(allowed = true)

        assertThat(line.detail).isEqualTo("Bercail lit tes conversations dans Beeper, sur le téléphone seulement")
        assertThat(line.touchable).isFalse()
    }

    @Test
    fun `Beeper relié sans l'accès aux notifications, la ligne Messages dit comment voir un message dès son arrivée`() {
        val line = messagesSourceLine(allowed = true, toldOfArrivals = false)

        assertThat(line.detail)
            .isEqualTo("Relié · pour voir un message dès qu'il arrive, autorise aussi « Musique en cours », plus haut")
        assertThat(line.touchable).isFalse()
    }
}
