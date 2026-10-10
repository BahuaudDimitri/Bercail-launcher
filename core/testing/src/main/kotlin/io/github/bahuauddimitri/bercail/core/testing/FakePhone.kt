package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppIcon
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
    private val icons = mutableMapOf<String, AppIcon>()

    override suspend fun icon(appId: String): AppIcon? {
        iconRequests += appId
        return icons[appId]
    }

    /** Gives an app a plain square icon of one color (ARGB). */
    fun paintIcon(appId: String, color: Int, size: Int = 4) {
        icons[appId] = AppIcon(size, size, IntArray(size * size) { color })
    }

    override fun open(appId: String) {
        opened += appId
    }

    fun install(app: App) = apps.update { it + app }

    fun uninstall(appId: String) = apps.update { all -> all.filterNot { it.id == appId } }
}

/** A phone that only remembers where it was sent. */
class FakePhone : Phone {
    /** Where the phone was sent so far, in order: "web: chat", "store: chat", "settings", "home screen settings". */
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
}
