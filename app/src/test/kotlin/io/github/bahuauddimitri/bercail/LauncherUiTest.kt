package io.github.bahuauddimitri.bercail

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.feature.home.HOME_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The whole app on a simulated phone: the home, its search in the drawer, the settings, the Home button. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class LauncherUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The settings are read from their file off the main thread: what depends on them arrives a moment later. */
    private fun awaitText(text: String) =
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun awaitDescription(text: String) = compose.waitUntil(TIMEOUT) {
        compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun pressBack() = compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

    private fun pressHome() = compose.activityRule.scenario.onActivity { activity ->
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, home)
    }

    private fun openSearch() {
        awaitDescription("Chercher")
        compose.onNodeWithContentDescription("Chercher").performClick()
        awaitText("Toutes les apps")
    }

    private fun openSettings() {
        openSearch()
        compose.onNodeWithText("Réglages de Bercail").performClick()
        awaitText("Fond d'écran")
    }

    @Test
    fun `Bercail répond quand Android cherche un écran d'accueil`() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val packageManager = ApplicationProvider.getApplicationContext<Context>().packageManager

        val activities = packageManager.queryIntentActivities(home, 0).map { it.activityInfo.name }

        assertThat(activities).contains(MainActivity::class.java.name)
    }

    @Test
    fun `l'accueil s'affiche au lancement, avec son tiroir et sa barre Chercher`() {
        compose.onNodeWithTag(HOME_TAG).assertExists()
        awaitDescription("Déplier le tiroir")
        compose.onNodeWithContentDescription("Chercher").assertExists()
    }

    @Test
    fun `la version de mise au point montre les données du prototype pour ce qui n'est pas encore branché`() {
        awaitText("2 non lus · Léa, Tom")
        compose.onNodeWithText("Salon 60 %, Volets ouverts").assertExists()
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
    }

    @Test
    fun `le tiroir s'ouvre sur les favoris et la maison`() {
        awaitDescription("Déplier le tiroir")
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Salon").assertExists()
    }

    @Test
    fun `toucher la barre Chercher ouvre la liste des apps, les Réglages de Bercail en tête`() {
        openSearch()

        compose.onNodeWithText("Réglages de Bercail").assertExists()
        compose.onNodeWithText("2 non lus · Léa, Tom").assertDoesNotExist()
    }

    @Test
    fun `le geste retour ferme la recherche et rend l'accueil`() {
        openSearch()

        pressBack()

        awaitText("2 non lus · Léa, Tom")
        compose.onNodeWithText("Toutes les apps").assertDoesNotExist()
    }

    @Test
    fun `les Réglages de Bercail s'ouvrent depuis la recherche, le retour ramène à la recherche`() {
        openSettings()
        compose.onNodeWithText("Ligne du temps").assertExists()
        // What the settings cover is no longer read out.
        compose.onNodeWithText("Toutes les apps").assertDoesNotExist()

        pressBack()

        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Fond d'écran").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Toutes les apps").assertExists()
    }

    @Test
    fun `un réglage est enregistré et change l'accueil tout de suite`() {
        openSettings()

        compose.onNodeWithText("Ouvert").performScrollTo().performClick()
        pressHome()

        awaitDescription("Replier le tiroir")
        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
    }

    @Test
    fun `le bouton Accueil referme les réglages et la recherche`() {
        openSettings()

        pressHome()

        awaitText("2 non lus · Léa, Tom")
        compose.onNodeWithText("Toutes les apps").assertDoesNotExist()
        compose.onNodeWithText("Fond d'écran").assertDoesNotExist()
    }

    @Test
    fun `le bouton Accueil ramène de l'écran Écoute à l'Accueil`() {
        awaitText("Looped · Kiasmos")
        compose.onNodeWithText("Looped · Kiasmos").performClick()
        awaitText("Ouvrir dans Spotify")

        pressHome()

        awaitText("Looped · Kiasmos")
    }

    @Test
    fun `le geste retour ne quitte pas l'accueil`() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()

            assertThat(activity.isFinishing).isFalse()
        }
    }

    @Test
    fun `le thème de l'accueil laisse voir le fond d'écran du téléphone`() {
        compose.activityRule.scenario.onActivity { activity ->
            val attributes = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowShowWallpaper))

            assertThat(attributes.getBoolean(0, false)).isTrue()
            attributes.recycle()
        }
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
