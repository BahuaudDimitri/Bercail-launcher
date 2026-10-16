package io.github.bahuauddimitri.bercail.core.designsystem.component

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
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
    val compose = createAndroidComposeRule<ComponentActivity>()

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

    /** The band over the background, risen for the search while [open]. */
    private fun showSearch() = compose.setContent {
        BcTheme {
            frost = rememberBcFrost()
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().frostSource(frost)) { drawRect(tint) }
                BcBand(
                    onOpen = {},
                    modifier = Modifier.align(Alignment.BottomCenter).testTag(BAND),
                    searching = open,
                    frost = if (frostable) frost else null
                ) {
                    BcText("Liste", modifier = Modifier.weight(1f, fill = open))
                }
            }
        }
    }

    @Test
    fun `la recherche plein écran se givre sur Brume immobile, comme les feuilles`() {
        showSearch()

        assertThat(glass()).isEqualTo(SheetGlass.Frosted)
        assertThat(frost.recordings).isGreaterThan(0)
    }

    @Test
    fun `sans Brume immobile derrière elle, la recherche garde un verre teinté`() {
        frostable = false
        showSearch()

        assertThat(glass()).isEqualTo(SheetGlass.Tinted)
        assertThat(frost.recordings).isEqualTo(0)
    }

    @Test
    fun `en économie d'énergie, la recherche n'est jamais floutée`() {
        shadowOf(context.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
        showSearch()

        assertThat(glass()).isEqualTo(SheetGlass.Tinted)
    }

    @Test
    fun `recherche fermée, Brume n'est plus enregistrée pour son verre`() {
        showSearch()
        open = false
        compose.waitForIdle()
        val before = frost.recordings

        tint = Color.Cyan
        compose.waitForIdle()

        assertThat(frost.recordings).isEqualTo(before)
    }

    @Test
    fun `le verre de la recherche n'a ni coins ni liseré sur les côtés`() {
        frostable = false
        showSearch()

        compose.redraw()
        val pixels = compose.onNodeWithTag(BAND).captureToImage().toPixelMap()
        val middle = pixels.height / 2
        // The top corner is glass, not the background: no rounded corner.
        assertThat(pixels[1, 1].distanceTo(Color.Magenta)).isGreaterThan(FAR)
        // The side is the same glass as the middle: no light edge.
        assertThat(pixels[0, middle].distanceTo(pixels[pixels.width / 2, middle])).isLessThan(CLOSE)
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val BAND = "band"
        const val FAR = 0.3f
        const val CLOSE = 0.02f
    }
}
