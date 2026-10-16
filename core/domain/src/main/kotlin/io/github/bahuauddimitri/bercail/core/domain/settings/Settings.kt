package io.github.bahuauddimitri.bercail.core.domain.settings

import io.github.bahuauddimitri.bercail.core.domain.apps.MAX_DOCK_APPS
import io.github.bahuauddimitri.bercail.core.domain.messages.MAX_FAVORITE_PEOPLE
import io.github.bahuauddimitri.bercail.core.domain.weather.City
import kotlinx.coroutines.flow.Flow

/** What is behind the home screen when nothing is loaded in a media app. */
enum class IdleBackground { Wallpaper, Brume }

/** The settings of Bercail, with their defaults. */
data class Settings(
    val favoritePeople: List<String> = emptyList(),
    /** The apps of the dock, in the order they were touched. */
    val dockApps: List<String> = emptyList(),
    val drawerExpandedAtStart: Boolean = false,
    val idleBackground: IdleBackground = IdleBackground.Wallpaper,
    /** The city whose weather is shown, or nothing for the place where the phone is. */
    val weatherCity: City? = null,
    /** The media app shown first in the « Écouter » sheet; without a choice, Spotify if it is there. */
    val mainMediaApp: String? = null
) {
    /**
     * Adds the app at the end of the dock, unless it is full, or takes it out if it is in. Apps no longer
     * [installed] leave the dock first: they keep no place.
     */
    fun withDockAppToggled(appId: String, installed: Collection<String>): Settings {
        val dock = dockApps.filter { it in installed }
        return when {
            appId in dock -> copy(dockApps = dock - appId)
            dock.size < MAX_DOCK_APPS -> copy(dockApps = dock + appId)
            else -> copy(dockApps = dock)
        }
    }

    /** Adds the conversation to the favorite people, unless they are already four, or removes it if it is one. */
    fun withFavoritePersonToggled(id: String): Settings = when {
        id in favoritePeople -> copy(favoritePeople = favoritePeople - id)
        favoritePeople.size < MAX_FAVORITE_PEOPLE -> copy(favoritePeople = favoritePeople + id)
        else -> this
    }
}

interface SettingsSource {
    val settings: Flow<Settings>

    /** Changes the settings and keeps them for the next time. */
    suspend fun update(transform: (Settings) -> Settings)
}
