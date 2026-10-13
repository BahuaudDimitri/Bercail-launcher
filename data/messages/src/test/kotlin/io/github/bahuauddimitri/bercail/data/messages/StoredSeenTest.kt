package io.github.bahuauddimitri.bercail.data.messages

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** What the user already saw of each conversation is kept in a file: the badges do not come back at a restart. */
class StoredSeenTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "messages.preferences_pb") }

    /** Opens the file as the app does at start, runs [block], then closes it as when the app stops. */
    private fun <T> withSeen(block: suspend (StoredSeen) -> T): T = runBlocking {
        val job = Job()
        val seen = StoredSeen(seenStore(file, CoroutineScope(Dispatchers.IO + job)))
        try {
            block(seen)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `au premier lancement, rien n'a encore été vu`() {
        assertThat(withSeen { it.seen.first() }).isEmpty()
    }

    @Test
    fun `ce qui a été vu est retrouvé après un redémarrage`() {
        withSeen { it.note("!lea:beeper.local", 1_000) }

        assertThat(withSeen { it.seen.first() }).isEqualTo(mapOf("!lea:beeper.local" to 1_000L))
    }

    @Test
    fun `revoir une conversation remplace ce qui était noté pour elle, sans toucher aux autres`() {
        withSeen {
            it.note("lea", 1_000)
            it.note("tom", 1_500)
            it.note("lea", 2_000)
        }

        assertThat(withSeen { it.seen.first() }).isEqualTo(mapOf("lea" to 2_000L, "tom" to 1_500L))
    }

    @Test
    fun `seules les deux cents conversations vues en dernier sont gardées`() {
        val seen = withSeen { journal ->
            (1..205L).forEach { journal.note("room$it", it) }
            journal.seen.first()
        }

        assertThat(seen).hasSize(200)
        assertThat(seen["room5"]).isNull()
        assertThat(seen["room6"]).isEqualTo(6L)
    }
}
