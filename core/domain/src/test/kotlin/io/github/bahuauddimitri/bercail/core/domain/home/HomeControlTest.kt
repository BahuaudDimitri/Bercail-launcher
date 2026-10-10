package io.github.bahuauddimitri.bercail.core.domain.home

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/** One wording per device, the same in the summary, on its tile and in the search. */
class HomeControlTest {
    private val living = HomeControl.Light("salon", "Salon", brightness = 60, on = true)
    private val cinema = HomeControl.Scene("cine", "Cinéma", active = false)
    private val shutters = HomeControl.Shutters("volets", "Volets", open = true)
    private val heating = HomeControl.Heating("chauf", "Chauffage", target = 18, comfort = false)

    @Test
    fun `une lampe allumée donne sa luminosité`() {
        assertThat(living.label).isEqualTo("Salon 60 %")
    }

    @Test
    fun `une lampe éteinte se dit éteinte`() {
        assertThat(living.copy(on = false).label).isEqualTo("Salon éteint")
        assertThat(living.copy(on = false).isOn).isFalse()
    }

    @Test
    fun `une scène est prête ou active`() {
        assertThat(cinema.label).isEqualTo("Cinéma prête")
        assertThat(cinema.copy(active = true).label).isEqualTo("Cinéma active")
        assertThat(cinema.copy(active = true).isOn).isTrue()
    }

    @Test
    fun `les volets sont ouverts ou fermés`() {
        assertThat(shutters.label).isEqualTo("Volets ouverts")
        assertThat(shutters.copy(open = false).label).isEqualTo("Volets fermés")
        assertThat(shutters.copy(open = false).isOn).isFalse()
    }

    @Test
    fun `le chauffage donne sa température`() {
        assertThat(heating.label).isEqualTo("Chauffage 18 °C")
        assertThat(heating.copy(target = 21, comfort = true).label).isEqualTo("Chauffage 21 °C")
        assertThat(heating.isOn).isFalse()
        assertThat(heating.copy(comfort = true).isOn).isTrue()
    }

    @Test
    fun `le résumé de la maison liste ce qui est allumé`() {
        val summary = homeSummary(listOf(living, cinema, shutters, heating))

        assertThat(summary).isEqualTo("Salon 60 %, Volets ouverts")
    }

    @Test
    fun `rien d'allumé, la maison est au repos`() {
        val summary = homeSummary(listOf(living.copy(on = false), cinema, shutters.copy(open = false), heating))

        assertThat(summary).isEqualTo("Maison au repos")
    }
}
