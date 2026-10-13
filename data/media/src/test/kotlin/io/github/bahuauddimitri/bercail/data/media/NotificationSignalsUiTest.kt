package io.github.bahuauddimitri.bercail.data.media

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationListenerService

/**
 * What Android's notifications tell Bercail: that an app just showed or removed one, and nothing else. The
 * service is only connected to Android while someone waits for such a signal.
 */
@RunWith(AndroidJUnit4::class)
class NotificationSignalsUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val service by lazy { Robolectric.setupService(MediaListener::class.java) }

    private fun allowNotificationAccess() = shadowOf(phone.getSystemService(NotificationManager::class.java))
        .setNotificationListenerAccessGranted(MediaAccess.listener(phone), true)

    /** Someone waits for the signals of an app, as the home screen does for Beeper while it is shown. */
    private fun waitFor(app: String, onSignal: () -> Unit = {}): CoroutineScope =
        CoroutineScope(Dispatchers.Unconfined).also { waiting ->
            waiting.launch { NotificationSignals.from(phone, app).collect { onSignal() } }
        }

    private fun notificationOf(app: String) =
        @Suppress("DEPRECATION")
        StatusBarNotification(app, app, 1, null, 0, 0, 0, Notification(), Process.myUserHandle(), 0L)

    @Test
    fun `tant que personne n'attend de signal, le service se détache dès qu'Android le relie`() {
        service.onListenerConnected()

        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(1)
    }

    @Test
    fun `dès que quelqu'un attend les signaux d'une app, Android est prié de rattacher le service`() {
        allowNotificationAccess()

        val waiting = waitFor(BEEPER)

        assertThat(ShadowNotificationListenerService.getRebindRequestCount()).isEqualTo(1)
        waiting.cancel()
    }

    @Test
    fun `sans l'accès aux notifications, rien n'est demandé à Android`() {
        val waiting = waitFor(BEEPER)

        assertThat(ShadowNotificationListenerService.getRebindRequestCount()).isEqualTo(0)
        waiting.cancel()
    }

    @Test
    fun `relié pendant que quelqu'un attend, le service reste relié`() {
        allowNotificationAccess()
        val waiting = waitFor(BEEPER)

        service.onListenerConnected()

        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(0)
        waiting.cancel()
    }

    @Test
    fun `une notification affichée ou retirée par l'app attendue donne un signal, pas celle d'une autre app`() {
        allowNotificationAccess()
        var signals = 0
        val waiting = waitFor(BEEPER) { signals++ }
        service.onListenerConnected()

        service.onNotificationPosted(notificationOf("com.bank"))
        assertThat(signals).isEqualTo(0)

        service.onNotificationPosted(notificationOf(BEEPER))
        assertThat(signals).isEqualTo(1)

        service.onNotificationRemoved(notificationOf(BEEPER))
        assertThat(signals).isEqualTo(2)
        waiting.cancel()
    }

    @Test
    fun `quand plus personne n'attend, le service demande à être détaché`() {
        allowNotificationAccess()
        val waiting = waitFor(BEEPER)
        service.onListenerConnected()

        waiting.cancel()

        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(1)
    }

    @Test
    fun `tant qu'il reste quelqu'un qui attend, le service reste relié, et n'est rattaché qu'une fois`() {
        allowNotificationAccess()
        val home = waitFor(BEEPER)
        val sheet = waitFor(BEEPER)
        service.onListenerConnected()

        sheet.cancel()

        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(0)
        assertThat(ShadowNotificationListenerService.getRebindRequestCount()).isEqualTo(1)
        home.cancel()
        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(1)
    }

    private companion object {
        const val BEEPER = "com.beeper.android"
    }
}
