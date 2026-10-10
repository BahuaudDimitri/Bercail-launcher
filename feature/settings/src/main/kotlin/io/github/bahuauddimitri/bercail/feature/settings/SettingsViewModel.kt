package io.github.bahuauddimitri.bercail.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.search.appDirectory
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the settings screen shows: the settings as stored, and the apps to choose favorites from. */
data class SettingsUiState(
    val settings: Settings = Settings(),
    /** Every app, from A to Z. */
    val apps: List<App> = emptyList()
) {
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
    fun onOpenHomeScreenSettings()
    fun onOpenPhoneSettings()
}

/** Bercail's settings. Sources are only listened to while the screen is shown. */
class SettingsViewModel(private val settings: SettingsSource, apps: AppsSource, private val phone: Phone) :
    ViewModel(),
    SettingsActions {
    val state: StateFlow<SettingsUiState> = combine(settings.settings, apps.apps) { current, installed ->
        SettingsUiState(current, appDirectory(installed, emptyList()).sections.flatMap { it.apps })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsUiState())

    override fun onIdleBackground(background: IdleBackground) = change { it.copy(idleBackground = background) }

    override fun onTimeline(orientation: TimelineOrientation) = change { it.copy(timeline = orientation) }

    override fun onDrawerExpandedAtStart(expanded: Boolean) = change { it.copy(drawerExpandedAtStart = expanded) }

    override fun onToggleFavoriteApp(appId: String) = change { it.withFavoriteAppToggled(appId) }

    override fun onOpenHomeScreenSettings() = phone.openHomeScreenSettings()

    override fun onOpenPhoneSettings() = phone.openSettings()

    private fun change(transform: (Settings) -> Settings) {
        viewModelScope.launch { settings.update(transform) }
    }
}
