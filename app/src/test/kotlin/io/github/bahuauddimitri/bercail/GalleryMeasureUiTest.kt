package io.github.bahuauddimitri.bercail

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The measurement modes of the debug gallery, which scripts/measure-brume.sh starts on the Pixel. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h923dp-420dpi")
class GalleryMeasureUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun launch(brume: String, sheet: String? = null): ActivityScenario<GalleryActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, GalleryActivity::class.java).putExtra("brume", brume)
        if (sheet != null) intent.putExtra("sheet", sheet)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun `pour mesurer Brume seule, la galerie ne montre aucune feuille`() {
        launch("still").use {
            compose.onNodeWithContentDescription("Fermer la feuille").assertDoesNotExist()
        }
    }

    @Test
    fun `pour mesurer le verre, la galerie pose une feuille givrée ou teintée sur Brume immobile`() {
        listOf("frosted", "tinted", "blinking").forEach { sheet ->
            launch("still", sheet).use {
                compose.onNodeWithContentDescription("Fermer la feuille").assertExists()
            }
        }
    }
}
