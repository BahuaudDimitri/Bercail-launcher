package io.github.bahuauddimitri.bercail.core.domain.settings

import kotlinx.coroutines.flow.Flow

enum class TimelineOrientation { Vertical, Horizontal }

/** What is behind the home screen when nothing is loaded in a media app. */
enum class IdleBackground { Wallpaper, Brume }

/** The settings of Bercail, with their defaults. */
data class Settings(
    val favoritePeople: List<String> = emptyList(),
    val favoriteApps: List<String> = emptyList(),
    val timeline: TimelineOrientation = TimelineOrientation.Vertical,
    val drawerExpandedAtStart: Boolean = false,
    val idleBackground: IdleBackground = IdleBackground.Wallpaper
) {
    /** Adds the app to the favorites, or removes it if it already is one. */
    fun withFavoriteAppToggled(appId: String): Settings =
        copy(favoriteApps = if (appId in favoriteApps) favoriteApps - appId else favoriteApps + appId)
}

interface SettingsSource {
    val settings: Flow<Settings>

    /** Changes the settings and keeps them for the next time. */
    suspend fun update(transform: (Settings) -> Settings)
}
