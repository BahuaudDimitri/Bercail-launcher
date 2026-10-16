package io.github.bahuauddimitri.bercail.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaSource
import io.github.bahuauddimitri.bercail.core.domain.apps.AppsSource
import io.github.bahuauddimitri.bercail.core.domain.apps.Phone
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import io.github.bahuauddimitri.bercail.core.domain.messages.FoundMessage
import io.github.bahuauddimitri.bercail.core.domain.messages.MessagesSource
import io.github.bahuauddimitri.bercail.core.domain.search.AppDirectory
import io.github.bahuauddimitri.bercail.core.domain.search.SearchResults
import io.github.bahuauddimitri.bercail.core.domain.search.SearchScope
import io.github.bahuauddimitri.bercail.core.domain.search.appDirectory
import io.github.bahuauddimitri.bercail.core.domain.search.search
import io.github.bahuauddimitri.bercail.core.domain.search.searchesMessages
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.domain.time.Clock
import io.github.bahuauddimitri.bercail.core.domain.update.Updates
import io.github.bahuauddimitri.bercail.core.domain.update.updateNotice
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/** What the search shows: every app from A to Z, or what the typed letters find. */
data class SearchUiState(
    val open: Boolean = false,
    val query: String = "",
    val directory: AppDirectory = AppDirectory(dock = emptyList(), sections = emptyList()),
    /** The favorite people, who keep their own pastel everywhere. */
    val favoritePeople: List<String> = emptyList(),
    /** Absent until something is typed. */
    val results: SearchResults? = null,
    /** What the row of Bercail's settings announces: a version waiting to be installed, or nothing. */
    val settingsNote: String? = null
)

/** What the search does by itself. */
interface SearchActions {
    fun onOpenChange(open: Boolean)
    fun onQueryChange(query: String)
    fun onOpenApp(appId: String)

    /** The keyboard's search key: opens the first app found, if any. */
    fun onSubmit()
    fun onToggleControl(id: String)
    fun onSearchWeb()
    fun onSearchStore()
}

/** What the search asks others to open. */
interface SearchLinks {
    fun onOpenSettings() = Unit
    fun onOpenConversation(id: String) = Unit

    /** Shows the day with this appointment brought forward. */
    fun onOpenDay(eventId: String) = Unit

    object None : SearchLinks
}

/**
 * The search of the drawer. Its sources (apps, people, house, agenda) are only listened to while it is open;
 * what they said last is kept, so that the list is there at once the next time.
 */
class SearchViewModel(
    private val apps: AppsSource,
    private val phone: Phone,
    clock: Clock,
    agenda: AgendaSource,
    private val messages: MessagesSource,
    private val home: HomeSource,
    settings: SettingsSource,
    updates: Updates
) : ViewModel(),
    SearchActions {
    private val open = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val known = MutableStateFlow(Known())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listening: Flow<Unit> = open
        .flatMapLatest { isOpen ->
            if (!isOpen) return@flatMapLatest emptyFlow()
            combine(
                apps.apps,
                messages.conversations,
                home.controls,
                agenda.eventsOn(clock.now().toLocalDate()),
                settings.settings
            ) { installed, people, controls, events, current ->
                Known(SearchScope(installed, people, controls, events), current.dockApps, current.favoritePeople)
            }
        }
        .onEach { known.value = it }
        .map { }
        .onStart { emit(Unit) }

    /**
     * The messages holding what is typed. The messaging app is only asked once the typing has paused, and what it
     * found for other words is dropped at once.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val found: Flow<List<FoundMessage>> = query.transformLatest { typed ->
        emit(emptyList())
        if (searchesMessages(typed)) {
            delay(TYPING_PAUSE)
            emit(messages.search(typed.trim()))
        }
    }

    private val typing: Flow<Typing> = combine(query, found, ::Typing)

    val state: StateFlow<SearchUiState> =
        combine(open, typing, known, updates.status, listening) { isOpen, typing, known, update, _ ->
            val typed = typing.query
            SearchUiState(
                open = isOpen,
                query = typed,
                directory = appDirectory(known.scope.apps, known.dockApps),
                favoritePeople = known.favoritePeople,
                results = if (typed.isBlank()) null else search(typed, known.scope.copy(messages = typing.found)),
                settingsNote = updateNotice(update)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SearchUiState())

    /** The icons of the apps, drawn once and kept for the rows that show them. */
    val icons = AppIcons(apps)

    override fun onOpenChange(open: Boolean) {
        if (!open) query.value = ""
        this.open.value = open
    }

    override fun onQueryChange(query: String) {
        if (open.value) this.query.value = query
    }

    override fun onOpenApp(appId: String) {
        apps.open(appId)
        onOpenChange(false)
    }

    override fun onSubmit() {
        state.value.results?.apps?.firstOrNull()?.let { onOpenApp(it.id) }
    }

    override fun onToggleControl(id: String) {
        viewModelScope.launch { home.toggle(id) }
    }

    override fun onSearchWeb() = sendTyped(phone::searchWeb)

    override fun onSearchStore() = sendTyped(phone::searchStore)

    private fun sendTyped(send: (String) -> Unit) {
        val typed = query.value.trim()
        if (typed.isEmpty()) return
        send(typed)
        onOpenChange(false)
    }

    /** What is typed, and the messages found for it so far. */
    private data class Typing(val query: String, val found: List<FoundMessage>)

    private data class Known(
        val scope: SearchScope = SearchScope(),
        val dockApps: List<String> = emptyList(),
        val favoritePeople: List<String> = emptyList()
    )

    private companion object {
        /** How long the typing must pause before the messaging app is asked. */
        val TYPING_PAUSE = 300.milliseconds
    }
}
