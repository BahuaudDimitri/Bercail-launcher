package io.github.bahuauddimitri.bercail.data.messages

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Beeper, played by the tests: it answers at the same address and with the same columns as the real one, with the
 * conversations it was given, and remembers what it was asked.
 */
class FakeBeeper : ContentProvider() {
    private val chats = mutableListOf<Map<String, Any?>>()

    /** Every question asked so far, in order. */
    val asked = mutableListOf<Uri>()

    /** True once the user took back, in Android's settings, the right to read Beeper. */
    var locked = false

    /** True when Beeper fails by itself: its interface for other apps is experimental. */
    var broken = false

    /** A conversation as Beeper stores it. [at] is when it last moved, in milliseconds. */
    fun chat(
        roomId: String,
        title: String?,
        protocol: String? = "whatsapp",
        unread: Int = 0,
        at: Long = 0,
        muted: Boolean = false
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
            "oneToOne" to 1
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
        return when (uri.path) {
            "/chats" -> cursorOf(CHAT_COLUMNS, chats)
            else -> null
        }
    }

    private fun cursorOf(columns: List<String>, rows: List<Map<String, Any?>>): Cursor =
        MatrixCursor(columns.toTypedArray()).apply { rows.forEach { row -> addRow(columns.map { row[it] }) } }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    private companion object {
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
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
