package io.github.bahuauddimitri.bercail.core.domain.agenda

import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.time.asClockTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** An appointment. One that lasts the [allDay] has no hour: it is listed, but never announced as "the next one". */
data class AgendaEvent(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false
)

/** The appointments of the phone's calendar. */
interface AgendaSource {
    /** The appointments of [day], emitted again when the calendar changes. Nothing is read while nobody listens. */
    fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>>

    /** Shows the appointment in the phone's calendar app. */
    fun open(event: AgendaEvent)

    /** Shows that day in the phone's calendar app. */
    fun openCalendar(day: LocalDate)
}

/** The title of an appointment that has none. */
const val UNTITLED_EVENT = "Sans titre"

/**
 * The three lines at the top of the home screen. [after] is absent when the day is over. With nothing left to come
 * today ([leadsToCalendar]), touching them opens the phone's calendar app rather than the day, which has nothing ahead.
 */
data class Glance(val countdown: String, val title: String, val after: String?, val leadsToCalendar: Boolean = false)

/** While Bercail may not read the calendar, the top of the home screen offers to link it: never a made-up free day. */
val closedGlance = Glance("Agenda pas encore relié", "Relier l'agenda", after = "Toucher pour autoriser sa lecture")

/** What the top of the home screen says at [now]: the next appointment, how far it is, and the one after. */
fun glanceAt(now: LocalDateTime, events: List<AgendaEvent>): Glance {
    val upcoming = events.filterNot { it.allDay }.sortedBy { it.start }.filter { it.start > now }
    val next = upcoming.firstOrNull() ?: return Glance(
        "Plus rien aujourd'hui",
        freeTitle(now),
        after = null,
        leadsToCalendar = true
    )
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

/**
 * An appointment on the line of the day: where it falls (0 = 7 am, 1 = midnight), its time, whether it is next,
 * and which appointment it is ([id], [title]), so that the line can show it and open it. A [brief] mark comes less
 * than 30 minutes after the one before: it only writes its time, so that two titles never overlap.
 */
data class DayMark(
    val position: Float,
    val time: String,
    val next: Boolean,
    val id: String,
    val title: String,
    val brief: Boolean = false
)

/** The day as a line from 7 am to midnight, with the point of now. */
data class DayLine(val now: Float, val marks: List<DayMark>)

fun dayLineAt(now: LocalDateTime, events: List<AgendaEvent>): DayLine {
    val sorted = events.filterNot { it.allDay }.sortedBy { it.start }
    val next = sorted.firstOrNull { it.start > now }
    return DayLine(
        now = positionOf(now.toLocalTime()),
        marks = sorted.mapIndexed { index, event ->
            val time = event.start.toLocalTime()
            val before = sorted.getOrNull(index - 1)
            DayMark(
                position = positionOf(time),
                time = time.asClockTime(),
                next = event == next,
                id = event.id,
                title = event.title,
                brief = before != null && Duration.between(before.start, event.start) < TITLES_APART
            )
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

/** Two titles on the line stand at least this far apart in time. */
private val TITLES_APART: Duration = Duration.ofMinutes(30)
