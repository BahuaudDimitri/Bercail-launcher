package io.github.bahuauddimitri.bercail.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBand
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The search list in the full-screen glass of the band, as the app shows it: pulled down once the list of every
 * app is at its top, the search closes as its arrow does; lower in the list, the slide scrolls it first.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class SearchPullUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val search = FakeWorld(Moment.Matin).searchViewModel()

    private fun show(typed: String = "") {
        search.onOpenChange(true)
        search.onQueryChange(typed)
        compose.setContent {
            BcTheme {
                val state by search.state.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    BcBand(
                        onOpen = null,
                        modifier = Modifier.align(Alignment.BottomCenter).testTag(GLASS),
                        searching = state.open,
                        onCloseSearch = { search.onOpenChange(false) }
                    ) { if (state.open) SearchList(state, search, search.icons) }
                }
            }
        }
    }

    private val list = hasScrollToIndexAction()

    /** How far the list of every app is scrolled down from its top, in pixels. */
    private fun listScroll() =
        compose.onNode(list).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun headerTop() = compose.onNodeWithText("Toutes les apps").getUnclippedBoundsInRoot().top.value

    /** Puts a finger on the list, then moves it down by [distance] dp, the screen drawn between steps. */
    private fun holdDown(distance: Float) {
        compose.onNode(list).performTouchInput { down(Offset(centerX, top + 10f)) }
        repeat(STEPS) {
            compose.onNodeWithTag(GLASS).performTouchInput {
                moveBy(Offset(0f, (distance / STEPS).dp.toPx()), delayMillis = 100)
            }
            compose.waitForIdle()
        }
    }

    /** Pulls the search down by 40 % of the screen, past the third where it closes, and lets go. */
    private fun pullPastAThird() {
        holdDown(distance = compose.onRoot().getUnclippedBoundsInRoot().height.value * 0.4f)
        compose.onNodeWithTag(GLASS).performTouchInput { up() }
        compose.waitForIdle()
    }

    @Test
    fun `la liste de toutes les apps en haut, la tirer vers le bas ferme la recherche`() {
        show()

        pullPastAThird()

        assertThat(search.state.value.open).isFalse()
    }

    @Test
    fun `quand rien ne défile, tirer les résultats vers le bas ferme la recherche et vide le champ`() {
        show(typed = "spo")

        pullPastAThird()

        assertThat(search.state.value.open).isFalse()
        assertThat(search.state.value.query).isEqualTo("")
    }

    @Test
    fun `plus bas dans la liste, glisser vers le bas la fait d'abord remonter, sans tirer la recherche`() {
        show()
        val resting = headerTop()
        compose.onNode(list).performScrollToIndex(LOW_IN_THE_LIST)
        val scrolled = listScroll()

        holdDown(distance = 60f)

        assertThat(listScroll()).isGreaterThan(0f)
        assertThat(scrolled - listScroll()).isGreaterThan(0f)
        assertThat(headerTop()).isCloseTo(resting, delta = 0.5f)
        assertThat(search.state.value.open).isTrue()
    }

    private companion object {
        const val GLASS = "glass"
        const val STEPS = 15
        const val LOW_IN_THE_LIST = 12
    }
}
