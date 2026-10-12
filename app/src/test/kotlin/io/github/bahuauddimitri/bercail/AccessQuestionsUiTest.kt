package io.github.bahuauddimitri.bercail

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.testing.FakePhone
import io.github.bahuauddimitri.bercail.data.media.MediaAccess
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSystemClock

/**
 * What the user lets Bercail read, and the question Bercail asks for it. Android's own screen is played by the
 * test, which answers yes or no when it wants.
 */
@RunWith(AndroidJUnit4::class)
class AccessQuestionsUiTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val android = AndroidAsked()
    private val access = DeviceAccess(app)
    private val phone = FakePhone()
    private val screen = Robolectric.buildActivity(ComponentActivity::class.java).create()
    private val questions = AccessQuestions(screen.get(), { access }, { phone }, android).also {
        screen.start().resume()
    }

    private fun userAllowsTheAgenda() = shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)

    @Test
    fun `tant que l'utilisateur n'a rien accordé, Bercail n'a le droit de rien lire`() {
        assertThat(access.granted.value).isEmpty()
    }

    @Test
    fun `ce qui a été accordé avant le lancement est connu dès le départ`() {
        userAllowsTheAgenda()

        assertThat(DeviceAccess(app).granted.value).isEqualTo(setOf(Access.Calendar))
    }

    @Test
    fun `un accord donné hors de Bercail n'est vu qu'au prochain coup d'œil`() {
        userAllowsTheAgenda()
        assertThat(access.granted.value).isEmpty()

        access.refresh()

        assertThat(access.granted.value).isEqualTo(setOf(Access.Calendar))
    }

    @Test
    fun `pour l'agenda, Bercail demande à Android le droit de le lire, et rien d'autre`() {
        questions.ask(Access.Calendar)

        assertThat(android.asked).containsExactly(Manifest.permission.READ_CALENDAR)
    }

    @Test
    fun `quand l'utilisateur accepte, Bercail le sait aussitôt`() {
        questions.ask(Access.Calendar)

        userAllowsTheAgenda()
        android.answers(agreed = true)

        assertThat(access.granted.value).isEqualTo(setOf(Access.Calendar))
        assertThat(phone.visits).isEmpty()
    }

    @Test
    fun `un refus donné sur l'écran d'Android n'ouvre rien d'autre`() {
        questions.ask(Access.Calendar)

        // The user reads Android's question, then says no.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(3))
        android.answers(agreed = false)

        assertThat(access.granted.value).isEmpty()
        assertThat(phone.visits).isEmpty()
    }

    @Test
    fun `si Android répond non sans rien afficher, Bercail ouvre sa page dans les Paramètres du téléphone`() {
        questions.ask(Access.Calendar)

        android.answers(agreed = false)

        assertThat(phone.visits).containsExactly("app settings")
    }

    @Test
    fun `pour ce qui joue, Bercail ouvre la page de son accès dans les réglages d'Android, sans autre question`() {
        questions.ask(Access.Notifications)

        val opened = shadowOf(app).nextStartedActivity
        assertThat(opened.action).isEqualTo(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        assertThat(opened.getStringExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME))
            .isEqualTo(MediaAccess.listener(app).flattenToString())
        assertThat(android.asked).isEmpty()
    }

    @Test
    fun `l'accès donné dans les réglages d'Android est vu au retour sur l'accueil`() {
        shadowOf(app.getSystemService(NotificationManager::class.java))
            .setNotificationListenerAccessGranted(MediaAccess.listener(app), true)
        assertThat(access.granted.value).isEmpty()

        access.refresh()

        assertThat(access.granted.value).isEqualTo(setOf(Access.Notifications))
    }

    /** Android's permission screen: it remembers what it was asked, and answers when the test says so. */
    private class AndroidAsked : ActivityResultRegistry() {
        val asked = mutableListOf<String>()
        private var question = 0

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            asked += input.toString()
            question = requestCode
        }

        fun answers(agreed: Boolean) {
            dispatchResult(question, agreed)
        }
    }
}
