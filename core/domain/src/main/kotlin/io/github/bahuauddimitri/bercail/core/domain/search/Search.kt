package io.github.bahuauddimitri.bercail.core.domain.search

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import java.text.Normalizer

/** The apps under one letter of the alphabet. */
data class AppSection(val letter: String, val apps: List<App>)

/** What the search list shows before anything is typed: favorite apps, then every app from A to Z. */
data class AppDirectory(val favorites: List<App>, val sections: List<AppSection>) {
    /** The alphabet along the edge: only the letters that have at least one app. */
    val letters: List<String> get() = sections.map { it.letter }
}

fun appDirectory(apps: List<App>, favoriteIds: List<String>): AppDirectory {
    val sections = apps
        .sortedBy { it.name.searchable() }
        .groupBy { it.letter() }
        .map { (letter, sameLetter) -> AppSection(letter, sameLetter) }
        .sortedBy { it.letter }
    return AppDirectory(
        favorites = favoriteIds.mapNotNull { id -> apps.firstOrNull { it.id == id } },
        sections = sections
    )
}

/** Everything a search looks into. */
data class SearchScope(
    val apps: List<App> = emptyList(),
    val people: List<Conversation> = emptyList(),
    val controls: List<HomeControl> = emptyList(),
    val events: List<AgendaEvent> = emptyList()
)

/** What a few typed letters find, family by family. [settings] is true when they point to Bercail's settings. */
data class SearchResults(
    val apps: List<App> = emptyList(),
    val people: List<Conversation> = emptyList(),
    val controls: List<HomeControl> = emptyList(),
    val events: List<AgendaEvent> = emptyList(),
    val settings: Boolean = false
) {
    /** Nothing on the phone: time to offer the web and the Play Store. */
    val isEmpty: Boolean
        get() = apps.isEmpty() && people.isEmpty() && controls.isEmpty() && events.isEmpty() && !settings
}

/** Finds [query] anywhere in a name, whatever its accents and capitals. Apps starting with it come first. */
fun search(query: String, scope: SearchScope): SearchResults {
    val typed = query.searchable()
    if (typed.isEmpty()) return SearchResults()
    fun String.matches() = typed in searchable()
    return SearchResults(
        apps = scope.apps
            .filter { it.name.matches() }
            .sortedWith(compareBy({ !it.name.searchable().startsWith(typed) }, { it.name.searchable() })),
        people = scope.people.filter { it.name.matches() },
        controls = scope.controls.filter { it.name.matches() },
        events = scope.events.filter { it.title.matches() },
        settings = typed.length >= SETTINGS_MIN_LENGTH &&
            SETTINGS_WORDS.any { it.startsWith(typed) || typed.startsWith(it) }
    )
}

/** A text as the search compares it: no accents, no capitals, no spaces around. */
internal fun String.searchable(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(ACCENTS, "").lowercase()

private fun App.letter(): String {
    val first = name.searchable().firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first.toString() else OTHER_LETTER
}

private val ACCENTS = Regex("\\p{Mn}+")
private val SETTINGS_WORDS = listOf("reglages", "parametres", "bercail", "fond", "launcher", "tiroir", "settings")
private const val SETTINGS_MIN_LENGTH = 3

/** Where apps whose name starts with a digit or a symbol go. */
private const val OTHER_LETTER = "#"
