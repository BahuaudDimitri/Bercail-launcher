package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isLessThanOrEqualTo
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
    fun `quand la place manque, clavier sorti, les lettres se serrent sans qu'aucune soit coupée`() {
        compose.setContent {
            BcTheme { BcAlphabetRail(alphabet, onLetter = {}, modifier = Modifier.height(240.dp).testTag("az")) }
        }

        val rail = compose.onNodeWithTag("az").getUnclippedBoundsInRoot()

        val first = railLetter("A").getUnclippedBoundsInRoot()
        val last = railLetter("Z").getUnclippedBoundsInRoot()
        assertThat((first.top + first.bottom) / 2).isGreaterThanOrEqualTo(rail.top)
        assertThat((last.top + last.bottom) / 2).isLessThanOrEqualTo(rail.bottom)
        // A letter that no longer fits must not be squeezed: it would show cut in half.
        assertThat(last.height).isEqualTo(first.height)
    }

    @Test
    fun `même serré, l'alphabet mène à la lettre qu'on touche`() {
        val reached = mutableListOf<String>()
        compose.setContent {
            BcTheme {
                BcAlphabetRail(alphabet, onLetter = { reached += it }, modifier = Modifier.height(240.dp).testTag("az"))
            }
        }
        val rail = compose.onNodeWithTag("az").getUnclippedBoundsInRoot()
        val z = railLetter("Z").getUnclippedBoundsInRoot()

        compose.onNodeWithTag("az").performTouchInput {
            click(Offset(centerX, ((z.top + z.bottom) / 2 - rail.top).toPx()))
        }

        assertThat(reached).containsExactly("Z")
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

    // ---------- the line of the day under the finger ----------

    private val day = listOf(
        BcTimelineMark(position = 0.06f, label = "08:00", title = "Stand-up", key = "standup"),
        BcTimelineMark(position = 0.15f, label = "09:30", next = true, title = "Café avec Tom", key = "cafe"),
        BcTimelineMark(position = 0.47f, label = "15:00", title = "Point design", key = "design"),
        BcTimelineMark(position = 0.76f, label = "20:00", title = "Dîner", key = "diner")
    )

    /** What the line opened: "day: design" for the day brought to an appointment, "mark: diner" for an appointment. */
    private val opened = mutableListOf<String>()

    /** The line of the day, 400 dp long, tagged "line", at the right of a 400 dp wide box; it opens [opened]. */
    private fun showLine(marks: List<BcTimelineMark> = day, haptics: HapticFeedback? = null, titlesBelow: Dp = 0.dp) {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides (haptics ?: LocalHapticFeedback.current)) {
                BcTheme {
                    Box(Modifier.padding(top = 120.dp).width(400.dp), contentAlignment = Alignment.TopEnd) {
                        BcTimeline(
                            now = 0.1f,
                            marks = marks,
                            titlesBelow = titlesBelow,
                            modifier = Modifier.height(400.dp).width(78.dp).testTag("line"),
                            onOpenDay = { opened += "day: ${it?.key}" },
                            onOpenMark = { opened += "mark: ${it.key}" }
                        )
                    }
                }
            }
        }
    }

    private val bubble get() = compose.onNodeWithTag(TIMELINE_BUBBLE)

    @Test
    fun `toucher la ligne du temps ouvre la journée sur le rendez-vous le plus proche du doigt`() {
        showLine()

        compose.onNodeWithTag("line").performTouchInput { click(Offset(centerX, height * 0.45f)) }

        assertThat(opened).containsExactly("day: design")
    }

    @Test
    fun `en glissant le long du fil, une bulle nomme le rendez-vous sous le doigt, et lâcher l'ouvre`() {
        showLine()
        bubble.assertDoesNotExist()

        compose.onNodeWithTag("line").performTouchInput { down(Offset(centerX, height * 0.06f)) }
        bubble.assert(hasAnyDescendant(hasText("08:00 · Stand-up")) or hasText("08:00 · Stand-up"))
        compose.onNodeWithTag("line").performTouchInput { moveTo(Offset(centerX, height * 0.8f)) }
        bubble.assert(hasAnyDescendant(hasText("20:00 · Dîner")) or hasText("20:00 · Dîner"))
        assertThat(opened).isEmpty()

        compose.onNodeWithTag("line").performTouchInput { up() }

        assertThat(opened).containsExactly("mark: diner")
        bubble.assertDoesNotExist()
    }

    @Test
    fun `un jour sans rendez-vous, toucher la ligne ouvre simplement la journée, sans bulle`() {
        showLine(marks = emptyList())

        compose.onNodeWithTag("line").performTouchInput { down(center) }
        bubble.assertDoesNotExist()
        compose.onNodeWithTag("line").performTouchInput { up() }

        assertThat(opened).containsExactly("day: null")
    }

    @Test
    fun `un doigt qui tremble à peine reste un toucher, qui ouvre la journée`() {
        showLine()

        compose.onNodeWithTag("line").performTouchInput {
            down(Offset(centerX, height * 0.76f))
            moveBy(Offset(1f, 2f))
            up()
        }

        assertThat(opened).containsExactly("day: diner")
    }

    @Test
    fun `écarter le doigt vers la gauche, au-delà de la bulle, annule, la bulle s'efface et lâcher n'ouvre rien`() {
        showLine()
        compose.onNodeWithTag("line").performTouchInput {
            down(Offset(centerX, height * 0.06f))
            moveTo(Offset(centerX, height * 0.47f))
        }
        bubble.assertExists()

        compose.onNodeWithTag("line").performTouchInput { moveTo(Offset(centerX - 300.dp.toPx(), height * 0.47f)) }
        compose.waitForIdle()
        bubble.assertDoesNotExist()
        compose.onNodeWithTag("line").performTouchInput { up() }

        assertThat(opened).isEmpty()
    }

    @Test
    fun `revenir sur le fil après l'avoir écarté reprend le geste`() {
        showLine()
        compose.onNodeWithTag("line").performTouchInput {
            down(Offset(centerX, height * 0.06f))
            moveTo(Offset(centerX, height * 0.47f))
            moveTo(Offset(centerX - 300.dp.toPx(), height * 0.47f))
            moveTo(Offset(centerX, height * 0.76f))
        }
        bubble.assertExists()

        compose.onNodeWithTag("line").performTouchInput { up() }

        assertThat(opened).containsExactly("mark: diner")
    }

    @Test
    fun `chaque rendez-vous atteint en glissant le long de la ligne donne un tic sous le doigt`() {
        var ticks = 0
        showLine(
            haptics = object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    ticks++
                }
            }
        )

        compose.onNodeWithTag("line").performTouchInput {
            swipe(start = Offset(centerX, 1f), end = Offset(centerX, height - 1f), durationMillis = 600)
        }

        assertThat(ticks).isEqualTo(day.size)
    }

    @Test
    fun `la ligne se courbe autour du doigt, l'heure la plus proche s'écarte du bord et les lointaines restent`() {
        showLine()
        val atRest = listOf("08:00", "15:00").associateWith { compose.onNodeWithText(it).getUnclippedBoundsInRoot() }

        compose.onNodeWithTag("line").performTouchInput { down(Offset(centerX, height * 0.47f)) }

        fun shift(time: String) =
            atRest.getValue(time).left - compose.onNodeWithText(time).getUnclippedBoundsInRoot().left
        assertThat(shift("15:00")).isGreaterThan(40.dp)
        assertThat(shift("08:00")).isLessThan(4.dp)
    }

    @Test
    fun `sans rien à ouvrir, la ligne du temps ne réagit pas au doigt`() {
        compose.setContent {
            BcTheme {
                BcTimeline(now = 0.1f, marks = day, modifier = Modifier.height(400.dp).width(78.dp).testTag("line"))
            }
        }

        compose.onNodeWithTag("line").performTouchInput { down(center) }

        bubble.assertDoesNotExist()
    }

    @Test
    fun `la ligne du temps propose aux lecteurs d'écran de voir la journée`() {
        showLine()

        compose.onNodeWithTag("line")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(opened).containsExactly("day: null")
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
    fun `le trait monte jusqu'au bord haut, et 7 h est placé sous la place laissée en haut`() {
        compose.setContent {
            BcTheme {
                BcTimeline(
                    now = 0.3f,
                    marks = listOf(BcTimelineMark(position = 0f, label = "07:00"), BcTimelineMark(0.5f, "15:30")),
                    headroom = 40.dp,
                    modifier = Modifier.height(440.dp).width(64.dp).testTag("line")
                )
            }
        }

        val line = compose.onNodeWithTag("line").getUnclippedBoundsInRoot()
        val seven = compose.onNodeWithText("07:00").getUnclippedBoundsInRoot()
        val halfway = compose.onNodeWithText("15:30").getUnclippedBoundsInRoot()
        assertThat(((seven.top + seven.bottom) / 2 - line.top).value).isCloseTo(40f, 1f)
        // Halfway between 7 am and midnight: halfway between the room left at the top and the bottom.
        assertThat(((halfway.top + halfway.bottom) / 2 - line.top).value).isCloseTo(240f, 1f)
    }

    @Test
    fun `chaque rendez-vous écrit son titre puis son heure à gauche du fil`() {
        showLine()

        val title = compose.onNodeWithText("Point design").getUnclippedBoundsInRoot()
        val time = compose.onNodeWithText("15:00").getUnclippedBoundsInRoot()
        val line = compose.onNodeWithTag("line").getUnclippedBoundsInRoot()
        assertThat(title.right).isLessThanOrEqualTo(time.left)
        assertThat(time.right).isLessThan(line.right - 24.dp)
    }

    @Test
    fun `un rendez-vous trop proche du précédent n'écrit que son heure`() {
        showLine(marks = day + BcTimelineMark(0.49f, "15:25", title = "Café avec Léa", key = "lea", brief = true))

        compose.onNodeWithText("15:25").assertExists()
        compose.onNodeWithText("Café avec Léa").assertDoesNotExist()
    }

    @Test
    fun `au-dessus de la hauteur donnée aux titres, un rendez-vous n'écrit que son heure`() {
        showLine(titlesBelow = 100.dp)

        compose.onNodeWithText("08:00").assertExists()
        compose.onNodeWithText("Stand-up").assertDoesNotExist()
        compose.onNodeWithText("Point design").assertExists()
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
