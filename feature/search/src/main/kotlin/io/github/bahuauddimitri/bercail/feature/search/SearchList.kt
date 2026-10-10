package io.github.bahuauddimitri.bercail.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcAlphabetRail
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcListRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPanelHeader
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.domain.apps.App
import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.search.AppDirectory
import io.github.bahuauddimitri.bercail.core.domain.search.SearchResults
import io.github.bahuauddimitri.bercail.core.domain.time.asClockTime
import kotlinx.coroutines.launch

internal const val RAIL_TAG = "search-rail"
internal const val RESULTS_TAG = "search-results"

/** The search list plugged into its sources. They are only listened to while the search is open. */
@Composable
fun SearchRoute(viewModel: SearchViewModel, modifier: Modifier = Modifier, links: SearchLinks = SearchLinks.None) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchList(state, viewModel, viewModel.icons, modifier, links)
}

/**
 * What fills the drawer while searching: a header with the way back, then every app from A to Z with the
 * alphabet along the edge, or, once something is typed, the results family by family, anchored at the bottom.
 */
@Composable
fun SearchList(
    state: SearchUiState,
    actions: SearchActions,
    icons: AppIcons,
    modifier: Modifier = Modifier,
    links: SearchLinks = SearchLinks.None
) {
    val results = state.results
    Column(modifier.fillMaxSize()) {
        BcPanelHeader(
            title = if (results == null) "Toutes les apps" else "Résultats",
            onBack = { actions.onOpenChange(false) },
            backDescription = "Fermer la recherche"
        )
        if (results == null) {
            Directory(state.directory, icons, actions, links, Modifier.weight(1f))
        } else {
            Results(state, results, icons, actions, links, Modifier.weight(1f))
        }
    }
}

/** Every app: Bercail's settings first, the favorites, then the alphabet. */
@Composable
private fun Directory(
    directory: AppDirectory,
    icons: AppIcons,
    actions: SearchActions,
    links: SearchLinks,
    modifier: Modifier = Modifier
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Where each letter starts in the list: the settings row, the favorites and their title come before.
    val letterStarts = remember(directory) {
        var next = 1 + if (directory.favorites.isEmpty()) 0 else directory.favorites.size + 1
        directory.sections.associate { section -> section.letter to next.also { next += section.apps.size + 1 } }
    }
    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize().padding(end = RAIL_ROOM), state = list) {
            item(key = "settings") { SettingsRow(links::onOpenSettings) }
            if (directory.favorites.isNotEmpty()) {
                item(key = "favorites") { BcSectionLabel("Apps favorites") }
                items(directory.favorites, key = { "favorite ${it.id}" }) { AppRow(it, icons, actions::onOpenApp) }
            }
            directory.sections.forEach { section ->
                item(key = "letter ${section.letter}") { BcSectionLabel(section.letter) }
                items(section.apps, key = { it.id }) { AppRow(it, icons, actions::onOpenApp) }
            }
        }
        BcAlphabetRail(
            letters = directory.letters,
            onLetter = { letter -> letterStarts[letter]?.let { scope.launch { list.scrollToItem(it) } } },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = BcSpacing.xl)
                .testTag(RAIL_TAG)
        )
    }
}

/** What the typed letters find, anchored at the bottom, near the thumb; or the way to the web and the store. */
@Composable
private fun Results(
    state: SearchUiState,
    results: SearchResults,
    icons: AppIcons,
    actions: SearchActions,
    links: SearchLinks,
    modifier: Modifier = Modifier
) {
    val list = rememberLazyListState()
    // Each new result list shows its end first: the closest to the search bar.
    LaunchedEffect(results) { list.scrollToItem(Int.MAX_VALUE) }
    LazyColumn(modifier.fillMaxSize().testTag(RESULTS_TAG), state = list, verticalArrangement = Arrangement.Bottom) {
        if (results.isEmpty) {
            nothingFound(state.query.trim(), actions)
        } else {
            found(results, state.favoritePeople, icons, actions, links)
        }
    }
}

private fun LazyListScope.found(
    results: SearchResults,
    favoritePeople: List<String>,
    icons: AppIcons,
    actions: SearchActions,
    links: SearchLinks
) {
    family("Apps", results.apps, key = { "app ${it.id}" }) { AppRow(it, icons, actions::onOpenApp) }
    family("Gens", results.people, key = { "person ${it.id}" }) { person ->
        BcListRow(
            title = person.name,
            onClick = { links.onOpenConversation(person.id) },
            subtitle = person.network,
            leading = BcLeading.Initials(person.initials, BcPastel.forKey(person.id, favoritePeople))
        )
    }
    family("Maison", results.controls, key = { "control ${it.id}" }) { control ->
        BcListRow(
            title = control.name,
            onClick = { actions.onToggleControl(control.id) },
            subtitle = control.state,
            leading = BcLeading.Icon(control.icon)
        )
    }
    family("Agenda", results.events, key = { "event ${it.id}" }) { event ->
        BcListRow(
            title = event.title,
            onClick = links::onOpenDay,
            leading = BcLeading.Time(event.start.toLocalTime().asClockTime())
        )
    }
    if (results.settings) {
        item(key = "settings title") { BcSectionLabel("Réglages") }
        item(key = "settings") { SettingsRow(links::onOpenSettings) }
    }
}

private fun <T> LazyListScope.family(title: String, members: List<T>, key: (T) -> Any, row: @Composable (T) -> Unit) {
    if (members.isEmpty()) return
    item(key = "title $title") { BcSectionLabel(title) }
    items(members, key = key) { row(it) }
}

private fun LazyListScope.nothingFound(typed: String, actions: SearchActions) {
    item {
        BcText(
            "Rien sur le téléphone pour « $typed »",
            modifier = Modifier.padding(vertical = BcSpacing.m),
            style = BcTextStyle.Message,
            color = BcTextColor.Muted
        )
    }
    item {
        BcListRow("Chercher « $typed » sur le web", actions::onSearchWeb, leading = BcLeading.Icon(BcIcons.Web))
    }
    item {
        BcListRow(
            title = "Chercher « $typed » sur le Play Store",
            onClick = actions::onSearchStore,
            leading = BcLeading.Icon(BcIcons.Store)
        )
    }
}

@Composable
private fun SettingsRow(onOpen: () -> Unit) {
    BcListRow("Réglages de Bercail", onClick = onOpen, leading = BcLeading.Icon(BcIcons.Settings))
}

/** An app with its icon; until the icon is drawn, or without one, its initial on the neutral tile. */
@Composable
private fun AppRow(app: App, icons: AppIcons, onOpen: (String) -> Unit) {
    val icon by produceState(initialValue = icons.known(app.id), app.id) { value = icons.of(app.id) }
    BcListRow(
        title = app.name,
        onClick = { onOpen(app.id) },
        leading = icon?.let { BcLeading.Picture(it) } ?: BcLeading.Initials(app.name.take(1).uppercase())
    )
}

private val HomeControl.icon: BcIcons
    get() = when (this) {
        is HomeControl.Light -> BcIcons.Lamp
        is HomeControl.Scene -> BcIcons.Film
        is HomeControl.Shutters -> BcIcons.Shutters
        is HomeControl.Heating -> BcIcons.Flame
    }

/** The room the alphabet takes along the right edge. */
private val RAIL_ROOM = 44.dp
