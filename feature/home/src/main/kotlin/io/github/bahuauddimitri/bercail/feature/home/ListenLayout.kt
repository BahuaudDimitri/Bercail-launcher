package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgressRadius
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCoverCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCoverWidth

/**
 * Where the Listen screen puts each thing, from the top edge of the screen: the arch ([archWidth] × [archHeight]),
 * the centre of its curve, the arc around it, then the titles, the remote, « Ouvrir dans … » and the chevron.
 */
internal data class ListenLayout(
    val archWidth: Dp,
    val archHeight: Dp,
    val arcRadius: Dp,
    val titlesTop: Dp,
    val remoteTop: Dp,
    val openTop: Dp,
    val chevronTop: Dp
) {
    /** How far below the top edge the centre of the arch's curve lies: the arc is drawn around it. */
    val curveCentre: Dp get() = archHeight - archWidth / 2

    val chevronBottom: Dp get() = chevronTop + CHEVRON_SIZE
}

/**
 * The Listen screen laid out on a screen of [width] × [height], over a navigation bar of [bottomInset], with text
 * enlarged by [fontScale]. The Pixel 9 (411 × 923 dp) is the reference, and anything as large keeps it exactly.
 *
 * Narrower, the arch and its arc shrink together, keeping their proportions, so that the arc fits with 16 dp on each
 * side. Shorter, the arch stays as tall as it can: first the gaps between the times, the titles, the remote, the
 * button and the chevron tighten down to their least; only then does the arch give up its straight part, the 150 dp
 * curve kept; only once the straight part is gone does the whole arch shrink. The controls never shrink.
 */
internal fun listenLayout(width: Dp, height: Dp, bottomInset: Dp = 0.dp, fontScale: Float = 1f): ListenLayout {
    val least = listOf(TIMES_LEAST * fontScale, TITLES_LEAST * fontScale, REMOTE_LEAST, OPEN_LEAST)
    val gaps = REFERENCE_GAPS.zip(least) { reference, floor -> max(reference, floor) }
    val bottom = BOTTOM_ROOM + bottomInset
    // The arc and its band, which keeps its 40 dp, fit with 16 dp on each side.
    var scale = ((width / 2 - ARC_SIDE - BAND_HALF) / ARC_RADIUS).coerceAtMost(1f)
    // From the top edge to the top of the times: the straight part, the arc under the curve, then 16 dp.
    val underCurve = { s: Float -> ARC_RADIUS * s + TIMES_UNDER }
    val excess = STRAIGHT * scale + underCurve(scale) + gaps.sum() + CHEVRON_SIZE + bottom - height
    // First the gaps tighten, each by the same share of what it can give; then the straight part goes.
    val slack = gaps.zip(least) { gap, floor -> gap - floor }.sum()
    val squeeze = if (slack > 0.dp) (excess / slack).coerceIn(0f, 1f) else 0f
    val tightened = gaps.zip(least) { gap, floor -> gap - (gap - floor) * squeeze }
    val straight = (STRAIGHT * scale - (excess - slack)).coerceIn(0.dp, STRAIGHT * scale)
    if (excess - slack > STRAIGHT * scale) {
        // Not enough even then: the whole arch shrinks, down to a quarter of its size at most.
        val room = height - TIMES_UNDER - least.sum() - CHEVRON_SIZE - bottom
        scale = (room / ARC_RADIUS).coerceIn(LEAST_SCALE, scale)
    }
    val archWidth = BcArchCoverWidth * scale
    val curveCentre = straight
    val timesTop = curveCentre + underCurve(scale)
    val titlesTop = timesTop + tightened[0]
    val remoteTop = titlesTop + tightened[1]
    val openTop = remoteTop + tightened[2]
    return ListenLayout(
        archWidth = archWidth,
        archHeight = curveCentre + archWidth / 2,
        arcRadius = ARC_RADIUS * scale,
        titlesTop = titlesTop,
        remoteTop = remoteTop,
        openTop = openTop,
        chevronTop = openTop + tightened[3]
    )
}

private fun List<Dp>.sum(): Dp = fold(0.dp) { total, gap -> total + gap }

private val ARC_RADIUS = BcArcProgressRadius
private val STRAIGHT = BcArchCoverCurveCentre
private val ARC_SIDE = 16.dp
private val BAND_HALF = 20.dp
private val TIMES_UNDER = 16.dp
internal val CHEVRON_SIZE = 28.dp

/** Room kept under the chevron, above the navigation bar. */
private val BOTTOM_ROOM = 24.dp

// On the reference, from the top of the times to the title (36), then to the remote (98), the button (98) and the
// chevron (74); and how close each may come: the times and 8 dp, two lines of title and its credits, the 76 dp
// remote and 8 dp, the 40 dp touch target of the button and 4 dp.
private val REFERENCE_GAPS = listOf(36.dp, 98.dp, 98.dp, 74.dp)
private val TIMES_LEAST = 28.dp
private val TITLES_LEAST = 93.dp
private val REMOTE_LEAST = 84.dp
private val OPEN_LEAST = 44.dp
private const val LEAST_SCALE = 0.25f
