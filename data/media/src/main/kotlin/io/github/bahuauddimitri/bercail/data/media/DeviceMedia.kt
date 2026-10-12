package io.github.bahuauddimitri.bercail.data.media

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.shownAmong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

/** The buttons of one media app. Android's own, except in the tests, which press theirs. */
internal interface MediaButtons {
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun seekTo(millis: Long)
}

private class SessionButtons(session: MediaController) : MediaButtons {
    private val controls = session.transportControls

    override fun play() = controls.play()
    override fun pause() = controls.pause()
    override fun next() = controls.skipToNext()
    override fun previous() = controls.skipToPrevious()
    override fun seekTo(millis: Long) = controls.seekTo(millis)
}

/**
 * What plays on the phone, as the media apps tell it through Android's media sessions. Nothing is read before
 * the user has given the "notification access", nor while nobody listens. Android itself tells when a session
 * comes, goes or changes: no timer, no loop. Only the position of a playing content is looked at once a second,
 * and only while the remote is on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceMedia internal constructor(
    context: Context,
    private val access: AccessSource,
    private val background: CoroutineDispatcher,
    private val elapsed: () -> Long,
    private val buttons: (MediaController) -> MediaButtons
) : MediaSource {
    constructor(context: Context, access: AccessSource) :
        this(context, access, Dispatchers.Default, SystemClock::elapsedRealtime, ::SessionButtons)

    private val context = context.applicationContext
    private val listener = MediaAccess.listener(this.context)
    private val android get() = checkNotNull(context.getSystemService(MediaSessionManager::class.java))
    private val reader = SessionReader(this.context)

    /** The session the home screen shows while someone listens: what the buttons and the position are about. */
    private val shown = MutableStateFlow<Heard?>(null)

    override val state: Flow<MediaState> = access.granted
        .map { Access.Notifications in it }
        .distinctUntilChanged()
        .flatMapLatest { allowed -> if (allowed) sessions() else flowOf(emptyList()) }
        .map { heard -> withContext(background) { chosenAmong(heard) } }
        .onCompletion { shown.value = null }
        .distinctUntilChanged()

    override val position: Flow<Duration> = shown
        .flatMapLatest { heard ->
            if (heard == null) return@flatMapLatest flowOf(Duration.ZERO)
            flow {
                emit(heard.positionAt(elapsed()))
                while (heard.isMoving) {
                    delay(1.seconds)
                    emit(heard.positionAt(elapsed()))
                }
            }
        }
        .distinctUntilChanged()

    override fun play() = press { play() }

    override fun pause() = press { pause() }

    override fun next() = press { next() }

    override fun previous() = press { previous() }

    override fun seekTo(position: Duration) = press { seekTo(position.inWholeMilliseconds) }

    override fun openApp() {
        shown.value?.session?.let(context::show)
    }

    /** The play button of the phone itself: Android hands it to the app that played last, as for a headset. */
    override fun resume() {
        val audio = checkNotNull(context.getSystemService(AudioManager::class.java))
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach {
            audio.dispatchMediaKeyEvent(KeyEvent(it, KeyEvent.KEYCODE_MEDIA_PLAY))
        }
    }

    private fun press(button: MediaButtons.() -> Unit) {
        shown.value?.let { buttons(it.session).button() }
    }

    /**
     * Every session of the phone, in Android's order (the one that played last first), told again each time a
     * session comes, goes or changes. Nothing stays registered once nobody listens.
     */
    private fun sessions(): Flow<List<Heard>> = callbackFlow {
        val main = Handler(Looper.getMainLooper())
        val followed = Followed(main) { trySend(it) }
        val changes = MediaSessionManager.OnActiveSessionsChangedListener { followed.follow(it.orEmpty()) }
        try {
            android.addOnActiveSessionsChangedListener(changes, listener, main)
            followed.follow(android.getActiveSessions(listener))
        } catch (_: SecurityException) {
            // The user took the access back since Bercail last looked.
            trySend(emptyList())
        }
        awaitClose {
            android.removeOnActiveSessionsChangedListener(changes)
            followed.follow(emptyList())
        }
    }.conflate()

    /** The session to show, by the rule of the domain. */
    private fun chosenAmong(heard: List<Heard>): MediaState {
        reader.keepOnly(heard)
        val loaded = heard.mapNotNull { one -> reader.loaded(one)?.let { one to it } }
        val chosen = shownAmong(loaded.map { it.second })
        shown.value = loaded.firstOrNull { it.second === chosen }?.first
        return chosen
    }
}

/**
 * Shows what [session] plays in its own app: by the way the app itself offers (its player, its video), otherwise
 * as its icon opens it.
 */
private fun Context.show(session: MediaController) {
    if (session.sessionActivity?.sentFrom(this) == true) return
    val launch = packageManager.getLaunchIntentForPackage(session.packageName) ?: return
    try {
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // The app is gone: the screen simply stays where it is.
    }
}

private fun PendingIntent.sentFrom(context: Context): Boolean = try {
    // The user asked for it with a touch: Android may let the app come to the front.
    val allowed = ActivityOptions.makeBasic()
        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE)
    send(context, 0, null, null, null, null, allowed.toBundle())
    true
} catch (_: PendingIntent.CanceledException) {
    false
}
