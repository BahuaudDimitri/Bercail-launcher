package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlin.time.Duration.Companion.minutes
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

    @Test
    fun `le matin prend l'accent pêche, l'après-midi le bleu-vert, le soir et la nuit la lavande`() {
        assertThat(DayPart.Morning.moment).isEqualTo(BcMoment.Morning)
        assertThat(DayPart.Afternoon.moment).isEqualTo(BcMoment.Day)
        assertThat(DayPart.Evening.moment).isEqualTo(BcMoment.Evening)
        assertThat(DayPart.Night.moment).isEqualTo(BcMoment.Evening)
    }

    private val playing = HomeUiState(
        media = MediaState.Loaded(MediaContent("Looped", "Kiasmos", app = "Spotify", duration = 4.minutes), true)
    )

    @Test
    fun `Brume bouge pendant la lecture tant qu'aucune feuille ne la couvre`() {
        assertThat(playing.brumeMoves(searching = false)).isTrue()
        assertThat(playing.copy(media = MediaState.None).brumeMoves(searching = false)).isFalse()
    }

    @Test
    fun `une feuille ouverte fige Brume, pour que son verre soit givré tout de suite`() {
        assertThat(playing.copy(drawerExpanded = true).brumeMoves(searching = false)).isFalse()
        assertThat(playing.copy(dayOpen = true).brumeMoves(searching = false)).isFalse()
        assertThat(playing.copy(listenOpen = true).brumeMoves(searching = false)).isFalse()
    }

    @Test
    fun `la recherche ouverte fige Brume aussi, comme les autres feuilles`() {
        assertThat(playing.brumeMoves(searching = true)).isFalse()
    }

    @Test
    fun `sur l'écran Écoute, la feuille des gens n'est pas ouverte et ne fige pas Brume`() {
        assertThat(playing.copy(drawerExpanded = true, page = HomePage.Listen).brumeMoves(searching = false)).isTrue()
    }
}
