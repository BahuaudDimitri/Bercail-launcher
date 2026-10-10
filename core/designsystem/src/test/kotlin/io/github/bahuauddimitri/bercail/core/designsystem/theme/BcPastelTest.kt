package io.github.bahuauddimitri.bercail.core.designsystem.theme

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import org.junit.Test

class BcPastelTest {
    @Test
    fun `les cinq pastels sont ceux du prototype`() {
        assertThat(BcPastel.entries.map { it.color }).containsExactlyInAnyOrder(*BcColors.pastels.toTypedArray())
    }

    @Test
    fun `une même personne garde toujours la même couleur`() {
        assertThat(BcPastel.forKey("!abc:beeper.com")).isEqualTo(BcPastel.forKey("!abc:beeper.com"))
    }

    @Test
    fun `les favoris prennent les pastels dans leur ordre, deux favoris n'ont jamais le même`() {
        val favorites = listOf("lea", "tom", "team", "mum")

        assertThat(favorites.map { BcPastel.forKey(it, ranked = favorites) })
            .isEqualTo(listOf(BcPastel.Coral, BcPastel.Blue, BcPastel.Mint, BcPastel.Lavender))
        assertThat(BcPastel.forKey("julien", ranked = favorites)).isEqualTo(BcPastel.forKey("julien"))
    }

    @Test
    fun `des personnes différentes se répartissent sur plusieurs couleurs`() {
        val colors = (1..20).map { BcPastel.forKey("room-$it") }.toSet()

        assertThat(colors.size).isGreaterThan(2)
    }
}
