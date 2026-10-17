package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Accueil ⇄ Écoute on a simulated Pixel 9: the arch comes down from the top edge as the finger slides, the hero
 * leaves to the left; letting go settles on the nearer side; a touch, the back gesture or the music stopping play the
 * same way alone, in about 400 ms, at 20 pictures per second at most; and then nothing moves any more.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeTransitionUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld(Moment.Matin)

    private fun show() {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
    }

    private fun listen() {
        show()
        compose.onNodeWithText("Looped · Kiasmos").performClick()
        compose.waitForIdle()
    }

    /** How far the top of the arch is from the top edge of the screen (negative: above it); null without arch. */
    private fun archTop(): Float? = compose.onAllNodesWithContentDescription(COVER).fetchSemanticsNodes()
        .singleOrNull()?.let { compose.onNodeWithContentDescription(COVER).getUnclippedBoundsInRoot().top.value }

    private fun heroLeft() = compose.onHeaderTitle("Stand-up").getUnclippedBoundsInRoot().left.value

    /** Where the arch stands at each frame for [millis], the clock driven by hand. */
    private fun archTopsFor(millis: Long): List<Float?> {
        val seen = mutableListOf(archTop())
        repeat((millis / FRAME_MILLIS).toInt()) {
            compose.mainClock.advanceTimeByFrame()
            seen += archTop()
        }
        return seen
    }

    private fun TouchInjectionScope.slideBy(x: Dp = 0.dp, y: Dp = 0.dp) = moveBy(Offset(x.toPx(), y.toPx()))

    /** Puts a finger down and slides it left past the slop, ready to move the transition. */
    private fun TouchInjectionScope.takeLeft() {
        down(center)
        moveBy(Offset(-viewConfiguration.touchSlop, 0f))
    }

    /** Lifts the finger after holding it still: no flick. */
    private fun TouchInjectionScope.letGoSlowly() {
        advanceEventTime(STILL_MILLIS)
        up()
    }

    private fun onHome() {
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
        assertThat(archTop()).isNull()
    }

    private fun onListen() {
        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText("Looped · Kiasmos").assertDoesNotExist()
        assertThat(archTop()).isEqualTo(0f)
    }

    // ---------- under the finger ----------

    @Test
    fun `en glissant vers la gauche, l'arche descend du bord haut et le héros part à gauche, à mesure du doigt`() {
        show()
        val heroAtRest = heroLeft()

        // A quarter of the width of the screen: a quarter of the way.
        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput {
            takeLeft()
            slideBy(x = -(PHONE_WIDTH / 4).dp)
        }
        compose.waitForIdle()

        assertThat(archTop()).isNotNull().isCloseTo(-ARCH_HEIGHT * 3 / 4, TOLERANCE)
        assertThat(heroLeft()).isCloseTo(heroAtRest - PHONE_WIDTH / 4, TOLERANCE)
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
    }

    @Test
    fun `le doigt qui revient en arrière fait remonter l'arche`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput {
            takeLeft()
            slideBy(x = -(PHONE_WIDTH / 2).dp)
            slideBy(x = (PHONE_WIDTH / 4).dp)
        }
        compose.waitForIdle()

        assertThat(archTop()).isNotNull().isCloseTo(-ARCH_HEIGHT * 3 / 4, TOLERANCE)
    }

    @Test
    fun `sur Écoute, glisser vers le haut fait remonter l'arche sous le doigt, et la redescendre`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            down(Offset(centerX, height * LOW_ON_SCREEN))
            moveBy(Offset(0f, -viewConfiguration.touchSlop))
            slideBy(y = -LIFT.dp)
        }
        compose.waitForIdle()
        assertThat(archTop()).isNotNull().isCloseTo(-LIFT, TOLERANCE)

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput { slideBy(y = (LIFT / 2).dp) }
        compose.waitForIdle()
        assertThat(archTop()).isNotNull().isCloseTo(-LIFT / 2, TOLERANCE)
    }

    @Test
    fun `sur Écoute, glisser de côté ne fait pas bouger l'arche`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            down(Offset(centerX, height * LOW_ON_SCREEN))
            slideBy(x = -(PHONE_WIDTH / 3).dp)
        }
        compose.waitForIdle()

        assertThat(archTop()).isEqualTo(0f)
    }

    // ---------- letting go ----------

    @Test
    fun `lâché avant la moitié, le doigt laisse revenir l'Accueil`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput {
            takeLeft()
            slideBy(x = -(PHONE_WIDTH * 0.4f).dp)
            letGoSlowly()
        }
        compose.waitForIdle()

        onHome()
    }

    @Test
    fun `lâché après la moitié, le doigt laisse Écoute se poser`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput {
            takeLeft()
            slideBy(x = -(PHONE_WIDTH * 0.6f).dp)
            letGoSlowly()
        }
        compose.waitForIdle()

        onListen()
    }

    @Test
    fun `un coup sec vers la gauche ouvre Écoute, même court`() {
        show()

        compose.onNodeWithTag(MIDDLE_TAG).performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - FLICK.dp.toPx(), durationMillis = FLICK_MILLIS)
        }
        compose.waitForIdle()

        onListen()
    }

    @Test
    fun `sur Écoute, lâché avant la moitié de la remontée, l'arche redescend et Écoute reste`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            down(Offset(centerX, height * LOW_ON_SCREEN))
            moveBy(Offset(0f, -viewConfiguration.touchSlop))
            slideBy(y = -(ARCH_HEIGHT * 0.4f).dp)
            letGoSlowly()
        }
        compose.waitForIdle()

        onListen()
    }

    @Test
    fun `sur Écoute, lâché après la moitié de la remontée, l'Accueil revient`() {
        listen()

        compose.onNodeWithTag(PLAYER_TAG).performTouchInput {
            down(Offset(centerX, height * LOW_ON_SCREEN))
            moveBy(Offset(0f, -viewConfiguration.touchSlop))
            slideBy(y = -(ARCH_HEIGHT * 0.6f).dp)
            letGoSlowly()
        }
        compose.waitForIdle()

        onHome()
    }

    // ---------- played alone ----------

    @Test
    fun `toucher la pastille joue la transition seule, en 400 ms environ, à 20 images par seconde au plus`() {
        show()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false

        compose.onNodeWithText("Looped · Kiasmos").performClick()
        val tops = archTopsFor(millis = 700)

        // The arch comes down by steps, never back up, and rests against the top edge. Only what shows counts.
        val moves = tops.filterNotNull().filter { it > -ARCH_HEIGHT }
        assertThat(moves.last()).isEqualTo(0f)
        moves.zipWithNext().forEach { (before, after) -> assertThat(before).isLessThanOrEqualTo(after) }
        val pictures = moves.distinct().size
        assertThat(pictures).isLessThanOrEqualTo(MAX_PICTURES)
        val arrival = tops.indexOfFirst { it == 0f } * FRAME_MILLIS
        assertThat(arrival).isBetween(SCREEN_MILLIS - FRAME_MILLIS, SCREEN_MILLIS + LATE_MILLIS)
    }

    @Test
    fun `le geste retour joue la transition à l'envers, l'arche remonte dans le bord haut`() {
        listen()
        compose.mainClock.autoAdvance = false

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        val tops = archTopsFor(millis = 700)

        val moves = tops.filterNotNull()
        assertThat(moves.any { it < 0f && it > -ARCH_HEIGHT }).isEqualTo(true)
        moves.zipWithNext().forEach { (before, after) -> assertThat(after).isLessThanOrEqualTo(before) }
        compose.mainClock.autoAdvance = true
        onHome()
    }

    @Test
    fun `quand la musique s'arrête sur Écoute, la transition se joue à l'envers`() {
        listen()
        compose.mainClock.autoAdvance = false

        world.media.stop()
        val tops = archTopsFor(millis = 700)

        assertThat(tops.filterNotNull().any { it < 0f && it > -ARCH_HEIGHT }).isEqualTo(true)
        assertThat(tops.last()).isNull()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Écouter…").assertExists()
    }

    @Test
    fun `une fois posée, plus rien ne bouge`() {
        listen()
        compose.mainClock.autoAdvance = false

        val tops = archTopsFor(millis = 1_000)

        assertThat(tops.distinct()).isEqualTo(listOf(0f))
    }

    private companion object {
        const val COVER = "Pochette de Looped"
        const val PHONE_WIDTH = 411f
        const val ARCH_HEIGHT = 484f
        const val LIFT = 110f
        const val FLICK = 80f
        const val FLICK_MILLIS = 100L
        const val LOW_ON_SCREEN = 0.9f
        const val STILL_MILLIS = 300L
        const val FRAME_MILLIS = 16L
        const val SCREEN_MILLIS = 400L
        const val LATE_MILLIS = 100L

        // 400 ms at 20 pictures per second, and the last one.
        const val MAX_PICTURES = 9
        const val TOLERANCE = 1.5f
    }
}
