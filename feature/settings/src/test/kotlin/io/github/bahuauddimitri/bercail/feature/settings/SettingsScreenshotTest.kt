package io.github.bahuauddimitri.bercail.feature.settings

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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Version
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

    private fun show(update: UpdateStatus = UpdateStatus.UpToDate(), refused: Set<Access> = emptySet()) {
        val world = FakeWorld()
        world.updates.status.value = update
        refused.forEach(world.access::refuse)
        val settings =
            SettingsViewModel(
                world.settings,
                world.apps,
                world.phone,
                world.updates,
                world.access,
                world.cities,
                world.mediaApps,
                world.messages
            )
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
    fun `les réglages avec une version prête à installer correspondent à leur capture`() {
        show(UpdateStatus.Ready(Version(0, 9, 0)))
        // The update row is at the bottom of the list.
        compose.onNodeWithText("Paramètres du téléphone").performScrollTo()

        capture("settings_update_ready")
    }

    @Test
    fun `les réglages sans aucun accès, chacun avec son bouton « Autoriser », correspondent à leur capture`() {
        show(refused = setOf(Access.Notifications, Access.Messages, Access.Calendar))

        capture("settings_access_missing")
    }

    @Test
    fun `la liste du dock, ses apps à leur place puis les autres, correspond à sa capture`() {
        show()
        compose.onNodeWithText("Dock").performScrollTo().performClick()

        capture("settings_dock")
    }

    @Test
    fun `la liste des personnes favorites correspond à sa capture`() {
        show()
        compose.onNodeWithText("Personnes favorites").performScrollTo().performClick()

        capture("settings_favorite_people")
    }

    @Test
    fun `le choix du lieu des prévisions, une ville cherchée, correspond à sa capture`() {
        show()
        compose.onNodeWithText("Lieu des prévisions").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Nom de la ville").performTextInput("Nantes")
        compose.onNodeWithText("Chercher cette ville").performClick()

        capture("settings_weather_place")
    }

    private companion object {
        /** Full screens are kept at half size: readable, and four times lighter in the repository. */
        val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
    }
}
