package io.github.bahuauddimitri.bercail.data.update

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.update.Release
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateNotes
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** What Bercail remembers about its updates is kept in a file: it survives a restart of the app. */
class StoredUpdateNotesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "update.preferences_pb") }
    private val monday = LocalDateTime.of(2026, 10, 12, 8, 0)
    private val found = Release(Version(0, 9, 0), "https://example.test/bercail-0.9.0.apk", 2_400_000, "ab".repeat(32))

    /** Opens the file as the app does at start, runs [block], then closes it as when the app stops. */
    private fun <T> withNotes(block: suspend (StoredUpdateNotes) -> T): T = runBlocking {
        val job = Job()
        val notes = StoredUpdateNotes(updateNotesStore(file, CoroutineScope(Dispatchers.IO + job)))
        try {
            block(notes)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `au premier lancement, Bercail n'a encore rien noté`() {
        assertThat(withNotes { it.read() }).isEqualTo(UpdateNotes())
    }

    @Test
    fun `la date de la dernière lecture est retrouvée après un redémarrage`() {
        withNotes { it.write(UpdateNotes(checkedAt = monday)) }

        assertThat(withNotes { it.read() }.checkedAt).isEqualTo(monday)
    }

    @Test
    fun `une lecture ratée est retenue comme telle`() {
        withNotes { it.write(UpdateNotes(checkedAt = monday, reached = false)) }

        assertThat(withNotes { it.read() }.reached).isEqualTo(false)
    }

    @Test
    fun `la version trouvée est retrouvée entière, avec son adresse, sa taille et son empreinte`() {
        withNotes { it.write(UpdateNotes(checkedAt = monday, found = found)) }

        assertThat(withNotes { it.read() }.found).isEqualTo(found)
    }

    @Test
    fun `une version trouvée sans empreinte reste sans empreinte`() {
        withNotes { it.write(UpdateNotes(checkedAt = monday, found = found.copy(sha256 = null))) }

        assertThat(withNotes { it.read() }.found?.sha256).isNull()
    }

    @Test
    fun `une version oubliée ne revient pas`() {
        withNotes { it.write(UpdateNotes(checkedAt = monday, found = found)) }
        withNotes { it.write(UpdateNotes(checkedAt = monday.plusDays(1))) }

        assertThat(withNotes { it.read() }).isEqualTo(UpdateNotes(checkedAt = monday.plusDays(1)))
    }

    @Test
    fun `un fichier abîmé est repris de zéro au lieu de bloquer les mises à jour`() {
        file.writeText("ceci n'est pas un fichier de notes")

        assertThat(withNotes { it.read() }).isEqualTo(UpdateNotes())
    }
}
