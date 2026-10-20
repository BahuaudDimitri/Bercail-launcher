package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme

/**
 * One icon of a [BcIconGrid]: [key] names its drawing, [name] is what the screen reader says. The [chosen] one is
 * marked with the accent of the moment.
 */
@Immutable
data class BcGridIcon(val key: String, val name: String, val chosen: Boolean = false, val onClick: () -> Unit)

/** A group of icons under its [title]. */
@Immutable
data class BcIconSection(val title: String, val icons: List<BcGridIcon>)

/**
 * A grid of icons that scrolls by itself, in sections, under an optional [header] as wide as the grid. Each icon
 * stands in a round 56 dp cell, its drawing in white as the dock draws it: asked from [image] by its key only once
 * its cell shows, and let go when it scrolls away. The chosen icon wears a ring and a drawing in the accent. It needs
 * a bounded height: inside a panel, one that does not scroll (`BcPanel(scrolls = false)`).
 */
@Composable
fun BcIconGrid(
    sections: List<BcIconSection>,
    image: suspend (key: String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(CELL),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = BcSpacing.xl)
    ) {
        if (header != null) item(key = HEADER, span = { GridItemSpan(maxLineSpan) }) { header() }
        sections.forEachIndexed { index, section ->
            item(key = "$index$TITLE", span = { GridItemSpan(maxLineSpan) }) { BcSectionLabel(section.title) }
            // The same drawing may stand in two sections (proposed, then in its pack): keys carry the section.
            items(section.icons, key = { "$index/${it.key}" }) { icon -> IconCell(icon, image) }
        }
    }
}

@Composable
private fun IconCell(icon: BcGridIcon, image: suspend (String) -> ImageBitmap?) {
    val drawing by produceState<ImageBitmap?>(null, icon.key) { value = image(icon.key) }
    val accent = BcTheme.moment.accent
    val ring = if (icon.chosen) Modifier.border(RING, accent, BcShapes.pill) else Modifier
    Box(
        Modifier
            .size(CELL)
            .clip(BcShapes.pill)
            .then(ring)
            .clickable(role = Role.Button, onClick = icon.onClick)
            .clearAndSetSemantics {
                contentDescription = icon.name
                role = Role.Button
                selected = icon.chosen
                onClick {
                    icon.onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        drawing?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.requiredSize(FRAME),
                colorFilter = ColorFilter.tint(if (icon.chosen) accent else BcColors.text)
            )
        }
    }
}

private const val HEADER = "header"
private const val TITLE = "/title"

/** A cell: room for a finger, six across the Pixel 9. */
private val CELL = 56.dp

/** The frame of a drawing, framed as the dock frames its glyphs: the drawing comes out at about 20 dp. */
private val FRAME = 40.dp
private val RING = 1.5.dp
