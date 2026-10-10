package io.github.bahuauddimitri.bercail.core.domain.agenda

import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.time.asClockTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

data class AgendaEvent(val id: String, val title: String, val start: LocalDateTime, val end: LocalDateTime)

/** The appointments of the phone's calendar. */
interface AgendaSource {
    /** The appointments of [day], emitted again when the calendar changes. */
    fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>>
}

/** The three lines at the top of the home screen. [after] is absent when the day is over. */
data class Glance(val countdown: String, val title: String, val after: String?)

/** What the top of the home screen says at [now]: the next appointment, how far it is, and the one after. */
fun glanceAt(now: LocalDateTime, events: List<AgendaEvent>): Glance {
    val upcoming = events.sortedBy { it.start }.filter { it.start > now }
    val next = upcoming.firstOrNull() ?: return Glance("Plus rien aujourd'hui", freeTitle(now), after = null)
    val after = upcoming.getOrNull(1)?.let { "Ensuite ${it.title} à ${it.start.toLocalTime().asClockTime()}" }
    return Glance(
        countdown = "${countdown(now, next.start)} · ${next.start.toLocalTime().asClockTime()}",
        title = next.title,
        after = after ?: "Rien d'autre aujourd'hui"
    )
}

/** With nothing left: the day is free until 6 pm, the evening after. */
private fun freeTitle(now: LocalDateTime): String = when (DayPart.at(now.toLocalTime())) {
    DayPart.Morning, DayPart.Afternoon -> "Journée libre"
    DayPart.Evening, DayPart.Night -> "Soirée libre"
}

/** "Dans 45 min" under an hour, "Dans 1 h 20" beyond. A minute already started still counts. */
private fun countdown(now: LocalDateTime, start: LocalDateTime): String {
    val seconds = Duration.between(now, start).seconds
    val minutes = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
    return if (minutes < MINUTES_PER_HOUR) {
        "Dans $minutes min"
    } else {
        "Dans ${minutes / MINUTES_PER_HOUR} h ${"%02d".format(minutes % MINUTES_PER_HOUR)}"
    }
}

/** An appointment on the line of the day: where it falls (0 = 7 am, 1 = midnight), its time, whether it is next. */
data class DayMark(val position: Float, val time: String, val next: Boolean)

/** The day as a line from 7 am to midnight, with the point of now. */
data class DayLine(val now: Float, val marks: List<DayMark>)

fun dayLineAt(now: LocalDateTime, events: List<AgendaEvent>): DayLine {
    val sorted = events.sortedBy { it.start }
    val next = sorted.firstOrNull { it.start > now }
    return DayLine(
        now = positionOf(now.toLocalTime()),
        marks = sorted.map { event ->
            val time = event.start.toLocalTime()
            DayMark(positionOf(time), time.asClockTime(), next = event == next)
        }
    )
}

private fun positionOf(time: LocalTime): Float =
    (time.toSecondOfDay() - LINE_START_HOUR * SECONDS_PER_HOUR) / (LINE_HOURS * SECONDS_PER_HOUR).toFloat()

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_HOUR = 3600
private const val LINE_START_HOUR = 7
private const val LINE_HOURS = 17
