package io.github.bahuauddimitri.bercail.data.media

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.media.LastHeard
import io.github.bahuauddimitri.bercail.core.testing.FakeAppsSource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Which installed apps can play sound or video, and what Bercail remembers of what it last saw loaded. */
@RunWith(AndroidJUnit4::class)
class DeviceMediaAppsUiTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val phone = ApplicationProvider.getApplicationContext<Application>()
    private val spotify = App("com.spotify.music/.MainActivity", "Spotify")
    private val youtube = App("com.google.android.youtube/.Home", "YouTube")
    private val podcasts = App("au.com.shiftyjelly.pocketcasts/.Main", "Pocket Casts")
    private val bank = App("com.bank/.Main", "Banque")
    private val installed = FakeAppsSource(listOf(youtube, bank, spotify))

    /** The app answers the play button of a headset, as every player does. */
    private fun answersMediaButtons(app: App) {
        val receiver = ComponentName(app.id.substringBefore('/'), "MediaButtonReceiver")
        shadowOf(phone.packageManager).addReceiverIfNotPresent(receiver)
        shadowOf(phone.packageManager).addIntentFilterForReceiver(receiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))
    }

    /** The app is declared to Android as a music app. */
    private fun isAMusicApp(app: App) {
        val music = checkNotNull(ComponentName.unflattenFromString(app.id))
        shadowOf(phone.packageManager).addActivityIfNotPresent(music)
        shadowOf(phone.packageManager).addIntentFilterForActivity(
            music,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }
        )
    }

    /** Runs [block] on the file of what was heard, then closes the file, as when the app stops. */
    private fun <T> withMediaApps(block: suspend (DeviceMediaApps) -> T): T = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val store = listeningStore(File(folder.root, "listening.preferences_pb"), scope)
        try {
            block(DeviceMediaApps(phone, installed, store))
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }

    @Test
    fun `les apps média sont celles qui répondent aux boutons d'un casque, de A à Z`() {
        answersMediaButtons(youtube)
        answersMediaButtons(spotify)

        val apps = withMediaApps { it.apps.first() }

        assertThat(apps).containsExactly(spotify, youtube)
    }

    @Test
    fun `une app déclarée comme app de musique en fait partie aussi`() {
        installed.install(podcasts)
        isAMusicApp(podcasts)
        answersMediaButtons(spotify)

        val apps = withMediaApps { it.apps.first() }

        assertThat(apps).containsExactly(podcasts, spotify)
    }

    @Test
    fun `sans app qui joue, il n'y a aucune app média`() {
        assertThat(withMediaApps { it.apps.first() }).isEmpty()
    }

    @Test
    fun `une app média désinstallée disparaît de la liste`() {
        answersMediaButtons(youtube)
        answersMediaButtons(spotify)

        val apps = withMediaApps {
            installed.uninstall(youtube.id)
            it.apps.first()
        }

        assertThat(apps).containsExactly(spotify)
    }

    @Test
    fun `au premier lancement, rien n'a encore été entendu`() {
        assertThat(withMediaApps { it.lastHeard.first() }).isNull()
    }

    @Test
    fun `le dernier contenu entendu est gardé d'un lancement à l'autre`() {
        withMediaApps { it.remember(LastHeard(spotify.id, "Looped")) }

        assertThat(withMediaApps { it.lastHeard.first() }).isEqualTo(LastHeard(spotify.id, "Looped"))
    }

    @Test
    fun `un nouveau contenu remplace le précédent`() {
        withMediaApps {
            it.remember(LastHeard(spotify.id, "Looped"))
            it.remember(LastHeard(youtube.id, "Tiny Desk Concert"))
        }

        assertThat(withMediaApps { it.lastHeard.first() }).isEqualTo(LastHeard(youtube.id, "Tiny Desk Concert"))
    }
}
