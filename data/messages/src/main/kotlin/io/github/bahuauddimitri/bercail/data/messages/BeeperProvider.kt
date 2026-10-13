package io.github.bahuauddimitri.bercail.data.messages

import android.content.ContentResolver
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** A conversation as Beeper answers it: when it last moved, and what it says of its last message. */
internal class Chat(val conversation: Conversation, val stamp: Long, private val preview: String?) {
    /** Changes each time something happens in the conversation. */
    val mark: String get() = "$stamp|${conversation.unread}|$preview"

    /** The conversation without unread messages, when the user saw it since it last moved. */
    fun unlessSeenAt(seen: Long?): Conversation =
        if (seen != null && stamp <= seen) conversation.copy(unread = 0) else conversation
}

/**
 * Beeper's door for other apps, on the phone: a content provider that lists the conversations, gives the messages
 * of one, and takes a text to send. Beeper calls it experimental: whatever fails there reads as "nothing".
 */
internal class BeeperProvider(private val beeper: ContentResolver) {
    /** One signal now, then one each time Beeper says its list changed. A burst of changes counts once. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val watched = try {
            beeper.registerContentObserver(CHATS, true, observer)
            true
        } catch (_: SecurityException) {
            // Beeper is not on this phone, or the user just took the access back.
            false
        }
        trySend(Unit)
        awaitClose { if (watched) beeper.unregisterContentObserver(observer) }
    }.conflate()

    /** The conversations that moved last, the most recent first, without what the user put away in Beeper. */
    fun chats(): List<Chat> = rows(RECENT_CHATS) { ChatColumns(it)::chatOf }.sortedByDescending { it.stamp }

    fun chat(roomId: String): Chat? = rows(
        CHATS.buildUpon().appendQueryParameter("roomIds", roomId).appendQueryParameter("limit", "1").build()
    ) { ChatColumns(it)::chatOf }.firstOrNull()

    /** The last messages of a conversation, the oldest first. Slow: about a second, more the first time. */
    fun messages(roomId: String, zone: ZoneId): List<Message> = rows(
        MESSAGES.buildUpon()
            .appendQueryParameter("roomIds", roomId)
            .appendQueryParameter("limit", HISTORY_LENGTH)
            .build()
    ) { rows -> MessageColumns(rows, zone)::messageOf }.filterNotNull().sortedBy { it.at }

    /** Beeper takes a message as an insertion at the address of the messages, and answers with its address. */
    fun send(roomId: String, text: String): Boolean = try {
        val letter = MESSAGES.buildUpon()
            .appendQueryParameter("roomId", roomId)
            .appendQueryParameter("text", text)
            .build()
        beeper.insert(letter, ContentValues()) != null
    } catch (@Suppress("TooGenericExceptionCaught") _: RuntimeException) {
        false
    }

    private fun <T> rows(address: Uri, reader: (Cursor) -> (Cursor) -> T): List<T> = try {
        beeper.query(address, null, null, null, null)?.use { rows ->
            val read = reader(rows)
            buildList { while (rows.moveToNext()) add(read(rows)) }
        }.orEmpty()
    } catch (@Suppress("TooGenericExceptionCaught") _: RuntimeException) {
        // The access was taken back, or Beeper's experimental interface failed: nothing to show.
        emptyList()
    }

    /** Where Beeper put each thing in the rows it answers. A column it stops giving reads as empty. */
    private class ChatColumns(rows: Cursor) {
        private val id = rows.getColumnIndex("roomId")
        private val title = rows.getColumnIndex("title")
        private val protocol = rows.getColumnIndex("protocol")
        private val unread = rows.getColumnIndex("unreadCount")
        private val muted = rows.getColumnIndex("isMuted")
        private val oneToOne = rows.getColumnIndex("oneToOne")
        private val time = rows.getColumnIndex("timestamp")
        private val preview = rows.getColumnIndex("messagePreview")

        fun chatOf(row: Cursor) = Chat(
            conversation = Conversation(
                id = row.text(id).orEmpty(),
                name = row.text(title)?.trim().orEmpty().ifEmpty { UNNAMED },
                network = networkName(row.text(protocol)),
                unread = row.number(unread).toInt(),
                muted = row.number(muted) == 1L,
                group = oneToOne >= 0 && row.number(oneToOne) == 0L
            ),
            stamp = row.number(time),
            preview = row.text(preview)
        )
    }

    private class MessageColumns(rows: Cursor, private val zone: ZoneId) {
        private val id = rows.getColumnIndex("originalId")
        private val text = rows.getColumnIndex("text_content")
        private val mine = rows.getColumnIndex("isSentByMe")
        private val deleted = rows.getColumnIndex("isDeleted")
        private val author = rows.getColumnIndex("displayName")
        private val time = rows.getColumnIndex("timestamp")

        /** The row as a message, unless it was deleted. Beeper only gives texts: the rest is "an attachment". */
        fun messageOf(row: Cursor): Message? = if (row.number(deleted) == 1L) {
            null
        } else {
            Message(
                id = row.text(id).orEmpty(),
                text = row.text(text)?.trim().orEmpty().ifEmpty { ATTACHMENT },
                mine = row.number(mine) == 1L,
                author = row.text(author).orEmpty(),
                at = Instant.ofEpochMilli(row.number(time)).atZone(zone).toLocalDateTime()
            )
        }
    }

    private companion object {
        const val UNNAMED = "Sans nom"
        const val ATTACHMENT = "Pièce jointe"

        /** What the sheet of a conversation shows; more would make a slow reading slower. */
        const val HISTORY_LENGTH = "30"
        val CHATS: Uri = Uri.parse("content://com.beeper.api/chats")
        val MESSAGES: Uri = Uri.parse("content://com.beeper.api/messages")

        /** The home screen has no use for more, nor for what the user put away in Beeper. */
        val RECENT_CHATS: Uri = CHATS.buildUpon()
            .appendQueryParameter("limit", "100")
            .appendQueryParameter("isArchived", "0")
            .appendQueryParameter("isLowPriority", "0")
            .build()
    }
}

private fun Cursor.text(column: Int): String? = if (column >= 0) getString(column) else null

private fun Cursor.number(column: Int): Long = if (column >= 0) getLong(column) else 0

/** The name people give to a network, from the one Beeper gives it: "whatsapp" is "WhatsApp". */
internal fun networkName(protocol: String?): String {
    val key = protocol.orEmpty().trim().lowercase()
    return NETWORKS.firstOrNull { (prefix, _) -> key.startsWith(prefix) }?.second
        ?: key.replaceFirstChar(Char::uppercase).ifEmpty { BEEPER }
}

private const val BEEPER = "Beeper"

/** Beeper names some networks after the bridge that reaches them ("instagramgo", "facebookgo"): the start decides. */
private val NETWORKS = listOf(
    "whatsapp" to "WhatsApp",
    "signal" to "Signal",
    "telegram" to "Telegram",
    "instagram" to "Instagram",
    "facebook" to "Messenger",
    "messenger" to "Messenger",
    "imessage" to "iMessage",
    "gmessages" to "Google Messages",
    "googlechat" to "Google Chat",
    "sms" to "SMS",
    "rcs" to "RCS",
    "discord" to "Discord",
    "slack" to "Slack",
    "linkedin" to "LinkedIn",
    "twitter" to "X",
    "beeper" to BEEPER,
    "matrix" to BEEPER,
    "hungryserv" to BEEPER
)
