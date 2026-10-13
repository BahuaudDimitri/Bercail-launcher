package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A messaging app holding [conversations] and the [messages] of each. It takes what is sent to it and gives it
 * back in the conversation, as the real one does; the tests can make it refuse, or be slow to answer.
 */
class FakeMessagesSource(
    conversations: List<Conversation> = emptyList(),
    messages: Map<String, List<Message>> = emptyMap(),
    private val now: () -> LocalDateTime = { LocalDateTime.MIN }
) : MessagesSource {
    override val conversations = MutableStateFlow(conversations)
    private val histories = mutableMapOf<String, MutableStateFlow<List<Message>?>>()

    init {
        messages.forEach { (id, all) -> histories[id] = MutableStateFlow(all) }
    }

    /** Everything sent so far, in order: "lea: J'arrive". */
    val sent = mutableListOf<String>()

    /** The conversations whose history was asked for so far, in order. */
    val read = mutableListOf<String>()

    /** True when the messaging app takes no message. */
    var refusing = false

    /** False when the messaging app is slow to give back, in the conversation, what was just sent. */
    var givesBack = true

    /** How many times the messaging app was asked for. */
    var appOpenings = 0
        private set

    private fun historyOf(id: String) = histories.getOrPut(id) { MutableStateFlow(emptyList()) }

    override fun history(conversationId: String): Flow<List<Message>?> {
        read += conversationId
        return historyOf(conversationId)
    }

    override suspend fun send(conversationId: String, text: String): Boolean {
        if (refusing) return false
        sent += "$conversationId: $text"
        if (givesBack) {
            historyOf(conversationId).update {
                it.orEmpty() + Message("sent-${sent.size}", text, mine = true, author = "", at = now())
            }
        }
        return true
    }

    override suspend fun markRead(conversationId: String) = readNow(conversationId)

    /** The user read the conversation, here or in the messaging app. */
    fun readNow(id: String) = change(id) { it.copy(unread = 0) }

    override fun openApp() {
        appOpenings++
    }

    /** One more unread message from this person; with a [text], it is also in the conversation. */
    fun receive(id: String, text: String? = null) {
        if (text != null) {
            historyOf(id).update { it.orEmpty() + Message("got-${it.orEmpty().size}", text, false, "", now()) }
        }
        change(id) { it.copy(unread = it.unread + 1) }
    }

    /** The messages of this conversation have not been read from the messaging app yet. */
    fun notReadYet(id: String) {
        historyOf(id).value = null
    }

    private fun change(id: String, transform: (Conversation) -> Conversation) =
        conversations.update { all -> all.map { if (it.id == id) transform(it) else it } }
}
