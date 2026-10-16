package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The sheets opened over the screen: a conversation, the day, the media apps. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcSheetUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var sheetVisible by mutableStateOf(true)

    private fun showSheet() = compose.setContent {
        BcTheme {
            BcSheet(visible = sheetVisible, onDismiss = {
                sheetVisible = false
            }, modifier = Modifier.testTag("sheet")) {
                BcText("Léa")
            }
        }
    }

    @Test
    fun `glisser une feuille vers le bas la ferme`() {
        showSheet()

        compose.onNodeWithTag("sheet").performTouchInput {
            swipe(
                start = Offset(centerX, top + 4f),
                end = Offset(centerX, top + 4f + 60.dp.toPx()),
                durationMillis = 300
            )
        }

        assertThat(sheetVisible).isEqualTo(false)
    }

    @Test
    fun `toucher au-dessus d'une feuille la ferme`() {
        showSheet()

        compose.onRoot().performTouchInput { click(Offset(centerX, top + 20f)) }

        assertThat(sheetVisible).isEqualTo(false)
    }

    @Test
    fun `un lecteur d'écran ferme toujours la feuille par « Fermer »`() {
        showSheet()

        compose.onNodeWithContentDescription("Fermer").performSemanticsAction(SemanticsActions.OnClick)

        assertThat(sheetVisible).isEqualTo(false)
    }

    @Test
    fun `une feuille ouverte n'assombrit plus l'écran au-dessus d'elle`() {
        compose.setContent {
            BcTheme {
                Box(Modifier.fillMaxSize().background(Color.Magenta)) {
                    BcSheet(visible = true, onDismiss = {}) { BcText("Léa") }
                }
            }
        }

        val pixels = compose.onRoot().captureToImage().toPixelMap()

        assertThat(pixels[pixels.width / 2, 2]).isEqualTo(Color.Magenta)
    }

    @Test
    fun `le geste retour d'Android ferme la feuille`() {
        showSheet()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertThat(sheetVisible).isEqualTo(false)
    }
}
