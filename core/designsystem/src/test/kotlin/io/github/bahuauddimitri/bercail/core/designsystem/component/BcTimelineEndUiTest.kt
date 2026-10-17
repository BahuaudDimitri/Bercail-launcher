package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The end of the line of the day when no alarm rings within 24 hours: a grey ring, « Aucun réveil ». */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BcTimelineEndUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()

    private val noAlarm = BcTimelineEnd(time = "", caption = "Aucun réveil", description = "Aucun réveil", set = false)
    private val alarm = BcTimelineEnd(time = "07:15", caption = "réveil · 9°", description = "Réveil demain à 07:15")

    private fun show(end: BcTimelineEnd) = compose.setContent {
        BcTheme {
            Box(Modifier.background(BcColors.base)) {
                BcTimeline(
                    now = 0.1f,
                    marks = listOf(BcTimelineMark(0.47f, "15:00", title = "Point design", key = "design")),
                    end = end,
                    modifier = Modifier.height(LENGTH).width(WIDTH).testTag(LINE),
                    onOpenDay = { opened += "day: ${it?.key}" },
                    onOpenMark = { opened += "mark: ${it.key}" },
                    onOpenEnd = { opened += "alarms" }
                )
            }
        }
    }

    @Test
    fun `sans réveil, le bout du fil écrit Aucun réveil à côté de l'anneau`() {
        show(noAlarm)

        val line = compose.onNodeWithTag(LINE).getUnclippedBoundsInRoot()
        val text = compose.onNodeWithText("Aucun réveil").getUnclippedBoundsInRoot()
        assertThat(((text.top + text.bottom) / 2).value).isCloseTo((line.bottom - RING_LIFT).value, 1f)
        assertThat(text.right).isLessThan(line.right - RING_INSET)
    }

    @Test
    fun `sans réveil, l'anneau est gris, pas dans l'accent du moment`() {
        show(noAlarm)

        assertThat(closest(BcColors.textMuted)).isLessThan(TOLERANCE)
        assertThat(closest(BcMoment.Day.accent)).isGreaterThan(FAR)
    }

    @Test
    fun `avec un réveil, l'anneau reste dans l'accent du moment`() {
        show(alarm)

        assertThat(closest(BcMoment.Day.accent)).isLessThan(TOLERANCE)
    }

    @Test
    fun `sans réveil, toucher l'anneau ouvre quand même les alarmes`() {
        show(noAlarm)

        compose.onNodeWithTag(LINE).performTouchInput { click(Offset(centerX, height - RING_LIFT.toPx())) }

        assertThat(opened).containsExactly("alarms")
    }

    @Test
    fun `sans réveil, glisser jusqu'au bout nomme Aucun réveil dans la bulle, et lâcher dessus ouvre les alarmes`() {
        show(noAlarm)

        compose.onNodeWithTag(LINE).performTouchInput {
            down(Offset(centerX, height * 0.5f))
            moveTo(Offset(centerX, height - RING_LIFT.toPx()))
        }
        compose.onNodeWithTag(TIMELINE_BUBBLE)
            .assert(hasAnyDescendant(hasText("Aucun réveil")) or hasText("Aucun réveil"))
        compose.onNodeWithTag(LINE).performTouchInput { up() }

        assertThat(opened).containsExactly("alarms")
    }

    @Test
    fun `sans réveil, les lecteurs d'écran entendent un bouton Aucun réveil qui ouvre les alarmes`() {
        show(noAlarm)

        val ring = compose.onNodeWithContentDescription("Aucun réveil")
        ring.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val action = ring.fetchSemanticsNode().config[SemanticsActions.OnClick]
        assertThat(action.label).isEqualTo("Ouvrir les alarmes")
        ring.performSemanticsAction(SemanticsActions.OnClick)

        assertThat(opened).containsExactly("alarms")
    }

    @Test
    fun `avec un réveil aussi, l'anneau est annoncé comme un bouton`() {
        show(alarm)

        compose.onNodeWithContentDescription("Réveil demain à 07:15")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    /** How close [color] comes to the right of the ring, within a 2 dp square: its stroke is read across its width. */
    private fun closest(color: Color): Float {
        compose.redraw()
        val pixels = compose.onNodeWithTag(LINE).captureToImage().toPixelMap()
        val (cx, cy) = with(compose.density) {
            (WIDTH - RING_INSET + RING_STROKE_RADIUS).roundToPx() to (LENGTH - RING_LIFT).roundToPx()
        }
        val reach = with(compose.density) { 1.dp.roundToPx() }
        var best = 1f
        for (dx in -reach..reach) {
            for (dy in -reach..reach) best = minOf(best, pixels[cx + dx, cy + dy].distanceTo(color))
        }
        return best
    }

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val LINE = "line"
        val LENGTH = 400.dp
        val WIDTH = 78.dp

        /** The ring stands 24 dp above the bottom, 12 dp from the right edge; its stroke runs 5.25 dp around it. */
        val RING_LIFT = 24.dp
        val RING_INSET = 12.dp
        val RING_STROKE_RADIUS: Dp = 5.25.dp
        const val TOLERANCE = 0.08f
        const val FAR = 0.15f
    }
}
