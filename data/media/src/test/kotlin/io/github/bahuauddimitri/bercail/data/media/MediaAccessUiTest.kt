package io.github.bahuauddimitri.bercail.data.media

import android.app.Application
import android.app.NotificationManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** The "notification access" Android asks for before it tells what the media apps play. */
@RunWith(AndroidJUnit4::class)
class MediaAccessUiTest {
    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val android = shadowOf(phone.getSystemService(NotificationManager::class.java))

    @Test
    fun `tant que l'utilisateur n'a rien accordé, l'accès n'est pas donné`() {
        assertThat(MediaAccess.granted(phone)).isFalse()
    }

    @Test
    fun `l'accès est donné quand Android dit que l'utilisateur l'a accordé à Bercail`() {
        android.setNotificationListenerAccessGranted(MediaAccess.listener(phone), true)

        assertThat(MediaAccess.granted(phone)).isTrue()
    }

    @Test
    fun `la question ouvre, dans les réglages d'Android, la page de l'accès de Bercail et aucune autre`() {
        val question = MediaAccess.question(phone)

        assertThat(question.action).isEqualTo(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        assertThat(question.getStringExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME))
            .isEqualTo(MediaAccess.listener(phone).flattenToString())
    }

    @Test
    fun `dès qu'Android relie le service, il demande à être détaché et ne lit donc aucune notification`() {
        val service = Robolectric.setupService(MediaListener::class.java)

        service.onListenerConnected()

        assertThat(shadowOf(service).unbindRequestCount).isEqualTo(1)
    }
}
