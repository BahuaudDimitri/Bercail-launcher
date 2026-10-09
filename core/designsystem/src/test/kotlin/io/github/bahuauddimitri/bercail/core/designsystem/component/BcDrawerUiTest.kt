package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

@RunWith(AndroidJUnit4::class)
class BcDrawerUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var expanded by mutableStateOf(false)
    private var buttonClicks = 0

    private fun showDrawer() = compose.setContent {
        BcTheme {
            Box(Modifier.fillMaxSize()) {
                BcDrawer(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.align(Alignment.BottomCenter).testTag("drawer")
                ) {
                    BcButton("Salon", onClick = { buttonClicks++ })
                }
            }
        }
    }

    private fun swipeOnDrawer(fromY: Float, distance: Float) = compose.onNodeWithTag("drawer").performTouchInput {
        swipe(start = Offset(centerX, fromY), end = Offset(centerX, fromY + distance), durationMillis = 300)
    }

    @Test
    fun `toucher la poignée déplie puis replie le tiroir`() {
        showDrawer()

        compose.onNodeWithContentDescription("Déplier le tiroir").assertHeightIsAtLeast(40.dp).performClick()
        assertThat(expanded).isEqualTo(true)

        compose.onNodeWithContentDescription("Replier le tiroir").performClick()
        assertThat(expanded).isEqualTo(false)
    }

    @Test
    fun `glisser de plus de 24 dp vers le haut déplie le tiroir`() {
        showDrawer()

        compose.onNodeWithTag("drawer").performTouchInput {
            swipe(
                start = Offset(centerX, bottom - 4f),
                end = Offset(centerX, bottom - 4f - 40.dp.toPx()),
                durationMillis = 300
            )
        }

        assertThat(expanded).isEqualTo(true)
    }

    @Test
    fun `glisser de moins de 24 dp ne change rien`() {
        showDrawer()

        compose.onNodeWithTag("drawer").performTouchInput {
            swipe(
                start = Offset(centerX, bottom - 4f),
                end = Offset(centerX, bottom - 4f - 16.dp.toPx()),
                durationMillis = 300
            )
        }

        assertThat(expanded).isEqualTo(false)
    }

    @Test
    fun `glisser vers le bas replie le tiroir`() {
        expanded = true
        showDrawer()

        compose.onNodeWithTag("drawer").performTouchInput {
            swipe(
                start = Offset(centerX, top + 4f),
                end = Offset(centerX, top + 4f + 40.dp.toPx()),
                durationMillis = 300
            )
        }

        assertThat(expanded).isEqualTo(false)
    }

    @Test
    fun `le glissement compte même si le doigt sort du tiroir`() {
        showDrawer()

        compose.onNodeWithTag("drawer").performTouchInput {
            swipe(
                start = Offset(centerX, bottom - 4f),
                end = Offset(centerX, top - 200.dp.toPx()),
                durationMillis = 300
            )
        }

        assertThat(expanded).isEqualTo(true)
    }

    @Test
    fun `un glissement qui commence sur un bouton n'active pas le bouton`() {
        showDrawer()

        compose.onNodeWithText("Salon").performTouchInput {
            swipe(start = center, end = Offset(centerX, centerY - 60.dp.toPx()), durationMillis = 300)
        }

        assertThat(buttonClicks).isEqualTo(0)
        assertThat(expanded).isEqualTo(true)
    }

    @Test
    fun `un bouton du tiroir réagit toujours au toucher`() {
        showDrawer()

        compose.onNodeWithText("Salon").performClick()

        assertThat(buttonClicks).isEqualTo(1)
    }

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
