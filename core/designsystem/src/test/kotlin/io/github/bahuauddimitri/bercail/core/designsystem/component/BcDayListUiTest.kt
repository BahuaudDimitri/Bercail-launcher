package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The list of the day in its sheet: the small line on the left, a dot per appointment. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcDayListUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val accent = BcMoment.Day.accent

    private fun show(entries: List<BcDayEntry>, focus: String? = null, height: Dp = TALL) = compose.setContent {
        BcTheme {
            Box(Modifier.width(WIDTH).height(height).background(BcColors.base)) {
                BcDayList(entries, onOpen = { opened += it }, modifier = Modifier.testTag(LIST), focus = focus)
            }
        }
    }

    private fun entry(title: String, mark: BcDayMark, time: String = "09:30") = BcDayEntry(title, time, title, mark)

    @Test
    fun `une ligne de la journée fait 48 dp, son heure tient dans une colonne de 54 dp`() {
        show(listOf(entry("Stand-up", BcDayMark.Coming), entry("Anniversaire de Léa", BcDayMark.AllDay, "Journée")))

        compose.onNodeWithText("Stand-up").assertHeightIsEqualTo(ROW)
        compose.onNodeWithText("09:30", useUnmergedTree = true).assertWidthIsEqualTo(TIME)
        compose.onNodeWithText("Journée", useUnmergedTree = true).assertWidthIsEqualTo(TIME)
    }

    @Test
    fun `toucher une ligne ouvre son rendez-vous`() {
        show(listOf(entry("Stand-up", BcDayMark.Past), entry("Courses", BcDayMark.Next)))

        compose.onNodeWithText("Courses").performClick()

        assertThat(opened).containsExactly("Courses")
    }

    @Test
    fun `le fil passe à gauche des lignes, un trait blanc à 22 pour cent`() {
        show(listOf(entry("Stand-up", BcDayMark.Coming), entry("Courses", BcDayMark.Coming)))

        // Between the two dots, at the bottom of the first row.
        assertThat(closest(BcColors.stroke.compositeOver(BcColors.base), DOT_X, ROW)).isLessThan(TOLERANCE)
    }

    @Test
    fun `un rendez-vous passé a son point dans l'accent du moment`() {
        show(listOf(entry("Stand-up", BcDayMark.Past)))

        assertThat(pixels().at(DOT_X, ROW / 2).distanceTo(accent)).isLessThan(TOLERANCE)
    }

    @Test
    fun `un rendez-vous à venir a un point blanc à 90 pour cent`() {
        show(listOf(entry("Courses", BcDayMark.Coming)))

        val white = BcColors.text.copy(alpha = 0.9f).compositeOver(BcColors.base)
        assertThat(pixels().at(DOT_X, ROW / 2).distanceTo(white)).isLessThan(TOLERANCE)
    }

    @Test
    fun `le prochain rendez-vous est cerclé d'accent, séparé de son point par un vide sombre`() {
        show(listOf(entry("Courses", BcDayMark.Next)))

        val white = BcColors.text.copy(alpha = 0.9f).compositeOver(BcColors.base)
        assertThat(pixels().at(DOT_X, ROW / 2).distanceTo(white)).isLessThan(TOLERANCE)
        assertThat(closest(accent, DOT_X + NEXT_RING, ROW / 2)).isLessThan(TOLERANCE)
        assertThat(pixels().at(DOT_X, ROW / 2 - GAP).distanceTo(BcColors.nightTop)).isLessThan(TOLERANCE)
    }

    @Test
    fun `une journée entière a un anneau creux, ni point plein ni accent`() {
        show(listOf(entry("Anniversaire de Léa", BcDayMark.AllDay, "Journée")))

        val ring = BcColors.text.copy(alpha = 0.5f).compositeOver(BcColors.base)
        assertThat(closest(ring, DOT_X + HOLLOW, ROW / 2)).isLessThan(TOLERANCE)
        // Its middle is the line, not a white dot.
        assertThat(pixels().at(DOT_X, ROW / 2).distanceTo(BcColors.text)).isGreaterThan(FAR)
    }

    @Test
    fun `ouverte sur un rendez-vous, la liste défile jusqu'à lui`() {
        val day = (0 until MANY).map { entry("Rendez-vous $it", BcDayMark.Coming) }
        show(day, focus = "Rendez-vous 15", height = SHORT)

        compose.onNodeWithText("Rendez-vous 15").assertIsDisplayed()
        compose.onNodeWithText("Rendez-vous 0").assertIsNotDisplayed()
    }

    @Test
    fun `sans rendez-vous choisi, la liste commence en haut`() {
        val day = (0 until MANY).map { entry("Rendez-vous $it", BcDayMark.Coming) }
        show(day, height = SHORT)

        compose.onNodeWithText("Rendez-vous 0").assertIsDisplayed()
        compose.onNodeWithText("Rendez-vous 15").assertIsNotDisplayed()
    }

    private fun pixels(): PixelMap {
        compose.redraw()
        return compose.onNodeWithTag(LIST).captureToImage().toPixelMap()
    }

    private fun PixelMap.at(x: Dp, y: Dp): Color = with(compose.density) { this@at[x.roundToPx(), y.roundToPx()] }

    /** How close [color] comes within a 2 dp square around ([x], [y]): strokes are read across their width. */
    private fun closest(color: Color, x: Dp, y: Dp): Float {
        val pixels = pixels()
        val reach = with(compose.density) { 1.dp.roundToPx() }
        val (cx, cy) = with(compose.density) { x.roundToPx() to y.roundToPx() }
        var best = 1f
        for (dx in -reach..reach) {
            for (dy in -reach..reach) best = minOf(best, pixels[cx + dx, cy + dy].distanceTo(color))
        }
        return best
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val LIST = "day"
        val WIDTH = 360.dp
        val TALL = 400.dp
        val SHORT = 200.dp
        const val MANY = 20
        val ROW = 48.dp
        val TIME = 54.dp

        /** The middle of the line and of the dots, from the left of the list. */
        val DOT_X = 7.5.dp

        /** The middle of the accent ring around the 3.5 dp dot: a 3 dp gap, then 1.5 dp of ring. */
        val NEXT_RING = 7.25.dp
        val GAP = 5.dp

        /** The middle of the 1 dp hollow ring of a whole day, 7 dp wide. */
        val HOLLOW = 3.dp
        const val TOLERANCE = 0.04f
        const val FAR = 0.3f
    }
}
