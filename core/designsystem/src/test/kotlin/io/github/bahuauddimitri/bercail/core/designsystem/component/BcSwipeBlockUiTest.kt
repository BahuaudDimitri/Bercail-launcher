package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A block of text, like the top of the home screen, touched or slid to the right. */
@RunWith(AndroidJUnit4::class)
class BcSwipeBlockUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var touched = 0
    private var opened = 0

    /** Two lines, 360 dp wide, tagged "block". Sliding them to the right opens the calendar. */
    private fun show() {
        compose.setContent {
            BcTheme {
                Box(Modifier.width(360.dp)) {
                    BcSwipeBlock(
                        swipe = BcRowSwipe("Ouvrir l'agenda") { opened++ },
                        onClick = { touched++ },
                        onClickLabel = "Voir la journée",
                        modifier = Modifier.testTag("block")
                    ) {
                        BcText("Dans 1 h 20 · 09:30", style = BcTextStyle.LabelStrong)
                        BcText("Stand-up", style = BcTextStyle.AgendaTitle)
                    }
                }
            }
        }
    }

    private val block get() = compose.onNodeWithTag("block")
    private val title get() = compose.onNodeWithText("Stand-up")

    @Test
    fun `glisser le bloc vers la droite ouvre ce qu'il annonce`() {
        show()

        block.performTouchInput { swipe(start = Offset(40f, centerY), end = Offset(40f + 140.dp.toPx(), centerY)) }

        assertThat(opened).isEqualTo(1)
        assertThat(touched).isEqualTo(0)
    }

    @Test
    fun `pendant qu'il glisse, le bloc suit le doigt et dit ce qui va s'ouvrir`() {
        show()
        val atRest = title.getUnclippedBoundsInRoot().left
        compose.onNodeWithText("Ouvrir l'agenda").assertDoesNotExist()

        block.performTouchInput {
            down(Offset(40f, centerY))
            moveBy(Offset(30.dp.toPx(), 0f))
            moveBy(Offset(50.dp.toPx(), 0f))
        }

        assertThat(title.getUnclippedBoundsInRoot().left - atRest).isGreaterThan(40.dp)
        compose.onNodeWithText("Ouvrir l'agenda").assertExists()
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `un glissement trop court n'ouvre rien, et le bloc revient à sa place`() {
        show()
        val atRest = title.getUnclippedBoundsInRoot().left

        block.performTouchInput { swipe(start = Offset(40f, centerY), end = Offset(40f + 36.dp.toPx(), centerY)) }

        assertThat(opened).isEqualTo(0)
        assertThat(title.getUnclippedBoundsInRoot().left).isEqualTo(atRest)
    }

    @Test
    fun `toucher le bloc fait ce que dit son bouton`() {
        show()

        compose.onNode(hasClickLabel("Voir la journée")).performClick()

        assertThat(touched).isEqualTo(1)
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `le bloc est un bouton, et ce que le glissement ouvre est proposé aux lecteurs d'écran`() {
        show()

        val button = compose.onNode(hasClickLabel("Voir la journée")).fetchSemanticsNode().config
        assertThat(button[SemanticsProperties.Role]).isEqualTo(Role.Button)
        val actions = button[SemanticsActions.CustomActions]
        assertThat(actions.map { it.label }).containsExactly("Ouvrir l'agenda")
        actions.single().action()

        assertThat(opened).isEqualTo(1)
    }

    private fun hasClickLabel(label: String) = SemanticsMatcher("touching it says « $label »") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }
}
