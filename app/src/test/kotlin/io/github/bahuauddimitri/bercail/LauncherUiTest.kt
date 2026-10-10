package io.github.bahuauddimitri.bercail

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.bahuauddimitri.bercail.feature.home.HOME_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class LauncherUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
        compose.onNodeWithContentDescription("Déplier le tiroir").assertExists()
        compose.onNodeWithContentDescription("Chercher").assertExists()
    }

    @Test
    fun `en attendant les vraies sources, l'accueil montre les données du prototype`() {
        compose.onNodeWithText("2 non lus · Léa, Tom").assertExists()
        compose.onNodeWithText("Salon 60 %, Volets ouverts").assertExists()
        compose.onNodeWithText("Looped · Kiasmos").assertExists()
    }

    @Test
    fun `le tiroir s'ouvre sur les favoris et la maison`() {
        compose.onNodeWithContentDescription("Déplier le tiroir").performClick()

        compose.onNodeWithContentDescription("Léa, 1 non lu").assertExists()
        compose.onNodeWithContentDescription("Salon").assertExists()
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
}
