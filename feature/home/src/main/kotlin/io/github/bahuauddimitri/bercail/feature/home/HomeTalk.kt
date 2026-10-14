package io.github.bahuauddimitri.bercail.feature.home

import io.github.bahuauddimitri.bercail.core.domain.messages.Bubble
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.messages.notYetIn
import io.github.bahuauddimitri.bercail.core.domain.messages.sendable
import io.github.bahuauddimitri.bercail.core.domain.messages.threadOf
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the conversation sheet shows while it is open. */
data class TalkUiState(
    val person: Conversation,
    /** The messages, the oldest first: the last one is at the bottom. */
    val bubbles: List<Bubble> = emptyList(),
    /** True until the messages were read once in the messaging app, which takes a moment. */
    val loading: Boolean = false,
    /** What is written in the field and not sent yet. */
    val draft: String = "",
    /** The bubble that was just sent and can still be taken back, if any. */
    val undoable: String? = null,
    /** True when the last message could not be sent: its text is back in the field. */
    val notSent: Boolean = false
) {
    val canSend: Boolean get() = sendable(draft) != null
}

/** What the home screen does about a conversation: opening it, answering, taking an answer back. */
interface TalkActions {
    /** Opens the sheet of a conversation, and counts its messages as read. */
    fun onOpenConversation(id: String)

    /** Closes the sheet. What was written and not sent stays for the next time. */
    fun onCloseConversation()
    fun onDraftChange(text: String)

    /** Sends what is in the field. It only leaves for good a few seconds later: until then it can be taken back. */
    fun onSend()

    /** Takes back the message that was just sent: nothing leaves, and its text comes back. */
    fun onUndoSend()
}

/**
 * The conversation sheet of the home screen. A message that is sent shows at once and only leaves for good after
 * [UNDO_WINDOW], unless it is taken back; sending another one, or closing the sheet, makes it leave right away.
 */
internal class HomeTalk(private val messages: MessagesSource, private val clock: Clock) :
    TalkActions,
    AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The conversations the home screen may show, set by it as they come. */
    val people = MutableStateFlow<List<Conversation>>(emptyList())
    private val open = MutableStateFlow<String?>(null)
    private val notes = MutableStateFlow(TalkNotes())
    private var countdown: Job? = null
    private var posted = 0

    /** The sheet, or nothing while it is closed. The messages are only read while it is open. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<TalkUiState?> = open.flatMapLatest { id ->
        if (id == null) return@flatMapLatest flowOf(null)
        val person = people.map { all -> all.firstOrNull { it.id == id } }
        person.map { it != null }.distinctUntilChanged().flatMapLatest { known ->
            if (!known) return@flatMapLatest flowOf(null)
            val history = messages.history(id).onEach { read ->
                if (read != null) notes.update { it.withoutGivenBack(id, read) }
            }
            combine(person.filterNotNull(), history, notes) { who, read, notes ->
                notes.shownTo(who, read, clock.now())
            }.onEach { if (it.person.unread > 0) scope.launch { messages.markRead(id) } }
        }
    }

    override fun onOpenConversation(id: String) {
        deliver()
        open.value = id
    }

    override fun onCloseConversation() {
        deliver()
        open.value = null
    }

    override fun onDraftChange(text: String) {
        val id = open.value ?: return
        notes.update { it.copy(drafts = it.drafts + (id to text), failedIn = null) }
    }

    override fun onSend() {
        val id = open.value ?: return
        sendable(notes.value.drafts[id].orEmpty())?.let { post(id, it) }
    }

    override fun onUndoSend() {
        val taken = notes.value.pending ?: return
        countdown?.cancel()
        notes.update { it.without(taken) }
    }

    private fun post(id: String, text: String) {
        deliver()
        val message = Message("local-${++posted}", text, mine = true, author = "", at = clock.now())
        notes.update { it.with(Posted(id, message)) }
        countdown = scope.launch {
            delay(UNDO_WINDOW)
            countdown = null
            deliver()
        }
    }

    /** The message that could still be taken back leaves for good. */
    private fun deliver() {
        val leaving = notes.value.pending ?: return
        countdown?.cancel()
        notes.update { it.copy(pending = null) }
        scope.launch {
            val taken = messages.send(leaving.conversationId, leaving.message.text)
            if (!taken) notes.update { it.without(leaving).copy(failedIn = leaving.conversationId) }
        }
    }

    override fun close() = scope.cancel()

    private companion object {
        /** How long « Annuler l'envoi » stays under a message. */
        val UNDO_WINDOW = 6.seconds
    }
}

/** A message sent from the sheet. */
internal data class Posted(val conversationId: String, val message: Message)

/**
 * What the sheet remembers by itself, conversation by conversation: what is written and not sent, what was sent
 * and is not in the messaging app yet, the message that can still be taken back.
 */
internal data class TalkNotes(
    val drafts: Map<String, String> = emptyMap(),
    val sent: List<Posted> = emptyList(),
    val pending: Posted? = null,
    val failedIn: String? = null
) {
    fun with(posted: Posted): TalkNotes =
        copy(drafts = drafts - posted.conversationId, sent = sent + posted, pending = posted, failedIn = null)

    /** The message never left: its text goes back to the field, unless something else was written since. */
    fun without(posted: Posted): TalkNotes {
        val id = posted.conversationId
        return copy(
            drafts = if (drafts[id].isNullOrBlank()) drafts + (id to posted.message.text) else drafts,
            sent = sent - posted,
            pending = pending.takeIf { it != posted }
        )
    }

    /** Forgets the messages sent in a conversation that the messaging app now gives back in [read]. */
    fun withoutGivenBack(conversationId: String, read: List<Message>): TalkNotes = copy(
        sent = sent.filter { posted ->
            posted.conversationId != conversationId ||
                posted == pending ||
                notYetIn(read, listOf(posted.message)).isNotEmpty()
        }
    )

    fun shownTo(person: Conversation, read: List<Message>?, now: LocalDateTime): TalkUiState {
        val mine = sent.filter { it.conversationId == person.id }.map { it.message }
        val waiting = pending?.takeIf { it.conversationId == person.id }?.message
        val notBack = notYetIn(read.orEmpty(), mine.filter { it != waiting }) + listOfNotNull(waiting)
        return TalkUiState(
            person = person,
            bubbles = threadOf(person, read.orEmpty(), notBack, now),
            loading = read == null && notBack.isEmpty(),
            draft = drafts[person.id].orEmpty(),
            undoable = waiting?.id,
            notSent = failedIn == person.id
        )
    }
}
