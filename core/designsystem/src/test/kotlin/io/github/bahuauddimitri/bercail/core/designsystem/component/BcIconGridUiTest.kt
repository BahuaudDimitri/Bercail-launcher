package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** A grid of icons in sections: each read by its name, the chosen one marked, drawings asked only when shown. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcIconGridUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val touched = mutableListOf<String>()
    private val asked = mutableListOf<String>()

    private fun icon(key: String, chosen: Boolean = false) =
        BcGridIcon(key = key, name = key.replace('_', ' '), chosen = chosen) { touched += key }

    private fun show(sections: List<BcIconSection>) = compose.setContent {
        BcTheme {
            BcIconGrid(
                sections = sections,
                image = { key ->
                    asked += key
                    ImageBitmap(DRAWING, DRAWING)
                },
                modifier = Modifier.height(GRID_HEIGHT),
                header = { BcText("En tête", style = BcTextStyle.Body) }
            )
        }
    }

    @Test
    fun `chaque icône est annoncée par son nom, et la toucher la choisit`() {
        show(listOf(BcIconSection("Lines Free", listOf(icon("google_maps"), icon("mail")))))

        compose.onNodeWithContentDescription("google maps").performClick()

        assertThat(touched).containsExactly("google_maps")
    }

    @Test
    fun `l'icône choisie est marquée, les autres non`() {
        show(listOf(BcIconSection("Lines Free", listOf(icon("google_maps", chosen = true), icon("mail")))))

        compose.onNodeWithContentDescription("google maps").assertIsSelected()
        compose.onNodeWithContentDescription("mail").assertIsNotSelected()
    }

    @Test
    fun `l'en-tête vient d'abord, puis chaque section sous son titre`() {
        show(listOf(BcIconSection("Proposées", listOf(icon("mail"))), BcIconSection("Whicons", listOf(icon("chat")))))

        compose.onNodeWithText("En tête").assertExists()
        compose.onNodeWithText("Proposées").assertExists()
        compose.onNodeWithText("Whicons").assertExists()
    }

    @Test
    fun `une même icône peut paraître dans deux sections`() {
        show(listOf(BcIconSection("Proposées", listOf(icon("mail"))), BcIconSection("Lines", listOf(icon("mail")))))

        compose.onNodeWithText("Lines").assertExists()
    }

    @Test
    fun `les dessins ne sont demandés que pour les icônes qui paraissent, au fil du défilement`() {
        show(listOf(BcIconSection("Lines Free", List(MANY) { icon("icon_$it") })))
        compose.waitForIdle()

        assertThat(asked).contains("icon_0")
        assertThat(asked).doesNotContain("icon_${MANY - 1}")
        assertThat(asked.size < MANY / 2).isTrue()

        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("icon ${MANY - 1}"))

        assertThat(asked).contains("icon_${MANY - 1}")
    }

    @Test
    fun `dans un panneau qui ne défile pas, la grille défile seule sous l'en-tête du panneau`() {
        compose.setContent {
            BcTheme {
                BcPanel("Icône", onBack = {}, backDescription = "Retour", scrolls = false) {
                    BcIconGrid(listOf(BcIconSection("Lines", List(MANY) { icon("icon_$it") })), image = { null })
                }
            }
        }

        compose.onNodeWithContentDescription("Retour").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("icon ${MANY - 1}"))
        compose.onNodeWithContentDescription("Retour").assertExists()
    }

    private companion object {
        const val MANY = 600
        const val DRAWING = 8
        val GRID_HEIGHT = 400.dp
    }
}
