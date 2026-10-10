package io.github.bahuauddimitri.bercail.core.domain.messages

import kotlinx.coroutines.flow.Flow

/** A conversation as the home screen needs it: who, on which network, how many unread messages. */
data class Conversation(val id: String, val name: String, val network: String, val unread: Int = 0) {
    val initials: String get() = name.take(1).uppercase()

    /** How the person is announced: "Léa, 1 non lu". */
    val description: String get() = if (unread > 0) "$name, ${unreadWording(unread)}" else name
}

interface MessagesSource {
    /** Every conversation, emitted again when one changes. The home screen never reads message history. */
    val conversations: Flow<List<Conversation>>
}

/** The messages line of the folded drawer: one dot per sender, "2 non lus · Léa, Tom". */
data class MessagesSummary(val unread: Int, val senders: List<Conversation>) {
    /** The part written in strong text, absent when nothing is unread. */
    val headline: String? get() = if (unread > 0) unreadWording(unread) else null

    val detail: String get() = if (unread > 0) senders.joinToString(", ") { it.name } else NO_MESSAGE

    /** How the line is announced by a screen reader. */
    val description: String
        get() = when (unread) {
            0 -> NO_MESSAGE
            1 -> "1 message non lu"
            else -> "$unread messages non lus"
        }
}

fun messagesSummary(conversations: List<Conversation>): MessagesSummary {
    val senders = conversations.filter { it.unread > 0 }
    return MessagesSummary(unread = senders.sumOf { it.unread }, senders = senders)
}

/** The favorite people, in the order chosen in the settings. */
fun favoritesAmong(conversations: List<Conversation>, favoriteIds: List<String>): List<Conversation> =
    favoriteIds.mapNotNull { id -> conversations.firstOrNull { it.id == id } }

/** What the "Tous" tile counts: unread messages of everyone who is not a favorite. */
fun unreadOutsideFavorites(conversations: List<Conversation>, favoriteIds: List<String>): Int =
    conversations.filterNot { it.id in favoriteIds }.sumOf { it.unread }

private fun unreadWording(count: Int) = if (count == 1) "1 non lu" else "$count non lus"

private const val NO_MESSAGE = "Aucun message"
