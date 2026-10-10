package io.github.bahuauddimitri.bercail.core.domain.screen

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.media.CoverColors
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/** Which of the two screens (Accueil, Écoute) may be shown, and what is behind them. */
class HomeScreenRulesTest {
    private val colors = CoverColors(1, 2)
    private val looped = MediaContent("Looped", "Kiasmos", "Spotify", 245.seconds, colors)
    private val playing = MediaState.Loaded(looped, playing = true)
    private val paused = MediaState.Loaded(looped, playing = false)

    @Test
    fun `l'écran Écoute est accessible en lecture et en pause`() {
        assertThat(HomePage.Listen.allowedWith(playing)).isEqualTo(HomePage.Listen)
        assertThat(HomePage.Listen.allowedWith(paused)).isEqualTo(HomePage.Listen)
    }

    @Test
    fun `sans contenu, l'écran Écoute n'existe pas`() {
        assertThat(HomePage.Listen.allowedWith(MediaState.None)).isEqualTo(HomePage.Home)
    }

    @Test
    fun `l'Accueil est toujours accessible`() {
        assertThat(HomePage.Home.allowedWith(MediaState.None)).isEqualTo(HomePage.Home)
        assertThat(HomePage.Home.allowedWith(playing)).isEqualTo(HomePage.Home)
    }

    @Test
    fun `avec un contenu chargé, le fond est Brume aux couleurs de la pochette`() {
        assertThat(backgroundFor(playing, IdleBackground.Wallpaper)).isEqualTo(HomeBackground.Brume(colors))
        assertThat(backgroundFor(paused, IdleBackground.Wallpaper)).isEqualTo(HomeBackground.Brume(colors))
    }

    @Test
    fun `sans contenu, le fond est l'image de l'utilisateur`() {
        assertThat(backgroundFor(MediaState.None, IdleBackground.Wallpaper)).isEqualTo(HomeBackground.Wallpaper)
    }

    @Test
    fun `sans contenu, Brume peut rester aux couleurs du moment si le réglage le demande`() {
        assertThat(backgroundFor(MediaState.None, IdleBackground.Brume)).isEqualTo(HomeBackground.Brume(cover = null))
    }
}
