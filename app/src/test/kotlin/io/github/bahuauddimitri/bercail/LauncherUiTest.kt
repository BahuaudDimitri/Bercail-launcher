package io.github.bahuauddimitri.bercail

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
    fun `l'accueil s'affiche au lancement`() {
        compose.onNodeWithTag(HOME_TAG).assertExists()
    }

    @Test
    fun `le geste retour ne quitte pas l'accueil`() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()

            assertThat(activity.isFinishing).isFalse()
        }
    }

    @Test
    fun `l'accueil laisse voir le fond d'écran du téléphone`() {
        compose.activityRule.scenario.onActivity { activity ->
            val flags = activity.window.attributes.flags

            assertThat(flags and WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER).isNotEqualTo(0)
        }
    }
}
