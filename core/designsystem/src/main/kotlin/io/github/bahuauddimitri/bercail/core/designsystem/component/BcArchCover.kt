package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors

/**
 * The cover of the Listen screen: an arch of 300 × 484 dp hanging from the top edge of the screen, flat on top with
 * square corners, its bottom a half circle of 150 dp, with a halo of its first color around the curve. Without
 * [image], it is the blend of its two [colors]; an image of another shape (a 16:9 video thumbnail) fills it, cropped
 * to its centre. Never any text inside.
 *
 * The centre of its curve lies [BcArchCoverCurveCentre] below its top edge, halfway across: the progress along its
 * curve is drawn around that point.
 *
 * On a smaller screen, it can be made narrower ([width]) and shorter ([height]): its bottom stays a half circle as
 * wide as the arch, whose centre then lies [bcArchCurveCentre] below the top edge, and its halo shrinks with it.
 */
@Composable
fun BcArchCover(
    image: ImageBitmap?,
    colors: Pair<Color, Color>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    width: Dp = BcArchCoverWidth,
    height: Dp = BcArchCoverHeight
) {
    val shape = RoundedCornerShape(0.dp, 0.dp, width / 2, width / 2)
    val announced = if (contentDescription == null) {
        Modifier
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
    Box(
        modifier
            .size(width, height)
            .then(announced)
            .drawBehind {
                val center = Offset(size.width / 2, bcArchCurveCentre(width, height).toPx())
                val radius = HALO_REACH.toPx() * (width / BcArchCoverWidth)
                val halo = listOf(colors.first.copy(alpha = HALO_ALPHA), colors.first.copy(alpha = 0f))
                drawCircle(Brush.radialGradient(halo, center, radius), radius, center)
            }
            .clip(shape)
            .background(Brush.linearGradient(colors.toList()))
            .border(EDGE, BcColors.segmentEdge, shape)
    ) {
        if (image != null) {
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                // The cover is kept at the size the arch shows it on the Pixel 9; a smaller one is enlarged smoothly.
                filterQuality = FilterQuality.High,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** How wide [BcArchCover] is on the Pixel 9: its bottom is a half circle of half this width. */
val BcArchCoverWidth = 300.dp

/** How far [BcArchCover] hangs from the top edge of the screen on the Pixel 9. */
val BcArchCoverHeight = 484.dp

/** How far below the top edge of an arch of [width] and [height] the centre of its curve lies. */
fun bcArchCurveCentre(width: Dp, height: Dp): Dp = height - width / 2

/** How far below the top edge of [BcArchCover] the centre of its curve lies: its height less its bottom radius. */
val BcArchCoverCurveCentre = bcArchCurveCentre(BcArchCoverWidth, BcArchCoverHeight)

// A fine light edge, white 12 %, as on the reference.
private val EDGE = 1.dp
private const val HALO_ALPHA = 0.45f

// The halo reaches a little beyond the progress drawn around the curve, 164 dp out.
private val HALO_REACH = 230.dp
