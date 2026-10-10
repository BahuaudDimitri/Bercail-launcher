package io.github.bahuauddimitri.bercail

import android.content.Intent
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
        awaitText("2 non lus · Léa, Tom")
    }

    @Test
    fun `toucher la poignée déplie le tiroir puis le replie`() {
        awaitDescription("Déplier le tiroir")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()

        compose.onNodeWithContentDescription("Replier le tiroir").performClick()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
    }

    @Test
    fun `toucher la pastille musique ouvre l’écran Écoute et sa télécommande`() {
        awaitText("Looped · Kiasmos")
        compose.onNodeWithText("Looped · Kiasmos").performClick()

        compose.onNodeWithText("Ouvrir dans Spotify").assertExists()
        compose.onNodeWithContentDescription("Pause").assertExists()
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

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
