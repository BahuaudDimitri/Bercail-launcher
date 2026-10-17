package io.github.bahuauddimitri.bercail.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcColors
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcShapes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSizes
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing

/** The most apps the dock holds: the room of the band. */
const val BC_DOCK_ROOM = 5

/**
 * An app of the dock: its name, read out but never written, and its glyph, or its initial without one. A long press
 * calls [onLongClick], which asks for its shortcuts; without it, a long press does nothing.
 */
@Immutable
data class BcDockApp(
    val name: String,
    val glyph: ImageBitmap?,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit
)

/**
 * The shortcuts of the dock app named [app], shown in a small glass menu above its icon. The back gesture or a touch
 * beside the menu calls [onDismiss].
 */
@Immutable
data class BcDockMenu(val app: String, val shortcuts: List<BcShortcut>, val onDismiss: () -> Unit)

/**
 * The dock of the band: five places of 44 dp across the width, each app as its icon in one colour, white, without a
 * ring nor label: its drawing comes out at about 28 dp. The apps fill the places from the left; the others stay empty,
 * so that an app never moves when another one goes. Five at most. While [menu] is given, the shortcuts of its app
 * stand above its icon.
 */
@Composable
fun BcDock(apps: List<BcDockApp>, modifier: Modifier = Modifier, menu: BcDockMenu? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(BC_DOCK_ROOM) { place ->
            val app = apps.getOrNull(place)
            if (app == null) Spacer(Modifier.size(BcSizes.dock)) else DockIcon(app, menu?.takeIf { it.app == app.name })
        }
    }
}

@Composable
private fun DockIcon(app: BcDockApp, menu: BcDockMenu?) {
    // A long press never opens the app: without shortcuts to ask for, it does nothing.
    val longClick = app.onLongClick ?: {}
    Box(
        Modifier
            .size(BcSizes.dock)
            .clip(BcShapes.pill)
            .combinedClickable(onLongClick = longClick, onClick = app.onClick)
            .clearAndSetSemantics {
                contentDescription = app.name
                role = Role.Button
                onClick {
                    app.onClick()
                    true
                }
                app.onLongClick?.let { ask ->
                    onLongClick(label = SHORTCUTS) {
                        ask()
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (app.glyph == null) {
            BcText(app.name.take(1).uppercase(), style = BcTextStyle.Initials, maxLines = 1)
        } else {
            // The glyph is framed as the icon is, its drawing in the middle half: a frame larger than the place
            // brings the drawing to about 28 dp, the size the ring used to take away.
            Image(
                bitmap = app.glyph,
                contentDescription = null,
                modifier = Modifier.requiredSize(GLYPH_FRAME).testTag(DOCK_GLYPH_TAG + app.name),
                colorFilter = ColorFilter.tint(BcColors.text)
            )
        }
        if (menu != null) ShortcutPopup(menu)
    }
}

/** Tags the glyph of an app of the dock, after its name: for the tests. */
internal const val DOCK_GLYPH_TAG = "dock-glyph-"

/** The frame the glyph of an app is drawn in: its drawing, half of it, comes out at about 28 dp. */
private val GLYPH_FRAME = 56.dp

/** The menu above the icon it belongs to, inside the screen; back or a touch beside it closes it. */
@Composable
private fun ShortcutPopup(menu: BcDockMenu) {
    val density = LocalDensity.current
    val above = with(density) { AboveAnchor(gap = BcSpacing.s.roundToPx(), margin = BcSpacing.l.roundToPx()) }
    Popup(
        popupPositionProvider = above,
        onDismissRequest = menu.onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        BackHandler { menu.onDismiss() }
        BcShortcutMenu(menu.shortcuts)
    }
}

/** Above the anchor, centred on it, kept [margin] away from the edges of the screen. */
internal class AboveAnchor(private val gap: Int, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val widest = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(margin, widest)
        val y = (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/** What a screen reader offers on a dock icon besides opening it. */
private const val SHORTCUTS = "Raccourcis"
