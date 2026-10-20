package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcListRowUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val moment = mutableStateOf(BcMoment.Day)
    private val content = mutableStateOf<@Composable () -> Unit>({})

    @Test
    fun `une ligne de liste fait 50 dp de haut et son anneau 36 dp`() {
        show { BcListRow("Spotify", onClick = {}, modifier = Modifier.testTag(ROW), leading = BcLeading.Initials("S")) }

        compose.onNodeWithTag(ROW).assertHeightIsEqualTo(50.dp)
        compose.onNodeWithTag(RING_TAG + "Spotify", useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun `l'anneau d'une ligne est un trait blanc à 35 %, sans fond`() {
        show { BcListRow("Spotify", onClick = {}, modifier = Modifier.testTag(ROW), leading = BcLeading.Initials("S")) }

        val pixels = rowPixels()
        assertThat(pixels.ringDistanceTo(BcColors.strokeRing.compositeOver(BcColors.base)))
            .isLessThan(TOLERANCE)
        assertThat(pixels.at(INSIDE_RING, MIDDLE).distanceTo(BcColors.base)).isLessThan(TOLERANCE)
    }

    @Test
    fun `l'anneau d'une personne est dans son pastel`() {
        show {
            BcListRow(
                "Léa",
                onClick = {},
                modifier = Modifier.testTag(ROW),
                leading = BcLeading.Initials("L", BcPastel.Coral)
            )
        }

        assertThat(rowPixels().ringDistanceTo(BcColors.pastelCoral)).isLessThan(TOLERANCE)
    }

    @Test
    fun `une ligne mise en avant n'a pas de fond, seulement son anneau dans l'accent du moment`() {
        show {
            BcListRow(
                "Maps",
                onClick = {},
                modifier = Modifier.testTag(ROW),
                leading = BcLeading.Initials("M"),
                emphasis = BcRowEmphasis.Accent
            )
        }

        BcMoment.entries.forEach { each ->
            moment.value = each
            compose.waitForIdle()

            val pixels = rowPixels()
            assertThat(pixels.ringDistanceTo(each.accent)).isLessThan(TOLERANCE)
            assertThat(pixels[pixels.width - 2, pixels.height / 2].distanceTo(BcColors.base)).isLessThan(TOLERANCE)
        }
    }

    @Test
    fun `l'icône d'une app est dans ses vraies couleurs, en 36 dp, sans anneau, même mise en avant`() {
        val red = ImageBitmap(PICTURE_PX, PICTURE_PX).apply {
            Canvas(this).drawRect(
                Rect(0f, 0f, PICTURE_PX.toFloat(), PICTURE_PX.toFloat()),
                Paint().apply {
                    color = Color.Red
                }
            )
        }
        show {
            BcListRow(
                "Spotify",
                onClick = {},
                modifier = Modifier.testTag(ROW),
                leading = BcLeading.Picture(red),
                emphasis = BcRowEmphasis.Accent
            )
        }

        compose.onNodeWithTag(PICTURE_TAG + "Spotify", useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
        compose.onNodeWithTag(RING_TAG + "Spotify", useUnmergedTree = true).assertDoesNotExist()
        val pixels = rowPixels()
        assertThat(pixels.at(PICTURE_MIDDLE, MIDDLE).distanceTo(Color.Red)).isLessThan(TOLERANCE)
        assertThat(pixels.ringDistanceTo(BcMoment.Day.accent)).isGreaterThan(FAR)
        assertThat(pixels.ringDistanceTo(BcColors.strokeRing.compositeOver(BcColors.base))).isGreaterThan(TOLERANCE)
    }

    @Test
    fun `les lettres trouvées du titre sont dans l'accent du moment, les autres non`() {
        show {
            Column {
                BcListRow("Maps", onClick = {}, leading = BcLeading.Initials("M"), found = 0..1)
                BcListRow("Mail", onClick = {}, leading = BcLeading.Initials("M"))
            }
        }

        assertThat(closestTo(BcMoment.Day.accent, "Maps")).isLessThan(TOLERANCE)
        assertThat(closestTo(BcMoment.Day.accent, "Mail")).isGreaterThan(FAR)
    }

    @Test
    fun `l'heure d'une ligne tient dans sa colonne de 48 dp, « Journée » compris`() {
        show { BcListRow("Ciné", onClick = {}, leading = BcLeading.Time("Journée")) }

        val time = compose.onNodeWithText("Journée", useUnmergedTree = true).assertWidthIsEqualTo(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        time.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertThat(layouts.single().isLineEllipsized(0)).isFalse()
    }

    @Test
    fun `un sous-titre permis sur deux lignes y passe au lieu d'être coupé, et la ligne grandit`() {
        val long = "Place 1 · toucher pour changer l'icône"
        show {
            Column(Modifier.width(NARROW)) {
                BcListRow(
                    "Chrome",
                    onClick = {},
                    modifier = Modifier.testTag(ROW),
                    subtitle = long,
                    subtitleLines = 2,
                    leading = BcLeading.Initials("C")
                )
            }
        }

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(long, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertThat(layout.lineCount).isEqualTo(2)
        assertThat(layout.isLineEllipsized(1)).isFalse()
        assertThat(compose.onNodeWithTag(ROW).getUnclippedBoundsInRoot().height).isGreaterThan(50.dp)
    }

    @Test
    fun `un long titre de section passe sur une deuxième ligne au lieu d'être coupé`() {
        show {
            Column(Modifier.width(NARROW)) {
                BcSectionLabel("Dock", Modifier.testTag("short"))
                BcSectionLabel("Cinq au plus : retire une app pour en ajouter une autre", Modifier.testTag("long"))
            }
        }

        val short = compose.onNodeWithTag("short").getUnclippedBoundsInRoot().height
        assertThat(compose.onNodeWithTag("long").getUnclippedBoundsInRoot().height).isGreaterThan(short)
    }

    /** Shows [shown] 360 dp wide on the night background, in the accent of [moment]. */
    private fun show(shown: @Composable () -> Unit) {
        content.value = shown
        compose.setContent {
            BcTheme {
                BcMomentScope(moment.value) {
                    Box(Modifier.width(ROW_WIDTH).background(BcColors.base)) { content.value() }
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * The row as drawn. A window whose content did not change since the last test may hand back a blank picture
     * (its own background, not the row): it is asked to draw again first.
     */
    private fun rowPixels(): PixelMap {
        compose.redraw()
        return compose.onNodeWithTag(ROW).captureToImage().toPixelMap()
    }
    private fun PixelMap.at(x: Dp, y: Dp): Color = with(compose.density) { this@at[x.roundToPx(), y.roundToPx()] }

    /**
     * How close the stroke of the ring comes to [color], read across its whole 1.5 dp width at the left of the ring,
     * halfway down the row: a single pixel may fall on its soft edge.
     */
    private fun PixelMap.ringDistanceTo(color: Color): Float = with(compose.density) {
        val middle = MIDDLE.roundToPx()
        var closest = 1f
        for (x in 0..RING_STROKE_WIDTH.roundToPx()) {
            for (y in middle - 1..middle + 1) closest = minOf(closest, this@ringDistanceTo[x, y].distanceTo(color))
        }
        closest
    }

    /** How close the drawn [text] comes to [color], over all its pixels. */
    private fun closestTo(color: Color, text: String): Float {
        compose.redraw()
        val pixels = compose.onNodeWithText(text, useUnmergedTree = true).captureToImage().toPixelMap()
        var closest = 1f
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) closest = minOf(closest, pixels[x, y].distanceTo(color))
        }
        return closest
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val ROW = "row"
        val ROW_WIDTH = 360.dp
        val NARROW = 200.dp

        /** The 1.5 dp stroke of the ring, at its left, read halfway down the 50 dp row. */
        val RING_STROKE_WIDTH = 1.5.dp
        val MIDDLE = 25.dp

        /** The middle of the picture of an app, 36 dp wide. */
        val PICTURE_MIDDLE = 18.dp
        const val PICTURE_PX = 96

        /** Inside the ring, beside its initial. */
        val INSIDE_RING = 5.dp
        const val TOLERANCE = 0.04f

        /** Far from the accent: white text never comes close to it. */
        const val FAR = 0.08f
    }
}
