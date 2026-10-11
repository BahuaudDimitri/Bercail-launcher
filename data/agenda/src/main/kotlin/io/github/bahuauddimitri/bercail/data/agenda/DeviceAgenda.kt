package io.github.bahuauddimitri.bercail.data.agenda

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.agenda.UNTITLED_EVENT
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * The calendar of the phone, with every account the user has put in it. Nothing is read before the user has
 * agreed, nor while nobody listens; Android itself tells when the calendar changes: no timer, no loop.
 */
class DeviceAgenda(
    context: Context,
    private val access: AccessSource,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val background: CoroutineDispatcher = Dispatchers.IO
) : AgendaSource {
    private val context = context.applicationContext
    private val calendar get() = context.contentResolver

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun eventsOn(day: LocalDate): Flow<List<AgendaEvent>> = access.granted
        .map { Access.Calendar in it }
        .distinctUntilChanged()
        .flatMapLatest { allowed -> if (allowed) changes().map { read(day) } else flowOf(emptyList()) }
        .distinctUntilChanged()
        .flowOn(background)

    /** One signal now, then one each time Android says the calendar changed. A burst of changes counts once. */
    private fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        calendar.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        trySend(Unit)
        awaitClose { calendar.unregisterContentObserver(observer) }
    }.conflate()

    private fun read(day: LocalDate): List<AgendaEvent> {
        val zone = zone()
        val whenToday = Instances.CONTENT_URI.buildUpon()
            .appendPath(day.atStartOfDay(zone).toInstant().toEpochMilli().toString())
            .appendPath(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli().toString())
            .build()
        return try {
            calendar.query(whenToday, COLUMNS, null, null, Instances.BEGIN)?.use { rows ->
                buildList { while (rows.moveToNext()) rows.eventOn(day, zone)?.let(::add) }
            }.orEmpty()
        } catch (_: SecurityException) {
            // The user took the access back while Bercail was reading.
            emptyList()
        }
    }

    /** The row as an appointment of [day], unless it is hidden, cancelled, declined or belongs to another day. */
    private fun Cursor.eventOn(day: LocalDate, zone: ZoneId): AgendaEvent? {
        val begin = getLong(BEGIN)
        val end = getLong(END)
        val allDay = getInt(ALL_DAY) == 1
        val shown = getInt(VISIBLE) == 1 &&
            getInt(STATUS) != Instances.STATUS_CANCELED &&
            getInt(ANSWER) != Attendees.ATTENDEE_STATUS_DECLINED
        // Android keeps whole days in universal time, whatever the zone of the phone.
        val readIn = if (allDay) ZoneOffset.UTC else zone
        val start = Instant.ofEpochMilli(begin).atZone(readIn).toLocalDateTime()
        val finish = Instant.ofEpochMilli(end).atZone(readIn).toLocalDateTime()
        val today = if (allDay) {
            start.toLocalDate() <= day && day < finish.toLocalDate()
        } else {
            start.toLocalDate() == day
        }
        return if (shown && today) {
            AgendaEvent(
                id = listOf(getLong(EVENT_ID), begin, end).joinToString(ID_SEPARATOR),
                title = getString(TITLE).orEmpty().ifBlank { UNTITLED_EVENT },
                start = if (allDay) day.atStartOfDay() else start,
                end = if (allDay) day.plusDays(1).atStartOfDay() else finish,
                allDay = allDay
            )
        } else {
            null
        }
    }

    override fun open(event: AgendaEvent) {
        val (id, begin, end) = event.id.split(ID_SEPARATOR).mapNotNull(String::toLongOrNull)
            .takeIf { it.size == ID_PARTS } ?: return
        show(
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        )
    }

    override fun openCalendar(day: LocalDate) {
        val time: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        ContentUris.appendId(time, day.atStartOfDay(zone()).toInstant().toEpochMilli())
        show(Intent(Intent.ACTION_VIEW, time.build()))
    }

    private fun show(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No calendar app on this phone: the screen simply stays where it is.
        }
    }

    private companion object {
        val COLUMNS = arrayOf(
            Instances.EVENT_ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.VISIBLE,
            Instances.STATUS,
            Instances.SELF_ATTENDEE_STATUS
        )
        const val EVENT_ID = 0
        const val TITLE = 1
        const val BEGIN = 2
        const val END = 3
        const val ALL_DAY = 4
        const val VISIBLE = 5
        const val STATUS = 6
        const val ANSWER = 7

        /** An occurrence is named by its event and its hours: a repeating event has one event, many occurrences. */
        const val ID_SEPARATOR = "/"
        const val ID_PARTS = 3
    }
}
