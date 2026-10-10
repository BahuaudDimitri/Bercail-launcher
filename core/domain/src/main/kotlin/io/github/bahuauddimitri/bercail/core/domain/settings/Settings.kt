package io.github.bahuauddimitri.bercail.core.domain.settings

import kotlinx.coroutines.flow.Flow

enum class TimelineOrientation { Vertical, Horizontal }

/** What is behind the home screen when nothing is loaded in a media app. */
enum class IdleBackground { Wallpaper, Brume }

/** The settings of Bercail the home screen reads, with their defaults. */
data class Settings(
    val favoritePeople: List<String> = emptyList(),
    val timeline: TimelineOrientation = TimelineOrientation.Vertical,
    val drawerExpandedAtStart: Boolean = false,
    val idleBackground: IdleBackground = IdleBackground.Wallpaper
)

interface SettingsSource {
    val settings: Flow<Settings>
}
