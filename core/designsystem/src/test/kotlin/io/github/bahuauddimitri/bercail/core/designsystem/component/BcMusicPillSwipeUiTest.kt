package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The music pill slid to the right: it opens the app that plays. A slide to the left is not its business: it is
 * left to the screen behind, which goes to the Listen screen.
 */
@RunWith(AndroidJUnit4::class)
class BcMusicPillSwipeUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var touched = 0
    private var opened = 0
    private var longPressed = 0

    /** How far the screen behind the pill saw a finger slide, to the right (positive) or to the left. */
    private var behind = 0f

    private fun show() {
        compose.setContent {
            BcTheme {
                Box(
                    Modifier.width(360.dp).padding(start = 40.dp).pointerInput(Unit) {
                        detectHorizontalDragGestures { _, amount -> behind += amount }
                    }
                ) {
                    BcMusicPill(
                        title = "Looped",
                        artist = "Kiasmos",
                        cover = null,
                        playing = true,
                        onClick = { touched++ },
                        modifier = Modifier.testTag("pill"),
                        still = true,
                        swipe = BcRowSwipe("Ouvrir Spotify") { opened++ },
                        onLongClick = { longPressed++ },
                        onLongClickLabel = "Apps d'écoute"
                    )
                }
            }
        }
    }

    private val pill get() = compose.onNodeWithTag("pill")

    @Test
    fun `glisser la pastille musique vers la droite ouvre l'app qui joue, et l'écran derrière ne bouge pas`() {
        show()

        pill.performTouchInput { swipe(start = Offset(20f, centerY), end = Offset(20f + 140.dp.toPx(), centerY)) }

        assertThat(opened).isEqualTo(1)
        assertThat(touched).isEqualTo(0)
        assertThat(behind).isEqualTo(0f)
    }

    @Test
    fun `glisser vers la gauche depuis la pastille reste l'affaire de l'écran derrière elle`() {
        show()

        pill.performTouchInput {
            swipe(start = Offset(centerX, centerY), end = Offset(centerX - 120.dp.toPx(), centerY))
        }

        assertThat(opened).isEqualTo(0)
        assertThat(behind).isLessThan(-40.dp.value)
    }

    @Test
    fun `pendant qu'elle glisse, la pastille dit quelle app va s'ouvrir`() {
        show()
        compose.onNodeWithText("Ouvrir Spotify").assertDoesNotExist()

        pill.performTouchInput {
            down(Offset(20f, centerY))
            moveBy(Offset(30.dp.toPx(), 0f))
            moveBy(Offset(50.dp.toPx(), 0f))
        }

        compose.onNodeWithText("Ouvrir Spotify").assertExists()
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `toucher la pastille garde son rôle, et l'app qui joue est aussi proposée aux lecteurs d'écran`() {
        show()

        pill.performClick()

        assertThat(touched).isEqualTo(1)
        val actions = compose.onNodeWithText(
            "Looped · Kiasmos"
        ).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertThat(actions.map { it.label }).containsExactly("Ouvrir Spotify")
    }

    @Test
    fun `un appui long sur la pastille musique demande les apps d'écoute, sans compter un toucher`() {
        show()

        pill.performTouchInput { longClick() }

        assertThat(longPressed).isEqualTo(1)
        assertThat(touched).isEqualTo(0)
        assertThat(opened).isEqualTo(0)
    }

    @Test
    fun `glisser lentement la pastille vers la droite ouvre l'app qui joue, sans compter un appui long`() {
        show()

        pill.performTouchInput {
            swipe(start = Offset(20f, centerY), end = Offset(20f + 140.dp.toPx(), centerY), durationMillis = SLOW)
        }

        assertThat(opened).isEqualTo(1)
        assertThat(longPressed).isEqualTo(0)
    }

    @Test
    fun `glisser lentement vers la gauche depuis la pastille reste l'affaire de l'écran derrière, sans appui long`() {
        show()

        pill.performTouchInput {
            swipe(
                start = Offset(centerX, centerY),
                end = Offset(centerX - 120.dp.toPx(), centerY),
                durationMillis = SLOW
            )
        }

        assertThat(behind).isLessThan(-40.dp.value)
        assertThat(longPressed).isEqualTo(0)
        assertThat(touched).isEqualTo(0)
    }

    @Test
    fun `un lecteur d'écran trouve l'action « Apps d'écoute » sur la pastille musique`() {
        show()

        val pillText = compose.onNodeWithText("Looped · Kiasmos")
        assertThat(pillText.fetchSemanticsNode().config[SemanticsActions.OnLongClick].label).isEqualTo("Apps d'écoute")
        pillText.performSemanticsAction(SemanticsActions.OnLongClick)
        assertThat(longPressed).isEqualTo(1)
    }

    private companion object {
        /** A slide that lasts longer than a long press takes to be noticed. */
        const val SLOW = 1_500L
    }
}
