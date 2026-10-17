package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Pulling the full-screen search down to close it: the glass follows the finger like a sheet; let go past a third
 * of the screen, or flicked down, the search closes and the keyboard goes away; let go before, it goes back up. Its
 * list scrolls first. Closed, the folded band comes back up in its place.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class BcBandSearchPullUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var searching by mutableStateOf(true)
    private var keyboardHidden = 0
    private lateinit var scroll: ScrollState

    private val keyboard = object : SoftwareKeyboardController {
        override fun show() = Unit

        override fun hide() {
            keyboardHidden++
        }
    }

    /** The band at the bottom of the screen, searching, around a list scrolled [start] pixels down. */
    private fun show(start: Int = 0) = compose.setContent {
        BcTheme {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                Box(Modifier.fillMaxSize()) {
                    BcBand(
                        onOpen = {},
                        modifier = Modifier.align(Alignment.BottomCenter),
                        searching = searching,
                        onCloseSearch = { searching = false }
                    ) {
                        scroll = rememberScrollState(start)
                        if (searching) {
                            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll).testTag(LIST)) {
                                repeat(ROWS) { BcText("App $it", Modifier.height(ROW.dp)) }
                            }
                        } else {
                            BcText("2 non lus · Léa, Tom")
                        }
                        BcText("Chercher", Modifier.testTag(BAR))
                    }
                }
            }
        }
    }

    private fun top(tag: String = LIST) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value

    private fun screen() = compose.onRoot().getUnclippedBoundsInRoot().height.value

    /** Puts a finger near the top of the list, then moves it down by [distance] dp, the screen drawn between steps. */
    private fun holdDown(distance: Float) {
        compose.onNodeWithTag(LIST).performTouchInput { down(Offset(centerX, top + 10f)) }
        repeat(STEPS) {
            compose.onNodeWithTag(BAR).performTouchInput {
                moveBy(Offset(0f, (distance / STEPS).dp.toPx()), delayMillis = STEP_MILLIS)
            }
            compose.waitForIdle()
        }
    }

    private fun lift() {
        compose.onNodeWithTag(BAR).performTouchInput { up() }
        compose.waitForIdle()
    }

    private fun touchSlop(): Float = compose.activity.let {
        ViewConfiguration.get(it).scaledTouchSlop / it.resources.displayMetrics.density
    }

    @Test
    fun `en recherche, le verre suit le doigt vers le bas`() {
        show()
        val resting = top()

        holdDown(distance = 150f)

        assertThat(top() - resting).isCloseTo(150f - touchSlop(), delta = 1f)
        assertThat(searching).isEqualTo(true)
    }

    @Test
    fun `lâché avant le tiers de l'écran, le verre remonte et la recherche reste ouverte`() {
        show()
        val resting = top()

        holdDown(distance = screen() / 4)
        lift()

        assertThat(top()).isCloseTo(resting, delta = 0.5f)
        assertThat(searching).isEqualTo(true)
        assertThat(keyboardHidden).isEqualTo(0)
    }

    @Test
    fun `lâché au-delà du tiers de l'écran, la recherche se ferme et le clavier s'en va`() {
        show()

        holdDown(distance = screen() * 0.4f)
        lift()

        assertThat(searching).isEqualTo(false)
        assertThat(keyboardHidden).isGreaterThan(0)
    }

    @Test
    fun `en recherche, la liste défile d'abord jusqu'en haut, puis le geste tire le verre`() {
        show(start = LIST_START)
        val resting = top()

        holdDown(distance = 60f)
        assertThat(scroll.value).isGreaterThan(0)
        assertThat(top()).isCloseTo(resting, delta = 0.5f)

        repeat(STEPS) {
            compose.onNodeWithTag(BAR).performTouchInput { moveBy(Offset(0f, 20.dp.toPx()), delayMillis = STEP_MILLIS) }
            compose.waitForIdle()
        }
        assertThat(scroll.value).isEqualTo(0)
        assertThat(top() - resting).isGreaterThan(100f)
    }

    @Test
    fun `fermée en tirant, le bandeau replié remonte à sa place sans passer au-dessus`() {
        show()
        holdDown(distance = screen() * 0.4f)

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(BAR).performTouchInput { up() }
        val seen = (0 until SETTLE_FRAMES).map {
            compose.mainClock.advanceTimeByFrame()
            top(BAR)
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val folded = top(BAR)

        assertThat(searching).isEqualTo(false)
        seen.forEach { assertThat(it).isGreaterThanOrEqualTo(folded - 0.5f) }
        assertThat(seen.last()).isCloseTo(folded, delta = 0.5f)
    }

    private companion object {
        const val LIST = "list"
        const val BAR = "bar"
        const val ROW = 50
        const val ROWS = 30
        const val LIST_START = 200
        const val STEPS = 15
        const val STEP_MILLIS = 100L
        const val SETTLE_FRAMES = 90
    }
}
