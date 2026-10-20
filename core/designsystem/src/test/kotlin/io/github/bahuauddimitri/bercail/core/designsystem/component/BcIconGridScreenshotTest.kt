package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcIconGridScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** A drawing of a pack as the grid gets it: a white ring of line art, a little larger each time. */
    private fun ring(step: Int): ImageBitmap {
        val drawing = ImageBitmap(SIZE, SIZE)
        val stroke = Paint().apply {
            color = Color.White
            style = PaintingStyle.Stroke
            strokeWidth = STROKE
        }
        Canvas(drawing).drawCircle(Offset(SIZE / 2f, SIZE / 2f), SIZE / 6f + step % 4 * 4f, stroke)
        return drawing
    }

    @Test
    fun `une grille d'icônes en sections, l'icône choisie dans l'accent, correspond à sa capture`() =
        compose.captureComponent("icon_grid") {
            BcIconGrid(
                sections = listOf(
                    BcIconSection("Proposées", List(3) { BcGridIcon("p$it", "proposée $it", chosen = it == 1) {} }),
                    BcIconSection("Lines Free", List(14) { BcGridIcon("$it", "icône $it") {} })
                ),
                image = { key -> ring(key.filter(Char::isDigit).toInt()) },
                modifier = Modifier.size(360.dp, 330.dp),
                header = { BcListRow("Icône du pack", onClick = {}, emphasis = BcRowEmphasis.Accent) }
            )
        }

    private companion object {
        const val SIZE = 105
        const val STROKE = 6f
    }
}
