package io.github.bahuauddimitri.bercail.core.domain.media

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/** Which media session Bercail shows when several apps have something loaded, and what its remote offers. */
class MediaSessionsTest {
    private val spotify = MediaContent("Looped", "Kiasmos", "Spotify", 245.seconds)
    private val youtube = MediaContent("Tiny Desk Concert", "NPR Music", "YouTube", 1080.seconds)
    private val music = MediaContent("Says", "Nils Frahm", "YouTube Music", 528.seconds)

    private fun paused(content: MediaContent) = MediaState.Loaded(content, playing = false)

    private fun playing(content: MediaContent) = MediaState.Loaded(content, playing = true)

    @Test
    fun `sans aucune session, rien n'est chargé`() {
        assertThat(shownAmong(emptyList())).isEqualTo(MediaState.None)
    }

    @Test
    fun `une seule session est montrée telle quelle`() {
        assertThat(shownAmong(listOf(paused(spotify)))).isEqualTo(paused(spotify))
    }

    @Test
    fun `plusieurs sessions en pause, c'est celle qui a joué en dernier qui est montrée`() {
        // Android gives the sessions with the one that played last first.
        assertThat(shownAmong(listOf(paused(music), paused(spotify)))).isEqualTo(paused(music))
        assertThat(shownAmong(listOf(paused(spotify), paused(music), paused(youtube)))).isEqualTo(paused(spotify))
    }

    @Test
    fun `une session en lecture passe avant celles en pause`() {
        assertThat(shownAmong(listOf(paused(spotify), playing(youtube)))).isEqualTo(playing(youtube))
    }

    @Test
    fun `sauf mention contraire, la télécommande a tous ses boutons`() {
        assertThat(playing(spotify).actions)
            .containsExactlyInAnyOrder(MediaAction.Previous, MediaAction.Next, MediaAction.Seek)
    }

    @Test
    fun `une app sans Précédent garde ses autres boutons`() {
        val video = MediaState.Loaded(youtube, playing = true, actions = setOf(MediaAction.Next, MediaAction.Seek))

        assertThat(video.actions).containsExactlyInAnyOrder(MediaAction.Next, MediaAction.Seek)
    }
}
