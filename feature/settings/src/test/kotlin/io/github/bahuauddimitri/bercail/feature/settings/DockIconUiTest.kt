package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.apps.DockIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.PackCatalog
import io.github.bahuauddimitri.bercail.core.domain.apps.PackIcon
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The icon of each app of the dock, chosen from Réglages › Dock among the drawings of every installed pack. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class DockIconUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld()
    private val lines = IconPack("com.natewren.linesfree", "Lines Free")
    private val whicons = IconPack("com.whicons.iconpack", "Whicons")
    private lateinit var settings: SettingsViewModel

    private val stored: Settings get() = world.settings.settings.value

    private fun show() {
        world.apps.iconPacks.value = listOf(lines, whicons)
        world.apps.catalogs[lines.id] = PackCatalog(
            lines,
            icons = listOf("google_maps", "musique_note", "mail", "chrome_alt"),
            apps = mapOf("chrome" to "browser")
        )
        world.apps.catalogs[whicons.id] = PackCatalog(whicons, icons = listOf("mail_box", "écouteurs"), emptyMap())
        settings = SettingsViewModel(
            world.settings,
            world.apps,
            world.phone,
            world.updates,
            world.access,
            world.cities,
            world.mediaApps,
            world.messages
        )
        compose.setContent { BcTheme { SettingsRoute(settings, onBack = {}) } }
        compose.onNodeWithText("Dock").performScrollTo().performClick()
    }

    private fun openChrome() {
        compose.onNodeWithText("Chrome").performClick()
        compose.onNodeWithText("Icône · Chrome").assertIsDisplayed()
    }

    private fun icon(name: String) = compose.onNode(hasScrollToIndexAction())
        .performScrollToNode(hasContentDescription(name))
        .let { compose.onAllNodesWithContentDescription(name).onFirst() }

    @Test
    fun `toucher une app du dock ouvre le choix de son icône, sa flèche ramène au dock`() {
        show()

        openChrome()
        compose.onNodeWithContentDescription("Retour au dock").performClick()

        compose.onNodeWithText("Autres apps").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `l'interrupteur d'une app du dock la retire toujours, sans ouvrir le choix d'icône`() {
        show()

        compose.onNode(isToggleable() and hasAnySibling(hasText("Beeper")), useUnmergedTree = true).performClick()

        assertThat(stored.dockApps).doesNotContain("beeper")
        compose.onNodeWithText("Autres apps").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `le choix offre l'icône du pack, le dessin d'origine, les icônes proposées puis chaque pack`() {
        show()
        openChrome()

        compose.onNodeWithText("Icône du pack").assertIsDisplayed()
        compose.onNodeWithText("Dessin d'origine").assertIsDisplayed()
        compose.onNodeWithText("Proposées").assertIsDisplayed()
        // The appfilter's drawing for Chrome, then the one named after it.
        compose.onNodeWithContentDescription("browser").assertIsDisplayed()
        icon("chrome alt").assertExists()
        icon("écouteurs").assertExists()
        compose.onNodeWithText("Whicons").assertExists()
    }

    @Test
    fun `chercher « MUSIQUE » ne garde que les icônes qui le contiennent, accents et majuscules à part`() {
        show()
        openChrome()

        compose.onNodeWithContentDescription("Chercher une icône").performTextInput("MUSIQUE")

        compose.onNodeWithContentDescription("musique note").assertIsDisplayed()
        compose.onNodeWithContentDescription("google maps").assertDoesNotExist()
        compose.onNodeWithText("Whicons").assertDoesNotExist()
    }

    @Test
    fun `chercher « ecouteurs » trouve « écouteurs »`() {
        show()
        openChrome()

        compose.onNodeWithContentDescription("Chercher une icône").performTextInput("ecouteurs")

        compose.onNodeWithContentDescription("écouteurs").assertIsDisplayed()
    }

    @Test
    fun `toucher une icône la choisit pour cette app, l'enregistre et ramène au dock`() {
        show()
        openChrome()

        icon("google maps").performClick()

        assertThat(stored.dockIconOf("chrome")).isEqualTo(DockIcon.Drawing(PackIcon(lines.id, "google_maps")))
        assertThat(stored.dockIconOf("agenda")).isEqualTo(DockIcon.FromPack)
        compose.onNodeWithText("Autres apps").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `l'icône choisie est marquée dans la grille`() {
        world.settings.change { it.withDockIcon("chrome", DockIcon.Drawing(PackIcon(lines.id, "mail"))) }
        show()
        openChrome()

        icon("mail").assertIsSelected()
    }

    @Test
    fun `« Dessin d'origine » puis « Icône du pack » reviennent au réglage par défaut`() {
        show()
        openChrome()

        compose.onNodeWithText("Dessin d'origine").performClick()
        assertThat(stored.dockIconOf("chrome")).isEqualTo(DockIcon.Origin)

        openChrome()
        compose.onNodeWithText("Icône du pack").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Autres apps").performScrollTo().assertIsDisplayed()

        assertThat(stored.dockIcons).isEqualTo(emptyMap())
    }

    @Test
    fun `les packs ne sont lus qu'à l'ouverture du choix, et oubliés à sa fermeture`() {
        show()
        assertThat(world.apps.catalogRequests).isEmpty()
        assertThat(world.apps.packIconRequests).isEmpty()

        openChrome()
        assertThat(world.apps.catalogRequests).containsExactly(lines.id, whicons.id)

        compose.onNodeWithContentDescription("Retour au dock").performClick()
        compose.waitForIdle()

        assertThat(settings.icons.state.value.catalogs).isNull()
    }

    @Test
    fun `sans pack installé, le choix le dit et garde ses deux choix`() {
        show()
        world.apps.iconPacks.value = emptyList()
        openChrome()

        compose.onNodeWithText("Aucun pack d'icônes installé").assertIsDisplayed()
        compose.onNodeWithText("Dessin d'origine").assertIsDisplayed()
    }
}
