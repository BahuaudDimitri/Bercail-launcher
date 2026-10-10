package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The settings panel and its list of apps, compared with their reference pictures. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class SettingsScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show() {
        val world = FakeWorld()
        val settings = SettingsViewModel(world.settings, world.apps, world.phone)
        compose.setContent {
            BcTheme { Box(Modifier.fillMaxSize().background(BcColors.base)) { SettingsRoute(settings, onBack = {}) } }
        }
    }

    private fun capture(name: String) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", HALF_SIZE)
    }

    @Test
    fun `les réglages correspondent à leur capture`() {
        show()

        capture("settings")
    }

    @Test
    fun `la liste des apps favorites correspond à sa capture`() {
        show()
        compose.onNodeWithText("Apps favorites").performScrollTo().performClick()

        capture("settings_favorite_apps")
    }

    private companion object {
        /** Full screens are kept at half size: readable, and four times lighter in the repository. */
        val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
    }
}
