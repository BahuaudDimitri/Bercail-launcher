package io.github.bahuauddimitri.bercail.core.designsystem.brume

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** While only Brume moves, the screen slows down to 20 Hz; a touch gives it back its normal rate for a moment. */
@RunWith(AndroidJUnit4::class)
class BcBrumeRefreshRateUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val state = BcBrumeState()

    private fun showBrume() = compose.setContent {
        BcTheme {
            BcBrume(
                colors = BcBrumeColors.Day,
                playing = true,
                state = state,
                modifier = Modifier.fillMaxSize().wakesBrume(state).testTag("brume")
            )
        }
    }

    private val preferredRate get() = compose.activity.window.attributes.preferredRefreshRate

    @Test
    fun `quand seule Brume s'anime, l'écran passe à 20 Hz`() {
        showBrume()

        assertThat(preferredRate).isEqualTo(20f)
    }

    @Test
    fun `pendant un toucher et la seconde qui suit, l'écran garde sa fréquence normale`() {
        showBrume()

        compose.onNodeWithTag("brume").performTouchInput { click() }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        assertThat(preferredRate).isEqualTo(0f)

        compose.mainClock.advanceTimeBy(1_000)
        assertThat(preferredRate).isEqualTo(20f)
    }

    @Test
    fun `quand Brume se fige, l'écran retrouve sa fréquence normale`() {
        showBrume()

        compose.mainClock.advanceTimeBy(11_000)

        assertThat(preferredRate).isEqualTo(0f)
    }
}
