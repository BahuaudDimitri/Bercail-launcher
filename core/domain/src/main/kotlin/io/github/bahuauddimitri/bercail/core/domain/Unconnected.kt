package io.github.bahuauddimitri.bercail.core.domain

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.weather.Weather
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import java.time.LocalDate
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The sources whose wave has not come yet. Each one says "nothing", so that the home screen shows its empty
 * states (no appointment, no message, house at rest, nothing playing) instead of made-up data.
 */
object Unconnected {
    val agenda: AgendaSource = object : AgendaSource {
        override fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>> = flowOf(emptyList())
    }

    val weather: WeatherSource = object : WeatherSource {
        override val weather: Flow<Weather?> = flowOf(null)
    }

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
    }
}
