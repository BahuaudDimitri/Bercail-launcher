package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The cover of the Listen screen: an arch hanging from the top edge of the screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcArchCoverUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `l'arche fait 300 sur 484 dp et s'annonce par son titre`() {
        show(image = null)

        compose.onNodeWithContentDescription(COVER).assertWidthIsEqualTo(300.dp).assertHeightIsEqualTo(484.dp)
    }

    @Test
    fun `seule la partie droite grandit, le centre de la courbe à 334 dp du haut, 150 dp au-dessus du bas`() {
        assertThat(BcArchCoverCurveCentre).isEqualTo(334.dp)
        assertThat(BcArchCoverHeight - BcArchCoverCurveCentre).isEqualTo(150.dp)
    }

    @Test
    fun `l'arche a le haut plat aux coins carrés et le bas arrondi en demi-cercle`() {
        show(image = solid(GREEN, width = 30, height = 44))

        val pixels = pixels()
        // The top corners are square: the image reaches them.
        assertThat(pixels.at(2.dp, 2.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
        assertThat(pixels.at(298.dp, 2.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
        // The bottom is a half circle of 150 dp: its middle is covered, its corners are not.
        assertThat(pixels.at(150.dp, 480.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
        assertThat(pixels.at(4.dp, 480.dp).green).isLessThan(HALF)
        assertThat(pixels.at(296.dp, 480.dp).green).isLessThan(HALF)
    }

    @Test
    fun `sans image, l'arche est le dégradé de ses deux couleurs`() {
        show(image = null)

        val pixels = pixels()
        val top = pixels.at(6.dp, 6.dp)
        val bottom = pixels.at(150.dp, 474.dp)
        assertThat(top.red).isGreaterThan(top.blue)
        assertThat(bottom.blue).isGreaterThan(bottom.red)
    }

    @Test
    fun `une vignette vidéo 16 9 est recadrée au centre, sans bandes ni déformation`() {
        // A thumbnail whose outer quarters are red and blue, its middle half green: once cropped to the centre of
        // the arch, only the green middle shows, from one side of the arch to the other.
        show(image = thumbnail())

        val pixels = pixels()
        assertThat(pixels.at(4.dp, 200.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
        assertThat(pixels.at(150.dp, 200.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
        assertThat(pixels.at(296.dp, 200.dp).distanceTo(GREEN)).isLessThan(TOLERANCE)
    }

    private fun show(image: ImageBitmap?) {
        compose.setContent {
            BcTheme {
                Box(Modifier.background(BcColors.base)) {
                    BcArchCover(image = image, colors = Color.Red to Color.Blue, contentDescription = COVER)
                }
            }
        }
        compose.redraw()
    }

    private fun pixels() = compose.onNodeWithContentDescription(COVER).captureToImage().toPixelMap()

    private fun PixelMap.at(x: Dp, y: Dp) = with(compose.density) { this@at[x.roundToPx(), y.roundToPx()] }

    private fun solid(color: Color, width: Int, height: Int): ImageBitmap {
        val image = ImageBitmap(width, height)
        Canvas(image).drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), Paint().apply { this.color = color })
        return image
    }

    /** 16:9, red on its left quarter, blue on its right quarter, green in between. */
    private fun thumbnail(): ImageBitmap {
        val image = solid(GREEN, THUMB_WIDTH, THUMB_HEIGHT)
        val quarter = THUMB_WIDTH / 4f
        val canvas = Canvas(image)
        canvas.drawRect(Rect(0f, 0f, quarter, THUMB_HEIGHT.toFloat()), Paint().apply { color = Color.Red })
        canvas.drawRect(
            Rect(THUMB_WIDTH - quarter, 0f, THUMB_WIDTH.toFloat(), THUMB_HEIGHT.toFloat()),
            Paint().apply { color = Color.Blue }
        )
        return image
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val COVER = "Pochette de Looped"
        const val TOLERANCE = 0.06f
        const val HALF = 0.5f
        const val THUMB_WIDTH = 160
        const val THUMB_HEIGHT = 90
        val GREEN = Color(0xFF20C040)
    }
}
