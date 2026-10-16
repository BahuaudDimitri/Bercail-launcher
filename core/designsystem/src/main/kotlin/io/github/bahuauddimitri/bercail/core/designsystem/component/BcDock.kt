package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes

/** The most apps the dock holds: the room of the band. */
const val BC_DOCK_ROOM = 5

/** An app of the dock: its name, read out but never written, and its glyph, or its initial without one. */
@Immutable
data class BcDockApp(val name: String, val glyph: ImageBitmap?, val onClick: () -> Unit)

/**
 * The dock of the band: five places of 44 dp across the width, each app in a fine ring with its icon in one colour,
 * without label. The apps fill the places from the left; the others stay empty, so that an app never moves when
 * another one goes. Five at most.
 */
@Composable
fun BcDock(apps: List<BcDockApp>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(BC_DOCK_ROOM) { place ->
            val app = apps.getOrNull(place)
            if (app == null) Spacer(Modifier.size(BcSizes.dock)) else DockRing(app)
        }
    }
}

@Composable
private fun DockRing(app: BcDockApp) {
    Box(
        Modifier
            .size(BcSizes.dock)
            .clip(BcShapes.pill)
            .clickable(onClick = app.onClick)
            .clearAndSetSemantics {
                contentDescription = app.name
                role = Role.Button
            }
    ) {
        val mark = app.glyph?.let { BcRingMark.Glyph(it) } ?: BcRingMark.Initial(app.name.take(1).uppercase())
        Ring(app.name, mark, BcRingTone.Neutral, size = BcSizes.dock)
    }
}
