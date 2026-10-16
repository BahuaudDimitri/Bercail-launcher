package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The header of a sheet: who or what, under it the detail, and a bordered button to the app on the right. */
@RunWith(AndroidJUnit4::class)
class BcSheetHeaderUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var opened = 0

    private fun show() = compose.setContent {
        BcTheme {
            BcSheetHeader(
                title = "Léa",
                modifier = Modifier.width(WIDTH).testTag(HEADER),
                subtitle = "WhatsApp via Beeper",
                action = "Beeper",
                onAction = { opened++ }
            )
        }
    }

    @Test
    fun `l'en-tête montre le titre, le détail dessous et le bouton à droite`() {
        show()

        compose.onNodeWithText("Léa").assertIsDisplayed()
        compose.onNodeWithText("WhatsApp via Beeper").assertIsDisplayed()
        val header = compose.onNodeWithTag(HEADER).getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText("Beeper").getUnclippedBoundsInRoot()
        assertThat(button.right.value).isCloseTo(header.right.value, EDGE)
    }

    @Test
    fun `le bouton de l'en-tête s'aligne sur le bas du détail`() {
        show()

        val detail = compose.onNodeWithText("WhatsApp via Beeper").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText("Beeper").getUnclippedBoundsInRoot()
        // The 40 dp touch target of the button stands on the bottom of the header, like the detail.
        assertThat(button.bottom.value).isCloseTo(detail.bottom.value, EDGE)
    }

    @Test
    fun `toucher le bouton de l'en-tête ouvre l'app`() {
        show()

        compose.onNodeWithText("Beeper").performClick()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `le titre de l'en-tête est annoncé comme un titre`() {
        show()

        val title = compose.onNodeWithText("Léa").fetchSemanticsNode()
        assertThat(title.config.contains(SemanticsProperties.Heading)).isTrue()
    }

    @Test
    fun `sans bouton ni détail, l'en-tête n'a que son titre`() {
        compose.setContent { BcTheme { BcSheetHeader("Écouter", Modifier.testTag(HEADER)) } }

        compose.onNodeWithText("Écouter").assertIsDisplayed()
    }

    private companion object {
        const val HEADER = "header"
        val WIDTH = 300.dp
        const val EDGE = 1f
    }
}
