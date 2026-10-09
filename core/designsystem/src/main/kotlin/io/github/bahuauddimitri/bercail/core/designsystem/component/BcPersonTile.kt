package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.LocalBcContentColor

internal const val UNREAD_BADGE = "unread-badge-"
private const val MAX_BADGE = 99

/**
 * A favorite person: pastel tile with initials (or photo), unread count, name below.
 * Without [pastel], it is the glass "Tous" tile.
 */
@Composable
fun BcPersonTile(
    name: String,
    initials: String,
    pastel: BcPastel?,
    unread: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = name,
    photo: ImageBitmap? = null
) {
    Column(
        modifier = modifier
            .width(COLUMN_WIDTH)
            .defaultMinSize(minHeight = BcSizes.minTouch)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_GAP)
    ) {
        Box {
            Tile(initials, pastel, photo)
            if (unread > 0) UnreadBadge(unread, Modifier.align(Alignment.TopEnd).testTag(UNREAD_BADGE + name))
        }
        BcText(
            text = name,
            style = BcTextStyle.LabelSmall,
            color = if (unread > 0) BcTextColor.Primary else BcTextColor.Muted,
            maxLines = 1
        )
    }
}

@Composable
private fun Tile(initials: String, pastel: BcPastel?, photo: ImageBitmap?) {
    val base = Modifier.size(BcSizes.personTile)
    val tile = if (pastel != null) {
        base.shadow(TILE_SHADOW, BcShapes.tile).background(pastel.color, BcShapes.tile)
    } else {
        base.clip(BcShapes.tile).background(BcColors.tile).border(1.dp, BcColors.tileEdge, BcShapes.tile)
    }
    Box(tile, contentAlignment = Alignment.Center) {
        if (photo != null) {
            Image(photo, null, Modifier.size(BcSizes.personTile).clip(BcShapes.tile), contentScale = ContentScale.Crop)
        } else {
            val ink = if (pastel != null) BcColors.onPastel else BcColors.text
            CompositionLocalProvider(LocalBcContentColor provides ink) {
                BcText(initials, style = BcTextStyle.Initials, maxLines = 1)
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int, modifier: Modifier) {
    Box(
        modifier = modifier
            .offset(x = BADGE_OFFSET, y = -BADGE_OFFSET)
            .defaultMinSize(minWidth = BADGE_SIZE, minHeight = BADGE_SIZE)
            .background(BcColors.text, BcShapes.pill)
            .padding(horizontal = BADGE_PADDING),
        contentAlignment = Alignment.Center
    ) {
        val label = if (count > MAX_BADGE) "$MAX_BADGE+" else "$count"
        BcText(label, style = BcTextStyle.Badge, color = BcTextColor.OnLight)
    }
}

private val COLUMN_WIDTH = 58.dp
private val TILE_GAP = 4.dp
private val TILE_SHADOW = 6.dp
private val BADGE_SIZE = 18.dp
private val BADGE_OFFSET = 4.dp
private val BADGE_PADDING = 5.dp
