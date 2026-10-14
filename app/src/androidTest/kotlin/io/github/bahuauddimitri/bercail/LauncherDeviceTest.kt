package io.github.bahuauddimitri.bercail

import android.content.Intent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.contains
import io.github.bahuauddimitri.bercail.feature.home.HOME_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on an emulator in CI, or on a phone plugged in over USB: ./gradlew connectedCheck. */
@RunWith(AndroidJUnit4::class)
class LauncherDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The settings are read from their file off the main thread: what depends on them arrives a moment later. */
    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitDescription(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `le téléphone propose Bercail comme écran d’accueil`() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val packageManager = InstrumentationRegistry.getInstrumentation().targetContext.packageManager

        val activities = packageManager.queryIntentActivities(home, 0).map { it.activityInfo.name }

        assertThat(activities).contains(MainActivity::class.java.name)
    }

    @Test
    fun `l’accueil s’affiche au lancement`() {
        compose.onNodeWithTag(HOME_TAG).assertExists()
        awaitText("Salon 60 %, Volets ouverts")
    }

    @Test
    fun `toucher la poignée déplie le tiroir puis le replie`() {
        awaitDescription("Déplier le tiroir")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        compose.onNodeWithContentDescription("Salon").assertExists()

        compose.onNodeWithContentDescription("Replier le tiroir").performClick()
        compose.onNodeWithText("Salon 60 %, Volets ouverts").assertExists()
    }

    @Test
    fun `toucher la barre Chercher ouvre la liste des apps du téléphone`() {
        awaitDescription("Chercher")
        compose.onNodeWithContentDescription("Chercher").performClick()

        awaitText("Toutes les apps")
        compose.onNodeWithText("Réglages de Bercail").assertExists()
    }

    @Test
    fun `les Réglages de Bercail s’ouvrent depuis la recherche`() {
        awaitDescription("Chercher")
        compose.onNodeWithContentDescription("Chercher").performClick()
        awaitText("Réglages de Bercail")

        compose.onNodeWithText("Réglages de Bercail").performClick()

        awaitText("Ligne du temps")
    }

    @Test
    fun `toucher l’en-tête ouvre la feuille de la journée et sa poignée la referme`() {
        awaitDescription("Chercher")
        compose.onNode(opensTheDay).performClick()
        awaitText("Aujourd'hui")

        compose.onNodeWithContentDescription("Fermer la feuille").performClick()

        awaitDescription("Chercher")
    }

    @Test
    fun `les Réglages de Bercail parlent de l’agenda du téléphone`() {
        awaitDescription("Chercher")
        compose.onNodeWithContentDescription("Chercher").performClick()
        awaitText("Réglages de Bercail")

        compose.onNodeWithText("Réglages de Bercail").performClick()

        awaitText("Agenda du téléphone")
    }

    private companion object {
        const val TIMEOUT = 10_000L

        /**
         * The top of the home screen, whatever it says: with or without the right to read the calendar. The line of
         * the day opens the day with the same words, but it is not a button.
         */
        val opensTheDay = SemanticsMatcher("l’en-tête ouvre la journée") {
            it.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Voir la journée"
        }
    }
}
