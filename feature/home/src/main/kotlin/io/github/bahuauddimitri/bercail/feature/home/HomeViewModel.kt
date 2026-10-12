package io.github.bahuauddimitri.bercail.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.access.AccessSource
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayLine
import io.github.bahuauddimitri.bercail.core.domain.agenda.DayRow
import io.github.bahuauddimitri.bercail.core.domain.agenda.Glance
import io.github.bahuauddimitri.bercail.core.domain.agenda.closedGlance
import io.github.bahuauddimitri.bercail.core.domain.agenda.dayLineAt
import io.github.bahuauddimitri.bercail.core.domain.agenda.dayRowsAt
import io.github.bahuauddimitri.bercail.core.domain.agenda.glanceAt
import io.github.bahuauddimitri.bercail.core.domain.agenda.inWords
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.home.homeSummary
import io.github.bahuauddimitri.bercail.core.domain.media.LastHeard
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAppRow
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.mediaAppRows
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
import io.github.bahuauddimitri.bercail.core.domain.weather.Forecast
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherCondition
import io.github.bahuauddimitri.bercail.core.domain.weather.WeatherSource
import io.github.bahuauddimitri.bercail.core.domain.weather.weatherAt
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    /** False until the user lets Bercail read the calendar: the screen then offers to link it. */
    val agendaAllowed: Boolean = true,
    /** The « Aujourd'hui » sheet: whether it is open, the date under its title, the appointments of the day. */
    val dayOpen: Boolean = false,
    val dayDate: String = "",
    val dayRows: List<DayRow> = emptyList(),
    val messages: MessagesSummary = MessagesSummary(unread = 0, senders = emptyList()),

    /** False until the user lets Bercail read Beeper: the drawer then offers to link it. */
    val messagesAllowed: Boolean = true,
    val favorites: List<Conversation> = emptyList(),
    val otherUnread: Int = 0,
    val controls: List<HomeControl> = emptyList(),
    val homeSummary: String = "",
    val media: MediaState = MediaState.None,
    /** False until the user lets Bercail see what the media apps play: the « Écouter » sheet offers to allow it. */
    val mediaAllowed: Boolean = true,
    /** The « Écouter » sheet, where the music pill leads while nothing is loaded. */
    val listenOpen: Boolean = false,

    /** The lines of the « Écouter » sheet while it is open: the main media app first. */
    val mediaApps: List<MediaAppRow> = emptyList(),
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
class HomeViewModel private constructor(
    clock: Clock,
    private val agenda: AgendaSource,
    weather: WeatherSource,
    messages: MessagesSource,
    private val home: HomeSource,
    settings: SettingsSource,
    access: AccessSource,
    private val listen: ListenSources,
    private val remote: HomeRemote
) : ViewModel(),
    HomeActions,
    ListenActions by remote {
    constructor(
        clock: Clock,
        agenda: AgendaSource,
        weather: WeatherSource,
        messages: MessagesSource,
        home: HomeSource,
        listen: ListenSources,
        settings: SettingsSource,
        access: AccessSource
    ) : this(clock, agenda, weather, messages, home, settings, access, listen, HomeRemote(listen))

    init {
        addCloseable(remote)
    }

    private val wantedPage = MutableStateFlow(HomePage.Home)

    /** What the user did with the drawer; until then, the setting decides. */
    private val drawerChoice = MutableStateFlow<Boolean?>(null)
    private val daySheet = MutableStateFlow(DaySheet())

    /** The day as it was last shown: what a touch on the sheet refers to. */
    private var shown: Day? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val day: Flow<Day> = combine(
        clock.minutes,
        clock.minutes.map { it.toLocalDate() }.distinctUntilChanged().flatMapLatest(agenda::eventsOn),
        weather.forecast,
        access.granted
    ) { now, events, forecast, granted ->
        // Without the user's agreement, nothing of the calendar is shown, whatever the source says.
        val allowed = Access.Calendar in granted
        Day(now, if (allowed) events else emptyList(), forecast, allowed)
    }.onEach { shown = it }

    private val drawer: Flow<Drawer> = combine(
        messages.conversations,
        home.controls,
        settings.settings,
        access.granted
    ) { conversations, controls, current, granted ->
        // Without the user's agreement, nothing of the conversations is shown, whatever the source says.
        val allowed = Access.Messages in granted
        Drawer(if (allowed) conversations else emptyList(), controls, current, allowed)
    }

    /**
     * What plays, once the user has agreed to let Bercail see it. When the content goes away, the Listen page goes
     * with it, and does not come back by itself; when a content comes, the « Écouter » sheet has done its job.
     */
    private val listening: Flow<Listening> = combine(listen.media.state, access.granted) { playing, granted ->
        val allowed = Access.Notifications in granted
        Listening(if (allowed) playing else MediaState.None, allowed)
    }.onEach {
        remote.shown = it.media
        if (it.media.isLoaded) remote.sheetOpen.value = false else wantedPage.value = HomePage.Home
        (it.media as? MediaState.Loaded)?.content?.let(::remember)
    }

    /** What was last noted as heard: the same content is not written twice. */
    private var noted: LastHeard? = null

    /** The media apps of the phone, looked for only while the « Écouter » sheet is open. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val mediaApps: Flow<List<MediaAppRow>> = remote.sheetOpen.flatMapLatest { open ->
        val chosen = settings.settings.map { it.mainMediaApp }.distinctUntilChanged()
        if (open) {
            combine(listen.mediaApps.apps, chosen, listen.mediaApps.lastHeard, ::mediaAppRows)
        } else {
            flowOf(emptyList())
        }
    }

    private val choices: Flow<Choices> = combine(wantedPage, drawerChoice, daySheet, remote.sheetOpen, ::Choices)

    val state: StateFlow<HomeUiState> = combine(day, drawer, listening, choices, mediaApps, ::stateOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HomeUiState())

    /** Where the content is. Only the remote of the Listen page collects it. */
    val position: StateFlow<Duration> =
        listen.media.position.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Duration.ZERO)

    override fun onDrawerExpandedChange(expanded: Boolean) {
        drawerChoice.value = expanded
    }

    override fun onPageWanted(page: HomePage) {
        wantedPage.value = page
    }

    override fun onDayOpenChange(open: Boolean, focus: String?) {
        daySheet.value = DaySheet(open, focus.takeIf { open })
    }

    override fun onOpenEvent(id: String) {
        shown?.events?.firstOrNull { it.id == id }?.let(agenda::open)
    }

    override fun onOpenCalendar() {
        shown?.let { agenda.openCalendar(it.now.toLocalDate()) }
    }

    override fun onToggleControl(id: String) {
        viewModelScope.launch { home.toggle(id) }
    }

    /** Keeps what plays for the day its app is gone: the main app will offer to play it again. */
    private fun remember(content: MediaContent) {
        val heard = LastHeard(content.appId, content.title)
        if (content.appId.isEmpty() || heard == noted) return
        noted = heard
        viewModelScope.launch { listen.mediaApps.remember(heard) }
    }

    private fun stateOf(
        day: Day,
        drawer: Drawer,
        listening: Listening,
        choices: Choices,
        mediaApps: List<MediaAppRow>
    ): HomeUiState {
        val media = listening.media
        val part = DayPart.at(day.now.toLocalTime())
        val favorites = favoritesAmong(drawer.conversations, drawer.settings.favoritePeople)
        val weather = day.forecast?.let { weatherAt(day.now, it) }
        return HomeUiState(
            glance = if (day.agendaAllowed) glanceAt(day.now, day.events) else closedGlance,
            weatherLine = weather?.line,
            weather = weather?.condition ?: WeatherCondition.Clear,
            dayPart = part,
            dayLine = dayLineAt(day.now, day.events),
            timeline = drawer.settings.timeline,
            agendaAllowed = day.agendaAllowed,
            dayOpen = choices.day.open,
            dayDate = day.now.toLocalDate().inWords(),
            dayRows = dayRowsAt(day.now, day.events, choices.day.focus),
            messages = messagesSummary(drawer.conversations),
            messagesAllowed = drawer.messagesAllowed,
            favorites = favorites,
            otherUnread = unreadOutsideFavorites(drawer.conversations, favorites.map { it.id }),
            controls = drawer.controls,
            homeSummary = homeSummary(drawer.controls),
            media = media,
            mediaAllowed = listening.allowed,
            listenOpen = choices.listen,
            mediaApps = mediaApps,
            page = choices.page.allowedWith(media),
            drawerExpanded = choices.drawerExpanded ?: drawer.settings.drawerExpandedAtStart,
            background = backgroundFor(media, drawer.settings.idleBackground),
            loaded = true
        )
    }

    private data class Day(
        val now: LocalDateTime,
        val events: List<AgendaEvent>,
        val forecast: Forecast?,
        val agendaAllowed: Boolean
    )

    private data class Drawer(
        val conversations: List<Conversation>,
        val controls: List<HomeControl>,
        val settings: Settings,
        val messagesAllowed: Boolean
    )

    /** The « Aujourd'hui » sheet as the user left it. [focus] is the appointment the search pointed at. */
    private data class DaySheet(val open: Boolean = false, val focus: String? = null)

    /** What plays, and whether the user lets Bercail see it. */
    private data class Listening(val media: MediaState, val allowed: Boolean)

    /** What the user chose on the screen: the page, the drawer, the sheets of the day and of the media apps. */
    private data class Choices(val page: HomePage, val drawerExpanded: Boolean?, val day: DaySheet, val listen: Boolean)
}
