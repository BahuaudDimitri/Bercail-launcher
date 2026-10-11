package io.github.bahuauddimitri.bercail.data.agenda

import android.app.Application
import android.content.Intent
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.testing.FakeAccess
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** The appointments of the day, read from Android's calendar. The calendar is played by [FakeCalendar]. */
@RunWith(AndroidJUnit4::class)
class DeviceAgendaUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val calendar = Robolectric.buildContentProvider(FakeCalendar::class.java)
        .create(CalendarContract.AUTHORITY)
        .get()
    private val access = FakeAccess()
    private val paris = ZoneId.of("Europe/Paris")
    private val day = LocalDate.of(2026, 10, 2)

    private fun agendaIn(zone: ZoneId) = DeviceAgenda(phone, access, zone = {
        zone
    }, background = Dispatchers.Unconfined)

    private val agenda = agendaIn(paris)

    private fun today(): List<AgendaEvent> = runBlocking { agenda.eventsOn(day).first() }

    private fun at(hour: Int, minute: Int = 0): LocalDateTime = day.atTime(hour, minute)

    @Test
    fun `les rendez-vous du jour sont lus avec leur titre et leurs heures`() {
        calendar.add(1, "Stand-up", at(9, 30), at(10), paris)
        calendar.add(2, "Déjeuner avec Léa", at(12, 30), at(13, 30), paris)

        val events = today()

        assertThat(events.map { it.title }).containsExactly("Stand-up", "Déjeuner avec Léa")
        assertThat(events.first().start).isEqualTo(at(9, 30))
        assertThat(events.first().end).isEqualTo(at(10))
        assertThat(events.first().allDay).isFalse()
    }

    @Test
    fun `Android est interrogé sur la journée seulement, de minuit à minuit`() {
        today()

        val midnight = day.atStartOfDay().millisIn(paris)
        val nextMidnight = day.plusDays(1).atStartOfDay().millisIn(paris)
        assertThat(calendar.asked.map { it.toString() })
            .containsExactly("content://com.android.calendar/instances/when/$midnight/$nextMidnight")
    }

    @Test
    fun `un rendez-vous qui se répète garde un identifiant par occurrence`() {
        calendar.add(7, "Pause", at(10), at(10, 15), paris)
        calendar.add(7, "Pause", at(16), at(16, 15), paris)

        val events = today()

        assertThat(events).hasSize(2)
        assertThat(events[0].id).isNotEqualTo(events[1].id)
    }

    @Test
    fun `un événement sur la journée entière est reconnu, et couvre la journée du téléphone`() {
        calendar.addWholeDay(3, "Anniversaire de Léa", day)

        val birthday = today().single()

        assertThat(birthday.allDay).isTrue()
        assertThat(birthday.start).isEqualTo(day.atStartOfDay())
        assertThat(birthday.end).isEqualTo(day.plusDays(1).atStartOfDay())
    }

    @Test
    fun `la journée entière d'hier ou de demain n'est pas celle d'aujourd'hui, quel que soit le fuseau`() {
        calendar.addWholeDay(3, "Hier", day.minusDays(1))
        calendar.addWholeDay(4, "Demain", day.plusDays(1))

        listOf("Europe/Paris", "America/Los_Angeles", "Asia/Tokyo").forEach { zone ->
            val events = runBlocking { agendaIn(ZoneId.of(zone)).eventsOn(day).first() }

            assertThat(events).isEmpty()
        }
    }

    @Test
    fun `un événement de plusieurs jours entiers est montré chacun de ses jours`() {
        calendar.addWholeDay(5, "Vacances", first = day.minusDays(2), last = day.plusDays(3))

        assertThat(today().single().title).isEqualTo("Vacances")
    }

    @Test
    fun `un rendez-vous commencé la veille n'est pas un rendez-vous d'aujourd'hui`() {
        calendar.add(8, "Soirée", day.minusDays(1).atTime(22, 0), at(2), paris)

        assertThat(today()).isEmpty()
    }

    @Test
    fun `un rendez-vous refusé, annulé ou rangé dans un agenda masqué n'est pas montré`() {
        calendar.add(1, "Refusé", at(9), at(10), paris, answer = Attendees.ATTENDEE_STATUS_DECLINED)
        calendar.add(2, "Annulé", at(11), at(12), paris, status = Instances.STATUS_CANCELED)
        calendar.add(3, "Masqué", at(13), at(14), paris, visible = false)
        calendar.add(4, "Accepté", at(15), at(16), paris, answer = Attendees.ATTENDEE_STATUS_ACCEPTED)

        assertThat(today().map { it.title }).containsExactly("Accepté")
    }

    @Test
    fun `un rendez-vous sans titre s'appelle « Sans titre »`() {
        calendar.add(1, null, at(9), at(10), paris)
        calendar.add(2, "  ", at(11), at(12), paris)

        assertThat(today().map { it.title }).containsExactly("Sans titre", "Sans titre")
    }

    @Test
    fun `sans autorisation, l'agenda n'est pas lu du tout`() {
        access.refuse(Access.Calendar)
        calendar.add(1, "Stand-up", at(9, 30), at(10), paris)

        assertThat(today()).isEmpty()
        assertThat(calendar.asked).isEmpty()
    }

    @Test
    fun `dès que l'autorisation est donnée, les rendez-vous arrivent`() {
        access.refuse(Access.Calendar)
        calendar.add(1, "Stand-up", at(9, 30), at(10), paris)
        val listener = CoroutineScope(Dispatchers.Unconfined)
        val seen = mutableListOf<List<String>>()
        listener.launch { agenda.eventsOn(day).collect { events -> seen += events.map { it.title } } }

        access.grant(Access.Calendar)

        assertThat(seen).containsExactly(emptyList<String>(), listOf("Stand-up"))
        listener.cancel()
    }

    @Test
    fun `quand l'agenda change, les rendez-vous sont relus`() {
        val listener = CoroutineScope(Dispatchers.Unconfined)
        val seen = mutableListOf<List<String>>()
        listener.launch { agenda.eventsOn(day).collect { events -> seen += events.map { it.title } } }

        calendar.add(1, "Dentiste", at(17), at(18), paris)
        phone.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)

        assertThat(seen).containsExactly(emptyList<String>(), listOf("Dentiste"))
        listener.cancel()
    }

    @Test
    fun `quand plus personne n'écoute, Android n'a plus à prévenir Bercail`() {
        val listener = CoroutineScope(Dispatchers.Unconfined)
        listener.launch { agenda.eventsOn(day).collect {} }
        assertThat(shadowOf(phone.contentResolver).getContentObservers(CalendarContract.CONTENT_URI)).hasSize(1)

        listener.cancel()

        assertThat(shadowOf(phone.contentResolver).getContentObservers(CalendarContract.CONTENT_URI)).isEmpty()
    }

    @Test
    fun `si Android reprend l'autorisation en cours de route, l'agenda se vide sans faire tomber l'accueil`() {
        calendar.add(1, "Stand-up", at(9, 30), at(10), paris)
        calendar.locked = true

        assertThat(today()).isEmpty()
    }

    @Test
    fun `toucher un rendez-vous l'ouvre dans l'app agenda du téléphone`() {
        calendar.add(12, "Stand-up", at(9, 30), at(10), paris)

        agenda.open(today().single())

        val sent = shadowOf(phone).nextStartedActivity
        assertThat(sent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(sent.dataString).isEqualTo("content://com.android.calendar/events/12")
        assertThat(sent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0)).isEqualTo(at(9, 30).millisIn(paris))
        assertThat(sent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0)).isEqualTo(at(10).millisIn(paris))
    }

    @Test
    fun `le bouton Agenda ouvre l'app agenda du téléphone à ce jour`() {
        agenda.openCalendar(day)

        val sent = shadowOf(phone).nextStartedActivity
        assertThat(sent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(sent.dataString)
            .isEqualTo("content://com.android.calendar/time/${day.atStartOfDay().millisIn(paris)}")
    }

    @Test
    fun `un rendez-vous que l'agenda du téléphone ne connaît pas n'ouvre rien`() {
        agenda.open(AgendaEvent("Stand-up", "Stand-up", at(9, 30), at(10)))

        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }

    @Test
    fun `sans app agenda sur le téléphone, rien ne se passe`() {
        shadowOf(phone).checkActivities(true)

        agenda.openCalendar(day)

        assertThat(shadowOf(phone).nextStartedActivity).isNull()
    }
}
