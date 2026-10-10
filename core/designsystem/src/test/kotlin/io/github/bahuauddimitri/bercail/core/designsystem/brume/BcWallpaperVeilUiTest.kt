package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Over the user's own wallpaper, the veil keeps the agenda readable, however bright the picture is. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcWallpaperVeilUiTest {
    /** Draws the veil over a white picture; gives the brightness left at the top and at the bottom (0 to 1). */
    private fun onWhiteWallpaper(): Pair<Float, Float> {
        val picture = ImageBitmap(WIDTH_PX, HEIGHT_PX)
        CanvasDrawScope().draw(
            density = Density(PIXEL_9_DENSITY),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(picture),
            size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat())
        ) {
            drawRect(Color.White)
            drawWallpaperVeil()
        }
        val pixels = picture.toPixelMap()
        return pixels[WIDTH_PX / 2, TOP_ROW].luminance() to pixels[WIDTH_PX / 2, HEIGHT_PX - 1].luminance()
    }

    @Test
    fun `sur une image blanche, le haut de l'écran devient assez sombre pour lire l'agenda`() {
        val (top, _) = onWhiteWallpaper()

        assertThat(top).isLessThan(0.1f)
    }

    @Test
    fun `le bas de l'image reste clair, seulement adouci`() {
        val (top, bottom) = onWhiteWallpaper()

        assertThat(bottom).isGreaterThan(0.5f)
        assertThat(bottom).isGreaterThan(top)
        assertThat(bottom).isLessThan(1f)
    }

    private companion object {
        const val WIDTH_PX = 540
        const val HEIGHT_PX = 1212
        const val PIXEL_9_DENSITY = 2.625f

        /** A few pixels under the top edge, where the weather line is written. */
        const val TOP_ROW = 40
    }
}
