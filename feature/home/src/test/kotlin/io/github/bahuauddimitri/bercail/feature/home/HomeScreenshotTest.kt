package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
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
    fun `le matin, tiroir replié, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin))

        capture("home_morning_folded")
    }

    @Test
    fun `le matin, tiroir ouvert, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin))
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

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
        compose.onNodeWithText("Point design").performClick()

        capture("home_day_sheet")
    }

    @Test
    fun `la feuille « Aujourd'hui » sans autorisation correspond à sa capture`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Calendar) })
        compose.onNodeWithText("Relier l'agenda").performClick()

        capture("home_day_sheet_locked")
    }

    @Test
    fun `le tiroir ouvert, tant que Beeper n'est pas relié, correspond à sa capture`() {
        show(FakeWorld(Moment.Matin).apply { access.refuse(Access.Messages) })
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        capture("home_drawer_unlinked")
    }

    @Test
    fun `la feuille « Écouter » et ses apps média, quand la musique s'est tue, correspond à sa capture`() {
        val world = FakeWorld(Moment.Matin)
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
    fun `la ligne du temps horizontale correspond à sa capture`() {
        val world = FakeWorld(Moment.Trajet).apply {
            media.stop()
            settings.change { it.copy(timeline = TimelineOrientation.Horizontal) }
        }
        show(world)

        capture("home_horizontal_timeline")
    }
}

private const val THUMBNAIL_WIDTH = 320
private const val THUMBNAIL_HEIGHT = 180
private const val APRICOT = 0xFFFFC48A.toInt()
private const val CORAL = 0xFFFF9A8A.toInt()

/** Full screens are kept at half size: the pictures stay readable and four times lighter in the repository. */
private val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
