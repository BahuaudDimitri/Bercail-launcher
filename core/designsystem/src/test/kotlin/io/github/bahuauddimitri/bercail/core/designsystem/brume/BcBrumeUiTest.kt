package io.github.bahuauddimitri.bercail.core.designsystem.brume

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class BcBrumeUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val state = BcBrumeState()
    private var playing by mutableStateOf(true)
    private var track by mutableIntStateOf(1)

    private fun showBrume() = compose.setContent {
        BcTheme {
            BcBrume(
                colors = BcBrumeColors.Evening,
                playing = playing,
                state = state,
                wakeKey = track,
                modifier = Modifier.fillMaxSize().wakesBrume(state).testTag("brume")
            )
        }
    }

    @Test
    fun `Brume s'anime pendant la lecture`() {
        showBrume()

        assertThat(state.isAnimating).isEqualTo(true)
    }

    @Test
    fun `Brume est immobile en pause`() {
        playing = false
        showBrume()

        assertThat(state.isAnimating).isEqualTo(false)
    }

    @Test
    fun `Brume se fige après 10 secondes sans toucher l'écran`() {
        showBrume()

        compose.mainClock.advanceTimeBy(9_000)
        assertThat(state.isAnimating).isEqualTo(true)

        compose.mainClock.advanceTimeBy(1_500)
        assertThat(state.isAnimating).isEqualTo(false)
    }

    @Test
    fun `toucher l'écran relance Brume`() {
        showBrume()
        compose.mainClock.advanceTimeBy(11_000)

        compose.onNodeWithTag("brume").performTouchInput { click() }
        compose.waitForIdle()

        assertThat(state.isAnimating).isEqualTo(true)
    }

    @Test
    fun `un nouveau morceau relance Brume`() {
        showBrume()
        compose.mainClock.advanceTimeBy(11_000)

        track = 2
        compose.waitForIdle()

        assertThat(state.isAnimating).isEqualTo(true)
    }

    @Test
    fun `Brume est immobile en économie d'énergie`() {
        shadowOf(context.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
        showBrume()

        assertThat(state.isAnimating).isEqualTo(false)
    }

    @Test
    fun `Brume est immobile quand Android supprime les animations`() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        showBrume()

        assertThat(state.isAnimating).isEqualTo(false)
    }
}
