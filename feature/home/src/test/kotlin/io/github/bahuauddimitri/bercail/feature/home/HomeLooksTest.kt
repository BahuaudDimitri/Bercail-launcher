package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.testing.Samples
import org.junit.Test

/** How the things of the domain look on the home screen. */
class HomeLooksTest {
    private val favorites = listOf("lea", "tom", "team", "mum")

    @Test
    fun `chaque favori a son pastel, dans l'ordre des favoris`() {
        val pastels = favorites.map { pastelOf(it, favorites) }

        assertThat(pastels).containsExactly(BcPastel.Coral, BcPastel.Blue, BcPastel.Mint, BcPastel.Lavender)
    }

    @Test
    fun `une personne hors favoris garde toujours le même pastel`() {
        assertThat(pastelOf("julien", favorites)).isEqualTo(pastelOf("julien", favorites))
        assertThat(pastelOf("julien", favorites)).isEqualTo(BcPastel.forKey("julien"))
    }

    @Test
    fun `chaque commande maison a son icône et son pastel`() {
        assertThat(Samples.home.map { it.icon })
            .containsExactly(BcIcons.Lamp, BcIcons.Film, BcIcons.Shutters, BcIcons.Flame)
        assertThat(Samples.home.map { it.pastel })
            .containsExactly(BcPastel.Apricot, BcPastel.Lavender, BcPastel.Blue, BcPastel.Coral)
    }

    @Test
    fun `sans pochette, Brume prend les couleurs du moment`() {
        assertThat(DayPart.Morning.brume).isEqualTo(BcBrumeColors.Morning)
        assertThat(DayPart.Afternoon.brume).isEqualTo(BcBrumeColors.Day)
        assertThat(DayPart.Evening.brume).isEqualTo(BcBrumeColors.Evening)
        assertThat(DayPart.Night.brume).isEqualTo(BcBrumeColors.Evening)
    }
}
