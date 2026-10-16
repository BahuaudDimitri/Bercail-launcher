package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The dock of the band: five apps above the search bar, on the home screen only. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class HomeDockUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val dockNames = listOf("Chrome", "Téléphone", "Beeper", "Appareil photo", "Agenda")

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)): FakeWorld {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
        return world
    }

    @Test
    fun `le bandeau montre les apps du dock, au-dessus de la barre Chercher`() {
        show()

        dockNames.forEach { compose.onNodeWithContentDescription(it).assertExists() }
        val dockBottom = compose.onNodeWithContentDescription("Chrome").getUnclippedBoundsInRoot().bottom
        val searchTop = compose.onNodeWithContentDescription("Chercher").getUnclippedBoundsInRoot().top
        assertThat(dockBottom).isLessThan(searchTop)
    }

    @Test
    fun `toucher une icône du dock ouvre l'app telle qu'elle a été laissée`() {
        val world = show()

        compose.onNodeWithContentDescription("Téléphone").performClick()

        assertThat(world.apps.opened).containsExactly("téléphone")
    }

    @Test
    fun `le dock suit l'ordre choisi dans les réglages`() {
        val world = show()

        world.settings.change { it.copy(dockApps = listOf("agenda", "chrome")) }

        compose.onNodeWithContentDescription("Téléphone").assertDoesNotExist()
        val agenda = compose.onNodeWithContentDescription("Agenda").getUnclippedBoundsInRoot().left
        val chrome = compose.onNodeWithContentDescription("Chrome").getUnclippedBoundsInRoot().left
        assertThat(agenda).isLessThan(chrome)
    }

    @Test
    fun `une app désinstallée quitte le dock`() {
        val world = show()

        world.apps.uninstall("chrome")

        compose.onNodeWithContentDescription("Chrome").assertDoesNotExist()
        compose.onNodeWithContentDescription("Téléphone").assertExists()
    }

    @Test
    fun `une app du dock montre son dessin, ou son initiale quand elle n'en a pas`() {
        val world = FakeWorld(Moment.Matin).apply { apps.paintGlyph("chrome") }
        show(world)

        compose.onAllNodesWithText("C", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("T", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `glisser vers le haut depuis le dock ouvre la feuille, sans ouvrir d'app`() {
        val world = show()

        compose.onNodeWithContentDescription("Beeper").performTouchInput { swipeUp() }

        compose.onNodeWithContentDescription("Salon").assertExists()
        assertThat(world.apps.opened).isEmpty()
    }

    @Test
    fun `la feuille ouverte couvre le dock`() {
        show()

        compose.openSheet()

        dockNames.forEach { compose.onNodeWithContentDescription(it).assertDoesNotExist() }
    }

    @Test
    fun `l'écran Écoute ne montre pas le dock`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeLeft() }

        compose.onNodeWithContentDescription("Pause").assertExists()
        dockNames.forEach { compose.onNodeWithContentDescription(it).assertDoesNotExist() }
    }
}
