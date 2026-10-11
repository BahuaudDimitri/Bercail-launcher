package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasNoClickAction
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
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Version
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
    private val asked = mutableListOf<Access>()

    private fun show() {
        val settings = SettingsViewModel(world.settings, world.apps, world.phone, world.updates, world.access)
        compose.setContent { BcTheme { SettingsRoute(settings, onBack = { closed++ }, onAskAccess = { asked += it }) } }
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
    fun `sans autorisation, la ligne « Agenda du téléphone » la demande à Android d'un toucher`() {
        world.access.refuse(Access.Calendar)
        show()
        compose.onNodeWithText("Toucher pour autoriser Bercail à lire tes rendez-vous").assertExists()

        compose.onNodeWithText("Agenda du téléphone").performScrollTo().performClick()

        assertThat(asked).containsExactly(Access.Calendar)
    }

    @Test
    fun `une fois l'agenda autorisé, sa ligne le dit et ne se touche plus`() {
        show()

        compose.onNodeWithText("Bercail lit tes rendez-vous, sans rien y changer").assertExists()
        compose.onNodeWithText("Agenda du téléphone").performScrollTo().assertHasNoClickAction()
    }

    @Test
    fun `la ligne de l'agenda suit l'autorisation dès qu'elle est donnée`() {
        world.access.refuse(Access.Calendar)
        show()

        world.access.grant(Access.Calendar)

        compose.onNodeWithText("Bercail lit tes rendez-vous, sans rien y changer").assertExists()
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
        SettingsViewModel(world.settings, world.apps, world.phone, world.updates, world.access)

        assertThat(world.apps.apps.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `la ligne de mise à jour donne la version installée et propose de vérifier`() {
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("À jour · toucher pour vérifier").assertExists()
    }

    @Test
    fun `toucher la ligne de mise à jour lance la vérification`() {
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().performClick()

        assertThat(world.updates.proceeded).isEqualTo(1)
    }

    @Test
    fun `la ligne suit la mise à jour, jusqu'à la version prête à installer`() {
        show()

        world.updates.status.value = UpdateStatus.Downloading(Version(0, 9, 0))
        compose.onNodeWithText("Téléchargement de la version 0.9.0…").assertExists()

        world.updates.status.value = UpdateStatus.Ready(Version(0, 9, 0), needsUser = true)
        compose.onNodeWithText("Version 0.9.0 prête · toucher pour l'installer").assertExists()
    }

    @Test
    fun `pendant un téléchargement, toucher la ligne ne relance rien`() {
        world.updates.status.value = UpdateStatus.Downloading(Version(0, 9, 0))
        show()

        compose.onNodeWithText("Bercail 0.8.1").performScrollTo().performClick()

        assertThat(world.updates.proceeded).isEqualTo(0)
    }

    @Test
    fun `tant que les réglages ne sont pas affichés, la mise à jour n'est pas écoutée`() {
        SettingsViewModel(world.settings, world.apps, world.phone, world.updates, world.access)

        assertThat(world.updates.status.subscriptionCount.value).isEqualTo(0)
    }
}
