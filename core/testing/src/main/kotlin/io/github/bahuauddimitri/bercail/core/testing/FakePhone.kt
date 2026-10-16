package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.Picture
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** The apps of a phone the tests fill by hand. It remembers which apps were opened; no icon unless painted. */
class FakeAppsSource(apps: List<App> = emptyList()) : AppsSource {
    override val apps = MutableStateFlow(apps)

    /** The ids of the apps opened so far, in order. */
    val opened = mutableListOf<String>()

    /** The apps whose icon was asked for so far, in order. */
    val iconRequests = mutableListOf<String>()
    private val icons = mutableMapOf<String, Picture>()

    override suspend fun icon(appId: String): Picture? {
        iconRequests += appId
        return icons[appId]
    }

    /** Gives an app a plain square icon of one color (ARGB). */
    fun paintIcon(appId: String, color: Int, size: Int = 4) {
        icons[appId] = Picture(size, size, IntArray(size * size) { color })
    }

    /** The apps whose glyph was asked for so far, in order. */
    val glyphRequests = mutableListOf<String>()
    private val glyphs = mutableMapOf<String, Picture>()

    override suspend fun glyph(appId: String): Picture? {
        glyphRequests += appId
        return glyphs[appId]
    }

    /** Gives an app a glyph: a white square filling the picture. */
    fun paintGlyph(appId: String, size: Int = 4) {
        glyphs[appId] = Picture(size, size, IntArray(size * size) { OPAQUE_WHITE })
    }

    override fun open(appId: String) {
        opened += appId
    }

    /** The shortcuts each app offers, as given by the tests; none unless given. */
    val offeredShortcuts = mutableMapOf<String, List<AppShortcut>>()

    /** The apps whose shortcuts were read so far, in order. */
    val shortcutRequests = mutableListOf<String>()

    /** The shortcuts opened so far, in order. */
    val openedShortcuts = mutableListOf<AppShortcut>()

    override suspend fun shortcuts(appId: String): List<AppShortcut> {
        shortcutRequests += appId
        return offeredShortcuts[appId].orEmpty()
    }

    /** A shortcut has the glyph painted under its id ([paintGlyph]). */
    override suspend fun shortcutGlyph(shortcut: AppShortcut): Picture? = glyphs[shortcut.id]

    override fun openShortcut(shortcut: AppShortcut) {
        openedShortcuts += shortcut
    }

    fun install(app: App) = apps.update { it + app }

    fun uninstall(appId: String) = apps.update { all -> all.filterNot { it.id == appId } }
}

private const val OPAQUE_WHITE = -0x1

/** A phone that only remembers where it was sent. */
class FakePhone : Phone {
    /** Where the phone was sent so far, in order: "web: chat", "store: chat", "settings", "app settings"… */
    val visits = mutableListOf<String>()

    override fun searchWeb(query: String) {
        visits += "web: $query"
    }

    override fun searchStore(query: String) {
        visits += "store: $query"
    }

    override fun openSettings() {
        visits += "settings"
    }

    override fun openHomeScreenSettings() {
        visits += "home screen settings"
    }

    override fun openAppSettings() {
        visits += "app settings"
    }
}
