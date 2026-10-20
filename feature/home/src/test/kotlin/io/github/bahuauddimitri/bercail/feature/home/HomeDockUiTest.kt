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
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.DockIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon
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
    private val dockIds = arrayOf("chrome", "téléphone", "beeper", "appareil photo", "agenda")

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
    fun `une app glissée à une autre place dans les réglages la prend aussi dans le dock`() {
        val world = show()

        world.settings.change { it.withDockAppMoved("agenda", to = 0, installed = dockIds.toList()) }

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

    private val lines = IconPack("com.natewren.linesfree", "Lines Free")
    private val whicons = IconPack("com.whicons.iconpack", "Whicons")

    @Test
    fun `avec « Aucun », le dock garde le dessin de chaque app`() {
        val world = show()

        compose.waitForIdle()

        assertThat(world.apps.glyphRequests).containsExactlyInAnyOrder(*dockIds)
        assertThat(world.apps.glyphPacks.toSet()).containsOnly(null)
    }

    @Test
    fun `avec un pack d'icônes choisi, le dock dessine chaque app par ce pack`() {
        val world = FakeWorld(Moment.Matin).apply {
            apps.iconPacks.value = listOf(lines)
            settings.change { it.copy(iconPack = lines.id) }
        }
        show(world)

        compose.waitForIdle()

        assertThat(world.apps.glyphRequests).containsExactlyInAnyOrder(*dockIds)
        assertThat(world.apps.glyphPacks.toSet()).containsOnly(lines.id)
    }

    @Test
    fun `choisir un autre pack d'icônes redessine le dock`() {
        val world = show()
        compose.waitForIdle()
        world.apps.glyphPacks.clear()

        world.settings.change { it.copy(iconPack = whicons.id) }
        compose.waitForIdle()

        assertThat(world.apps.glyphPacks).containsExactly(*Array(dockIds.size) { whicons.id })
    }

    @Test
    fun `une icône choisie pour une app du dock la dessine aussitôt, les autres gardent l'icône du pack`() {
        val world = FakeWorld(Moment.Matin).apply {
            apps.iconPacks.value = listOf(lines)
            settings.change { it.copy(iconPack = lines.id) }
        }
        show(world)
        compose.waitForIdle()
        world.apps.glyphRequests.clear()
        world.apps.glyphDrawings.clear()
        val maps = PackIcon(lines.id, "google_maps")

        world.settings.change { it.withDockIcon("chrome", DockIcon.Drawing(maps)) }
        compose.waitForIdle()

        // Only Chrome is drawn again, with its drawing and the dock's pack to fall back on.
        assertThat(world.apps.glyphRequests).containsExactly("chrome")
        assertThat(world.apps.glyphDrawings).containsExactly(maps)
        assertThat(world.apps.glyphPacks.last()).isEqualTo(lines.id)
    }

    @Test
    fun `avec « Dessin d'origine », le dock dessine l'app sans le pack choisi`() {
        val world = FakeWorld(Moment.Matin).apply {
            apps.iconPacks.value = listOf(lines)
            settings.change { it.copy(iconPack = lines.id).withDockIcon("chrome", DockIcon.Origin) }
        }
        show(world)

        compose.waitForIdle()

        val chrome = world.apps.glyphRequests.indexOf("chrome")
        assertThat(world.apps.glyphPacks[chrome]).isNull()
        assertThat(world.apps.glyphDrawings[chrome]).isNull()
    }

    @Test
    fun `une icône choisie dont le pack est désinstallé laisse l'app au dessin du pack du dock`() {
        val maps = PackIcon(whicons.id, "google_maps")
        val world = FakeWorld(Moment.Matin).apply {
            apps.iconPacks.value = listOf(lines)
            apps.paintGlyph("chrome", iconPack = lines.id)
            apps.packIcons[maps] = Picture(1, 1, intArrayOf(-1))
            settings.change { it.copy(iconPack = lines.id).withDockIcon("chrome", DockIcon.Drawing(maps)) }
        }
        show(world)

        compose.waitForIdle()

        // Chrome is asked with its drawing and the dock's pack: the source falls back on the pack's glyph.
        val chrome = world.apps.glyphRequests.indexOf("chrome")
        assertThat(world.apps.glyphDrawings[chrome]).isEqualTo(maps)
        assertThat(world.apps.glyphPacks[chrome]).isEqualTo(lines.id)
        assertThat(world.settings.settings.value.dockIconOf("chrome")).isEqualTo(DockIcon.Drawing(maps))
    }

    @Test
    fun `l'accueil ne lit jamais les icônes des packs, même avec une icône choisie`() {
        val world = FakeWorld(Moment.Matin).apply {
            apps.iconPacks.value = listOf(lines)
            settings.change { it.withDockIcon("chrome", DockIcon.Drawing(PackIcon(lines.id, "google_maps"))) }
        }
        show(world)

        compose.waitForIdle()

        assertThat(world.apps.catalogRequests).isEmpty()
        assertThat(world.apps.packIconRequests).isEmpty()
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
