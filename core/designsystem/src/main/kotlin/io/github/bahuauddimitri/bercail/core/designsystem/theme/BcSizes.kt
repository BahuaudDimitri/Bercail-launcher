package io.github.bahuauddimitri.bercail.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. */
object BcSpacing {
    val xxs = 4.dp
    val xs = 6.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Sizes of the touchable elements and of the main shapes. */
object BcSizes {
    /** Smallest touch target allowed anywhere. */
    val minTouch = 40.dp
    val personTile = 46.dp
    val homeToggle = 48.dp
    val iconButton = 44.dp
    val listRow = 48.dp
    val icon = 22.dp

    val touchTargets: Map<String, Dp> = mapOf(
        "minTouch" to minTouch,
        "personTile" to personTile,
        "homeToggle" to homeToggle,
        "iconButton" to iconButton,
        "listRow" to listRow
    )
}

/** Corner shapes. */
object BcShapes {
    val tile = RoundedCornerShape(16.dp)
    val drawer = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val card = RoundedCornerShape(22.dp)
    val pill = CircleShape
    val bubbleTheirs = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
    val bubbleMine = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp)
}
