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

    /**
     * The icon of an app as a white glyph whose shape is in the alpha ([monochromeGlyph], [silhouetteGlyph]), or
     * nothing when it has none that reads, or is gone: its initial then stands in for it.
     */
    suspend fun glyph(appId: String): Picture?

    fun open(appId: String)
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
