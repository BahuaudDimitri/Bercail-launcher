package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcShortcutMenuScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** A glyph as a shortcut gives it: a white plus in the middle half, framed as an icon is. */
    private fun plus(): ImageBitmap {
        val image = ImageBitmap(GLYPH_PX, GLYPH_PX)
        val paint = Paint().apply {
            color = Color.White
            style = PaintingStyle.Stroke
            strokeWidth = GLYPH_PX / STROKE_PART
        }
        val low = GLYPH_PX / EDGE_PART
        val high = GLYPH_PX - low
        val middle = GLYPH_PX / 2f
        Canvas(image).apply {
            drawLine(Offset(low, middle), Offset(high, middle), paint)
            drawLine(Offset(middle, low), Offset(middle, high), paint)
        }
        return image
    }

    // Drawn once, before the capture: the screen only shows them.
    private val shortcuts by lazy {
        listOf(
            BcShortcut("Nouvel onglet", plus()) {},
            BcShortcut("Navigation privée", plus()) {},
            BcShortcut("Le Monde", null) {}
        )
    }

    @Test
    fun `le menu des raccourcis d'une app correspond à sa capture`() {
        val shown = shortcuts
        compose.captureComponent("shortcut_menu") { BcShortcutMenu(shown) }
    }

    private companion object {
        const val GLYPH_PX = 116
        const val STROKE_PART = 12f
        const val EDGE_PART = 3f
    }
}
