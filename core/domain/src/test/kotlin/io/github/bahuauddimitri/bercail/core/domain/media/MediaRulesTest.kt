package io.github.bahuauddimitri.bercail.core.domain.media

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/** The three media states (none, paused, playing) and the remote's rules. */
class MediaRulesTest {
    private val looped = MediaContent("Looped", "Kiasmos", "Spotify", 245.seconds, CoverColors(1, 2))

    @Test
    fun `sans session média, rien n'est chargé`() {
        assertThat(MediaState.None.isLoaded).isFalse()
        assertThat(MediaState.None.isPlaying).isFalse()
    }

    @Test
    fun `en pause, un contenu est chargé mais ne joue pas`() {
        val paused = MediaState.Loaded(looped, playing = false)

        assertThat(paused.isLoaded).isTrue()
        assertThat(paused.isPlaying).isFalse()
    }

    @Test
    fun `en lecture, un contenu est chargé et joue`() {
        val playing = MediaState.Loaded(looped, playing = true)

        assertThat(playing.isLoaded).isTrue()
        assertThat(playing.isPlaying).isTrue()
    }

    @Test
    fun `Précédent revient au début si la position dépasse 5 secondes`() {
        assertThat(previousAction(position = 72.seconds)).isEqualTo(PreviousAction.Restart)
        assertThat(previousAction(position = 6.seconds)).isEqualTo(PreviousAction.Restart)
    }

    @Test
    fun `Précédent passe au morceau d'avant dans les 5 premières secondes`() {
        assertThat(previousAction(position = 5.seconds)).isEqualTo(PreviousAction.PreviousTrack)
        assertThat(previousAction(position = 0.seconds)).isEqualTo(PreviousAction.PreviousTrack)
    }

    @Test
    fun `le temps d'un morceau s'écrit en minutes et secondes`() {
        assertThat(72.seconds.asTrackTime()).isEqualTo("1:12")
        assertThat(245.seconds.asTrackTime()).isEqualTo("4:05")
        assertThat(0.seconds.asTrackTime()).isEqualTo("0:00")
        assertThat(1080.seconds.asTrackTime()).isEqualTo("18:00")
    }

    @Test
    fun `la progression est la part du morceau déjà jouée`() {
        assertThat(progress(position = 61.seconds, duration = 244.seconds)).isEqualTo(0.25f)
        assertThat(progress(position = 300.seconds, duration = 244.seconds)).isEqualTo(1f)
    }

    @Test
    fun `sans durée connue, la progression reste à zéro`() {
        assertThat(progress(position = 10.seconds, duration = 0.seconds)).isEqualTo(0f)
    }

    @Test
    fun `toucher la barre de progression donne une position dans le morceau`() {
        assertThat(positionAt(fraction = 0.5f, duration = 244.seconds)).isEqualTo(122.seconds)
        assertThat(positionAt(fraction = 1.4f, duration = 244.seconds)).isEqualTo(244.seconds)
    }
}
