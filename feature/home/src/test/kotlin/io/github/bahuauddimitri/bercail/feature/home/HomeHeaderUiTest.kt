package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The top of the home screen on a simulated phone: touched, it opens the day, or the phone's calendar app once nothing
 * is left to come today; slid to the right, it always opens the calendar app.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PIXEL_9)
class HomeHeaderUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(world: FakeWorld = FakeWorld(Moment.Matin)): FakeWorld {
        val home = world.homeViewModel()
        compose.setContent { BcTheme { HomeRoute(home) } }
        return world
    }

    /** The morning of the prototype's day, with nothing in the calendar. */
    private fun emptyDay() = FakeWorld(Moment.Matin).apply { agenda.set(emptyList()) }

    /** The morning of the prototype's day, with only [events] in the calendar. */
    private fun dayWith(vararg events: AgendaEvent) = FakeWorld(Moment.Matin).apply { agenda.set(events.toList()) }

    /** A slide long enough to count, starting near the left edge of what is touched. */
    private fun TouchInjectionScope.swipeToTheRight() =
        swipe(start = Offset(10f, centerY), end = Offset(10f + 140.dp.toPx(), centerY))

    /** The top of the home screen, a button that says what touching it does. The line of the day is not a button. */
    private fun headerSaying(label: String) = SemanticsMatcher("the header says « $label »") {
        it.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
            it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    @Test
    fun `une journée sans rendez-vous, toucher l'en-tête ouvre l'app agenda du téléphone et pas la feuille`() {
        val world = show(emptyDay())

        compose.onNodeWithText("Journée libre").performClick()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
        compose.onNodeWithText("Rien de prévu aujourd'hui").assertDoesNotExist()
    }

    @Test
    fun `une journée dont tout est passé, toucher l'en-tête ouvre l'app agenda du téléphone et pas la feuille`() {
        val world =
            show(dayWith(AgendaEvent("footing", "Footing", Samples.day.atTime(6, 30), Samples.day.atTime(7, 15))))

        compose.onNodeWithText("Journée libre").performClick()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
    }

    @Test
    fun `une journée dont tout est passé, l'en-tête annonce qu'il ouvre l'agenda`() {
        show(dayWith(AgendaEvent("footing", "Footing", Samples.day.atTime(6, 30), Samples.day.atTime(7, 15))))

        compose.onNode(headerSaying("Ouvrir l'agenda")).assertExists()
    }

    @Test
    fun `une journée entière ne compte pas comme à venir, toucher l'en-tête ouvre l'app agenda du téléphone`() {
        val day = Samples.day
        val world = show(dayWith(AgendaEvent("off", "Congé", day.atStartOfDay(), day.plusDays(1).atStartOfDay(), true)))

        compose.onNodeWithText("Journée libre").performClick()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
    }

    @Test
    fun `avec des rendez-vous, toucher l'en-tête ouvre la feuille et pas l'app agenda`() {
        val world = show()

        compose.onHeaderTitle("Stand-up").performClick()

        compose.onNodeWithText("Aujourd'hui").assertExists()
        assertThat(world.agenda.opened).isEmpty()
    }

    @Test
    fun `glisser l'en-tête vers la droite ouvre l'app agenda du téléphone`() {
        val world = show()

        compose.onHeaderTitle("Stand-up").performTouchInput { swipeToTheRight() }

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
        compose.onNodeWithText("Aujourd'hui").assertDoesNotExist()
    }

    @Test
    fun `une journée sans rendez-vous, glisser l'en-tête vers la droite ouvre aussi l'app agenda`() {
        val world = show(emptyDay())

        compose.onNodeWithText("Journée libre").performTouchInput { swipeToTheRight() }

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
    }

    @Test
    fun `l'en-tête propose « Ouvrir l'agenda » aux lecteurs d'écran`() {
        val world = show()

        val header = compose.onNode(headerSaying("Voir la journée")).fetchSemanticsNode().config
        val actions = header[SemanticsActions.CustomActions]
        assertThat(actions.map { it.label }).containsExactly("Ouvrir l'agenda")
        actions.single().action()

        assertThat(world.agenda.opened).containsExactly("calendar: 2026-10-02")
    }

    @Test
    fun `une journée sans rendez-vous, l'en-tête annonce qu'il ouvre l'agenda`() {
        show(emptyDay())

        compose.onNode(headerSaying("Ouvrir l'agenda")).assertExists()
        compose.onNode(headerSaying("Voir la journée")).assertDoesNotExist()
    }
}
