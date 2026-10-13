package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BcNavigationUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val letters = listOf("A", "C", "M", "S")

    @Test
    fun `toucher une lettre de l'alphabet y emmène`() {
        val reached = mutableListOf<String>()
        compose.setContent {
            BcTheme {
                BcAlphabetRail(letters, onLetter = { reached += it }, modifier = Modifier.height(400.dp).testTag("az"))
            }
        }

        compose.onNodeWithTag("az").performTouchInput { click(Offset(centerX, height * 0.6f)) }

        assertThat(reached).containsExactly("M")
    }

    @Test
    fun `glisser le long de l'alphabet passe une seule fois par chaque lettre`() {
        val reached = mutableListOf<String>()
        compose.setContent {
            BcTheme {
                BcAlphabetRail(letters, onLetter = { reached += it }, modifier = Modifier.height(400.dp).testTag("az"))
            }
        }

        compose.onNodeWithTag("az").performTouchInput {
            swipe(start = Offset(centerX, 1f), end = Offset(centerX, height - 1f), durationMillis = 600)
        }

        assertThat(reached).containsExactly("A", "C", "M", "S")
    }

    private val alphabet = ('A'..'Z').map { it.toString() }

    /** A letter of the rail tagged "az", not the one the bubble repeats. */
    private fun railLetter(letter: String) = compose.onNode(
        hasText(letter) and hasAnyAncestor(hasTestTag("az")) and hasAnyAncestor(hasTestTag(ALPHABET_BUBBLE)).not()
    )

    @Test
    fun `tant qu'on touche l'alphabet, une bulle porte la lettre atteinte, et s'efface quand le doigt se lève`() {
        compose.setContent {
            BcTheme { BcAlphabetRail(letters, onLetter = {}, modifier = Modifier.height(400.dp).testTag("az")) }
        }
        compose.onNodeWithTag(ALPHABET_BUBBLE).assertDoesNotExist()

        compose.onNodeWithTag("az").performTouchInput { down(Offset(centerX, height * 0.6f)) }
        compose.onNode(hasText("M") and hasAnyAncestor(hasTestTag(ALPHABET_BUBBLE))).assertExists()

        compose.onNodeWithTag("az").performTouchInput { up() }
        compose.onNodeWithTag(ALPHABET_BUBBLE).assertDoesNotExist()
    }

    @Test
    fun `les lettres s'écartent du bord en vague autour du doigt, la plus proche le plus loin, sans grossir`() {
        compose.setContent {
            BcTheme {
                Box(Modifier.width(240.dp), contentAlignment = Alignment.CenterEnd) {
                    BcAlphabetRail(alphabet, onLetter = {}, modifier = Modifier.height(520.dp).testTag("az"))
                }
            }
        }
        val atRest = listOf("A", "K", "M").associateWith { railLetter(it).getUnclippedBoundsInRoot() }

        // The finger is on M, the thirteenth letter of twenty-six.
        compose.onNodeWithTag("az").performTouchInput { down(Offset(centerX, height * 12.5f / 26f)) }

        val moved = listOf("A", "K", "M").associateWith { railLetter(it).getUnclippedBoundsInRoot() }
        fun shift(letter: String) = atRest.getValue(letter).left - moved.getValue(letter).left
        assertThat(shift("M")).isGreaterThan(40.dp)
        assertThat(shift("K")).isGreaterThan(8.dp)
        assertThat(shift("K")).isLessThan(shift("M"))
        assertThat(shift("A")).isLessThan(4.dp)
        assertThat(moved.getValue("M").width).isEqualTo(atRest.getValue("M").width)
        assertThat(moved.getValue("M").top).isEqualTo(atRest.getValue("M").top)
    }

    @Test
    fun `chaque lettre atteinte en glissant donne un tic sous le doigt`() {
        var ticks = 0
        val counting = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                ticks++
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides counting) {
                BcTheme { BcAlphabetRail(letters, onLetter = {}, modifier = Modifier.height(400.dp).testTag("az")) }
            }
        }

        compose.onNodeWithTag("az").performTouchInput {
            swipe(start = Offset(centerX, 1f), end = Offset(centerX, height - 1f), durationMillis = 600)
        }

        assertThat(ticks).isEqualTo(letters.size)
    }

    @Test
    fun `la ligne du temps montre l'heure de chaque rendez-vous de la journée`() {
        compose.setContent {
            BcTheme {
                BcTimeline(
                    now = 0.3f,
                    marks = listOf(
                        BcTimelineMark(position = 0.2f, label = "09:00"),
                        BcTimelineMark(position = 0.4f, label = "11:30", next = true)
                    ),
                    modifier = Modifier.height(400.dp).width(64.dp)
                )
            }
        }

        compose.onNodeWithText("09:00").assertExists()
        compose.onNodeWithText("11:30").assertExists()
    }

    @Test
    fun `un rendez-vous hors de la journée n'apparaît pas sur la ligne`() {
        compose.setContent {
            BcTheme {
                BcTimeline(
                    now = 0.3f,
                    marks = listOf(BcTimelineMark(position = 1.2f, label = "01:00")),
                    modifier = Modifier.height(400.dp).width(64.dp)
                )
            }
        }

        compose.onNodeWithText("01:00").assertDoesNotExist()
    }

    @Test
    fun `la ligne du temps est annoncée par son résumé`() {
        compose.setContent {
            BcTheme {
                BcTimeline(
                    now = 0.3f,
                    marks = emptyList(),
                    description = "Plus rien aujourd'hui",
                    modifier = Modifier.height(400.dp).width(64.dp).testTag("line")
                )
            }
        }

        assertThat(
            compose.onNodeWithTag("line").fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
            ]
        ).isEqualTo(listOf("Plus rien aujourd'hui"))
    }
}
