package io.github.bahuauddimitri.bercail.core.domain

import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The sources whose wave has not come yet. Each one says "nothing", so that the home screen shows its empty
 * states (no message, house at rest, nothing playing) instead of made-up data.
 */
object Unconnected {
    val messages: MessagesSource = object : MessagesSource {
        override val conversations: Flow<List<Conversation>> = flowOf(emptyList())
    }

    val home: HomeSource = object : HomeSource {
        override val controls: Flow<List<HomeControl>> = flowOf(emptyList())

        override suspend fun toggle(id: String) = Unit
    }

    val media: MediaSource = object : MediaSource {
        override val state: Flow<MediaState> = flowOf(MediaState.None)
        override val position: Flow<Duration> = flowOf(Duration.ZERO)

        override fun play() = Unit
        override fun pause() = Unit
        override fun next() = Unit
        override fun previous() = Unit
        override fun seekTo(position: Duration) = Unit
        override fun openApp() = Unit
        override fun resume() = Unit
    }
}
