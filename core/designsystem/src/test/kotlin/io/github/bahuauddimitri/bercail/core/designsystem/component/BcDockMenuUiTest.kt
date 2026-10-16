package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The menu of shortcuts of an app of the dock, called by a long press on its icon. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcDockMenuUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val asked = mutableListOf<String>()
    private val launched = mutableListOf<String>()
    private var menu by mutableStateOf<BcDockMenu?>(null)

    private fun app(name: String, longClick: Boolean = true) = BcDockApp(
        name = name,
        glyph = ImageBitmap(4, 4),
        onLongClick = if (longClick) ({ asked += name }) else null
    ) { opened += name }

    private fun shortcut(label: String) = BcShortcut(label, glyph = ImageBitmap(4, 4)) { launched += label }

    private fun chromeMenu() = BcDockMenu(
        app = "Chrome",
        shortcuts = listOf(shortcut("Nouvel onglet"), shortcut("Navigation privée")),
        onDismiss = { menu = null }
    )

    private fun show(vararg apps: BcDockApp) = compose.setContent { BcTheme { BcDock(apps.toList(), menu = menu) } }

    private fun pressBack() = compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun `un toucher long sur une icône du dock demande ses raccourcis, sans ouvrir l'app`() {
        show(app("Chrome"), app("Beeper"))

        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        assertThat(asked).containsExactly("Chrome")
        assertThat(opened).isEmpty()
    }

    @Test
    fun `sans raccourcis à demander, un toucher long ne fait rien`() {
        show(app("Chrome", longClick = false))

        compose.onNodeWithContentDescription("Chrome").performTouchInput { longClick() }

        assertThat(opened).isEmpty()
        compose.onAllNodesWithText("Nouvel onglet").assertCountEquals(0)
    }

    @Test
    fun `le menu d'une app montre une ligne par raccourci, chacune un bouton de 48 dp`() {
        menu = chromeMenu()
        show(app("Chrome"), app("Beeper"))

        listOf("Nouvel onglet", "Navigation privée").forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertHeightIsAtLeast(BcSizes.listRow)
        }
        compose.onNodeWithContentDescription("Chrome").assertExists()
    }

    @Test
    fun `le menu se pose au-dessus de l'icône, centré sur elle, sans sortir de l'écran`() {
        val above = AboveAnchor(gap = 10, margin = 40)
        val screen = IntSize(1080, 2400)
        val menuSize = IntSize(600, 300)

        val middle = above.calculatePosition(IntRect(500, 2000, 600, 2100), screen, LayoutDirection.Ltr, menuSize)
        val atTheEdge = above.calculatePosition(IntRect(0, 2000, 100, 2100), screen, LayoutDirection.Ltr, menuSize)

        assertThat(middle).isEqualTo(IntOffset(550 - 300, 2000 - 10 - 300))
        assertThat(atTheEdge.x).isEqualTo(40)
        assertThat(atTheEdge.y + menuSize.height).isLessThan(2000)
    }

    @Test
    fun `toucher un raccourci le lance`() {
        menu = chromeMenu()
        show(app("Chrome"))

        compose.onNodeWithText("Navigation privée", useUnmergedTree = true).performClick()

        assertThat(launched).containsExactly("Navigation privée")
        assertThat(opened).isEmpty()
    }

    @Test
    fun `le geste retour ferme le menu`() {
        menu = chromeMenu()
        show(app("Chrome"))

        pressBack()

        compose.onAllNodesWithText("Nouvel onglet").assertCountEquals(0)
        compose.onNodeWithContentDescription("Chrome").assertExists()
    }

    @Test
    fun `le menu d'une autre app ne s'ouvre pas sur celle-ci`() {
        menu = chromeMenu().copy(app = "Spotify")
        show(app("Chrome"))

        compose.onAllNodesWithText("Nouvel onglet").assertCountEquals(0)
    }
}
