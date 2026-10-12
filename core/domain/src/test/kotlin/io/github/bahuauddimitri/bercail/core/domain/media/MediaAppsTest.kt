package io.github.bahuauddimitri.bercail.core.domain.media

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import org.junit.Test

/** The lines of the « Écouter » sheet: the main media app first, then the other apps that can play. */
class MediaAppsTest {
    private val pocketCasts = App("pocketcasts", "Pocket Casts")
    private val spotify = App("spotify", "Spotify")
    private val youtube = App("youtube", "YouTube")
    private val installed = listOf(pocketCasts, spotify, youtube)

    @Test
    fun `l'app principale est en tête, les autres suivent dans leur ordre`() {
        val rows = mediaAppRows(installed, chosen = "youtube", last = null)

        assertThat(rows.map { it.app.name }).containsExactly("YouTube", "Pocket Casts", "Spotify")
        assertThat(rows.map { it.main }).containsExactly(true, false, false)
    }

    @Test
    fun `sans choix, l'app principale est Spotify si elle est installée`() {
        assertThat(mainMediaApp(installed, chosen = null)).isEqualTo(spotify)
    }

    @Test
    fun `sans choix ni Spotify, la première app média tient ce rôle`() {
        assertThat(mainMediaApp(listOf(pocketCasts, youtube), chosen = null)).isEqualTo(pocketCasts)
    }

    @Test
    fun `une app choisie puis désinstallée laisse la place à Spotify`() {
        assertThat(mainMediaApp(installed, chosen = "deezer")).isEqualTo(spotify)
    }

    @Test
    fun `l'app principale propose de reprendre ce qu'elle jouait en dernier`() {
        val rows = mediaAppRows(installed, chosen = null, last = LastHeard("spotify", "Looped"))

        assertThat(rows.first().resume).isEqualTo("Reprendre « Looped »")
        assertThat(rows.drop(1).map { it.resume }).containsExactly(null, null)
    }

    @Test
    fun `si le dernier contenu vient d'une autre app, rien n'est proposé à reprendre`() {
        val rows = mediaAppRows(installed, chosen = null, last = LastHeard("youtube", "Tiny Desk Concert"))

        assertThat(rows.first().app).isEqualTo(spotify)
        assertThat(rows.first().resume).isNull()
    }

    @Test
    fun `sans app média installée, la feuille n'a aucune ligne`() {
        assertThat(mediaAppRows(emptyList(), chosen = "spotify", last = null)).isEmpty()
        assertThat(mainMediaApp(emptyList(), chosen = null)).isNull()
    }
}
