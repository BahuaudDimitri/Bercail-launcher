package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

/** The sheets opened over the screen: a conversation, the day, the media apps. */
@RunWith(AndroidJUnit4::class)
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
    fun `toucher le voile derrière une feuille la ferme`() {
        showSheet()

        compose.onNodeWithContentDescription("Fermer").performTouchInput { click(Offset(centerX, top + 20f)) }

        assertThat(sheetVisible).isEqualTo(false)
    }

    @Test
    fun `le geste retour d'Android ferme la feuille`() {
        showSheet()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        assertThat(sheetVisible).isEqualTo(false)
    }
}
