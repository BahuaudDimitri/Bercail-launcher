package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
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
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A line of the folded drawer slid to the right: it follows the finger, says what it opens, and opens it. */
@RunWith(AndroidJUnit4::class)
class BcSummarySwipeUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var touched = 0
    private var opened = 0
    private var ticks = 0
    private val counting = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            ticks++
        }
    }

    /** The messages line, 360 dp wide, tagged "row". With [swipe], sliding it to the right opens Beeper. */
    private fun show(swipe: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides counting) {
                BcTheme {
                    Box(Modifier.width(360.dp)) {
                        BcSummaryRow(
                            text = "Léa, Tom",
                            strong = "2 non lus",
                            leading = BcSummaryLeading.Dots(listOf(BcPastel.Coral, BcPastel.Blue)),
                            onClick = { touched++ },
                            modifier = Modifier.testTag("row"),
                            swipe = if (swipe) BcRowSwipe("Ouvrir Beeper") { opened++ } else null
                        )
                    }
                }
            }
        }
    }

    private val row get() = compose.onNodeWithTag("row")
    private val line get() = compose.onNodeWithText("2 non lus · Léa, Tom")

    @Test
    fun `glisser une ligne du résumé vers la droite ouvre ce qu'elle annonce, avec un tic quand c'est assez`() {
        show()

        row.performTouchInput { swipe(start = Offset(40f, centerY), end = Offset(40f + 140.dp.toPx(), centerY)) }

        assertThat(opened).isEqualTo(1)
        assertThat(touched).isEqualTo(0)
        assertThat(ticks).isEqualTo(1)
    }

    @Test
    fun `pendant qu'elle glisse, la ligne suit le doigt et dit ce qui va s'ouvrir`() {
        show()
        val atRest = line.getUnclippedBoundsInRoot().left
        compose.onNodeWithText("Ouvrir Beeper").assertDoesNotExist()

        row.performTouchInput {
            down(Offset(40f, centerY))
            moveBy(Offset(30.dp.toPx(), 0f))
            moveBy(Offset(50.dp.toPx(), 0f))
        }

        assertThat(line.getUnclippedBoundsInRoot().left - atRest).isGreaterThan(40.dp)
        compose.onNodeWithText("Ouvrir Beeper").assertExists()
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `un glissement trop court n'ouvre rien, et la ligne revient à sa place`() {
        show()
        val atRest = line.getUnclippedBoundsInRoot().left

        row.performTouchInput { swipe(start = Offset(40f, centerY), end = Offset(40f + 36.dp.toPx(), centerY)) }

        assertThat(opened).isEqualTo(0)
        assertThat(ticks).isEqualTo(0)
        assertThat(line.getUnclippedBoundsInRoot().left).isEqualTo(atRest)
        compose.onNodeWithText("Ouvrir Beeper").assertDoesNotExist()
    }

    @Test
    fun `glisser vers la gauche ne fait rien`() {
        show()
        val atRest = line.getUnclippedBoundsInRoot().left

        row.performTouchInput {
            down(Offset(300f, centerY))
            moveBy(Offset(-30.dp.toPx(), 0f))
            moveBy(Offset(-80.dp.toPx(), 0f))
        }
        assertThat(line.getUnclippedBoundsInRoot().left).isEqualTo(atRest)
        row.performTouchInput { up() }

        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `toucher la ligne garde son rôle`() {
        show()

        row.performClick()

        assertThat(touched).isEqualTo(1)
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `ce que le glissement ouvre est aussi proposé aux lecteurs d'écran`() {
        show()

        val actions = line.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertThat(actions.map { it.label }).containsExactly("Ouvrir Beeper")
        actions.single().action()

        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `une ligne qui n'ouvre rien en glissant ne suit pas le doigt`() {
        show(swipe = false)
        val atRest = line.getUnclippedBoundsInRoot().left

        row.performTouchInput {
            down(Offset(40f, centerY))
            moveBy(Offset(30.dp.toPx(), 0f))
            moveBy(Offset(50.dp.toPx(), 0f))
        }

        assertThat(line.getUnclippedBoundsInRoot().left).isEqualTo(atRest)
        assertThat(line.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)).isNull()
    }
}
