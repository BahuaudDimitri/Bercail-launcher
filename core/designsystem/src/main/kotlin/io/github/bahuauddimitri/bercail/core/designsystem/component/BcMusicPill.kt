package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberAnimationSeconds
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import kotlin.math.PI
import kotlin.math.sin

/**
 * The music pill on the home screen: round cover, "Title · Artist" and an equalizer, frozen when paused.
 * Without [cover] image, the disc is a blend of [coverColors], or a note when there are none.
 */
@Composable
fun BcMusicPill(
    title: String,
    artist: String,
    cover: ImageBitmap?,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverColors: Pair<Color, Color>? = null
) {
    Row(
        modifier = modifier
            .widthIn(max = MAX_WIDTH)
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .background(BcColors.tile, BcShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = if (playing) "En lecture" else "En pause" }
            .padding(start = START_PADDING, end = END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        Box(Modifier.size(COVER).clip(BcShapes.pill).background(BcColors.tile), contentAlignment = Alignment.Center) {
            if (cover != null) {
                Image(
                    cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(COVER)
                )
            } else if (coverColors != null) {
                Box(Modifier.size(COVER).background(Brush.linearGradient(coverColors.toList())))
            } else {
                BcIcon(BcIcons.Note, contentDescription = null, size = NOTE, tint = BcTextColor.Muted)
            }
        }
        BcText(
            "$title · $artist",
            modifier = Modifier.weight(1f, fill = false),
            style = BcTextStyle.Label,
            maxLines = 1
        )
        Equalizer(playing)
    }
}

/** Three bars that dance at 20 frames per second while playing and rest low when paused. */
@Composable
private fun Equalizer(playing: Boolean) {
    val seconds by rememberAnimationSeconds(running = playing)
    Canvas(Modifier.size(EQ_WIDTH, EQ_HEIGHT)) {
        val bar = BAR_WIDTH.toPx()
        val gap = (size.width - BARS * bar) / (BARS - 1)
        repeat(BARS) { index ->
            val level = if (playing) {
                MIN_LEVEL + (1 - MIN_LEVEL) * (0.5f + 0.5f * sin(2 * PI.toFloat() * (seconds + index * PHASE)))
            } else {
                PAUSED_LEVEL
            }
            val height = size.height * level
            drawRoundRect(
                color = BcColors.text,
                topLeft = Offset(index * (bar + gap), size.height - height),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2)
            )
        }
    }
}

private const val BARS = 3
private const val MIN_LEVEL = 0.25f
private const val PAUSED_LEVEL = 0.25f
private const val PHASE = 0.3f
private val MAX_WIDTH = 260.dp
private val START_PADDING = 6.dp
private val END_PADDING = 14.dp
private val GAP = 8.dp
private val COVER = 28.dp
private val NOTE = 14.dp
private val EQ_WIDTH = 10.dp
private val EQ_HEIGHT = 12.dp
private val BAR_WIDTH = 2.dp
