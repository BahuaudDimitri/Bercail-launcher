package io.github.bahuauddimitri.bercail.core.domain.agenda

import io.github.bahuauddimitri.bercail.core.domain.time.asClockTime
import java.time.LocalDate
import java.time.LocalDateTime

/** Past: over. Next: the next appointment to begin, always the one the accent ring marks. Coming: the others. */
enum class DayRowState { Past, Coming, Next }

/**
 * One appointment in the list of the day. [allDay] marks one that lasts the whole day; its [time] is then
 * "Journée".
 */
data class DayRow(
    val id: String,
    val time: String,
    val title: String,
    val state: DayRowState,
    val allDay: Boolean = false
)

/**
 * The day as the « Aujourd'hui » sheet lists it at [now]: whole-day events first, then the appointments in the
 * order of their hour. The next one to begin is brought forward, whichever appointment the sheet opens on.
 */
fun dayRowsAt(now: LocalDateTime, events: List<AgendaEvent>): List<DayRow> {
    val sorted = events.sortedWith(compareBy({ !it.allDay }, { it.start }))
    val next = sorted.firstOrNull { !it.allDay && it.start > now }?.id
    return sorted.map { event ->
        DayRow(
            id = event.id,
            time = if (event.allDay) WHOLE_DAY else event.start.toLocalTime().asClockTime(),
            title = event.title,
            state = when {
                event.id == next -> DayRowState.Next
                !event.allDay && event.end <= now -> DayRowState.Past
                else -> DayRowState.Coming
            },
            allDay = event.allDay
        )
    }
}

/** A date as the sheet writes it under its title: "vendredi 2 octobre", "samedi 1er août". */
fun LocalDate.inWords(): String {
    val number = if (dayOfMonth == 1) "1er" else dayOfMonth.toString()
    return "${WEEK_DAYS[dayOfWeek.value - 1]} $number ${MONTHS[monthValue - 1]}"
}

private const val WHOLE_DAY = "Journée"
private val WEEK_DAYS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
private val MONTHS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre"
)
