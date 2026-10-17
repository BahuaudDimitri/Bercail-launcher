package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The pieces of the Listen screen: the arch of the cover and the progress around its curve. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcListenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `l'arche sans image correspond à sa capture`() = compose.captureComponent("arch_cover") {
        BcArchCover(image = null, colors = DUSK to CORAL, contentDescription = null)
    }

    @Test
    fun `l'arche d'une vignette vidéo recadrée au centre correspond à sa capture`() =
        compose.captureComponent("arch_cover_video") {
            BcArchCover(image = thumbnail, colors = APRICOT to SALMON, contentDescription = null)
        }

    /** A 16:9 thumbnail, as in the reference: apricot above, salmon below, a light disc for its subject. */
    private val thumbnail by lazy {
        val image = ImageBitmap(THUMB_WIDTH, THUMB_HEIGHT)
        val canvas = Canvas(image)
        val middle = THUMB_HEIGHT / 2f
        canvas.drawRect(Rect(0f, 0f, THUMB_WIDTH.toFloat(), middle), Paint().apply { color = APRICOT })
        canvas.drawRect(
            Rect(0f, middle, THUMB_WIDTH.toFloat(), THUMB_HEIGHT.toFloat()),
            Paint().apply { color = SALMON }
        )
        canvas.drawCircle(
            Offset(THUMB_WIDTH / 2f, middle),
            THUMB_HEIGHT * SUBJECT_PART,
            Paint().apply { color = Color.White.copy(alpha = SUBJECT_ALPHA) }
        )
        image
    }

    private companion object {
        const val THUMB_WIDTH = 320
        const val THUMB_HEIGHT = 180
        const val SUBJECT_PART = 0.18f
        const val SUBJECT_ALPHA = 0.35f
        val DUSK = Color(0xFF6A7FB3)
        val CORAL = Color(0xFFF2A49A)
        val APRICOT = Color(0xFFFFC98A)
        val SALMON = Color(0xFFFF9E8A)
    }
}
