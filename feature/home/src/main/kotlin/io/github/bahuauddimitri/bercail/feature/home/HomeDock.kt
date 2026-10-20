package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDock
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDockApp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDockMenu
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcShortcut
import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.DockIcon
import io.github.bahuauddimitri.bercail.core.domain.apps.drawing
import io.github.bahuauddimitri.bercail.core.domain.apps.packFor
import io.github.bahuauddimitri.bercail.core.domain.apps.shortcutsShown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The shortcuts of an app of the dock, open above its icon after a long press. */
data class DockMenu(val appId: String, val shortcuts: List<AppShortcut>)

/** What the home screen does with the dock: its icons, and the shortcuts a long press shows. */
interface DockActions {
    /**
     * The icon of an app of the dock in one colour, drawn when the dock first shows it, or the drawing the chosen
     * [iconPack] gives it, or the [icon] chosen for it in the settings; nothing for its initial.
     */
    suspend fun glyphOf(appId: String, iconPack: String?, icon: DockIcon = DockIcon.FromPack): Picture?

    /** A long press on an app of the dock: its shortcuts are read now, and stand above it if it has any. */
    fun onDockLongPress(appId: String)

    /** Closes the menu of shortcuts: the back gesture, a touch beside it, the Home button. */
    fun onCloseDockMenu()

    /** Opens the app on a shortcut of its menu, which closes. */
    fun onOpenShortcut(shortcut: AppShortcut)

    /** The icon of a shortcut in one colour; nothing for its initial. */
    suspend fun shortcutGlyphOf(shortcut: AppShortcut): Picture?
}

/**
 * The dock of the home screen. The shortcuts of an app are read only on a long press on its icon, never before
 * nor after: Android gives them to the default home app only, so elsewhere a long press shows nothing.
 */
internal class HomeDock(private val apps: AppsSource) :
    DockActions,
    AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val open = MutableStateFlow<DockMenu?>(null)
    private var reading: Job? = null

    /** The menu of shortcuts that is open, if any. */
    val menu: StateFlow<DockMenu?> = open

    /** « Dessin d'origine » asks for no pack; a chosen drawing comes with the dock's pack, to fall back on. */
    override suspend fun glyphOf(appId: String, iconPack: String?, icon: DockIcon): Picture? =
        apps.glyph(appId, icon.packFor(iconPack), icon.drawing)

    override fun onDockLongPress(appId: String) {
        reading?.cancel()
        reading = scope.launch {
            val shown = shortcutsShown(apps.shortcuts(appId))
            open.value = if (shown.isEmpty()) null else DockMenu(appId, shown)
        }
    }

    override fun onCloseDockMenu() {
        reading?.cancel()
        open.value = null
    }

    override fun onOpenShortcut(shortcut: AppShortcut) {
        onCloseDockMenu()
        apps.openShortcut(shortcut)
    }

    override suspend fun shortcutGlyphOf(shortcut: AppShortcut): Picture? = apps.shortcutGlyph(shortcut)

    override fun close() = scope.cancel()
}

/**
 * The apps of the dock, each drawn in one colour once it first shows: by the icon chosen for it in [icons], or by
 * the chosen [iconPack] if it draws it;
 * until then, and without one, its initial.
 * A long press asks for the shortcuts of the app; while [menu] is open, they stand above its icon.
 */
@Composable
internal fun Dock(
    apps: List<App>,
    iconPack: String?,
    icons: Map<String, DockIcon>,
    menu: DockMenu?,
    actions: DockActions,
    onOpenApp: (String) -> Unit
) {
    val dock = apps.map { app ->
        key(app.id) {
            val icon = icons[app.id] ?: DockIcon.FromPack
            val glyph by produceState<Picture?>(null, app.id, iconPack, icon) {
                value = actions.glyphOf(app.id, iconPack, icon)
            }
            BcDockApp(app.name, glyph.asImage(), onLongClick = { actions.onDockLongPress(app.id) }) {
                onOpenApp(app.id)
            }
        }
    }
    val owner = menu?.let { open -> apps.firstOrNull { it.id == open.appId } }
    val shown = if (menu != null && owner != null) {
        BcDockMenu(owner.name, shortcutsOf(menu, actions), actions::onCloseDockMenu)
    } else {
        null
    }
    BcDock(dock, menu = shown)
}

/** The shortcuts of the menu, each in one colour once the menu shows; until then, and without one, its initial. */
@Composable
private fun shortcutsOf(menu: DockMenu, actions: DockActions): List<BcShortcut> = menu.shortcuts.map { shortcut ->
    key(shortcut.appId, shortcut.id) {
        val glyph by produceState<Picture?>(null, shortcut) { value = actions.shortcutGlyphOf(shortcut) }
        BcShortcut(shortcut.label, glyph.asImage()) { actions.onOpenShortcut(shortcut) }
    }
}
