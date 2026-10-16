package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The field to answer in a conversation. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcReplyFieldUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `le curseur du champ de réponse est dans l'accent du moment, comme celui de la barre Chercher`() {
        compose.setContent {
            BcTheme {
                BcMomentScope(BcMoment.Morning) { BcReplyField(value = "", onValueChange = {}, onSend = {}) }
            }
        }

        compose.onNodeWithContentDescription("Répondre").performClick()

        compose.redraw()
        val pixels = compose.onNodeWithTag(REPLY_FIELD_TAG).captureToImage().toPixelMap()
        var closest = 1f
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) closest = minOf(closest, pixels[x, y].distanceTo(BcMoment.Morning.accent))
        }
        assertThat(closest).isLessThan(TOLERANCE)
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val TOLERANCE = 0.04f
    }
}
