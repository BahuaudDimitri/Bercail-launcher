package io.github.bahuauddimitri.bercail.core.testing

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.home.homeSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The fake house behaves like the prototype's: four controls, and a Cinéma scene that sets the room. */
class FakeHomeSourceTest {
    private val home = FakeHomeSource()

    private suspend fun labels() = home.controls.first().map { it.label }

    @Test
    fun `la maison factice démarre avec le salon allumé et les volets ouverts`() = runTest {
        assertThat(labels()).containsExactly("Salon 60 %", "Cinéma prête", "Volets ouverts", "Chauffage 18 °C")
    }

    @Test
    fun `toucher une lampe l'éteint, la retoucher la rallume`() = runTest {
        home.toggle("salon")
        assertThat(labels().first()).isEqualTo("Salon éteint")

        home.toggle("salon")
        assertThat(labels().first()).isEqualTo("Salon 60 %")
    }

    @Test
    fun `la scène Cinéma met le Salon à 10 pour cent et ferme les volets d'un seul toucher`() = runTest {
        home.toggle("cine")

        assertThat(labels()).containsExactly("Salon 10 %", "Cinéma active", "Volets fermés", "Chauffage 18 °C")
    }

    @Test
    fun `la scène Cinéma rallume le Salon s'il était éteint`() = runTest {
        home.toggle("salon")

        home.toggle("cine")

        assertThat(labels().first()).isEqualTo("Salon 10 %")
    }

    @Test
    fun `quitter la scène Cinéma rend au Salon sa luminosité`() = runTest {
        home.toggle("cine")

        home.toggle("cine")

        assertThat(labels()).containsExactly("Salon 60 %", "Cinéma prête", "Volets fermés", "Chauffage 18 °C")
    }

    @Test
    fun `le chauffage passe de 18 à 21 degrés`() = runTest {
        home.toggle("chauf")

        assertThat(labels().last()).isEqualTo("Chauffage 21 °C")
    }

    @Test
    fun `tout éteindre met la maison au repos`() = runTest {
        home.toggle("salon")
        home.toggle("volets")

        assertThat(homeSummary(home.controls.first())).isEqualTo("Maison au repos")
    }

    @Test
    fun `un identifiant inconnu ne change rien`() = runTest {
        home.toggle("grenier")

        assertThat(labels()).containsExactly("Salon 60 %", "Cinéma prête", "Volets ouverts", "Chauffage 18 °C")
    }
}
