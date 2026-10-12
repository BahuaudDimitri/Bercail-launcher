package io.github.bahuauddimitri.bercail

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.data.media.MediaAccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * What plays on the phone, in the whole app on a simulated phone: Bercail asks before looking at it, shows nothing
 * of it until the user agrees, then the music pill, the Listen screen and its remote follow the media apps.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class MediaUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<BercailApplication>()

    private fun showHome(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

    /** The user gives Bercail the "notification access", on the page of Android's settings. */
    private fun allowToSeeWhatPlays() = shadowOf(app.getSystemService(NotificationManager::class.java))
        .setNotificationListenerAccessGranted(MediaAccess.listener(app), true)

    /** Spotify is installed and plays "Looped". */
    private fun spotifyPlays() {
        val spotify = ApplicationInfo().apply {
            packageName = SPOTIFY
            nonLocalizedLabel = "Spotify"
        }
        shadowOf(app.packageManager).installPackage(
            PackageInfo().apply {
                packageName = SPOTIFY
                applicationInfo = spotify
            }
        )
        val session = MediaController(app, MediaSession(app, SPOTIFY).sessionToken)
        shadowOf(session).setPackageName(SPOTIFY)
        shadowOf(session).setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Looped")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Kiasmos")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 245_000)
                .build()
        )
        shadowOf(session).setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 72_000, 1f)
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT)
                .build()
        )
        shadowOf(app.getSystemService(MediaSessionManager::class.java)).addController(session)
    }

    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitNoText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    private fun ActivityScenario<MainActivity>.pressHome() = onActivity { activity ->
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, home)
    }

    @Test
    fun `sans l'accès aux notifications, rien de ce qui joue n'est montré`() {
        spotifyPlays()

        showHome()

        awaitText("Écouter…")
        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
    }

    @Test
    fun `la feuille Écouter mène à la page d'Android où l'accès se donne, celle de Bercail`() {
        showHome()
        awaitText("Écouter…")
        compose.onNodeWithText("Écouter…").performClick()
        awaitText("Autoriser dans Android")

        compose.onNodeWithText("Autoriser dans Android").performClick()

        val opened = shadowOf(app).nextStartedActivity
        assertThat(opened.action).isEqualTo(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        assertThat(opened.getStringExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME))
            .isEqualTo(MediaAccess.listener(app).flattenToString())
    }

    @Test
    fun `si Android refuse, la feuille Écouter ouvre la page de Bercail dans les Paramètres du téléphone`() {
        showHome()
        awaitText("Écouter…")
        compose.onNodeWithText("Écouter…").performClick()
        awaitText("Page de Bercail dans les Paramètres")

        compose.onNodeWithText("Page de Bercail dans les Paramètres").performClick()

        val opened = shadowOf(app).nextStartedActivity
        assertThat(opened.action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        assertThat(opened.data.toString()).isEqualTo("package:${app.packageName}")
    }

    @Test
    fun `une fois l'accès donné dans Android, ce qui joue s'affiche au retour sur l'accueil`() {
        spotifyPlays()
        val home = showHome()
        awaitText("Écouter…")

        // The user leaves for Android's settings, agrees, and comes back.
        home.moveToState(Lifecycle.State.CREATED)
        allowToSeeWhatPlays()
        home.moveToState(Lifecycle.State.RESUMED)

        awaitText("Looped · Kiasmos")
    }

    @Test
    fun `la pastille ouvre l'écran Écoute, avec les seuls boutons que l'app accepte`() {
        allowToSeeWhatPlays()
        spotifyPlays()
        showHome()
        awaitText("Looped · Kiasmos")

        compose.onNodeWithText("Looped · Kiasmos").performClick()

        awaitText("Ouvrir dans Spotify")
        compose.onNodeWithText("4:05").assertExists()
        compose.onNodeWithContentDescription("Suivant").assertExists()
        compose.onNodeWithContentDescription("Précédent").assertDoesNotExist()
    }

    @Test
    fun `le bouton Accueil ramène de l'écran Écoute à l'Accueil`() {
        allowToSeeWhatPlays()
        spotifyPlays()
        val home = showHome()
        awaitText("Looped · Kiasmos")
        compose.onNodeWithText("Looped · Kiasmos").performClick()
        awaitText("Ouvrir dans Spotify")

        home.pressHome()

        awaitText("Looped · Kiasmos")
    }

    @Test
    fun `le bouton Accueil referme la feuille Écouter`() {
        val home = showHome()
        awaitText("Écouter…")
        compose.onNodeWithText("Écouter…").performClick()
        awaitText("Autoriser dans Android")

        home.pressHome()

        awaitNoText("Autoriser dans Android")
        awaitText("Écouter…")
    }

    private companion object {
        const val SPOTIFY = "com.spotify.music"
        const val TIMEOUT = 5_000L
    }
}
