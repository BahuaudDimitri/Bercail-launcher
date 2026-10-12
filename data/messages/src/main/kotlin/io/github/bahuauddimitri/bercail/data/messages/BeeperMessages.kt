package io.github.bahuauddimitri.bercail.data.messages

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** The two rights Beeper gives to another app, on Android's own screen: to read, and to send. */
object BeeperAccess {
    val permissions = listOf(
        "com.beeper.android.permission.READ_PERMISSION",
        "com.beeper.android.permission.SEND_PERMISSION"
    )

    fun granted(context: Context): Boolean =
        permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}

/**
 * The conversations of every network, as Beeper keeps them on the phone. Nothing is read before the user has
 * agreed, nor while nobody listens; Beeper itself tells when the list changes: no timer, no loop.
 */
class BeeperMessages(
    context: Context,
    private val access: AccessSource,
    private val background: CoroutineDispatcher = Dispatchers.IO
) : MessagesSource {
    private val context = context.applicationContext
    private val beeper get() = context.contentResolver

    @OptIn(ExperimentalCoroutinesApi::class)
    override val conversations: Flow<List<Conversation>> = access.granted
        .map { Access.Messages in it }
        .distinctUntilChanged()
        .flatMapLatest { allowed -> if (allowed) changes().map { read() } else flowOf(emptyList()) }
        .distinctUntilChanged()
        .flowOn(background)

    /** One signal now, then one each time Beeper says its list changed. A burst of changes counts once. */
    private fun changes(): Flow<Unit> = callbackFlow {
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

    private fun read(): List<Conversation> = try {
        beeper.query(RECENT_CHATS, null, null, null, null)?.use { rows ->
            val columns = ChatColumns(rows)
            buildList { while (rows.moveToNext()) add(columns.timeOf(rows) to columns.conversationOf(rows)) }
        }.orEmpty().sortedByDescending { it.first }.map { it.second }
    } catch (@Suppress("TooGenericExceptionCaught") _: RuntimeException) {
        // The access was taken back, or Beeper's interface, which it calls experimental, failed: no conversation.
        emptyList()
    }

    override fun openApp() {
        val beeperApp = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return
        try {
            context.startActivity(beeperApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Beeper went away in between: the screen simply stays where it is.
        }
    }

    /** Where Beeper put each thing in the rows it answers. A column it stops giving reads as empty. */
    private class ChatColumns(rows: Cursor) {
        private val id = rows.getColumnIndex("roomId")
        private val title = rows.getColumnIndex("title")
        private val protocol = rows.getColumnIndex("protocol")
        private val unread = rows.getColumnIndex("unreadCount")
        private val muted = rows.getColumnIndex("isMuted")
        private val time = rows.getColumnIndex("timestamp")

        fun timeOf(row: Cursor): Long = if (time >= 0) row.getLong(time) else 0

        fun conversationOf(row: Cursor) = Conversation(
            id = row.text(id).orEmpty(),
            name = row.text(title)?.trim().orEmpty().ifEmpty { UNNAMED },
            network = networkName(row.text(protocol)),
            unread = if (unread >= 0) row.getInt(unread) else 0,
            muted = muted >= 0 && row.getInt(muted) == 1
        )

        private fun Cursor.text(column: Int): String? = if (column >= 0) getString(column) else null
    }

    private companion object {
        const val PACKAGE = "com.beeper.android"
        const val UNNAMED = "Sans nom"
        val CHATS: Uri = Uri.parse("content://com.beeper.api/chats")

        /** The home screen has no use for more, nor for what the user put away in Beeper. */
        val RECENT_CHATS: Uri = CHATS.buildUpon()
            .appendQueryParameter("limit", "100")
            .appendQueryParameter("isArchived", "0")
            .appendQueryParameter("isLowPriority", "0")
            .build()
    }
}

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
