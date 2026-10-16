package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** A shortcut of an app (« Nouvel onglet »): its name, its icon in one colour, or its initial without one. */
@Immutable
data class BcShortcut(val label: String, val glyph: ImageBitmap?, val onClick: () -> Unit)

/**
 * The shortcuts of an app in a small glass menu: one row per shortcut, its icon in a fine ring and its name; a
 * touch on a row opens the app on it. Tinted glass: it stands over the band, where nothing is frosted.
 */
@Composable
fun BcShortcutMenu(shortcuts: List<BcShortcut>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = MENU_MIN_WIDTH, max = MENU_MAX_WIDTH)
            .clip(BcShapes.card)
            .background(BcColors.glassSearch)
            .border(EDGE, BcColors.sheetEdge, BcShapes.card)
            .padding(vertical = BcSpacing.xs)
    ) {
        shortcuts.forEach { ShortcutRow(it) }
    }
}

@Composable
private fun ShortcutRow(shortcut: BcShortcut) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BcSizes.listRow)
            .clickable(onClick = shortcut.onClick)
            .clearAndSetSemantics {
                contentDescription = shortcut.label
                role = Role.Button
                onClick {
                    shortcut.onClick()
                    true
                }
            }
            .padding(horizontal = BcSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BcSpacing.m)
    ) {
        val mark =
            shortcut.glyph?.let { BcRingMark.Glyph(it) } ?: BcRingMark.Initial(shortcut.label.take(1).uppercase())
        Ring(shortcut.label, mark, BcRingTone.Neutral, size = RING)
        BcText(shortcut.label, style = BcTextStyle.Body, maxLines = 1)
    }
}

private val RING = 32.dp
private val EDGE = 1.dp
private val MENU_MIN_WIDTH = 200.dp
private val MENU_MAX_WIDTH = 280.dp
