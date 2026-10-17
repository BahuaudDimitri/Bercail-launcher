package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcDockUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()

    private fun app(name: String, glyph: ImageBitmap? = ImageBitmap(4, 4)) = BcDockApp(name, glyph) { opened += name }

    private fun show(vararg apps: BcDockApp) = compose.setContent { BcTheme { BcDock(apps.toList()) } }

    @Test
    fun `chaque icône du dock porte le nom de son app, sans l'écrire`() {
        show(app("Chrome"), app("Téléphone"))

        compose.onNodeWithContentDescription("Chrome")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        compose.onNodeWithContentDescription("Téléphone").assertExists()
        compose.onAllNodesWithText("Chrome", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `toucher une icône du dock ouvre son app`() {
        show(app("Chrome"), app("Téléphone"), app("Beeper"))

        compose.onNodeWithContentDescription("Beeper").performClick()
        compose.onNodeWithContentDescription("Chrome").performClick()

        assertThat(opened).containsExactly("Beeper", "Chrome")
    }

    @Test
    fun `une app sans dessin montre son initiale, seule`() {
        show(app("Chrome"), app("Beeper", glyph = null))

        compose.onNodeWithText("B", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("C", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `le dock ne montre jamais plus de cinq apps`() {
        show(app("Chrome"), app("Téléphone"), app("Beeper"), app("Appareil photo"), app("Agenda"), app("Spotify"))

        compose.onAllNodesWithContentDescription("Spotify").assertCountEquals(0)
        compose.onNodeWithContentDescription("Agenda").assertExists()
    }

    @Test
    fun `chaque icône du dock est une place de 44 dp sans anneau, son dessin cadré sur 56 dp, touchable partout`() {
        show(app("Chrome"))

        assertThat(BcSizes.dock).isEqualTo(44.dp)
        compose.onNodeWithTag(RING_TAG + "Chrome", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(DOCK_GLYPH_TAG + "Chrome", useUnmergedTree = true)
            .assertWidthIsAtLeast(BcSizes.dock)
            .assertHeightIsAtLeast(BcSizes.dock)
        compose.onNodeWithContentDescription("Chrome")
            .assertWidthIsAtLeast(BcSizes.dock)
            .assertHeightIsAtLeast(BcSizes.dock)
    }
}
