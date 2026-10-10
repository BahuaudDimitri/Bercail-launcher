package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
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
    fun `sans source branchée, l'accueil vide correspond à sa capture`() {
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

/** Full screens are kept at half size: the pictures stay readable and four times lighter in the repository. */
private val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
