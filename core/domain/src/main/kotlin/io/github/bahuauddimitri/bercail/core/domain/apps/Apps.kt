package io.github.bahuauddimitri.bercail.core.domain.apps

import kotlinx.coroutines.flow.Flow

/** An installed app the launcher can open. [id] names it for good: its name may change with the language. */
data class App(val id: String, val name: String)

/** The icon of an app as plain pixels (ARGB, row by row), so that the domain stays free of Android. */
class AppIcon(val width: Int, val height: Int, val pixels: IntArray)

interface AppsSource {
    /** Every app that can be opened, emitted again when one is installed, removed or renamed. */
    val apps: Flow<List<App>>

    /** The icon of an app, or nothing if it has none or is gone. */
    suspend fun icon(appId: String): AppIcon?

    fun open(appId: String)
}

/** The few places outside Bercail the search and the settings send to. */
interface Phone {
    fun searchWeb(query: String)
    fun searchStore(query: String)
    fun openSettings()

    /** Where Android lets the user choose the home screen app. */
    fun openHomeScreenSettings()
}
