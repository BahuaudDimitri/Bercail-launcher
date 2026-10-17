package io.github.bahuauddimitri.bercail.core.designsystem.motion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.all
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMotion
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The transition between two screens held by the finger: it follows the finger, settles on the nearer side or the
 * side it is flung to, plays alone in 400 ms, changes at most 20 times per second, and nothing runs once it rests.
 */
@RunWith(AndroidJUnit4::class)
class BcHeldTransitionUiTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var transition: BcHeldTransition

    private fun show(resting: Float = 0f, animatorScale: Float = 1f) {
        // Frames are advanced by hand, to count them.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            BcTheme {
                CompositionLocalProvider(LocalBcMotion provides BcMotion.forAnimatorScale(animatorScale)) {
                    transition = rememberBcHeldTransition(resting)
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun act(block: BcHeldTransition.() -> Unit) = compose.runOnUiThread { transition.block() }

    /** What the transition shows at each frame for [millis]. */
    private fun framesFor(millis: Long): List<Float> {
        val seen = mutableListOf(transition.progress)
        repeat((millis / FRAME_MILLIS).toInt()) {
            compose.mainClock.advanceTimeByFrame()
            seen += transition.progress
        }
        return seen
    }

    /** At which frames the picture changed. */
    private fun List<Float>.changes() = zipWithNext().withIndex().filter { (_, pair) -> pair.first != pair.second }
        .map { it.index }

    @Test
    fun `sous le doigt, la transition suit le glissement, dans un sens puis dans l'autre`() {
        show()

        act { drag(0.3f) }
        framesFor(millis = 100)
        assertThat(transition.progress).isCloseTo(0.3f, TOLERANCE)

        act { drag(-0.1f) }
        framesFor(millis = 100)
        assertThat(transition.progress).isCloseTo(0.2f, TOLERANCE)
    }

    @Test
    fun `le doigt ne la pousse pas au-delà de ses deux bouts`() {
        show()

        act { drag(-0.5f) }
        framesFor(millis = 100)
        assertThat(transition.progress).isEqualTo(0f)

        act { drag(2f) }
        framesFor(millis = 100)
        assertThat(transition.progress).isEqualTo(1f)
    }

    @Test
    fun `lâchée avant la moitié, elle revient au départ, sans rebond`() {
        show()
        act { drag(0.4f) }
        framesFor(millis = 100)

        act { release() }
        val frames = framesFor(millis = 600)

        assertThat(frames.last()).isEqualTo(0f)
        frames.zipWithNext().forEach { (before, after) -> assertThat(after).isLessThanOrEqualTo(before) }
        assertThat(frames).each { it.isBetween(0f, 1f) }
    }

    @Test
    fun `lâchée après la moitié, elle va au bout, sans rebond`() {
        show()
        act { drag(0.6f) }
        framesFor(millis = 100)

        act { release() }
        val frames = framesFor(millis = 600)

        assertThat(frames.last()).isEqualTo(1f)
        frames.zipWithNext().forEach { (before, after) -> assertThat(after).isGreaterThanOrEqualTo(before) }
    }

    @Test
    fun `lancée d'un coup sec, elle va du côté du geste, même avant la moitié`() {
        show()
        act { drag(0.2f) }
        framesFor(millis = 100)

        act { release(velocity = 3f) }
        framesFor(millis = 600)

        assertThat(transition.progress).isEqualTo(1f)
    }

    @Test
    fun `jouée seule, elle traverse en 400 ms environ, à 20 images par seconde au plus`() {
        show()

        act { playTo(1f) }
        val frames = framesFor(millis = 600)

        val changes = frames.changes()
        val arrival = changes.last() * FRAME_MILLIS
        assertThat(frames.last()).isEqualTo(1f)
        assertThat(arrival).isBetween(SCREEN_MILLIS - FRAME_MILLIS, SCREEN_MILLIS + MAX_WAIT_MILLIS)
        // At 20 frames per second, 400 ms make 8 pictures, and one more for the end.
        assertThat(changes.size).isLessThanOrEqualTo(SCREEN_MILLIS.toInt() * MAX_ANIMATION_FPS / MILLIS + 1)
        changes.zipWithNext().forEach { (a, b) -> assertThat((b - a) * FRAME_MILLIS).isGreaterThan(MIN_GAP_MILLIS) }
    }

    @Test
    fun `sous le doigt aussi, elle change 20 fois par seconde au plus`() {
        show()

        val seen = mutableListOf(transition.progress)
        repeat(FINGER_STEPS) {
            act { drag(0.01f) }
            compose.mainClock.advanceTimeByFrame()
            seen += transition.progress
        }

        // 30 frames, 480 ms: one picture every 50 ms at most.
        assertThat(seen.changes().size).isLessThanOrEqualTo(FINGER_STEPS * FRAME_MILLIS.toInt() / MIN_GAP + 1)
    }

    @Test
    fun `une fois posée, plus rien ne tourne`() {
        show()
        act { playTo(1f) }
        framesFor(millis = 600)

        val after = framesFor(millis = 1_000)

        assertThat(transition.isMoving).isFalse()
        assertThat(after.changes().size).isEqualTo(0)
    }

    @Test
    fun `tant que le doigt la tient, elle n'est pas posée`() {
        show()

        act { drag(1f) }
        framesFor(millis = 100)

        assertThat(transition).all {
            transform { it.progress }.isEqualTo(1f)
            transform { it.isMoving }.isTrue()
        }
    }

    @Test
    fun `envoyée vers le côté où elle est déjà, elle ne bouge pas`() {
        show(resting = 1f)

        act { playTo(1f) }

        assertThat(framesFor(millis = 200).changes().size).isEqualTo(0)
        assertThat(transition.isMoving).isFalse()
    }

    @Test
    fun `quand Android supprime les animations, elle se pose d'un coup`() {
        show(animatorScale = 0f)

        act { playTo(1f) }
        framesFor(millis = 2 * FRAME_MILLIS)

        assertThat(transition.progress).isEqualTo(1f)
        assertThat(transition.isMoving).isFalse()
    }

    private companion object {
        const val FRAME_MILLIS = 16L
        const val SCREEN_MILLIS = 400L
        const val MAX_WAIT_MILLIS = 64L
        const val MIN_GAP_MILLIS = 47L
        const val MIN_GAP = 50
        const val MILLIS = 1_000
        const val FINGER_STEPS = 30
        const val TOLERANCE = 0.001f
    }
}
