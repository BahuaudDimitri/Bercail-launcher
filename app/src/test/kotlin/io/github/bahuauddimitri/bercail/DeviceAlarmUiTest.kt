package io.github.bahuauddimitri.bercail

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Looper
import android.provider.AlarmClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
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
import org.robolectric.Shadows.shadowOf

/** The alarm of the phone's clock, read when the home screen shows and again when Android says it changed. */
@RunWith(AndroidJUnit4::class)
class DeviceAlarmUiTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = app.getSystemService(AlarmManager::class.java)
    private val alarm = DeviceAlarm(app)
    private val tomorrow = LocalDateTime.of(2026, 10, 3, 7, 15)

    /** What the clock app does when the user sets an alarm (here, in the test only). */
    private fun clockSets(time: LocalDateTime) {
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ring = PendingIntent.getBroadcast(app, 0, Intent("ring"), PendingIntent.FLAG_IMMUTABLE)
        alarms.setAlarmClock(AlarmManager.AlarmClockInfo(millis, null), ring)
    }

    private fun listeners() = shadowOf(app).registeredReceivers.filter {
        it.intentFilter.hasAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
    }

    @Test
    fun `le prochain réveil d'Android arrive sur l'accueil`() = runBlocking {
        clockSets(tomorrow)

        assertThat(alarm.next.first()).isEqualTo(tomorrow)
    }

    @Test
    fun `un réveil changé dans l'horloge arrive tout de suite`() {
        clockSets(tomorrow)
        val seen = mutableListOf<LocalDateTime?>()
        val listening = CoroutineScope(Dispatchers.Main.immediate)
        listening.launch { alarm.next.collect { seen += it } }

        clockSets(tomorrow.plusMinutes(30))
        app.sendBroadcast(Intent(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()
        listening.cancel()

        assertThat(seen).containsExactly(tomorrow, tomorrow.plusMinutes(30))
    }

    @Test
    fun `rien n'écoute les réveils tant que l'accueil n'est pas affiché`() {
        assertThat(listeners()).isEmpty()
        val listening = CoroutineScope(Dispatchers.Main.immediate)
        listening.launch { alarm.next.collect {} }
        assertThat(listeners()).isNotEmpty()

        listening.cancel()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listeners()).isEmpty()
    }

    @Test
    fun `toucher le réveil ouvre les alarmes de l'horloge du téléphone`() {
        alarm.openAlarms()

        assertThat(shadowOf(app).nextStartedActivity.action).isEqualTo(AlarmClock.ACTION_SHOW_ALARMS)
    }
}
