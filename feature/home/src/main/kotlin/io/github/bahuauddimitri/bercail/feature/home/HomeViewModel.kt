package io.github.bahuauddimitri.bercail.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayLine
import io.github.bahuauddimitri.bercail.core.domain.agenda.Glance
import io.github.bahuauddimitri.bercail.core.domain.agenda.dayLineAt
import io.github.bahuauddimitri.bercail.core.domain.agenda.glanceAt
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.home.homeSummary
import io.github.bahuauddimitri.bercail.core.domain.media.MediaSource
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.PreviousAction
import io.github.bahuauddimitri.bercail.core.domain.media.positionAt
import io.github.bahuauddimitri.bercail.core.domain.media.previousAction
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSummary
import io.github.bahuauddimitri.bercail.core.domain.messages.favoritesAmong
import io.github.bahuauddimitri.bercail.core.domain.messages.messagesSummary
import io.github.bahuauddimitri.bercail.core.domain.messages.unreadOutsideFavorites
import io.github.bahuauddimitri.bercail.core.domain.screen.HomeBackground
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.core.domain.screen.backgroundFor
import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.settings.TimelineOrientation
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import io.github.bahuauddimitri.bercail.core.domain.time.DayPart
import io.github.bahuauddimitri.bercail.core.domain.weather.Weather
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import io.github.bahuauddimitri.bercail.core.domain.weather.weatherLine
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the home screen shows. Texts are already written by the domain rules. */
data class HomeUiState(
    val glance: Glance = Glance("", "", after = null),
    val weatherLine: String? = null,
    val weather: WeatherCondition = WeatherCondition.Clear,
    val dayPart: DayPart = DayPart.Morning,
    val dayLine: DayLine = DayLine(now = 0f, marks = emptyList()),
    val timeline: TimelineOrientation = TimelineOrientation.Vertical,
    val messages: MessagesSummary = MessagesSummary(unread = 0, senders = emptyList()),
    val favorites: List<Conversation> = emptyList(),
    val otherUnread: Int = 0,
    val controls: List<HomeControl> = emptyList(),
    val homeSummary: String = "",
    val media: MediaState = MediaState.None,
    val page: HomePage = HomePage.Home,
    val drawerExpanded: Boolean = false,
    val background: HomeBackground = HomeBackground.Wallpaper,
    /** False until the sources have been read once: nothing is drawn before. */
    val loaded: Boolean = false
)

/**
 * Turns what the sources say into what the home screen shows. Nothing is listened to while the screen is hidden:
 * the sources are only collected while someone collects [state].
 */
class HomeViewModel(
    clock: Clock,
    agenda: AgendaSource,
    weather: WeatherSource,
    messages: MessagesSource,
    private val home: HomeSource,
    private val media: MediaSource,
    settings: SettingsSource
) : ViewModel(),
    HomeActions {
    private val wantedPage = MutableStateFlow(HomePage.Home)

    /** What the user did with the drawer; until then, the setting decides. */
    private val drawerChoice = MutableStateFlow<Boolean?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val day: Flow<Day> = combine(
        clock.minutes,
        clock.minutes.map { it.toLocalDate() }.distinctUntilChanged().flatMapLatest(agenda::eventsOn),
        weather.weather,
        ::Day
    )

    private val drawer: Flow<Drawer> = combine(messages.conversations, home.controls, settings.settings, ::Drawer)

    /** When the content goes away, the Listen page goes with it, and does not come back by itself. */
    private val loaded: Flow<MediaState> = media.state.onEach { if (!it.isLoaded) wantedPage.value = HomePage.Home }

    val state: StateFlow<HomeUiState> = combine(day, drawer, loaded, wantedPage, drawerChoice, ::stateOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HomeUiState())

    /** Where the content is. Only the remote of the Listen page collects it. */
    val position: StateFlow<Duration> =
        media.position.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Duration.ZERO)

    override fun onDrawerExpandedChange(expanded: Boolean) {
        drawerChoice.value = expanded
    }

    override fun onPageWanted(page: HomePage) {
        wantedPage.value = page
    }

    override fun onToggleControl(id: String) {
        viewModelScope.launch { home.toggle(id) }
    }

    override fun onPlayPause() {
        if (state.value.media.isPlaying) media.pause() else media.play()
    }

    override fun onNext() = media.next()

    override fun onPrevious() {
        viewModelScope.launch {
            when (previousAction(media.position.first())) {
                PreviousAction.Restart -> media.seekTo(Duration.ZERO)
                PreviousAction.PreviousTrack -> media.previous()
            }
        }
    }

    override fun onSeek(fraction: Float) {
        val loaded = state.value.media as? MediaState.Loaded ?: return
        media.seekTo(positionAt(fraction, loaded.content.duration))
    }

    private fun stateOf(
        day: Day,
        drawer: Drawer,
        media: MediaState,
        wanted: HomePage,
        drawerChoice: Boolean?
    ): HomeUiState {
        val part = DayPart.at(day.now.toLocalTime())
        val favorites = drawer.settings.favoritePeople
        return HomeUiState(
            glance = glanceAt(day.now, day.events),
            weatherLine = day.weather?.let { weatherLine(it, part) },
            weather = day.weather?.condition ?: WeatherCondition.Clear,
            dayPart = part,
            dayLine = dayLineAt(day.now, day.events),
            timeline = drawer.settings.timeline,
            messages = messagesSummary(drawer.conversations),
            favorites = favoritesAmong(drawer.conversations, favorites),
            otherUnread = unreadOutsideFavorites(drawer.conversations, favorites),
            controls = drawer.controls,
            homeSummary = homeSummary(drawer.controls),
            media = media,
            page = wanted.allowedWith(media),
            drawerExpanded = drawerChoice ?: drawer.settings.drawerExpandedAtStart,
            background = backgroundFor(media, drawer.settings.idleBackground),
            loaded = true
        )
    }

    private data class Day(val now: LocalDateTime, val events: List<AgendaEvent>, val weather: Weather?)

    private data class Drawer(
        val conversations: List<Conversation>,
        val controls: List<HomeControl>,
        val settings: Settings
    )
}
