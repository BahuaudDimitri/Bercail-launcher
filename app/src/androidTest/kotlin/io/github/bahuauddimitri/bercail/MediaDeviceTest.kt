package io.github.bahuauddimitri.bercail

import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.data.media.MediaAccess
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What plays on the phone, from end to end on a real Android: a real media session is opened, as a music app
 * does, and the home screen must show it and drive it. Needs the "notification access": on an emulator the test
 * gives it itself; on a real phone only its owner does, and without it the test is skipped.
 */
@RunWith(AndroidJUnit4::class)
class MediaDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val phone = instrumentation.targetContext

    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitDescription(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `ce qui joue sur le téléphone s’affiche sur l’accueil et se pilote de là`() {
        assumeTrue("Bercail may not see what plays on this phone", mayHearWhatPlays())
        val paused = CountDownLatch(1)
        lateinit var session: MediaSession
        instrumentation.runOnMainSync {
            session = MediaSession(phone, "Bercail test").apply {
                setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, "Essai")
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, "Bercail")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, SONG_MS)
                        .build()
                )
                setPlaybackState(playback(PlaybackState.STATE_PLAYING))
                setCallback(object : MediaSession.Callback() {
                    override fun onPause() {
                        setPlaybackState(playback(PlaybackState.STATE_PAUSED))
                        paused.countDown()
                    }
                })
                isActive = true
            }
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                awaitText("Essai · Bercail")
                compose.onNodeWithText("Essai · Bercail").performClick()

                awaitDescription("Pause")
                compose.onNodeWithText("3:00").assertExists()
                // This app accepts neither "previous" nor "next": the remote does not offer them.
                compose.onNodeWithContentDescription("Précédent").assertDoesNotExist()
                // Listen is laid out for a phone of the Pixel's height: on a shorter screen, the remote is below
                // the bottom edge and a touch on it would land outside the window.
                compose.onNodeWithContentDescription("Pause").assertIsDisplayed().performClick()

                assertThat(paused.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
                awaitDescription("Lecture")
            }
        } finally {
            instrumentation.runOnMainSync { session.release() }
        }
    }

    private fun playback(state: Int): PlaybackState = PlaybackState.Builder()
        .setState(state, 0, 1f)
        .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)
        .build()

    /** Whether Bercail has the "notification access" here. An emulator belongs to nobody: the test gives it. */
    private fun mayHearWhatPlays(): Boolean {
        if (!MediaAccess.granted(phone) && isEmulator) {
            val listener = MediaAccess.listener(phone).flattenToString()
            instrumentation.uiAutomation.executeShellCommand("cmd notification allow_listener $listener").close()
            val deadline = SystemClock.elapsedRealtime() + TIMEOUT
            while (!MediaAccess.granted(phone) && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(POLL_MS)
        }
        return MediaAccess.granted(phone)
    }

    private val isEmulator: Boolean
        get() = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish" || Build.PRODUCT.startsWith("sdk_")

    private companion object {
        const val TIMEOUT = 10_000L
        const val POLL_MS = 100L
        const val SONG_MS = 180_000L
    }
}
