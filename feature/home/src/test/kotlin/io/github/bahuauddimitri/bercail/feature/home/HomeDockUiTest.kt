package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
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
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
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

    private val newTab = AppShortcut("chrome", "new_tab", "Nouvel onglet", AppShortcut.Kind.Manifest)
    private val lemonde = AppShortcut("chrome", "lemonde", "Le Monde", AppShortcut.Kind.Pinned)

    /** Chrome offers its shortcuts, as Android gives them to the default home app. */
    private fun chromeShortcuts() = FakeWorld(Moment.Matin).apply {
        apps.offeredShortcuts["chrome"] = listOf(lemonde, newTab)
        apps.paintGlyph(newTab.id)
    }

    private fun pressBack() = compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun `un toucher long sur une icône du dock montre les raccourcis de son app, sans l'ouvrir`() {
        val world = show(chromeShortcuts())

        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        compose.onNodeWithContentDescription("Nouvel onglet").assertExists()
        compose.onNodeWithContentDescription("Le Monde").assertExists()
        assertThat(world.apps.opened).isEmpty()
    }

    @Test
    fun `les raccourcis déclarés par l'app viennent en tête du menu`() {
        show(chromeShortcuts())

        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        val first = compose.onNodeWithContentDescription("Nouvel onglet").getUnclippedBoundsInRoot().top
        val pinned = compose.onNodeWithContentDescription("Le Monde").getUnclippedBoundsInRoot().top
        assertThat(first).isLessThan(pinned)
    }

    @Test
    fun `l'icône d'un raccourci est son dessin d'une seule couleur, ou son initiale`() {
        show(chromeShortcuts())

        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        compose.onAllNodesWithText("N", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("L", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `toucher un raccourci l'ouvre dans son app et referme le menu`() {
        val world = show(chromeShortcuts())
        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        compose.onNodeWithContentDescription("Nouvel onglet").performClick()

        assertThat(world.apps.openedShortcuts).containsExactly(newTab)
        compose.onAllNodes(isPopup()).assertCountEquals(0)
    }

    @Test
    fun `le geste retour ferme le menu des raccourcis`() {
        show(chromeShortcuts())
        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        pressBack()

        compose.onAllNodes(isPopup()).assertCountEquals(0)
        compose.onNodeWithContentDescription("Chrome").assertExists()
    }

    @Test
    fun `les raccourcis ne sont lus qu'au toucher long, et une app sans raccourci n'a pas de menu`() {
        val world = show()
        assertThat(world.apps.shortcutRequests).isEmpty()

        compose.onNodeWithContentDescription("Beeper").performTouchInput { longClick() }

        assertThat(world.apps.shortcutRequests).containsExactly("beeper")
        compose.onAllNodes(isPopup()).assertCountEquals(0)
        assertThat(world.apps.opened).isEmpty()
    }

    @Test
    fun `l'écran Écoute ne montre pas le dock`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput { swipeLeft() }

        compose.onNodeWithContentDescription("Pause").assertExists()
        dockNames.forEach { compose.onNodeWithContentDescription(it).assertDoesNotExist() }
    }
}
