package io.github.bahuauddimitri.bercail.feature.home

import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.PreviousAction
import io.github.bahuauddimitri.bercail.core.domain.media.positionAt
import io.github.bahuauddimitri.bercail.core.domain.media.previousAction
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the home screen does about what plays: the remote, the way to the app, the « Écouter » sheet. */
interface ListenActions {
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onSeek(fraction: Float)

    /** Opens or closes the « Écouter » sheet, where the music pill leads while nothing is loaded. */
    fun onListenOpenChange(open: Boolean)

    /** Shows what is loaded in its own app. */
    fun onOpenMediaApp()
}

/** The remote of the home screen. Its buttons are about [shown], what the screen shows at that moment. */
internal class HomeRemote(val media: MediaSource) :
    ListenActions,
    AutoCloseable {
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

    override fun close() = scope.cancel()
}
