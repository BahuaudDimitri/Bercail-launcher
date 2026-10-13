package io.github.bahuauddimitri.bercail

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.data.messages.BeeperAccess
import io.github.bahuauddimitri.bercail.data.messages.BeeperMessages
import io.github.bahuauddimitri.bercail.data.messages.SeenJournal
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real Beeper, on a real phone: what Bercail expects of its door for other apps, which Beeper calls
 * experimental. Only runs where Beeper is installed and its owner let Bercail read it; skipped everywhere else,
 * emulators included. It reads dates and counts, and writes no name and no message anywhere. It sends nothing.
 */
@RunWith(AndroidJUnit4::class)
class MessagesDeviceTest {
    private val phone = InstrumentationRegistry.getInstrumentation().targetContext
    private val everything = object : AccessSource {
        override val granted: Flow<Set<Access>> = MutableStateFlow(Access.entries.toSet())
    }
    private val nothingSeen = object : SeenJournal {
        override val seen: Flow<Map<String, Long>> = MutableStateFlow(emptyMap())

        override suspend fun note(conversationId: String, stamp: Long) = Unit
    }
    private val messages = BeeperMessages(phone, everything, nothingSeen)

    private fun conversations(): List<Conversation> = runBlocking {
        withTimeout(TIMEOUT) { messages.conversations.first() }
    }

    /** When Beeper says each of the latest conversations last moved, by conversation. */
    private fun lastMoves(): Map<String, Long> {
        val latest = Uri.parse("content://com.beeper.api/chats?limit=100&isArchived=0&isLowPriority=0")
        return phone.contentResolver.query(latest, null, null, null, null)?.use { rows ->
            val id = rows.getColumnIndexOrThrow("roomId")
            val time = rows.getColumnIndexOrThrow("timestamp")
            buildMap { while (rows.moveToNext()) put(rows.getString(id), rows.getLong(time)) }
        }.orEmpty()
    }

    @Test
    fun `Beeper donne ses conversations avec un nom et un réseau`() {
        assumeTrue("Beeper is not linked on this phone", BeeperAccess.granted(phone))

        val all = conversations()

        Log.i(TAG, "conversations=${all.size} networks=${all.groupingBy { it.network }.eachCount()}")
        Log.i(TAG, "muted=${all.count { it.muted }} groups=${all.count { it.group }} unread=${all.sumOf { it.unread }}")
        assertThat(all).isNotEmpty()
        assertThat(all.all { it.id.isNotBlank() && it.name.isNotBlank() && it.network.isNotBlank() }).isTrue()
    }

    @Test
    fun `les messages lus dans une conversation sont ses plus récents - du plus ancien au plus récent`() {
        assumeTrue("Beeper is not linked on this phone", BeeperAccess.granted(phone))
        val moves = lastMoves()
        val zone = ZoneId.systemDefault()
        var upToDate = 0
        val probed = conversations().take(PROBED)

        probed.forEachIndexed { index, conversation ->
            val read = runBlocking { withTimeout(TIMEOUT) { messages.history(conversation.id).first { it != null } } }
            val times = read.orEmpty().map { it.at.atZone(zone).toInstant().toEpochMilli() }
            val gap = times.maxOrNull()?.let { (moves.getValue(conversation.id) - it) / MILLIS_PER_SECOND }
            Log.i(
                TAG,
                "conversation#$index messages=${times.size} mine=${read.orEmpty().count {
                    it.mine
                }} gapSeconds=$gap"
            )
            assertThat(times).isEqualTo(times.sorted())
            if (gap != null && gap in -CLOSE..CLOSE) upToDate++
        }

        // A conversation also moves for what is not a message (a reaction, a deletion): most must match, not all.
        assertThat(upToDate * 2 >= probed.size).isTrue()
    }

    @Test
    fun `chercher un mot dans les messages répond en quelques secondes au plus`() {
        assumeTrue("Beeper is not linked on this phone", BeeperAccess.granted(phone))
        val started = SystemClock.elapsedRealtime()

        val found = runBlocking { withTimeout(TIMEOUT) { messages.search("bonjour") } }

        val took = SystemClock.elapsedRealtime() - started
        Log.i(TAG, "search found=${found.size} conversations=${found.map { it.conversationId }.toSet().size} ms=$took")
        assertThat(took < SLOWEST_SEARCH).isTrue()
    }

    private companion object {
        const val TAG = "Bercail"
        const val TIMEOUT = 30_000L
        const val PROBED = 5
        const val SLOWEST_SEARCH = 5_000L
        const val MILLIS_PER_SECOND = 1_000

        /** How far, in seconds, the last message may be from the last move of its conversation. */
        const val CLOSE = 120L
    }
}
