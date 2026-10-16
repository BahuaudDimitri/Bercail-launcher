package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcDockScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * A glyph as an app gives it: white, its shape in the alpha, framed as the icon is (the drawing in the middle
     * half). [ring] draws a circle outline, otherwise a filled square.
     */
    private fun glyph(ring: Boolean): ImageBitmap {
        val size = GLYPH_PX
        val image = ImageBitmap(size, size)
        val paint = Paint().apply {
            color = Color.White
            if (ring) {
                style = PaintingStyle.Stroke
                strokeWidth = size / STROKE_PART
            }
        }
        val canvas = Canvas(image)
        if (ring) {
            canvas.drawCircle(Offset(size / 2f, size / 2f), size / RADIUS_PART, paint)
        } else {
            canvas.drawRect(
                Rect(size / EDGE_PART, size / EDGE_PART, size - size / EDGE_PART, size - size / EDGE_PART),
                paint
            )
        }
        return image
    }

    // Drawn once, before the capture: the screen only shows them.
    private val apps by lazy {
        listOf(
            BcDockApp("Chrome", glyph(ring = true)) {},
            BcDockApp("Téléphone", glyph(ring = false)) {},
            BcDockApp("Beeper", null) {},
            BcDockApp("Appareil photo", glyph(ring = true)) {},
            BcDockApp("Agenda", glyph(ring = false)) {}
        )
    }

    // One capture for both docks: a second capture in the same class sometimes comes out blank.
    @Test
    fun `le dock de cinq apps, puis celui de trois qui garde les places de gauche, correspondent à leur capture`() {
        val five = apps
        val three = apps.take(3)
        compose.captureComponent("dock") {
            Column(verticalArrangement = Arrangement.spacedBy(BcSpacing.l)) {
                BcDock(five, Modifier.width(348.dp))
                BcDock(three, Modifier.width(348.dp))
            }
        }
    }

    private companion object {
        const val GLYPH_PX = 116
        const val STROKE_PART = 14f
        const val RADIUS_PART = 5f
        const val EDGE_PART = 3f
    }
}
