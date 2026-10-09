package io.github.bahuauddimitri.bercail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class HomeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `l'accueil vide correspond à sa capture de référence`() {
        compose.setContent { HomeScreen() }

        compose.onRoot().captureRoboImage("src/test/screenshots/home_empty.png")
    }
}

/** Screen of the reference phone: 1080 × 2424 px at 420 dpi. */
const val PIXEL_9 = "w411dp-h923dp-420dpi"
