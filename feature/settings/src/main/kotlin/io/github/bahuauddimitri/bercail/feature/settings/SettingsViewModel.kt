package io.github.bahuauddimitri.bercail.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.access.SourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.agendaSourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.mediaSourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.messagesSourceLine
import io.github.bahuauddimitri.bercail.core.domain.access.weatherSourceLine
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.apps.dockOf
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppsSource
import io.github.bahuauddimitri.bercail.core.domain.media.mainMediaApp
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.search.appDirectory
import io.github.bahuauddimitri.bercail.core.domain.settings.IdleBackground
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
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

/** What the settings screen shows: the settings as stored, the apps to choose the dock from, the sources. */
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
    val mediaApps: List<App> = emptyList(),

    /** Whether Bercail may read the conversations Beeper keeps, and how to let it. */
    val messages: SourceLine = messagesSourceLine(allowed = false),
    val messagesAllowed: Boolean = false,

    /** The conversations to choose the favorite people from, the most recent first; none until Beeper is linked. */
    val people: List<Conversation> = emptyList()
) {
    /** The favorite people by name, as their row lists them. */
    val favoritePeopleNames: String
        get() = when {
            !messagesAllowed -> "À choisir une fois Beeper relié"

            settings.favoritePeople.isEmpty() -> "Les quatre dernières conversations, tant que tu n'as pas choisi"

            else -> settings.favoritePeople.joinToString(", ") { id ->
                people.firstOrNull { it.id == id }?.name ?: GONE
            }
        }

    /** The media app the « Écouter » sheet shows first: the chosen one, otherwise Spotify or the first one. */
    val mainMediaApp: App? get() = mainMediaApp(mediaApps, settings.mainMediaApp)

    /** The apps of the dock, in their order. */
    val dock: List<App> get() = dockOf(apps, settings.dockApps)

    /** The apps of the dock by name, as their row lists them, with the room it has. */
    val dockNames: String
        get() = dock.joinToString(", ") { it.name }.ifEmpty { "Aucune pour l'instant" } + " · cinq au plus"
}

/** What the settings screen does. Every change is stored at once. */
interface SettingsActions {
    fun onIdleBackground(background: IdleBackground)
    fun onDrawerExpandedAtStart(expanded: Boolean)
    fun onToggleDockApp(appId: String)
    fun onMainMediaApp(appId: String)
    fun onToggleFavoritePerson(id: String)

    /** Looks for a newer version, fetches it, or installs a ready one: whatever the update row or its button offers. */
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
    mediaApps: MediaAppsSource,
    messages: MessagesSource
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
            combine(mediaApps.apps, messages.conversations, ::Pair)
        ) { current, installed, update, granted, (players, conversations) ->
            val messagesAllowed = Access.Messages in granted
            SettingsUiState(
                mediaApps = players,
                messages = messagesSourceLine(messagesAllowed, toldOfArrivals = Access.Notifications in granted),
                messagesAllowed = messagesAllowed,
                people = if (messagesAllowed) conversations else emptyList(),
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

    override fun onDrawerExpandedAtStart(expanded: Boolean) = change { it.copy(drawerExpandedAtStart = expanded) }

    override fun onToggleDockApp(appId: String) {
        val installed = state.value.apps.map { it.id }
        change { it.withDockAppToggled(appId, installed) }
    }

    override fun onMainMediaApp(appId: String) = change { it.copy(mainMediaApp = appId) }

    override fun onToggleFavoritePerson(id: String) = change { it.withFavoritePersonToggled(id) }

    override fun onUpdateTouched() {
        viewModelScope.launch { updates.proceed() }
    }

    override fun onOpenHomeScreenSettings() = phone.openHomeScreenSettings()

    override fun onOpenPhoneSettings() = phone.openSettings()

    private fun change(transform: (Settings) -> Settings) {
        viewModelScope.launch { settings.update(transform) }
    }
}

/** A favorite whose conversation is no longer among the recent ones. */
private const val GONE = "(conversation absente)"
