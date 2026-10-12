package io.github.bahuauddimitri.bercail.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.access.SourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.agendaSourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.mediaSourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.weatherSourceLine
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.mainMediaApp
import io.github.bahuauddimitri.bercail.core.domain.search.appDirectory
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateLine
import io.github.bahuauddimitri.bercail.core.domain.update.UpdateStatus
import io.github.bahuauddimitri.bercail.core.domain.update.Updates
import io.github.bahuauddimitri.bercail.core.domain.update.updateLine
import io.github.bahuauddimitri.bercail.core.domain.weather.CitySearch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the settings screen shows: the settings as stored, the apps to choose favorites from, the sources. */
data class SettingsUiState(
    val settings: Settings = Settings(),
    /** Every app, from A to Z. */
    val apps: List<App> = emptyList(),
    /** The version running and where its update stands. */
    val update: UpdateLine = updateLine(null, UpdateStatus.Off),
    /** Whether Bercail may read the calendar, and how to let it. */
    val agenda: SourceLine = agendaSourceLine(allowed = false),
    /** Whether Bercail may know roughly where the phone is, for the weather. */
    val weather: SourceLine = weatherSourceLine(allowed = false),
    val locationAllowed: Boolean = false,
    /** Whether Bercail may see what the media apps play, and how to let it. */
    val media: SourceLine = mediaSourceLine(allowed = false),

    /** The installed apps that can play sound or video, from A to Z. */
    val mediaApps: List<App> = emptyList()
) {
    /** The media app the « Écouter » sheet shows first: the chosen one, otherwise Spotify or the first one. */
    val mainMediaApp: App? get() = mainMediaApp(mediaApps, settings.mainMediaApp)

    /** The favorite apps by name, as their row lists them. */
    val favoriteAppNames: String
        get() = settings.favoriteApps
            .mapNotNull { id -> apps.firstOrNull { it.id == id }?.name }
            .joinToString(", ")
            .ifEmpty { "Aucune pour l'instant" }
}

/** What the settings screen does. Every change is stored at once. */
interface SettingsActions {
    fun onIdleBackground(background: IdleBackground)
    fun onTimeline(orientation: TimelineOrientation)
    fun onDrawerExpandedAtStart(expanded: Boolean)
    fun onToggleFavoriteApp(appId: String)
    fun onMainMediaApp(appId: String)

    /** Looks for a newer version, fetches it or installs it: whatever the update row offers. */
    fun onUpdateTouched()
    fun onOpenHomeScreenSettings()
    fun onOpenPhoneSettings()
}

/** Bercail's settings. Sources are only listened to while the screen is shown. */
class SettingsViewModel(
    private val settings: SettingsSource,
    apps: AppsSource,
    private val phone: Phone,
    private val updates: Updates,
    access: AccessSource,
    cities: CitySearch,
    mediaApps: MediaAppsSource
) : ViewModel(),
    SettingsActions {
    /** The choice of the place of the weather, with its search for a city. */
    val place = PlacePicker(settings, cities, viewModelScope)

    val state: StateFlow<SettingsUiState> =
        combine(
            settings.settings,
            apps.apps,
            updates.status,
            access.granted,
            mediaApps.apps
        ) { current, installed, update, granted, players ->
            SettingsUiState(
                mediaApps = players,
                settings = current,
                apps = appDirectory(installed, emptyList()).sections.flatMap { it.apps },
                update = updateLine(updates.current, update),
                agenda = agendaSourceLine(allowed = Access.Calendar in granted),
                weather = weatherSourceLine(Access.Location in granted, current.weatherCity?.name),
                locationAllowed = Access.Location in granted,
                media = mediaSourceLine(allowed = Access.Notifications in granted)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsUiState())

    override fun onIdleBackground(background: IdleBackground) = change { it.copy(idleBackground = background) }

    override fun onTimeline(orientation: TimelineOrientation) = change { it.copy(timeline = orientation) }

    override fun onDrawerExpandedAtStart(expanded: Boolean) = change { it.copy(drawerExpandedAtStart = expanded) }

    override fun onToggleFavoriteApp(appId: String) = change { it.withFavoriteAppToggled(appId) }

    override fun onMainMediaApp(appId: String) = change { it.copy(mainMediaApp = appId) }

    override fun onUpdateTouched() {
        viewModelScope.launch { updates.proceed() }
    }

    override fun onOpenHomeScreenSettings() = phone.openHomeScreenSettings()

    override fun onOpenPhoneSettings() = phone.openSettings()

    private fun change(transform: (Settings) -> Settings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
