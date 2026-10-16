package io.github.bahuauddimitri.bercail.feature.home

import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.PreviousAction
import io.github.bahuauddimitri.bercail.core.domain.media.positionAt
import io.github.bahuauddimitri.bercail.core.domain.media.previousAction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the home screen needs to listen: what plays, the apps that can play, and the way to open them. */
class ListenSources(val media: MediaSource, val mediaApps: MediaAppsSource, val apps: AppsSource)

/** What the home screen does about what plays: the remote, the way to the apps, the « Écouter » sheet. */
interface ListenActions {
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onSeek(fraction: Float)

    /** Opens or closes the « Écouter » sheet, where the music pill leads while nothing is loaded. */
    fun onListenOpenChange(open: Boolean)

    /** Shows what is loaded in its own app. */
    fun onOpenMediaApp()

    /** Opens an app as it was left: a media app chosen in the « Écouter » sheet, an app of the dock. */
    fun onOpenApp(appId: String)

    /**
     * Asks the main media app to play again what it last played, without leaving the home screen. If nothing
     * plays a moment later, the app is opened instead: the touch always does something.
     */
    fun onResume(appId: String)
}

/** The remote of the home screen. Its buttons are about [shown], what the screen shows at that moment. */
internal class HomeRemote(private val sources: ListenSources) :
    ListenActions,
    AutoCloseable {
    private val media get() = sources.media
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Whether the « Écouter » sheet is open. */
    val sheetOpen = MutableStateFlow(false)

    var shown: MediaState = MediaState.None

    override fun onPlayPause() {
        if (shown.isPlaying) media.pause() else media.play()
    }

    override fun onNext() = media.next()

    override fun onPrevious() {
        // Going back to the start needs an app that lets the remote move in the content.
        val seeks = MediaAction.Seek in (shown as? MediaState.Loaded)?.actions.orEmpty()
        scope.launch {
            when (if (seeks) previousAction(media.position.first()) else PreviousAction.PreviousTrack) {
                PreviousAction.Restart -> media.seekTo(Duration.ZERO)
                PreviousAction.PreviousTrack -> media.previous()
            }
        }
    }

    override fun onSeek(fraction: Float) {
        (shown as? MediaState.Loaded)?.let { media.seekTo(positionAt(fraction, it.content.duration)) }
    }

    override fun onListenOpenChange(open: Boolean) {
        sheetOpen.value = open
    }

    override fun onOpenMediaApp() = media.openApp()

    override fun onOpenApp(appId: String) = sources.apps.open(appId)

    override fun onResume(appId: String) {
        media.resume()
        scope.launch {
            delay(RESUME_WAIT)
            if (!shown.isLoaded) sources.apps.open(appId)
        }
    }

    override fun close() = scope.cancel()

    private companion object {
        /** How long an app may take to start playing again before Bercail opens it instead. */
        val RESUME_WAIT = 2.seconds
    }
}
