package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The home screen at the prototype's moments, compared with its reference pictures on a Pixel 9 screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class HomeScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(world: FakeWorld) {
        val home = world.homeViewModel()
        // The night color stands for the user's wallpaper, which the system draws behind the real screen.
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.nightBottom)) { HomeRoute(home) } }
        }
    }

    private fun capture(name: String) {
        // One more frame before capturing: an image still being drawn sometimes comes out blank.
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", HALF_SIZE)
    }

    @Test
    fun `le matin, bandeau replié, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin))

        capture("home_morning_folded")
    }

    @Test
    fun `le matin, feuille ouverte, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin))
        compose.openSheet()

        capture("home_morning_open")
    }

    @Test
    fun `le trajet sous la pluie, en pause, correspond à sa capture`() {
        show(FakeWorld(Moment.Trajet, playing = false))

        capture("home_commute_rain")
    }

    @Test
    fun `le soir, sur l'écran Écoute, correspond à sa capture`() {
        show(FakeWorld(Moment.Soir))
        compose.onNodeWithContentDescription("Écran Écoute").performClick()

        capture("home_evening_listen")
    }

    @Test
    fun `sans musique, sur l'image de l'utilisateur, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin).apply { media.stop() })

        capture("home_morning_silent")
    }

    @Test
    fun `la feuille « Aujourd'hui », pendant le trajet, correspond à sa capture`() {
        val world = FakeWorld(Moment.Trajet, playing = false)
        val day = Samples.day
        val birthday = AgendaEvent("b", "Anniversaire de Léa", day.atStartOfDay(), day.plusDays(1).atStartOfDay(), true)
        world.agenda.set(Samples.eventsOn(day) + birthday)
        show(world)
        compose.onHeaderTitle("Point design").performClick()

        capture("home_day_sheet")
    }

    @Test
    fun `la feuille « Aujourd'hui » sans autorisation correspond à sa capture`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) })
        compose.onNodeWithText("Relier l'agenda").performClick()

        capture("home_day_sheet_locked")
    }

    @Test
    fun `la feuille ouverte, tant que Beeper n'est pas relié, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })
        compose.openSheet()

        capture("home_drawer_unlinked")
    }

    @Test
    fun `la feuille de conversation, pendant le trajet, correspond à sa capture`() {
        show(FakeWorld(Moment.Trajet, playing = false))
        compose.openSheet()
        compose.onNodeWithContentDescription("Léa, 1 non lu").performClick()

        capture("home_talk")
    }

    @Test
    fun `une réponse tout juste envoyée, avec « Annuler l'envoi », correspond à sa capture`() {
        show(FakeWorld(Moment.Trajet, playing = false))
        compose.openSheet()
        compose.onNodeWithContentDescription("Léa, 1 non lu").performClick()
        compose.onNodeWithContentDescription("Répondre").performTextInput("J'arrive")
        compose.onNodeWithContentDescription("Envoyer").performClick()

        capture("home_talk_undo")
    }

    @Test
    fun `la feuille « Écouter » et ses apps média, quand la musique s'est tue, correspond à sa capture`() {
        val world = FakeWorld(Moment.Matin).apply { apps.paintIcon("spotify", color = SPOTIFY_GREEN, size = ICON_PX) }
        show(world)
        // The music was heard, then its app went away: the main app offers to play it again.
        world.media.stop()
        compose.onNodeWithText("Écouter…").performClick()

        capture("home_listen_sheet")
    }

    @Test
    fun `la feuille « Écouter » sans l'accès aux notifications correspond à sa capture`() {
        show(
            FakeWorld(Moment.Matin).apply {
                media.stop()
                access.refuse(Access.Notifications)
            }
        )
        compose.onNodeWithText("Écouter…").performClick()

        capture("home_listen_sheet_locked")
    }

    @Test
    fun `l'écran Écoute d'une vidéo, avec sa vignette et sans bouton Précédent, correspond à sa capture`() {
        // A thumbnail as a video app gives it: wider than high, here two bands of color.
        val thumbnail = Picture(
            THUMBNAIL_WIDTH,
            THUMBNAIL_HEIGHT,
            IntArray(THUMBNAIL_WIDTH * THUMBNAIL_HEIGHT) {
                if (it / THUMBNAIL_WIDTH < THUMBNAIL_HEIGHT / 2) APRICOT else CORAL
            }
        )
        val video = Samples.youtube.first().copy(cover = thumbnail)
        val world = FakeWorld(Moment.Soir)
        val home = world.homeViewModel(
            FakeMediaSource(listOf(video), playing = true, actions = setOf(MediaAction.Next, MediaAction.Seek))
        )
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.nightBottom)) { HomeRoute(home) } }
        }
        compose.onNodeWithContentDescription("Écran Écoute").performClick()

        capture("home_listen_video")
    }

    @Test
    fun `juste après l'installation, l'accueil vide correspond à sa capture`() {
        val home = unconnectedHomeViewModel(hour = 9)
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.nightBottom)) { HomeRoute(home) } }
        }

        capture("home_unconnected")
    }

    @Test
    fun `l'accueil, un doigt posé sur la ligne du temps, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin))
        compose.onNodeWithTag(TIMELINE_TAG).performTouchInput { down(Offset(width - 30f, height * 0.68f)) }

        capture("home_timeline_touched")
    }
}

private const val THUMBNAIL_WIDTH = 320
private const val THUMBNAIL_HEIGHT = 180
private const val APRICOT = 0xFFFFC48A.toInt()
private const val CORAL = 0xFFFF9A8A.toInt()
private const val SPOTIFY_GREEN = 0xFF1ED760.toInt()
private const val ICON_PX = 96

/** Full screens are kept at half size: the pictures stay readable and four times lighter in the repository. */
private val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
