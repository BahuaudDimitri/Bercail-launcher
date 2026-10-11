package io.github.bahuauddimitri.bercail.data.agenda

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Android's calendar, played by the tests: it answers like the real one (same address, same columns) with the
 * appointments it was given, and remembers what it was asked.
 */
class FakeCalendar : ContentProvider() {
    private val instances = mutableListOf<Map<String, Any?>>()

    /** Every question asked so far, in order. */
    val asked = mutableListOf<Uri>()

    /** True once Android no longer lets Bercail read the calendar. */
    var locked = false

    /** An appointment as Android stores it: hours in milliseconds, in the zone of the phone. */
    fun add(
        eventId: Long,
        title: String?,
        start: LocalDateTime,
        end: LocalDateTime,
        zone: ZoneId,
        visible: Boolean = true,
        status: Int? = null,
        answer: Int = Attendees.ATTENDEE_STATUS_NONE
    ) {
        instances += row(eventId, title, start.millisIn(zone), end.millisIn(zone), allDay = false) +
            mapOf(
                Instances.VISIBLE to visible.toInt(),
                Instances.STATUS to status,
                Instances.SELF_ATTENDEE_STATUS to answer
            )
    }

    /** A whole-day event: Android keeps its days in universal time, whatever the zone of the phone. */
    fun addWholeDay(eventId: Long, title: String, first: LocalDate, last: LocalDate = first) {
        val start = first.atStartOfDay().millisIn(ZoneOffset.UTC)
        val end = last.plusDays(1).atStartOfDay().millisIn(ZoneOffset.UTC)
        instances += row(eventId, title, start, end, allDay = true)
    }

    fun clear() = instances.clear()

    private fun row(eventId: Long, title: String?, begin: Long, end: Long, allDay: Boolean): Map<String, Any?> = mapOf(
        Instances.EVENT_ID to eventId,
        Instances.TITLE to title,
        Instances.BEGIN to begin,
        Instances.END to end,
        Instances.ALL_DAY to allDay.toInt(),
        Instances.VISIBLE to 1,
        Instances.STATUS to null,
        Instances.SELF_ATTENDEE_STATUS to Attendees.ATTENDEE_STATUS_NONE
    )

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (locked) throw SecurityException("READ_CALENDAR was taken back")
        asked += uri
        val columns = checkNotNull(projection) { "Bercail names the columns it reads." }
        return MatrixCursor(columns).apply {
            instances.forEach { instance -> addRow(columns.map { instance[it] }) }
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}

internal fun LocalDateTime.millisIn(zone: ZoneId): Long = atZone(zone).toInstant().toEpochMilli()

private fun Boolean.toInt() = if (this) 1 else 0
