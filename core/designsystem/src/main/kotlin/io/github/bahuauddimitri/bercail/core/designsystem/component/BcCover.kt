package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

/**
 * The large cover of the Listen screen, 200 dp, with a halo of its first color underneath.
 * Without [image], the cover is a blend of its two [colors].
 */
@Composable
fun BcCover(
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
            .size(COVER)
            .then(announced)
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2 + HALO_DROP.toPx())
                val radius = size.width * HALO_REACH
                val halo = listOf(colors.first.copy(alpha = HALO_ALPHA), colors.first.copy(alpha = 0f))
                drawCircle(Brush.radialGradient(halo, center, radius), radius, center)
            }
            .clip(COVER_SHAPE)
            .background(Brush.linearGradient(colors.toList()))
    ) {
        if (image != null) {
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

private const val HALO_ALPHA = 0.45f
private const val HALO_REACH = 0.72f
private val COVER = 200.dp
private val COVER_SHAPE = RoundedCornerShape(30.dp)
private val HALO_DROP = 22.dp
