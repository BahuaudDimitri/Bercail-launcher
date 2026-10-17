package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.motion.MAX_ANIMATION_FPS
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Sliding a sheet down to close it: the glass follows the finger; let go past a third of its height, or flicked
 * down, it closes; let go before, it goes back up, without a bounce. A list inside scrolls first. A conversation
 * only closes from its header. At most 20 pictures per second, and nothing runs once it rests.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class BcSheetPullUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var visible by mutableStateOf(true)
    private lateinit var scroll: ScrollState

    /** A sheet 400 dp high: its handle, then [body]. */
    private fun show(grip: BcSheetGrip = BcSheetGrip.Anywhere, body: @Composable () -> Unit) = compose.setContent {
        BcTheme {
            BcSheet(
                visible = visible,
                onDismiss = { visible = false },
                modifier = Modifier.height(SHEET_HEIGHT.dp).testTag(SHEET),
                grip = grip
            ) { body() }
        }
    }

    private fun showPlain() = show { Box(Modifier.fillMaxWidth().height(BODY.dp).testTag(BODY_TAG)) }

    /** A sheet holding a list that can scroll, scrolled a little down. */
    private fun showList() {
        show {
            scroll = rememberScrollState(LIST_START)
            Column(Modifier.fillMaxWidth().height(BODY.dp).verticalScroll(scroll).testTag(BODY_TAG)) {
                repeat(ROWS) { BcText("Ligne $it", Modifier.height(ROW.dp)) }
            }
        }
    }

    /** A conversation: its header, then the messages, scrolled from their end. */
    private fun showConversation() {
        show(grip = BcSheetGrip.Header) {
            BcSheetHeader("Léa", Modifier.testTag(HEADER_TAG), subtitle = "WhatsApp via Beeper", action = "Beeper")
            scroll = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().height(MESSAGES.dp).verticalScroll(scroll, reverseScrolling = true)
                    .testTag(BODY_TAG)
            ) {
                repeat(ROWS) { BcText("Message $it", Modifier.height(ROW.dp)) }
            }
        }
    }

    /** Where the glass stands: the top of what it holds, which moves with it. */
    private fun top() = compose.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().top.value

    /**
     * Puts a finger on [tag] and moves it down by [distance] dp, step by step, the screen drawn between two steps as
     * on the phone, without lifting it.
     */
    private fun holdDown(tag: String = BODY_TAG, distance: Float) {
        compose.onNodeWithTag(tag).performTouchInput { down(Offset(centerX, top + 10f)) }
        stepDown { distance.dp.toPx() }
    }

    /** Moves the finger already down by [pixels], step by step. */
    private fun stepDown(pixels: Density.() -> Float) = repeat(STEPS) {
        compose.onNodeWithTag(SHEET).performTouchInput {
            moveBy(Offset(0f, pixels() / STEPS), delayMillis = SLOW_STEP_MILLIS)
        }
        compose.waitForIdle()
    }

    private fun lift() = compose.onNodeWithTag(SHEET).performTouchInput { up() }

    /** How far the finger goes before a slide starts, in dp: the sheet follows it from there. */
    private fun touchSlop(): Float = compose.activity.let {
        ViewConfiguration.get(it).scaledTouchSlop / it.resources.displayMetrics.density
    }

    @Test
    fun `la feuille suit le doigt vers le bas`() {
        showPlain()
        val resting = top()

        holdDown(distance = 100f)

        assertThat(top() - resting).isCloseTo(100f - touchSlop(), delta = 1f)
        assertThat(visible).isEqualTo(true)
    }

    @Test
    fun `lâchée avant le tiers, elle remonte à sa place, sans rebond`() {
        showPlain()
        val resting = top()
        holdDown(distance = SHEET_HEIGHT * 0.25f)

        compose.mainClock.autoAdvance = false
        lift()
        val seen = (0 until SETTLE_FRAMES).map {
            compose.mainClock.advanceTimeByFrame()
            top()
        }

        assertThat(seen.last()).isCloseTo(resting, delta = 0.5f)
        seen.forEach { assertThat(it).isGreaterThanOrEqualTo(resting - 0.5f) }
        seen.zipWithNext().forEach { (before, after) -> assertThat(after).isLessThanOrEqualTo(before + 0.5f) }
        assertThat(visible).isEqualTo(true)
    }

    @Test
    fun `lâchée au-delà du tiers, elle descend jusqu'au bout et se ferme`() {
        showPlain()
        holdDown(distance = SHEET_HEIGHT * 0.4f)
        assertThat(visible).isEqualTo(true)

        lift()
        compose.waitForIdle()

        assertThat(visible).isEqualTo(false)
    }

    @Test
    fun `un coup sec vers le bas la ferme, même avant le tiers`() {
        showPlain()

        compose.onNodeWithTag(BODY_TAG).performTouchInput {
            swipe(Offset(centerX, top + 10f), Offset(centerX, top + 10f + (SHEET_HEIGHT * 0.15f).dp.toPx()), 40)
        }
        compose.waitForIdle()

        assertThat(visible).isEqualTo(false)
    }

    @Test
    fun `la liste défile d'abord vers son haut, puis le même geste tire la feuille`() {
        showList()
        val resting = top()

        compose.onNodeWithTag(BODY_TAG).performTouchInput { down(Offset(centerX, top + 10f)) }
        stepDown { LIST_START + 100.dp.toPx() }

        assertThat(scroll.value).isEqualTo(0)
        assertThat(top() - resting).isCloseTo(100f - touchSlop(), delta = 1f)
    }

    @Test
    fun `tant que la liste peut remonter, glisser vers le bas ne tire pas la feuille`() {
        showList()
        val resting = top()

        holdDown(distance = 40f)
        compose.waitForIdle()

        assertThat(scroll.value).isGreaterThan(0)
        assertThat(top()).isCloseTo(resting, delta = 0.5f)
    }

    @Test
    fun `un glissement sur les messages d'une conversation ne la ferme jamais`() {
        showConversation()
        val resting = top()

        compose.onNodeWithTag(BODY_TAG).performTouchInput {
            swipe(Offset(centerX, top + 10f), Offset(centerX, bottom - 10f), 40)
        }
        compose.waitForIdle()
        assertThat(scroll.value).isGreaterThan(0)
        holdDown(distance = 150f)
        compose.waitForIdle()

        assertThat(top()).isCloseTo(resting, delta = 0.5f)
        lift()
        compose.waitForIdle()
        assertThat(visible).isEqualTo(true)
    }

    @Test
    fun `une conversation se ferme en glissant son en-tête`() {
        showConversation()

        holdDown(tag = HEADER_TAG, distance = SHEET_HEIGHT * 0.5f)
        lift()
        compose.waitForIdle()

        assertThat(visible).isEqualTo(false)
    }

    @Test
    fun `tirée par le doigt, la feuille change d'image 20 fois par seconde au plus`() {
        showPlain()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(BODY_TAG).performTouchInput { down(Offset(centerX, top + 10f)) }

        val seen = (0 until FINGER_FRAMES).map {
            compose.onNodeWithTag(BODY_TAG).performTouchInput { moveBy(Offset(0f, 2.dp.toPx()), delayMillis = 0) }
            compose.mainClock.advanceTimeByFrame()
            top()
        }

        val changes = seen.zipWithNext().count { (before, after) -> before != after }
        // 30 frames of 16 ms make 480 ms: one picture every 50 ms at most.
        assertThat(changes).isBetween(1, FINGER_FRAMES * FRAME_MILLIS / (MILLIS / MAX_ANIMATION_FPS) + 1)
    }

    @Test
    fun `revenue à sa place, plus rien ne bouge`() {
        showPlain()
        holdDown(distance = 60f)
        lift()
        compose.waitForIdle()
        val settled = top()

        compose.mainClock.autoAdvance = false
        val seen = (0 until SETTLE_FRAMES).map {
            compose.mainClock.advanceTimeByFrame()
            top()
        }

        seen.forEach { assertThat(it).isEqualTo(settled) }
        assertThat(visible).isEqualTo(true)
    }

    private companion object {
        const val SHEET = "sheet"
        const val BODY_TAG = "body"
        const val HEADER_TAG = "header"
        const val SHEET_HEIGHT = 400f
        const val BODY = 300
        const val MESSAGES = 240
        const val ROW = 50
        const val ROWS = 30
        const val LIST_START = 300
        const val STEPS = 20
        const val SLOW_STEP_MILLIS = 100L
        const val SETTLE_FRAMES = 60
        const val FINGER_FRAMES = 30
        const val FRAME_MILLIS = 16
        const val MILLIS = 1_000
    }
}
