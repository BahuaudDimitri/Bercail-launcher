package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** The frosted glass of the sheets: Brume blurred behind them, only while a sheet is open. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcFrostUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var open by mutableStateOf(true)
    private var tint by mutableStateOf(Color.Magenta)
    private var frostable by mutableStateOf(true)
    private lateinit var frost: BcFrost

    /** A background that redraws whenever [tint] changes, like Brume, and the sheet over it. */
    private fun show() = compose.setContent {
        BcTheme {
            frost = rememberBcFrost()
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().frostSource(frost)) { drawRect(tint) }
                BcBandSheet(
                    visible = open,
                    onClose = { open = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    frost = if (frostable) frost else null
                ) {
                    Box(Modifier.height(200.dp))
                }
            }
        }
    }

    private fun glass(): SheetGlass =
        compose.onNode(SemanticsMatcher.keyIsDefined(SheetGlassKey)).fetchSemanticsNode().config[SheetGlassKey]

    @Test
    fun `une feuille ouverte sur Brume immobile est en verre givré`() {
        show()

        assertThat(glass()).isEqualTo(SheetGlass.Frosted)
        assertThat(frost.recordings).isGreaterThan(0)
    }

    @Test
    fun `sans Brume immobile derrière elle, la feuille garde un verre teinté à 62 pour cent`() {
        frostable = false
        show()

        assertThat(glass()).isEqualTo(SheetGlass.Tinted)
        assertThat(frost.recordings).isEqualTo(0)
    }

    @Test
    fun `feuille fermée, Brume n'est plus enregistrée pour le verre`() {
        show()
        open = false
        compose.waitForIdle()
        val before = frost.recordings

        tint = Color.Cyan
        compose.waitForIdle()

        assertThat(frost.recordings).isEqualTo(before)
    }

    @Test
    fun `en économie d'énergie, la feuille n'est jamais floutée`() {
        shadowOf(context.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
        show()

        assertThat(glass()).isEqualTo(SheetGlass.Tinted)
        assertThat(frost.recordings).isEqualTo(0)
    }

    @Test
    fun `quand Android supprime les animations, la feuille n'est jamais floutée`() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        show()

        assertThat(glass()).isEqualTo(SheetGlass.Tinted)
    }

    @Test
    fun `une feuille par-dessus l'écran se givre aussi`() {
        compose.setContent {
            BcTheme {
                frost = rememberBcFrost()
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize().frostSource(frost)) { drawRect(tint) }
                    BcSheet(visible = true, onDismiss = {}, frost = frost) { Box(Modifier.height(200.dp)) }
                }
            }
        }

        assertThat(glass()).isEqualTo(SheetGlass.Frosted)
    }
}
