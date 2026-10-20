package io.github.bahuauddimitri.bercail.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.apps.IconPack
import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS
import io.github.bahuauddimitri.bercail.core.domain.apps.PackCatalog
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Version
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Every page of Bercail's settings on the Pixel 9 and on smaller screens: no text or control overlaps another, none
 * leaves the screen, and the title and the way back are shown whole.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class SettingsScreenSizesUiTest(size: ScreenSize) {
    @get:Rule(order = 0)
    val screen = ScreenSizeRule(size)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(update: UpdateStatus = UpdateStatus.UpToDate(), refused: Set<Access> = emptySet()) {
        val world = FakeWorld()
        world.updates.status.value = update
        refused.forEach(world.access::refuse)
        world.apps.iconPacks.value =
            listOf(IconPack("com.arcticons.dark", "Arcticons"), IconPack("com.natewren.linesfree", "Lines Free"))
        world.apps.iconPacks.value.forEach { pack ->
            val names = List(ICONS) { "icone_$it" } + "chrome_alt"
            world.apps.catalogs[pack.id] = PackCatalog(pack, names, mapOf("chrome" to "chrome_alt"))
        }
        val settings = SettingsViewModel(
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

    private fun assertPage(title: String, back: String) {
        compose.assertWholeAndApart(
            compose.onAllNodes(hasText(title), useUnmergedTree = true).onFirst(),
            compose.onNodeWithContentDescription(back)
        )
        compose.assertNothingOverlaps()
    }

    private fun open(row: String) = compose.onNodeWithText(row).performScrollTo().performClick()

    @Test
    fun `la page des réglages tient, avec une mise à jour prête et chaque accès à donner`() {
        show(
            update = UpdateStatus.Ready(Version(0, 9, 0)),
            refused = setOf(Access.Notifications, Access.Messages, Access.Calendar)
        )

        assertPage("Réglages de Bercail", "Retour à la recherche")
    }

    @Test
    fun `la page du dock tient`() {
        show()
        open("Dock")

        assertPage("Dock", "Retour aux réglages")
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `sous chaque app du dock, « toucher pour changer l'icône » s'écrit en entier`() {
        show()
        open("Dock")

        val subtitles = compose.onAllNodes(hasText(PICK_HINT, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertThat(subtitles.size).isEqualTo(MAX_DOCK_APPS)
        subtitles.forEach { subtitle ->
            val layouts = mutableListOf<TextLayoutResult>()
            subtitle.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.single()
            assertThat((0 until layout.lineCount).none(layout::isLineEllipsized)).isTrue()
        }
    }

    @Test
    fun `la page des packs d'icônes tient`() {
        show()
        open("Pack d'icônes")

        assertPage("Pack d'icônes", "Retour aux réglages")
    }

    @Test
    fun `la page du choix de l'icône d'une app du dock tient`() {
        show()
        open("Dock")
        compose.onNodeWithText("Chrome").performClick()

        assertPage("Icône · Chrome", "Retour au dock")
    }

    @Test
    fun `la page des personnes favorites tient`() {
        show()
        open("Personnes favorites")

        assertPage("Personnes favorites", "Retour aux réglages")
    }

    @Test
    fun `la page du lieu des prévisions tient, une ville cherchée`() {
        show()
        open("Lieu des prévisions")
        compose.onNodeWithContentDescription("Nom de la ville").performTextInput("Nantes")
        compose.onNodeWithText("Chercher cette ville").performClick()

        assertPage("Lieu des prévisions", "Retour aux réglages")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun sizes() = SCREEN_SIZES.map { arrayOf<Any>(it) }

        /** Enough drawings in each pack to fill the screen. */
        private const val ICONS = 60

        private const val PICK_HINT = "toucher pour changer l'icône"
    }
}
