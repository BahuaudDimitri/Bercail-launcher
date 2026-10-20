package io.github.bahuauddimitri.bercail.core.domain.apps

import io.github.bahuauddimitri.bercail.core.domain.search.searchable

/** The icon an app of the dock is drawn with, chosen in Réglages › Dock. */
sealed interface DockIcon {
    /** « Icône du pack », the default: what the dock's icon pack gives it, or else its own glyph. */
    data object FromPack : DockIcon

    /** « Dessin d'origine »: its own glyph in one colour, whatever the pack. */
    data object Origin : DockIcon

    /** One drawing of an installed icon pack; while that pack or drawing is gone, the default stands in. */
    data class Drawing(val icon: PackIcon) : DockIcon
}

/** The pack the dock asks to draw the app with: none for « Dessin d'origine ». */
fun DockIcon.packFor(iconPack: String?): String? = if (this == DockIcon.Origin) null else iconPack

/** The drawing chosen for the app, if one was. */
val DockIcon.drawing: PackIcon? get() = (this as? DockIcon.Drawing)?.icon

/** One drawing of an icon pack: the package of the [pack] and the [name] of its drawable. */
data class PackIcon(val pack: String, val name: String) {
    /** Its name made readable, for the screen reader and the search: « google_maps » reads « google maps ». */
    val label: String get() = name.replace('_', ' ').trim()
}

/**
 * What an icon pack holds: the names of every drawing it offers ([icons], from its `drawable.xml`, or else from its
 * `appfilter`), and the drawing its `appfilter` gives each app, by the app's id ([apps]).
 */
data class PackCatalog(val pack: IconPack, val icons: List<String>, val apps: Map<String, String>)

/** The drawings of one pack the picker shows, from A to Z. */
data class PackIcons(val pack: IconPack, val icons: List<PackIcon>)

/** What the icon picker of an app shows: the drawings proposed for it, then every drawing, pack by pack. */
data class IconChoices(val proposed: List<PackIcon>, val packs: List<PackIcons>)

/**
 * The drawings the [catalogs] offer for the [app], those whose name holds the [query] (capitals and accents aside).
 * Proposed first: those the packs' `appfilter` give the app, then those named after it. Then every drawing, pack by
 * pack, from A to Z; a pack with none left is not shown.
 */
fun iconChoices(app: App, catalogs: List<PackCatalog>, query: String): IconChoices {
    val wanted = query.searchable()
    // Each name is made comparable once: a pack holds thousands of drawings.
    val found = catalogs.map { catalog ->
        catalog.icons.distinct()
            .map { name -> PackIcon(catalog.pack.id, name).let { Named(it, it.label.searchable()) } }
            .filter { it.key.contains(wanted) }
            .sortedBy { it.key }
    }
    val appName = app.name.searchable().withoutSpaces()
    val mapped = catalogs.mapNotNull { catalog -> catalog.apps[app.id]?.let { PackIcon(catalog.pack.id, it) } }
    val named = found.flatten().filter { it.key.withoutSpaces().contains(appName) }.map { it.icon }
    return IconChoices(
        proposed = (mapped.filter { it.label.searchable().contains(wanted) } + named).distinct(),
        packs = catalogs.zip(found) { catalog, icons -> PackIcons(catalog.pack, icons.map { it.icon }) }
            .filter { it.icons.isNotEmpty() }
    )
}

/** A drawing with its name as the search compares it. */
private class Named(val icon: PackIcon, val key: String)

private fun String.withoutSpaces() = replace(" ", "")
