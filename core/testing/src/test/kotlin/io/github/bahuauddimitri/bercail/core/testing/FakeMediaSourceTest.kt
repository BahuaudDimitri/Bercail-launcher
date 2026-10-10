package io.github.bahuauddimitri.bercail.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The fake player behaves like a real one: a playlist, play and pause, and time that only moves while watched. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeMediaSourceTest {
    private suspend fun FakeMediaSource.title() = (state.first() as MediaState.Loaded).content.title

    @Test
    fun `sans rien de chargé, l'état est Aucun`() = runTest {
        assertThat(FakeMediaSource().state.first()).isEqualTo(MediaState.None)
    }

    @Test
    fun `un contenu chargé peut être mis en pause puis relancé`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true)

        media.pause()
        assertThat(media.state.first().isPlaying).isFalse()

        media.play()
        assertThat(media.state.first().isPlaying).isTrue()
    }

    @Test
    fun `Suivant passe au morceau d'après et revient au premier après le dernier`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true)

        media.next()
        assertThat(media.title()).isEqualTo("Says")

        media.next()
        media.next()
        assertThat(media.title()).isEqualTo("Looped")
    }

    @Test
    fun `Précédent passe au morceau d'avant, au début`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true, position = 3.seconds)

        media.previous()

        assertThat(media.title()).isEqualTo("Avril 14th")
        assertThat(media.position.first()).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `se déplacer dans le morceau change la position`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = false)

        media.seekTo(100.seconds)

        assertThat(media.position.first()).isEqualTo(100.seconds)
    }

    @Test
    fun `arrêter la musique revient à l'état Aucun`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true)

        media.stop()

        assertThat(media.state.first()).isEqualTo(MediaState.None)
    }

    @Test
    fun `ouvrir une app charge sa liste et lance la lecture`() = runTest {
        val media = FakeMediaSource()

        media.load(Samples.youtube)

        assertThat(media.title()).isEqualTo("Ramen maison en 20 minutes")
        assertThat(media.state.first().isPlaying).isTrue()
    }

    @Test
    fun `sans rien de chargé, les boutons ne font rien`() = runTest {
        val media = FakeMediaSource()

        media.play()
        media.next()
        media.previous()
        media.seekTo(10.seconds)

        assertThat(media.state.first()).isEqualTo(MediaState.None)
    }

    @Test
    fun `le temps avance d'une seconde par seconde pendant la lecture, tant qu'on le regarde`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true, position = 72.seconds, ticking = true)
        val watching = launch { media.position.collect {} }

        advanceTimeBy(3_000)
        runCurrent()
        watching.cancel()

        assertThat(media.position.first()).isEqualTo(75.seconds)
    }

    @Test
    fun `le temps n'avance pas quand personne ne le regarde`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true, position = 72.seconds, ticking = true)

        advanceTimeBy(60_000)

        assertThat(media.position.first()).isEqualTo(72.seconds)
    }

    @Test
    fun `le temps n'avance pas en pause`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = false, position = 72.seconds, ticking = true)
        val watching = launch { media.position.collect {} }

        advanceTimeBy(3_000)
        runCurrent()
        watching.cancel()

        assertThat(media.position.first()).isEqualTo(72.seconds)
    }

    @Test
    fun `à la fin d'un morceau, le suivant démarre`() = runTest {
        val media = FakeMediaSource(Samples.spotify, playing = true, position = 244.seconds, ticking = true)
        val watching = launch { media.position.collect {} }

        advanceTimeBy(1_000)
        runCurrent()
        watching.cancel()

        assertThat(media.title()).isEqualTo("Says")
        assertThat(media.position.first()).isEqualTo(Duration.ZERO)
    }
}
