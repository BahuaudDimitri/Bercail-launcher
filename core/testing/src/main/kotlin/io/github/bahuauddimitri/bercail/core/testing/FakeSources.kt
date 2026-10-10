package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import io.github.bahuauddimitri.bercail.core.domain.weather.Weather
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** A clock the tests set by hand. */
class FakeClock(start: LocalDateTime) : Clock {
    private val time = MutableStateFlow(start)

    override fun now(): LocalDateTime = time.value

    override val minutes: Flow<LocalDateTime> = time

    fun set(now: LocalDateTime) {
        time.value = now
    }

    fun advanceMinutes(minutes: Long) = time.update { it.plusMinutes(minutes) }
}

/** A calendar holding [events]. With [everyDay], the same day repeats on every date. */
class FakeAgendaSource(events: List<AgendaEvent> = emptyList(), private val everyDay: Boolean = false) : AgendaSource {
    private val events = MutableStateFlow(events)

    override fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>> = events.map { all ->
        if (everyDay) {
            all.map { it.copy(start = day.atTime(it.start.toLocalTime()), end = day.atTime(it.end.toLocalTime())) }
        } else {
            all.filter { it.start.toLocalDate() == day }
        }
    }

    fun set(events: List<AgendaEvent>) {
        this.events.value = events
    }
}

class FakeWeatherSource(weather: Weather? = null) : WeatherSource {
    override val weather = MutableStateFlow(weather)

    fun set(weather: Weather?) {
        this.weather.value = weather
    }
}

class FakeMessagesSource(conversations: List<Conversation> = emptyList()) : MessagesSource {
    override val conversations = MutableStateFlow(conversations)

    /** One more unread message from this person. */
    fun receive(id: String) = change(id) { it.copy(unread = it.unread + 1) }

    fun markRead(id: String) = change(id) { it.copy(unread = 0) }

    private fun change(id: String, transform: (Conversation) -> Conversation) =
        conversations.update { all -> all.map { if (it.id == id) transform(it) else it } }
}

class FakeSettingsSource(settings: Settings = Settings()) : SettingsSource {
    override val settings = MutableStateFlow(settings)

    fun update(transform: (Settings) -> Settings) = settings.update(transform)
}
