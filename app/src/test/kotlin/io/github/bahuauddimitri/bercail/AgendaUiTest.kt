package io.github.bahuauddimitri.bercail

import android.Manifest
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.testing.FakeAgendaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeClock
import io.github.bahuauddimitri.bercail.core.testing.Samples
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The agenda in the whole app, on a simulated phone: Bercail asks before reading it, shows nothing of it until
 * the user agrees, then the day opens from the top of the home screen and from the search.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class AgendaUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<BercailApplication>()

    /** The calendar of the phone holds the prototype's day, and it is 8:10 that morning. */
    private val agenda = FakeAgendaSource(Samples.eventsOn(Samples.day))
    private val clock = FakeClock(Samples.day.atTime(8, 10))

    private fun showHome(): ActivityScenario<MainActivity> {
        app.sources = Sources(app, agenda = agenda, clock = clock)
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun allowTheAgenda() = shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)

    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitNoText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

    private fun openTheDay() {
        awaitText("Relier l'agenda")
        compose.onNodeWithText("Relier l'agenda").performClick()
        awaitText("Autoriser la lecture de l'agenda")
    }

    private fun ActivityScenario<MainActivity>.asked(): List<String> {
        var asked = emptyList<String>()
        onActivity { asked = shadowOf(it).lastRequestedPermission?.requestedPermissions.orEmpty().toList() }
        return asked
    }

    @Test
    fun `sans autorisation, l'accueil ne montre aucun rendez-vous et propose de relier l'agenda`() {
        showHome().use {
            awaitText("Relier l'agenda")

            compose.onNodeWithText("Agenda pas encore relié").assertExists()
            compose.onNodeWithText("Stand-up").assertDoesNotExist()
        }
    }

    @Test
    fun `Bercail ne pose la question d'Android que lorsqu'on touche le bouton`() {
        showHome().use { home ->
            openTheDay()
            assertThat(home.asked()).isEqualTo(emptyList())

            compose.onNodeWithText("Autoriser la lecture de l'agenda").performClick()

            assertThat(home.asked()).containsExactly(Manifest.permission.READ_CALENDAR)
        }
    }

    @Test
    fun `dès que l'utilisateur accepte, la feuille montre les rendez-vous du jour`() {
        showHome().use {
            openTheDay()

            // On this simulated phone, Android answers "yes" by itself: the user's agreement is given just before.
            allowTheAgenda()
            compose.onNodeWithText("Autoriser la lecture de l'agenda").performClick()

            awaitText("Déjeuner avec Léa")
            compose.onNodeWithText("Autoriser la lecture de l'agenda").assertDoesNotExist()
        }
    }

    @Test
    fun `une autorisation donnée dans les Paramètres du téléphone est vue au retour sur l'accueil`() {
        showHome().use { home ->
            awaitText("Relier l'agenda")
            home.moveToState(Lifecycle.State.CREATED)

            allowTheAgenda()
            home.moveToState(Lifecycle.State.RESUMED)

            awaitText("Stand-up")
            compose.onNodeWithText("Relier l'agenda").assertDoesNotExist()
        }
    }

    @Test
    fun `l'agenda se relie aussi depuis les Réglages de Bercail`() {
        showHome().use { home ->
            compose.waitUntil(TIMEOUT) {
                compose.onAllNodesWithContentDescription("Chercher").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Chercher").performClick()
            awaitText("Réglages de Bercail")
            compose.onNodeWithText("Réglages de Bercail").performClick()
            awaitText("Agenda du téléphone")

            compose.onNodeWithText("Agenda du téléphone").performScrollTo().performClick()

            assertThat(home.asked()).containsExactly(Manifest.permission.READ_CALENDAR)
        }
    }

    @Test
    fun `toucher un rendez-vous trouvé par la recherche ouvre la journée, ce rendez-vous mis en avant`() {
        allowTheAgenda()
        showHome().use {
            awaitText("Stand-up")
            compose.onNodeWithContentDescription("Chercher").performClick()
            awaitText("Toutes les apps")
            compose.onNodeWithContentDescription("Chercher").performTextInput("courses")
            awaitText("Courses")

            compose.onNodeWithText("Courses").performClick()

            awaitText("Aujourd'hui")
            compose.onNode(hasText("Courses") and hasAnyAncestor(hasTestTag("home-day"))).assertExists()
            compose.onNodeWithText("Résultats").assertDoesNotExist()
        }
    }

    @Test
    fun `le bouton Accueil referme la feuille de la journée`() {
        allowTheAgenda()
        showHome().use { home ->
            awaitText("Stand-up")
            // The top of the home screen, not the same title written beside the line of the day.
            compose.onNode(hasText("Stand-up") and !hasAnyAncestor(hasTestTag("home-timeline"))).performClick()
            awaitText("Aujourd'hui")

            home.onActivity { activity ->
                val pressed = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, pressed)
            }

            awaitNoText("Aujourd'hui")
        }
    }

    /** The real calendar of the phone, allowed but empty: on this simulated phone, it holds nothing. */
    private fun showAnEmptyDay(): ActivityScenario<MainActivity> {
        allowTheAgenda()
        app.sources = Sources(app, clock = clock)
        return ActivityScenario.launch<MainActivity>(MainActivity::class.java).also { awaitText("Journée libre") }
    }

    /** What Bercail last asked Android to show: the phone's calendar app, on today, or nothing. */
    private fun calendarShown(): String? = shadowOf(app).nextStartedActivity
        ?.takeIf { it.action == Intent.ACTION_VIEW }
        ?.dataString

    private val today = "content://com.android.calendar/time/" +
        Samples.day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `une journée sans rendez-vous, toucher l'en-tête ouvre l'app agenda du téléphone à la date du jour`() {
        showAnEmptyDay().use {
            compose.onNodeWithText("Journée libre").performClick()

            assertThat(calendarShown()).isEqualTo(today)
            compose.onNodeWithText("Rien de prévu aujourd'hui").assertDoesNotExist()
        }
    }

    @Test
    fun `glisser l'en-tête vers la droite ouvre l'app agenda du téléphone`() {
        showAnEmptyDay().use {
            compose.onNodeWithText("Journée libre").performTouchInput {
                swipe(start = Offset(10f, centerY), end = Offset(10f + 140.dp.toPx(), centerY))
            }

            assertThat(calendarShown()).isEqualTo(today)
        }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
