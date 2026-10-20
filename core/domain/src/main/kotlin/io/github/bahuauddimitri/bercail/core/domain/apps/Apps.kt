package io.github.bahuauddimitri.bercail.core.domain.apps

import io.github.bahuauddimitri.bercail.core.domain.Picture
import kotlinx.coroutines.flow.Flow

/** An installed app the launcher can open. [id] names it for good: its name may change with the language. */
data class App(val id: String, val name: String)

interface AppsSource {
    /** Every app that can be opened, emitted again when one is installed, removed or renamed. */
    val apps: Flow<List<App>>

    /** The icon of an app, or nothing if it has none or is gone. */
    suspend fun icon(appId: String): Picture?

    /** The icon packs installed, from A to Z, emitted again when one is installed or removed. */
    val iconPacks: Flow<List<IconPack>>

    /**
     * The icon of an app as a white glyph whose shape is in the alpha ([monochromeGlyph], [iconGlyph]), or
     * nothing when it has none that reads, or is gone: its initial then stands in for it. With an [iconPack] that
     * draws the app, its drawing instead ([packGlyph]); a pack that does not draw it, or is gone, changes nothing.
     * With a [drawing] chosen for the app, that drawing before all, as long as its pack still has it.
     */
    suspend fun glyph(appId: String, iconPack: String? = null, drawing: PackIcon? = null): Picture?

    /**
     * Every drawing an installed icon [pack] offers, and which one its `appfilter` gives each app: read when the icon
     * picker asks, never for the home. Empty when the pack is gone.
     */
    suspend fun iconCatalog(pack: IconPack): PackCatalog

    /** One drawing of a pack as a small white glyph, as the dock would draw it; nothing when it is gone or empty. */
    suspend fun packIconGlyph(icon: PackIcon): Picture?

    fun open(appId: String)

    /**
     * The shortcuts an app offers into itself, read when asked (a long press in the dock), in no order
     * ([shortcutsShown] sorts them). None when it has none, or while Android does not let Bercail read them: only
     * the default home app may.
     */
    suspend fun shortcuts(appId: String): List<AppShortcut>

    /** The icon of a shortcut as a white glyph, like the glyph of an app; nothing when it has none that reads. */
    suspend fun shortcutGlyph(shortcut: AppShortcut): Picture?

    /** Opens the app on its shortcut. A shortcut that is gone or turned off opens nothing. */
    fun openShortcut(shortcut: AppShortcut)
}

/** The few places outside Bercail the search and the settings send to. */
interface Phone {
    fun searchWeb(query: String)
    fun searchStore(query: String)
    fun openSettings()

    /** Where Android lets the user choose the home screen app. */
    fun openHomeScreenSettings()

    /** The page of Bercail in the phone settings, where what it may read can be changed. */
    fun openAppSettings()
}
