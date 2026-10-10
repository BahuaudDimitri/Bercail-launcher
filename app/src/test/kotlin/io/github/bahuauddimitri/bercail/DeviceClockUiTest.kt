package io.github.bahuauddimitri.bercail

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The phone's clock: Android tells it when a minute passes, and nothing runs while nobody listens. */
@RunWith(AndroidJUnit4::class)
class DeviceClockUiTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private var time = LocalDateTime.of(2026, 10, 2, 8, 10)
    private val clock = DeviceClock(app, now = { time })

    private fun android(says: String) {
        app.sendBroadcast(Intent(says))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun listeners() = shadowOf(app).registeredReceivers.filter {
        it.intentFilter.hasAction(Intent.ACTION_TIME_TICK)
    }

    @Test
    fun `l'horloge donne l'heure du téléphone`() {
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 10, 2, 8, 10))
    }

    @Test
    fun `l'heure est donnée dès qu'on l'écoute`() = runBlocking {
        assertThat(clock.minutes.first()).isEqualTo(LocalDateTime.of(2026, 10, 2, 8, 10))
    }

    @Test
    fun `à chaque minute qui passe, Android prévient et l'heure est redonnée`() {
        val seen = mutableListOf<String>()
        val listening = CoroutineScope(Dispatchers.Main.immediate)
        listening.launch { clock.minutes.collect { seen += it.toLocalTime().toString() } }

        time = time.plusMinutes(1)
        android(says = Intent.ACTION_TIME_TICK)
        listening.cancel()

        assertThat(seen).containsExactly("08:10", "08:11")
    }

    @Test
    fun `un changement d'heure ou de fuseau redonne l'heure`() {
        val seen = mutableListOf<String>()
        val listening = CoroutineScope(Dispatchers.Main.immediate)
        listening.launch { clock.minutes.collect { seen += it.toLocalTime().toString() } }

        time = time.plusHours(1)
        android(says = Intent.ACTION_TIMEZONE_CHANGED)
        time = time.plusMinutes(5)
        android(says = Intent.ACTION_TIME_CHANGED)
        listening.cancel()

        assertThat(seen).containsExactly("08:10", "09:10", "09:15")
    }

    @Test
    fun `quand plus personne n'écoute, l'horloge ne demande plus rien à Android`() {
        val listening = CoroutineScope(Dispatchers.Main.immediate)
        listening.launch { clock.minutes.collect {} }
        assertThat(listeners().size).isEqualTo(1)

        listening.cancel()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listeners()).isEmpty()
    }
}
