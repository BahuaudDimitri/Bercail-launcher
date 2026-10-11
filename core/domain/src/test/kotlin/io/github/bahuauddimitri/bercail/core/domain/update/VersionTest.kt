package io.github.bahuauddimitri.bercail.core.domain.update

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.time.LocalDateTime
import org.junit.Test

/** Versions as the releases number them, and how often Bercail looks at its releases. */
class VersionTest {
    private val monday = LocalDateTime.of(2026, 10, 12, 8, 0)

    @Test
    fun `une version se lit avec ou sans son « v »`() {
        assertThat(Version.parse("0.8.1")).isEqualTo(Version(0, 8, 1))
        assertThat(Version.parse("v0.8.1")).isEqualTo(Version(0, 8, 1))
    }

    @Test
    fun `une version s'écrit sans « v »`() {
        assertThat(Version(1, 2, 3).toString()).isEqualTo("1.2.3")
    }

    @Test
    fun `la version d'un build local n'est pas une version`() {
        assertThat(Version.parse("0.0.0-dev")).isNull()
        assertThat(Version.parse("")).isNull()
        assertThat(Version.parse("dernière")).isNull()
    }

    @Test
    fun `la 0·10·0 est plus récente que la 0·9·3, et la 1·0·0 que les deux`() {
        assertThat(Version(0, 10, 0) > Version(0, 9, 3)).isTrue()
        assertThat(Version(1, 0, 0) > Version(0, 10, 0)).isTrue()
        assertThat(Version(0, 9, 4) > Version(0, 9, 3)).isTrue()
        assertThat(Version(0, 9, 3) > Version(0, 9, 3)).isFalse()
    }

    @Test
    fun `sans lecture précédente, il est temps de lire les versions publiées`() {
        assertThat(isCheckDue(UpdateNotes(), monday)).isTrue()
    }

    @Test
    fun `après une lecture réussie, la suivante attend un jour entier`() {
        val notes = UpdateNotes(checkedAt = monday)

        assertThat(isCheckDue(notes, monday.plusHours(23).plusMinutes(59))).isFalse()
        assertThat(isCheckDue(notes, monday.plusDays(1))).isTrue()
    }

    @Test
    fun `après une lecture ratée, la suivante attend une heure`() {
        val notes = UpdateNotes(checkedAt = monday, reached = false)

        assertThat(isCheckDue(notes, monday.plusMinutes(59))).isFalse()
        assertThat(isCheckDue(notes, monday.plusHours(1))).isTrue()
    }

    @Test
    fun `si l'horloge du téléphone a reculé, la lecture n'est pas bloquée`() {
        assertThat(isCheckDue(UpdateNotes(checkedAt = monday), monday.minusDays(3))).isTrue()
    }
}
