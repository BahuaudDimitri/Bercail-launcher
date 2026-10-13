package io.github.bahuauddimitri.bercail.core.domain.messages

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** One message of a conversation, as the messaging app keeps it. [author] is who wrote it, when it is not mine. */
data class Message(val id: String, val text: String, val mine: Boolean, val author: String, val at: LocalDateTime)

/** A message the messaging app found for a few typed words, in the conversation it belongs to. */
data class FoundMessage(val conversationId: String, val text: String, val at: LocalDateTime, val mine: Boolean = false)

/**
 * One bubble of the conversation sheet. A bubble always says who truly wrote it: [author] is "Moi" for mine, never
 * the name of the contact.
 */
data class Bubble(val id: String, val text: String, val mine: Boolean, val author: String, val header: String)

/** The network a conversation goes through, as its header writes it: "WhatsApp via Beeper". */
val Conversation.via: String get() = if (network == MESSAGING_APP) MESSAGING_APP else "$network via $MESSAGING_APP"

/**
 * The bubbles of a conversation, the oldest first: what the messaging app keeps, then what was just [sent] from
 * here and is not in it yet. In a group, each message received names who wrote it.
 */
fun threadOf(person: Conversation, history: List<Message>, sent: List<Message>, now: LocalDateTime): List<Bubble> =
    (history.sortedBy { it.at } + sent).map { message ->
        val time = messageTime(message.at, now)
        val writer = message.author.ifBlank { person.name }
        Bubble(
            id = message.id,
            text = message.text,
            mine = message.mine,
            author = if (message.mine) ME else writer,
            header = when {
                message.mine -> "$MINE · $time"
                person.group -> "$writer · $time"
                else -> time
            }
        )
    }

/**
 * Among the messages [sent] from here, those the messaging app does not give back yet. One is given back once the
 * history holds a message of mine with the same text, written since.
 */
fun notYetIn(history: List<Message>, sent: List<Message>): List<Message> = sent.filterNot { echo ->
    history.any { it.mine && it.text == echo.text && Duration.between(echo.at, it.at) > CLOCK_GAP.negated() }
}

/** When a message was written: its hour today, "hier 09:05", "mar. 18:00" this week, its day before that. */
fun messageTime(at: LocalDateTime, now: LocalDateTime): String {
    val hour = "%02d:%02d".format(at.hour, at.minute)
    val daysAgo = ChronoUnit.DAYS.between(at.toLocalDate(), now.toLocalDate())
    val day = if (at.dayOfMonth == 1) "1er" else at.dayOfMonth.toString()
    return when {
        daysAgo <= 0L -> hour
        daysAgo == 1L -> "hier $hour"
        daysAgo < DAYS_IN_A_WEEK -> "${SHORT_WEEK_DAYS[at.dayOfWeek.value - 1]} $hour"
        at.year == now.year -> "$day ${SHORT_MONTHS[at.monthValue - 1]}"
        else -> "$day ${SHORT_MONTHS[at.monthValue - 1]} ${at.year}"
    }
}

/** What really leaves when the user sends [typed]: the text without the blanks around it, or nothing. */
fun sendable(typed: String): String? = typed.trim().ifEmpty { null }

private const val MESSAGING_APP = "Beeper"
private const val ME = "Moi"
private const val MINE = "Toi"
private const val DAYS_IN_A_WEEK = 7L

/** The phone and the messaging app do not date a message at the very same second. */
private val CLOCK_GAP = Duration.ofMinutes(1)
private val SHORT_WEEK_DAYS = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.")
private val SHORT_MONTHS = listOf(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc."
)
