package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.AlarmSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
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

/** A calendar holding [events]. It remembers what was shown in the calendar app. */
class FakeAgendaSource(events: List<AgendaEvent> = emptyList()) : AgendaSource {
    private val events = MutableStateFlow(events)

    /** What the calendar app was asked to show so far, in order: "event: Stand-up", "calendar: 2026-10-02". */
    val opened = mutableListOf<String>()

    override fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>> =
        events.map { all -> all.filter { it.start.toLocalDate() == day } }

    override fun open(event: AgendaEvent) {
        opened += "event: ${event.title}"
    }

    override fun openCalendar(day: LocalDate) {
        opened += "calendar: $day"
    }

    fun set(events: List<AgendaEvent>) {
        this.events.value = events
    }
}

/** An alarm clock holding the [next] alarm. It counts how many times its alarms were shown. */
class FakeAlarmSource(next: LocalDateTime? = null) : AlarmSource {
    override val next = MutableStateFlow(next)

    var shown = 0
        private set

    override fun openAlarms() {
        shown++
    }
}

class FakeSettingsSource(settings: Settings = Settings()) : SettingsSource {
    override val settings = MutableStateFlow(settings)

    override suspend fun update(transform: (Settings) -> Settings) = settings.update(transform)

    /** The same change, for tests that are not in a coroutine. */
    fun change(transform: (Settings) -> Settings) = settings.update(transform)
}
