package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** « Réglages de Bercail » on a simulated phone: each choice changes the stored settings at once. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class SettingsUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld()
    private var closed = 0

    private fun show() {
        val settings = SettingsViewModel(world.settings, world.apps, world.phone)
        compose.setContent { BcTheme { SettingsRoute(settings, onBack = { closed++ }) } }
    }

    private val stored: Settings get() = world.settings.settings.value

    @Test
    fun `les réglages s'appellent « Réglages de Bercail » et se ferment par leur flèche`() {
        show()

        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed()
        compose.onNodeWithContentDescription("Retour à la recherche").performClick()

        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `le fond sans musique est mon image par défaut, on peut choisir la brume du moment`() {
        show()
        assertThat(stored.idleBackground).isEqualTo(IdleBackground.Wallpaper)

        compose.onNodeWithText("Brume du moment").performClick()

        assertThat(stored.idleBackground).isEqualTo(IdleBackground.Brume)
    }

    @Test
    fun `la ligne du temps est verticale par défaut, on peut la coucher`() {
        show()

        compose.onNodeWithText("Horizontale").performClick()
        assertThat(stored.timeline).isEqualTo(TimelineOrientation.Horizontal)

        compose.onNodeWithText("Verticale").performClick()
        assertThat(stored.timeline).isEqualTo(TimelineOrientation.Vertical)
    }

    @Test
    fun `le tiroir démarre replié par défaut, on peut le faire démarrer ouvert`() {
        show()
        assertThat(stored.drawerExpandedAtStart).isFalse()

        compose.onNodeWithText("Ouvert").performClick()

        assertThat(stored.drawerExpandedAtStart).isTrue()
    }

    @Test
    fun `les apps favorites sont nommées sur leur ligne`() {
        show()

        compose.onNodeWithText("Spotify, YouTube, Beeper, Agenda").assertExists()
    }

    @Test
    fun `sans app favorite, la ligne le dit`() {
        world.settings.change { it.copy(favoriteApps = emptyList()) }
        show()

        compose.onNodeWithText("Aucune pour l'instant").assertExists()
    }

    @Test
    fun `toucher « Apps favorites » ouvre la liste des apps, où l'on coche et décoche`() {
        show()

        compose.onNodeWithText("Apps favorites").performScrollTo().performClick()
        compose.onNodeWithText("Banque").performScrollTo().performClick()
        compose.onNodeWithText("YouTube").performScrollTo().performClick()

        assertThat(stored.favoriteApps).containsExactly("spotify", "beeper", "agenda", "banque")
    }

    @Test
    fun `le retour de la liste des apps ramène aux réglages`() {
        show()
        compose.onNodeWithText("Apps favorites").performScrollTo().performClick()

        compose.onNodeWithContentDescription("Retour aux réglages").performClick()

        compose.onNodeWithText("Réglages de Bercail").assertIsDisplayed()
        assertThat(closed).isEqualTo(0)
    }

    @Test
    fun `les réglages renvoient vers le choix de l'écran d'accueil et les Paramètres du téléphone`() {
        show()

        compose.onNodeWithText("Écran d'accueil par défaut").performScrollTo().performClick()
        compose.onNodeWithText("Paramètres du téléphone").performScrollTo().performClick()

        assertThat(world.phone.visits).containsExactly("home screen settings", "settings")
    }

    @Test
    fun `tant que les réglages ne sont pas affichés, la liste des apps n'est pas écoutée`() {
        SettingsViewModel(world.settings, world.apps, world.phone)

        assertThat(world.apps.apps.subscriptionCount.value).isEqualTo(0)
    }
}
