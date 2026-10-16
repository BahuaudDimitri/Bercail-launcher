package io.github.bahuauddimitri.bercail.core.domain.search

import io.github.bahuauddimitri.bercail.core.domain.agenda.AgendaEvent
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.apps.dockOf
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.messages.Conversation
import io.github.bahuauddimitri.bercail.core.domain.messages.FoundMessage
import java.text.Normalizer

/** The apps under one letter of the alphabet. */
data class AppSection(val letter: String, val apps: List<App>)

/** What the search list shows before anything is typed: the dock, then every app from A to Z. */
data class AppDirectory(val dock: List<App>, val sections: List<AppSection>) {
    /** The alphabet along the edge: only the letters that have at least one app. */
    val letters: List<String> get() = sections.map { it.letter }
}

fun appDirectory(apps: List<App>, dockIds: List<String>): AppDirectory {
    val sections = apps
        .sortedBy { it.name.searchable() }
        .groupBy { it.letter() }
        .map { (letter, sameLetter) -> AppSection(letter, sameLetter) }
        .sortedBy { it.letter }
    return AppDirectory(
        dock = dockOf(apps, dockIds),
        sections = sections
    )
}

/** Everything a search looks into. */
data class SearchScope(
    val apps: List<App> = emptyList(),
    val people: List<Conversation> = emptyList(),
    val controls: List<HomeControl> = emptyList(),
    val events: List<AgendaEvent> = emptyList(),

    /** What the messaging app found for the typed words; it is asked apart, a moment after the last letter. */
    val messages: List<FoundMessage> = emptyList()
)

/** One line of the « Messages » family: who the conversation is with, and what was written. */
data class MessageLine(val person: Conversation, val text: String)

/** What a few typed letters find, family by family. [settings] is true when they point to Bercail's settings. */
data class SearchResults(
    val apps: List<App> = emptyList(),
    val people: List<Conversation> = emptyList(),
    val messages: List<MessageLine> = emptyList(),
    val controls: List<HomeControl> = emptyList(),
    val events: List<AgendaEvent> = emptyList(),
    val settings: Boolean = false
) {
    /** Nothing on the phone: time to offer the web and the Play Store. */
    val isEmpty: Boolean
        get() = listOf(apps, people, messages, controls, events).all { it.isEmpty() } && !settings
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
        messages = messageLines(scope.messages, scope.people),
        controls = scope.controls.filter { it.name.matches() },
        events = scope.events.filter { it.title.matches() },
        settings = typed.length >= SETTINGS_MIN_LENGTH &&
            SETTINGS_WORDS.any { it.startsWith(typed) || typed.startsWith(it) }
    )
}

/**
 * Where [query] sits in [text] as it is written, as the search finds it (whatever the accents and capitals): the
 * letters a row shows as found. Nothing when the text does not hold it, or when nothing is typed.
 */
fun foundIn(text: String, query: String): IntRange? {
    val typed = query.searchable()
    // The text as the search compares it, with the place each of its letters was written at.
    val written = mutableListOf<Int>()
    val compared = buildString {
        text.forEachIndexed { index, letter ->
            letter.toString().comparable().forEach {
                append(it)
                written += index
            }
        }
    }
    val start = if (typed.isEmpty()) -1 else compared.indexOf(typed)
    if (start < 0) return null
    var end = written[start + typed.length - 1]
    // An accent written apart, after the last letter found, belongs to it.
    while (end + 1 < text.length && text[end + 1].toString().comparable().isEmpty()) end++
    return written[start]..end
}

/** The messaging app is only asked once enough is typed to mean something. */
fun searchesMessages(query: String): Boolean = query.searchable().length >= MESSAGES_MIN_LENGTH

/**
 * The messages found, the most recent first, each under the person it was exchanged with. A message of a
 * conversation the home screen does not know cannot be opened from here: it is left out.
 */
private fun messageLines(found: List<FoundMessage>, people: List<Conversation>): List<MessageLine> = found
    .sortedByDescending { it.at }
    .mapNotNull { message ->
        people.firstOrNull { it.id == message.conversationId }?.let { person ->
            MessageLine(person, if (message.mine) "$MINE : ${message.text}" else message.text)
        }
    }
    .take(MOST_MESSAGES)

/** A text as the search compares it: no accents, no capitals, no spaces around. */
internal fun String.searchable(): String = trim().comparable()

/** A text without accents nor capitals, its spaces kept. */
private fun String.comparable(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(ACCENTS, "").lowercase()

private fun App.letter(): String {
    val first = name.searchable().firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first.toString() else OTHER_LETTER
}

private val ACCENTS = Regex("\\p{Mn}+")
private val SETTINGS_WORDS = listOf("reglages", "parametres", "bercail", "fond", "launcher", "tiroir", "settings")
private const val SETTINGS_MIN_LENGTH = 3
private const val MESSAGES_MIN_LENGTH = 3
private const val MOST_MESSAGES = 5
private const val MINE = "Toi"

/** Where apps whose name starts with a digit or a symbol go. */
private const val OTHER_LETTER = "#"
