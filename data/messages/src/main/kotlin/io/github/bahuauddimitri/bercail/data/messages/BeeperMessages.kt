package io.github.bahuauddimitri.bercail.data.messages

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.FoundMessage
import io.github.bahuauddimitri.bercail.core.domain.messages.Message
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

/** The two rights Beeper gives to another app, on Android's own screen: to read, and to send. */
object BeeperAccess {
    /** Beeper, as Android names it. */
    const val PACKAGE = "com.beeper.android"

    val permissions = listOf(
        "com.beeper.android.permission.READ_PERMISSION",
        "com.beeper.android.permission.SEND_PERMISSION"
    )

    fun granted(context: Context): Boolean =
        permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}

/**
 * The conversations of every network, as Beeper keeps them on the phone. Nothing is read before the user has
 * agreed, nor while nobody listens; Beeper itself tells when the list changes: no timer, no loop. The messages of
 * a conversation, slow to read, are only read while that conversation is open, and kept for the next time.
 */
class BeeperMessages(
    context: Context,
    access: AccessSource,
    private val seen: SeenJournal,
    /** One signal each time Beeper shows or removes a notification: see [changes]. */
    private val notifications: Flow<Unit> = emptyFlow(),
    private val settle: Duration = 1.seconds,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val background: CoroutineDispatcher = Dispatchers.IO
) : MessagesSource {
    private val context = context.applicationContext
    private val beeper = BeeperProvider(this.context.contentResolver)
    private val allowed: Flow<Boolean> = access.granted.map { Access.Messages in it }.distinctUntilChanged()

    /** What was last read of each conversation opened since Bercail started. Never written to a file. */
    private val read = ConcurrentHashMap<String, List<Message>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val conversations: Flow<List<Conversation>> = allowed
        .flatMapLatest { agreed ->
            if (!agreed) return@flatMapLatest flowOf(emptyList())
            combine(changes().map { beeper.chats() }, seen.seen) { chats, seen ->
                chats.map { it.unlessSeenAt(seen[it.conversation.id]) }
            }
        }
        .distinctUntilChanged()
        .flowOn(background)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun history(conversationId: String): Flow<List<Message>?> = allowed
        .flatMapLatest { agreed ->
            if (!agreed) return@flatMapLatest flowOf(emptyList())
            changes()
                // All that is known is that "something" changed: the messages are read again if it is here.
                .map { beeper.chat(conversationId)?.mark }
                .distinctUntilChanged()
                .map<String?, List<Message>?> {
                    beeper.messages(conversationId, zone()).also { read[conversationId] = it }
                }
                .onStart { emit(read[conversationId]) }
        }
        .distinctUntilChanged()
        .flowOn(background)

    /**
     * One signal now, then one each time something may have changed. Beeper is supposed to tell when its list
     * changes; measured on the Pixel, it does not when a message arrives. Its notifications do: each one that shows
     * or goes away is a signal too, and a second look follows a moment later, once Beeper has put its list in order.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun changes(): Flow<Unit> = merge(
        beeper.changes(),
        notifications.transformLatest {
            emit(Unit)
            delay(settle)
            emit(Unit)
        }
    ).conflate()

    override suspend fun send(conversationId: String, text: String): Boolean =
        allowed.first() && withContext(background) { beeper.send(conversationId, text) }

    override suspend fun search(text: String): List<FoundMessage> =
        if (allowed.first()) withContext(background) { beeper.search(text, zone()) } else emptyList()

    /**
     * Beeper has no way to be told that a conversation was read. Bercail notes when the conversation last moved:
     * until it moves again, its unread messages no longer count here.
     */
    override suspend fun markRead(conversationId: String) {
        if (!allowed.first()) return
        withContext(background) { beeper.chat(conversationId) }?.let { seen.note(conversationId, it.stamp) }
    }

    override fun openApp() {
        val beeperApp = context.packageManager.getLaunchIntentForPackage(BeeperAccess.PACKAGE) ?: return
        try {
            context.startActivity(beeperApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Beeper went away in between: the screen simply stays where it is.
        }
    }
}
