package io.github.bahuauddimitri.bercail.core.domain.media

import io.github.bahuauddimitri.bercail.core.domain.apps.App
import kotlinx.coroutines.flow.Flow

/** What Bercail last saw loaded in a media app: what "Reprendre" is about. */
data class LastHeard(val appId: String, val title: String)

/** The apps of the phone that can play sound or video. They are named as the installed apps are. */
interface MediaAppsSource {
    /** Those apps from A to Z, emitted again when one is installed or removed. */
    val apps: Flow<List<App>>

    /** What was last seen loaded, kept from one day to the next. */
    val lastHeard: Flow<LastHeard?>

    /** Remembers what is loaded now. */
    suspend fun remember(heard: LastHeard)
}

/** One line of the « Écouter » sheet. Only the [main] app offers to [resume] what it last played. */
data class MediaAppRow(val app: App, val main: Boolean, val resume: String?)

/** The main media app: the one the user [chosen], if it is still installed; otherwise Spotify, or the first one. */
fun mainMediaApp(apps: List<App>, chosen: String?): App? = apps.firstOrNull { it.id == chosen }
    ?: apps.firstOrNull { it.name == DEFAULT_MEDIA_APP }
    ?: apps.firstOrNull()

/** The « Écouter » sheet: the main app first, then the others in their order. */
fun mediaAppRows(apps: List<App>, chosen: String?, last: LastHeard?): List<MediaAppRow> {
    val main = mainMediaApp(apps, chosen) ?: return emptyList()
    val resume = last?.takeIf { it.appId == main.id }?.let { "Reprendre « ${it.title} »" }
    return listOf(MediaAppRow(main, main = true, resume)) + (apps - main).map { MediaAppRow(it, false, null) }
}

private const val DEFAULT_MEDIA_APP = "Spotify"
