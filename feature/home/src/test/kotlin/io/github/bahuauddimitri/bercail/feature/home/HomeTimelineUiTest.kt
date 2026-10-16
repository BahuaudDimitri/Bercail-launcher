package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRowState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The line of the day under the finger, on a simulated phone: it names the appointments and opens the day. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeTimelineUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The home screen being shown, for what the screen itself does not say: which row is brought forward. */
    private lateinit var home: HomeViewModel

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)) {
        home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
    }

    private val line get() = compose.onNodeWithTag(TIMELINE_TAG)

    @Test
    fun `le fil part du bord haut de l'écran`() {
        show()

        assertThat(line.getUnclippedBoundsInRoot().top).isEqualTo(0.dp)
    }

    @Test
    fun `à la hauteur du haut de l'accueil, un rendez-vous n'écrit que son heure`() {
        val world = FakeWorld(Moment.Matin)
        world.agenda.set(listOf(event("Café", 7, 30), event("Déjeuner avec Léa", 12, 30)))
        show(world)

        compose.onNode(hasText("07:30") and onTheLine).assertExists()
        compose.onNodeWithText("Café").assertDoesNotExist()
        compose.onNode(hasText("Déjeuner avec Léa") and onTheLine).assertExists()
    }

    private val onTheLine = hasAnyAncestor(hasTestTag(TIMELINE_TAG))

    private fun event(title: String, hour: Int, minute: Int) = Samples.day.atTime(hour, minute).let { start ->
        AgendaEvent(id = title, title = title, start = start, end = start.plusMinutes(30))
    }

    @Test
    fun `toucher la ligne du temps ouvre la feuille de la journée`() {
        show()

        line.performTouchInput { click(Offset(width - 30f, height * 0.9f)) }

        compose.onNodeWithText("Aujourd'hui").assertExists()
    }

    @Test
    fun `glisser le long de la ligne du temps nomme le rendez-vous atteint, et lâcher ouvre la journée sur lui`() {
        show()

        // « Courses » is at 18:30: two thirds of the way from 7 am to midnight.
        line.performTouchInput {
            down(Offset(width - 30f, height * 0.2f))
            moveTo(Offset(width - 30f, height * 0.68f))
        }
        compose.onNodeWithText("18:30 · Courses").assertExists()
        line.performTouchInput { up() }

        compose.onNodeWithText("Aujourd'hui").assertExists()
        assertThat(home.state.value.dayRows.single { it.state == DayRowState.Highlighted }.title).isEqualTo("Courses")
    }

    @Test
    fun `à côté de la ligne, sur les heures écrites, un toucher n'ouvre rien`() {
        show()

        // Only the line and a little around it take the finger: the rest of the screen keeps its own gestures.
        line.performTouchInput { click(Offset(10f, height * 0.5f)) }

        assertThat(home.state.value.dayOpen).isEqualTo(false)
    }
}
