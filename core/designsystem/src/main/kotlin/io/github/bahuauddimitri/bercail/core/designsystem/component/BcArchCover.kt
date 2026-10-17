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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors

/**
 * The cover of the Listen screen: an arch of 300 × 440 dp hanging from the top edge of the screen, flat on top with
 * square corners, its bottom a half circle of 150 dp, with a halo of its first color around the curve. Without
 * [image], it is the blend of its two [colors]; an image of another shape (a 16:9 video thumbnail) fills it, cropped
 * to its centre. Never any text inside.
 *
 * The centre of its curve lies [BcArchCoverCurveCentre] below its top edge, halfway across: the progress along its
 * curve is drawn around that point.
 */
@Composable
fun BcArchCover(
    image: ImageBitmap?,
    colors: Pair<Color, Color>,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
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
            .size(ARCH_WIDTH, ARCH_HEIGHT)
            .then(announced)
            .drawBehind {
                val center = Offset(size.width / 2, BcArchCoverCurveCentre.toPx())
                val radius = HALO_REACH.toPx()
                val halo = listOf(colors.first.copy(alpha = HALO_ALPHA), colors.first.copy(alpha = 0f))
                drawCircle(Brush.radialGradient(halo, center, radius), radius, center)
            }
            .clip(ARCH_SHAPE)
            .background(Brush.linearGradient(colors.toList()))
            .border(EDGE, BcColors.segmentEdge, ARCH_SHAPE)
    ) {
        if (image != null) {
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** How far below the top edge of [BcArchCover] the centre of its curve lies: its height less its bottom radius. */
val BcArchCoverCurveCentre = 290.dp

private val ARCH_WIDTH = 300.dp
private val ARCH_HEIGHT = 440.dp
private val ARCH_RADIUS = ARCH_HEIGHT - BcArchCoverCurveCentre
private val ARCH_SHAPE = RoundedCornerShape(0.dp, 0.dp, ARCH_RADIUS, ARCH_RADIUS)

// A fine light edge, white 12 %, as on the reference.
private val EDGE = 1.dp
private const val HALO_ALPHA = 0.45f

// The halo reaches a little beyond the progress drawn around the curve, 164 dp out.
private val HALO_REACH = 230.dp
