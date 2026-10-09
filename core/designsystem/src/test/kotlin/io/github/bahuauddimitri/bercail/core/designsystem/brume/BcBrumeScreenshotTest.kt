package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Brume is drawn straight into a picture, in software: a full-screen animated drawing captured through the
 * simulated screen sometimes came out blank, depending on timing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcBrumeScreenshotTest {
    private fun capture(
        name: String,
        colors: BcBrumeColors,
        weather: BcWeather = BcWeather.Clear,
        night: Boolean = false
    ) {
        val picture = ImageBitmap(WIDTH_PX, HEIGHT_PX)
        CanvasDrawScope().draw(
            density = Density(PIXEL_9_DENSITY),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(picture),
            size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat())
        ) { drawBrume(BRUME_START_SECONDS, colors, weather, night) }
        picture.asAndroidBitmap().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun `Brume du matin correspond à sa capture`() = capture("brume_morning", BcBrumeColors.Morning)

    @Test
    fun `Brume du soir avec ses étoiles correspond à sa capture`() =
        capture("brume_evening_night", BcBrumeColors.Evening, night = true)

    @Test
    fun `Brume sous la pluie correspond à sa capture`() = capture("brume_rain", BcBrumeColors.Day, BcWeather.Rain)

    @Test
    fun `Brume dans le vent correspond à sa capture`() = capture("brume_wind", BcBrumeColors.Day, BcWeather.Wind)

    @Test
    fun `Brume sous la neige correspond à sa capture`() = capture("brume_snow", BcBrumeColors.Morning, BcWeather.Snow)

    private companion object {
        /** 260 × 520 dp at the Pixel 9 density. */
        const val PIXEL_9_DENSITY = 2.625f
        const val WIDTH_PX = 683
        const val HEIGHT_PX = 1365
    }
}
