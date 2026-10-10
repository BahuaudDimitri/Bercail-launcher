package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A player with a playlist. Without [playlist], nothing is loaded. With [ticking], time moves by itself,
 * one second per second, only while playing and while someone watches the position.
 */
class FakeMediaSource(
    playlist: List<MediaContent> = emptyList(),
    playing: Boolean = false,
    position: Duration = Duration.ZERO,
    private val ticking: Boolean = false
) : MediaSource {
    private data class Playback(
        val playlist: List<MediaContent>,
        val index: Int,
        val playing: Boolean,
        val position: Duration
    ) {
        val content: MediaContent? get() = playlist.getOrNull(index)

        fun moved(step: Int) = copy(index = (index + step).mod(playlist.size), position = Duration.ZERO, playing = true)
    }

    private val playback = MutableStateFlow(Playback(playlist, index = 0, playing = playing, position = position))

    override val state: Flow<MediaState> = playback
        .map { it.content?.let { content -> MediaState.Loaded(content, it.playing) } ?: MediaState.None }
        .distinctUntilChanged()

    override val position: Flow<Duration> = flow {
        coroutineScope {
            if (ticking) {
                launch {
                    while (true) {
                        delay(1.seconds)
                        tick()
                    }
                }
            }
            emitAll(playback.map { it.position }.distinctUntilChanged())
        }
    }

    override fun play() = whenLoaded { it.copy(playing = true) }

    override fun pause() = whenLoaded { it.copy(playing = false) }

    override fun next() = whenLoaded { it.moved(1) }

    override fun previous() = whenLoaded { it.moved(-1) }

    override fun seekTo(position: Duration) = whenLoaded { it.copy(position = position) }

    /** The media app closes its session: nothing is loaded any more. */
    fun stop() = playback.update {
        it.copy(playlist = emptyList(), index = 0, playing = false, position = Duration.ZERO)
    }

    /** A media app starts playing [playlist]. */
    fun load(playlist: List<MediaContent>) =
        playback.update { Playback(playlist, index = 0, playing = true, position = Duration.ZERO) }

    private fun tick() = whenLoaded { now ->
        val content = now.content
        when {
            !now.playing || content == null -> now
            now.position + 1.seconds >= content.duration -> now.moved(1)
            else -> now.copy(position = now.position + 1.seconds)
        }
    }

    private fun whenLoaded(transform: (Playback) -> Playback) =
        playback.update { if (it.content == null) it else transform(it) }
}
