package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import io.github.bahuauddimitri.bercail.core.designsystem.PIXEL_9
import io.github.bahuauddimitri.bercail.core.designsystem.redraw
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMoment
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The progress of the Listen screen: the half circle under the arch, dragged by the finger's angle. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PIXEL_9)
class BcArcProgressUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val seeks = mutableListOf<Float>()

    @Test
    fun `un arc plus petit, pour une arche plus petite, garde sa bande de 40 dp et les temps sous son bas`() {
        show(progress = 0.3f, radius = SMALL_RADIUS)

        // As wide as the half circle and its 40 dp band; the times 16 dp under the bottom of the half circle.
        compose.onNodeWithTag(ARC).assertWidthIsEqualTo(SMALL_RADIUS * 2 + 40.dp)
        val times = compose.onNodeWithText("5:24").getUnclippedBoundsInRoot()
        assertThat(times.top.value).isCloseTo((BcArcProgressCurveCentre + SMALL_RADIUS + 16.dp).value, 1f)
        // Touching the bottom of the smaller half circle still moves to the middle.
        compose.onNodeWithTag(ARC).performTouchInput {
            click(centre() + arcPoint(0.5f, SMALL_RADIUS.toPx()))
        }
        assertThat(seeks.single()).isCloseTo(0.5f, 0.02f)
    }

    @Test
    fun `sous l'arc, le temps écoulé puis la durée`() {
        show(progress = 0.3f)

        compose.onNodeWithText("5:24").assertExists()
        compose.onNodeWithText("18:00").assertExists()
        compose.onNodeWithText("En direct").assertDoesNotExist()
    }

    @Test
    fun `toucher le bas de l'arc amène la lecture au milieu`() {
        show(progress = 0.3f)

        compose.onNodeWithTag(ARC).performTouchInput { click(point(0.5f)) }

        assertThat(seeks.lastOrNull()).isNotNull().isCloseTo(0.5f, 0.01f)
    }

    @Test
    fun `glisser le long de l'arc suit l'angle du doigt et s'arrête au bout droit`() {
        show(progress = 0.1f)

        compose.onNodeWithTag(ARC).performTouchInput {
            swipe(start = point(0.1f), end = point(0.6f))
        }
        assertThat(seeks.lastOrNull()).isNotNull().isCloseTo(0.6f, 0.01f)

        compose.onNodeWithTag(ARC).performTouchInput {
            swipe(start = point(0.6f), end = centre() + Offset(px(170.dp), -px(15.dp)))
        }
        assertThat(seeks.lastOrNull()).isNotNull().isCloseTo(1f, 0.001f)
    }

    @Test
    fun `un glissement parti loin de l'arc ne déplace rien`() {
        show(progress = 0.3f)

        compose.onNodeWithTag(ARC).performTouchInput {
            // From the middle of the circle, under the arch, down across the arc.
            swipe(start = centre() + Offset(0f, px(60.dp)), end = point(0.5f))
            click(centre())
        }

        assertThat(seeks).isEmpty()
    }

    @Test
    fun `sans déplacement possible dans l'app, toucher l'arc ne fait rien`() {
        show(progress = 0.3f, seekable = false)

        compose.onNodeWithTag(ARC).performTouchInput { click(point(0.5f)) }

        assertThat(seeks).isEmpty()
        compose.onNodeWithTag(ARC).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun `un lecteur d'écran lit la position et peut la régler`() {
        show(progress = 0.3f)

        compose.onNodeWithTag(ARC)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        0.3f,
                        0f..1f
                    )
                )
            )
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }

        assertThat(seeks.lastOrNull()).isNotNull().isCloseTo(0.75f, 0.001f)
    }

    @Test
    fun `la partie jouée est dans l'accent du moment, l'anneau blanc marque la position, le reste est gris`() {
        show(progress = 0.4f)

        val pixels = pixels()
        assertThat(pixels.at(point(0.2f)).distanceTo(BcMoment.Evening.accent)).isLessThan(TOLERANCE)
        assertThat(pixels.at(point(0.4f)).distanceTo(BcColors.text)).isLessThan(TOLERANCE)
        assertThat(pixels.at(point(0.7f)).distanceTo(BcColors.stroke.compositeOver(BcColors.base)))
            .isLessThan(LOOSE_TOLERANCE)
    }

    @Test
    fun `un direct garde le demi-cercle en gris, sans anneau, et dit En direct à la place des temps`() {
        show(progress = 0.4f, live = true)

        compose.onNodeWithText("En direct").assertExists()
        compose.onNodeWithText("5:24").assertDoesNotExist()
        compose.onNodeWithText("18:00").assertDoesNotExist()
        val pixels = pixels()
        val grey = BcColors.stroke.compositeOver(BcColors.base)
        assertThat(pixels.at(point(0.2f)).distanceTo(grey)).isLessThan(LOOSE_TOLERANCE)
        assertThat(pixels.at(point(0.4f)).distanceTo(grey)).isLessThan(LOOSE_TOLERANCE)
        assertThat(pixels.at(point(0.4f)).distanceTo(BcColors.text)).isGreaterThan(TOLERANCE)
    }

    @Test
    fun `un direct ne se déplace pas, même si l'app l'accepte`() {
        show(progress = 0.4f, live = true)

        compose.onNodeWithTag(ARC).performTouchInput { click(point(0.5f)) }

        assertThat(seeks).isEmpty()
        compose.onNodeWithTag(ARC).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    private fun show(progress: Float, seekable: Boolean = true, live: Boolean = false, radius: Dp = RADIUS) {
        compose.setContent {
            BcTheme {
                BcMomentScope(BcMoment.Evening) {
                    Box(Modifier.background(BcColors.base)) {
                        BcArcProgress(
                            progress = progress,
                            elapsed = "5:24",
                            duration = "18:00",
                            onSeek = if (seekable) ({ seeks += it }) else null,
                            live = live,
                            radius = radius,
                            modifier = Modifier.testTag(ARC)
                        )
                    }
                }
            }
        }
        compose.redraw()
    }

    private fun px(dp: Dp) = with(compose.density) { dp.toPx() }

    /** The centre of the half circle, in the component: halfway across, [BcArcProgressCurveCentre] down. */
    private fun centre(): Offset {
        val width = compose.onNodeWithTag(ARC).fetchSemanticsNode().size.width
        return Offset(width / 2f, px(BcArcProgressCurveCentre))
    }

    private fun point(fraction: Float) = centre() + arcPoint(fraction, px(RADIUS))

    private fun pixels() = compose.onNodeWithTag(ARC).captureToImage().toPixelMap()

    private fun PixelMap.at(point: Offset) = this[point.x.toInt(), point.y.toInt()]

    private fun Color.distanceTo(other: Color) =
        maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

    private companion object {
        const val ARC = "arc"
        const val TOLERANCE = 0.08f
        const val LOOSE_TOLERANCE = 0.12f
        val RADIUS = 164.dp
        val SMALL_RADIUS = 120.dp
    }
}
