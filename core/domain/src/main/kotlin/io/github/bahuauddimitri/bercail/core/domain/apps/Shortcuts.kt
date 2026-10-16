package io.github.bahuauddimitri.bercail.core.domain.apps

/**
 * A shortcut an app offers into itself (« Nouvel onglet », « Nouveau message »), shown on a long press on its icon
 * in the dock. [id] names it within its app; [rank] is the place the app gives it among those of its [kind].
 */
data class AppShortcut(val appId: String, val id: String, val label: String, val kind: Kind, val rank: Int = 0) {
    /** Where the shortcut comes from, in the order they are shown. */
    enum class Kind {
        /** Declared once and for all by the app. */
        Manifest,

        /** Offered by the app as it is used: the last conversations, the last places. */
        Dynamic,

        /** Pinned by the user on a home screen. */
        Pinned
    }
}

/** The room of the menu of a dock app: as many shortcuts as the Pixel's own launcher shows. */
const val MAX_SHORTCUTS = 4

/**
 * The shortcuts the menu of a dock app shows: those the app declares first, then those it offers as it is used,
 * then the pinned ones, each in the order the app gives; each once, never one without a name, four at most. None:
 * no menu.
 */
fun shortcutsShown(offered: List<AppShortcut>): List<AppShortcut> = offered
    .filter { it.label.isNotBlank() }
    .sortedWith(compareBy<AppShortcut> { it.kind }.thenBy { it.rank })
    .distinctBy { it.id }
    .take(MAX_SHORTCUTS)
