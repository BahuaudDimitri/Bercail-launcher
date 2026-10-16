package io.github.bahuauddimitri.bercail.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.testing.FakeAppsSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The search list compared with its reference pictures, and the icons it draws. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class SearchScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val world = FakeWorld(Moment.Matin).apply {
        apps.paintGlyph("spotify", size = GLYPH_PX, framed = true)
        apps.paintGlyph("agenda", size = GLYPH_PX, framed = true)
    }

    private fun capture(name: String, typed: String = "") {
        val search = world.searchViewModel()
        search.onOpenChange(true)
        search.onQueryChange(typed)
        compose.setContent {
            BcTheme {
                Box(Modifier.fillMaxSize().background(BcColors.base).padding(BcSpacing.l)) { SearchRoute(search) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", HALF_SIZE)
    }

    @Test
    fun `toutes les apps, avec le dock et l'alphabet, correspondent à leur capture`() = capture("search_all_apps")

    @Test
    fun `les résultats par famille correspondent à leur capture`() = capture("search_results", typed = "l")

    @Test
    fun `les propositions web et Play Store correspondent à leur capture`() = capture("search_nothing", typed = "ramen")

    @Test
    fun `le dessin d'une app n'est demandé au téléphone qu'une seule fois, jamais son icône en couleurs`() =
        runBlocking {
            val apps = FakeAppsSource(Samples.apps).apply { paintGlyph("spotify") }
            val icons = AppIcons(apps)
            assertThat(icons.known("spotify")).isNull()

            val first = icons.of("spotify")
            val second = icons.of("spotify")

            assertThat(apps.glyphRequests).containsExactly("spotify")
            assertThat(apps.iconRequests).isEmpty()
            assertThat(second).isEqualTo(first)
            assertThat(icons.known("spotify")!!.asAndroidBitmap().getPixel(0, 0)).isEqualTo(OPAQUE_WHITE)
        }

    @Test
    fun `une app sans dessin garde son initiale, sans être redemandée`() = runBlocking {
        val apps = FakeAppsSource(Samples.apps)
        val icons = AppIcons(apps)

        assertThat(icons.of("beeper")).isNull()
        assertThat(icons.of("beeper")).isNull()

        assertThat(apps.glyphRequests).containsExactly("beeper")
    }

    @Test
    fun `seuls les dessins les plus récents sont gardés en mémoire`() = runBlocking {
        val apps = FakeAppsSource(Samples.apps).apply {
            listOf("spotify", "agenda", "beeper").forEach { paintGlyph(it) }
        }
        val icons = AppIcons(apps, capacity = 2)

        icons.of("spotify")
        icons.of("agenda")
        icons.of("beeper")

        assertThat(icons.known("spotify")).isNull()
        assertThat(icons.known("beeper")!!.width).isEqualTo(4)
    }

    private companion object {
        const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()
        const val GLYPH_PX = 30

        /** Full screens are kept at half size: readable, and four times lighter in the repository. */
        val HALF_SIZE = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5))
    }
}
