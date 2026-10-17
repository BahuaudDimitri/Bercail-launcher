package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Pulling the sheets of the home down to close them: the day and « Écouter » from anywhere, their list scrolling
 * first; the conversation only from its header, never from its messages.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeSheetPullUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(world: FakeWorld): HomeViewModel {
        val home = world.homeViewModel(world.media)
        compose.setContent { BcTheme { HomeRoute(home) } }
        return home
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value

    /** Puts a finger near the top of [from]'s node, then moves it down by [distance] dp ([moveDown]). */
    private fun pullDown(tag: String, from: SemanticsMatcher, distance: Float) {
        compose.onNode(from).performTouchInput { down(Offset(centerX, top + 10f)) }
        moveDown(tag, distance)
    }

    /** Moves the finger already down by [distance] dp, step by step, the screen drawn between two steps. */
    private fun moveDown(tag: String, distance: Float, steps: Int = STEPS) {
        repeat(steps) {
            compose.onNodeWithTag(tag).performTouchInput {
                moveBy(Offset(0f, (distance / steps).dp.toPx()), delayMillis = STEP_MILLIS)
            }
            compose.waitForIdle()
        }
    }

    private fun lift(tag: String) {
        compose.onNodeWithTag(tag).performTouchInput { up() }
        compose.waitForIdle()
    }

    // ---------- the day ----------

    /** A busy day: twelve appointments, every half hour from 8:30, more than the sheet shows at once. */
    private fun busyDay() = FakeWorld(Moment.Matin).apply {
        agenda.set(
            (0 until BUSY).map {
                val start = LocalTime.of(8, 30).plusMinutes(30L * it)
                AgendaEvent(
                    "rdv $it",
                    "Rendez-vous $it",
                    Samples.day.atTime(start),
                    Samples.day.atTime(start.plusMinutes(25))
                )
            }
        )
    }

    private val inTheDay = hasAnyAncestor(hasTestTag(DAY_TAG))

    /** How far the list of the day is scrolled down from its top, in pixels. */
    private fun listScroll() = compose.onNode(hasScrollAction() and inTheDay).fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test
    fun `la liste de la journée défile d'abord jusqu'en haut, puis le même geste tire la feuille et la ferme`() {
        val home = show(busyDay())
        compose.runOnUiThread { home.onDayOpenChange(true, focus = "rdv ${BUSY - 1}") }
        compose.waitForIdle()
        assertThat(listScroll()).isGreaterThan(0f)
        val resting = top(DAY_TAG)

        // The list can still go up: it scrolls, the sheet stays in its place.
        pullDown(DAY_TAG, from = hasScrollAction() and inTheDay, distance = 60f)
        assertThat(listScroll()).isGreaterThan(0f)
        assertThat(top(DAY_TAG)).isCloseTo(resting, delta = 0.5f)

        // The same finger goes on: the list reaches its top, then the sheet goes down under the finger.
        moveDown(DAY_TAG, distance = 330f)
        assertThat(listScroll()).isEqualTo(0f)
        assertThat(top(DAY_TAG) - resting).isGreaterThan(150f)
        lift(DAY_TAG)

        compose.onNodeWithTag(DAY_TAG).assertDoesNotExist()
    }

    // ---------- Écouter ----------

    @Test
    fun `la feuille « Écouter » se ferme en tirant sa liste vers le bas`() {
        show(FakeWorld(Moment.Matin).apply { media.stop() })
        compose.onNodeWithText("Écouter…").performClick()

        pullDown(LISTEN_TAG, from = hasText("Spotify") and hasAnyAncestor(hasTestTag(LISTEN_TAG)), distance = 250f)
        lift(LISTEN_TAG)

        compose.onNodeWithTag(LISTEN_TAG).assertDoesNotExist()
    }

    // ---------- conversation ----------

    private fun openTalk() {
        show(FakeWorld(Moment.Trajet))
        compose.openSheet()
        compose.onNodeWithContentDescription("Léa, 1 non lu").performClick()
        compose.waitForIdle()
    }

    private val leaSaysThanks = hasContentDescription("Léa : Merci pour le déj !")

    @Test
    fun `glisser sur les messages d'une conversation ne la ferme jamais, même d'un coup sec`() {
        openTalk()
        val resting = top(TALK_TAG)

        compose.onNode(leaSaysThanks).performTouchInput {
            swipe(Offset(centerX, top), Offset(centerX, top + 300.dp.toPx()), durationMillis = 40)
        }
        compose.waitForIdle()
        pullDown(TALK_TAG, from = leaSaysThanks, distance = 300f)
        assertThat(top(TALK_TAG)).isCloseTo(resting, delta = 0.5f)
        lift(TALK_TAG)

        compose.onNodeWithTag(TALK_TAG).assertExists()
    }

    @Test
    fun `glisser l'en-tête d'une conversation vers le bas la ferme`() {
        openTalk()

        pullDown(
            TALK_TAG,
            from = hasText("Léa") and isHeading() and hasAnyAncestor(hasTestTag(TALK_TAG)),
            distance = 300f
        )
        lift(TALK_TAG)

        compose.onNodeWithTag(TALK_TAG).assertDoesNotExist()
    }

    private companion object {
        const val BUSY = 12
        const val STEPS = 15
        const val STEP_MILLIS = 100L
    }
}
