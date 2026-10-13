package io.github.bahuauddimitri.bercail.core.domain.messages

import kotlinx.coroutines.flow.Flow

/**
 * A conversation as the home screen needs it: who, on which network, how many unread messages. A [muted] one is
 * a conversation the user silenced in the messaging app: it never speaks for itself on the home screen. In a
 * [group], several people write.
 */
data class Conversation(
    val id: String,
    val name: String,
    val network: String,
    val unread: Int = 0,
    val muted: Boolean = false,
    val group: Boolean = false
) {
    /** The first letter of the name, or its first emoji, whole. */
    val initials: String
        get() = if (name.isEmpty()) "" else name.substring(0, name.offsetByCodePoints(0, 1)).uppercase()

    /** How the person is announced: "Léa, 1 non lu". */
    val description: String get() = if (unread > 0) "$name, ${unreadWording(unread)}" else name
}

interface MessagesSource {
    /**
     * Every conversation, the most recent first, emitted again when one changes. The home screen never reads
     * message history.
     */
    val conversations: Flow<List<Conversation>>

    /**
     * The last messages of a conversation, emitted again when it changes; nothing (null) until they were read
     * once. Reading them is slow: they are only read while someone listens, that is while the conversation is open.
     */
    fun history(conversationId: String): Flow<List<Message>?>

    /** Sends [text] in the conversation. False when the messaging app could not take it. */
    suspend fun send(conversationId: String, text: String): Boolean

    /** The messages holding [text], whatever their conversation. Only asked when the user searches. */
    suspend fun search(text: String): List<FoundMessage>

    /** The user has seen the conversation: its unread messages no longer count. */
    suspend fun markRead(conversationId: String)

    /** Shows the messaging app, where every conversation is. */
    fun openApp()
}

/** The messages line of the folded drawer: one dot per sender, "2 non lus · Léa, Tom". */
data class MessagesSummary(val unread: Int, val senders: List<Conversation>) {
    /** The part written in strong text, absent when nothing is unread. */
    val headline: String? get() = if (unread > 0) unreadWording(unread) else null

    val detail: String get() = if (unread > 0) senders.joinToString(", ") { it.name } else "Aucun message"
}

fun messagesSummary(conversations: List<Conversation>): MessagesSummary {
    val senders = conversations.filter { it.unread > 0 && !it.muted }
    return MessagesSummary(unread = senders.sumOf { it.unread }, senders = senders)
}

/**
 * The favorite people, in the order chosen in the settings. Until the user has chosen, they are the first
 * conversations that are not muted: the people who wrote last.
 */
fun favoritesAmong(conversations: List<Conversation>, favoriteIds: List<String>): List<Conversation> =
    if (favoriteIds.isEmpty()) {
        conversations.filterNot { it.muted }.take(MAX_FAVORITE_PEOPLE)
    } else {
        favoriteIds.mapNotNull { id -> conversations.firstOrNull { it.id == id } }
    }

/** What the "Tous" tile counts: unread messages of everyone who is neither a favorite nor muted. */
fun unreadOutsideFavorites(conversations: List<Conversation>, favoriteIds: List<String>): Int =
    conversations.filterNot { it.id in favoriteIds || it.muted }.sumOf { it.unread }

/** How the "Tous" tile is announced: it opens the messaging app and counts what the favorites do not show. */
fun allMessagesDescription(otherUnread: Int): String {
    val all = "Tous les messages dans Beeper"
    return if (otherUnread > 0) "$all, ${unreadWording(otherUnread)}" else all
}

/** The drawer has room for four people next to the "Tous" tile. */
const val MAX_FAVORITE_PEOPLE = 4

/** What the messages line says until the user lets Bercail read Beeper, and the button that asks for it. */
const val MESSAGES_NOT_LINKED = "Beeper n'est pas relié"
const val LINK_MESSAGES = "Relier Beeper"

private fun unreadWording(count: Int) = if (count == 1) "1 non lu" else "$count non lus"
