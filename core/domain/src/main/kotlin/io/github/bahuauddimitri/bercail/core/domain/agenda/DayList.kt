package io.github.bahuauddimitri.bercail.core.domain.agenda

import io.github.bahuauddimitri.bercail.core.domain.time.asClockTime
import java.time.LocalDate
import java.time.LocalDateTime

/** Past: over. Highlighted: the next one, or the one the search pointed at. */
enum class DayRowState { Past, Coming, Highlighted }

/** One appointment in the list of the day. [time] is "Journée" for one that lasts the whole day. */
data class DayRow(val id: String, val time: String, val title: String, val state: DayRowState)

/**
 * The day as the « Aujourd'hui » sheet lists it at [now]: whole-day events first, then the appointments in the
 * order of their hour. The one named by [focus] is brought forward; without it, the next one is.
 */
fun dayRowsAt(now: LocalDateTime, events: List<AgendaEvent>, focus: String? = null): List<DayRow> {
    val sorted = events.sortedWith(compareBy({ !it.allDay }, { it.start }))
    val highlighted = focus ?: sorted.firstOrNull { !it.allDay && it.start > now }?.id
    return sorted.map { event ->
        DayRow(
            id = event.id,
            time = if (event.allDay) WHOLE_DAY else event.start.toLocalTime().asClockTime(),
            title = event.title,
            state = when {
                event.id == highlighted -> DayRowState.Highlighted
                !event.allDay && event.end <= now -> DayRowState.Past
                else -> DayRowState.Coming
            }
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
