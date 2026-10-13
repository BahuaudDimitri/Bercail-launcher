package io.github.bahuauddimitri.bercail.data.messages

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Beeper, played by the tests: it answers at the same address and with the same columns as the real one, with the
 * conversations and the messages it was given, takes what is sent to it, and remembers what it was asked.
 */
class FakeBeeper : ContentProvider() {
    private val chats = mutableListOf<Map<String, Any?>>()
    private val messages = mutableListOf<Map<String, Any?>>()

    /** Every question asked so far, in order. */
    val asked = mutableListOf<Uri>()

    /** Everything Bercail sent so far, in order: the conversation, then the text. */
    val sent = mutableListOf<Pair<String, String>>()

    /** True once the user took back, in Android's settings, the right to read Beeper. */
    var locked = false

    /** True when Beeper fails by itself: its interface for other apps is experimental. */
    var broken = false

    /** True when Beeper takes no message. */
    var refusing = false

    /** A conversation as Beeper stores it. [at] is when it last moved, in milliseconds. */
    fun chat(
        roomId: String,
        title: String?,
        protocol: String? = "whatsapp",
        unread: Int = 0,
        at: Long = 0,
        muted: Boolean = false,
        group: Boolean = false
    ) {
        chats.removeAll { it["roomId"] == roomId }
        chats += mapOf(
            "roomId" to roomId,
            "title" to title,
            "messagePreview" to "…",
            "senderEntityId" to "@someone:beeper.local",
            "protocol" to protocol,
            "isMuted" to muted.toInt(),
            "unreadCount" to unread,
            "timestamp" to at,
            "oneToOne" to (!group).toInt()
        )
    }

    /** A message as Beeper stores it. [at] is when it was written, in milliseconds. */
    fun message(
        roomId: String,
        id: String,
        text: String?,
        at: Long,
        mine: Boolean = false,
        sender: String? = "Léa",
        deleted: Boolean = false,
        type: String = "TEXT"
    ) {
        messages += mapOf(
            "roomId" to roomId,
            "originalId" to id,
            "senderContactId" to "@$sender:beeper.local",
            "timestamp" to at,
            "isSentByMe" to mine.toInt(),
            "isDeleted" to deleted.toInt(),
            "type" to type,
            "text_content" to text,
            "reactions" to null,
            "displayName" to sender,
            "is_search_match" to 1
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (locked) throw SecurityException("READ_PERMISSION was taken back")
        check(!broken) { "Beeper stumbled" }
        asked += uri
        val rooms = uri.getQueryParameter("roomIds")?.split(",")
        val limit = uri.getQueryParameter("limit")?.toInt() ?: DEFAULT_LIMIT
        val words = uri.getQueryParameter("query")?.lowercase()
        return when (uri.path) {
            "/chats" -> cursorOf(CHAT_COLUMNS, chats.filter { rooms == null || it["roomId"] in rooms }.take(limit))

            // The real Beeper answers with the latest messages, the most recent first.
            "/messages" -> cursorOf(
                MESSAGE_COLUMNS,
                messages.filter { rooms == null || it["roomId"] in rooms }
                    .filter { words == null || words in (it["text_content"] as? String).orEmpty().lowercase() }
                    .sortedByDescending { it["timestamp"] as Long }
                    .take(limit)
            )

            else -> null
        }
    }

    private fun cursorOf(columns: List<String>, rows: List<Map<String, Any?>>): Cursor =
        MatrixCursor(columns.toTypedArray()).apply { rows.forEach { row -> addRow(columns.map { row[it] }) } }

    /** Sending is an insertion at the address of the messages, with the conversation and the text in the address. */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (locked) throw SecurityException("SEND_PERMISSION was taken back")
        check(!broken) { "Beeper stumbled" }
        val letter = uri.takeUnless { refusing || it.path != "/messages" }
        val roomId = letter?.getQueryParameter("roomId")
        val text = letter?.getQueryParameter("text")
        if (roomId == null || text == null) return null
        sent += roomId to text
        val id = "\$sent${sent.size}"
        val latest = (chats.maxOfOrNull { it["timestamp"] as Long } ?: 0) + 1
        message(roomId, id, text, at = latest, mine = true, sender = null)
        chats.replaceAll {
            if (it["roomId"] ==
                roomId
            ) {
                it + mapOf("timestamp" to latest, "messagePreview" to text)
            } else {
                it
            }
        }
        return uri.buildUpon().appendQueryParameter("messageId", id).build()
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    private companion object {
        const val DEFAULT_LIMIT = 100
        val CHAT_COLUMNS = listOf(
            "roomId",
            "title",
            "messagePreview",
            "senderEntityId",
            "protocol",
            "isMuted",
            "unreadCount",
            "timestamp",
            "oneToOne"
        )
        val MESSAGE_COLUMNS = listOf(
            "roomId",
            "originalId",
            "senderContactId",
            "timestamp",
            "isSentByMe",
            "isDeleted",
            "type",
            "text_content",
            "reactions",
            "displayName",
            "is_search_match"
        )
    }
}

/** What was seen, kept only as long as the test: the real one is a file, [StoredSeen]. */
class MemorySeen : SeenJournal {
    override val seen = MutableStateFlow(emptyMap<String, Long>())

    override suspend fun note(conversationId: String, stamp: Long) = seen.update { it + (conversationId to stamp) }
}

internal fun List<Message>?.texts(): List<String>? = this?.map { it.text }

private fun Boolean.toInt() = if (this) 1 else 0
