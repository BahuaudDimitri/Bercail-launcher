package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBand
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBandSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRing
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingTone
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage

/** What the band holds above the search bar. */
private enum class BandContent { Summary, Search }

/**
 * The bottom of Accueil: the flat band (the summary line and the search bar; the search list when the bar is
 * touched), and the sheet of people and house that rises over it and covers it, search bar included. The search bar
 * never moves. The sheet goes down as Écoute opens, and comes back with Accueil.
 */
@Composable
internal fun HomeDrawer(
    state: HomeUiState,
    actions: HomeActions,
    links: HomeLinks,
    search: HomeSearch?,
    frost: BcFrost?,
    modifier: Modifier = Modifier
) {
    val listening = state.page == HomePage.Listen
    val searching = search?.open == true
    val sheetOpen = state.drawerExpanded && !listening && !searching
    val fade = BcTheme.motion.fadeMillis
    ClosesSearch(search)
    Box(modifier.testTag(DRAWER_TAG)) {
        // Under the sheet, the band fades away: nothing shows through the glass, nothing is read out.
        AnimatedVisibility(
            visible = !sheetOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(fade)),
            exit = fadeOut(tween(fade))
        ) {
            BcBand(
                onOpen = { actions.onDrawerExpandedChange(true) },
                searching = searching,
                // Pulled down, the search closes as the arrow does: the field emptied, the keyboard put away.
                onCloseSearch = search?.let { s -> { s.onOpenChange(false) } },
                frost = frost
            ) {
                val content = if (searching) BandContent.Search else BandContent.Summary
                AnimatedContent(
                    targetState = content,
                    modifier = if (searching) Modifier.weight(1f) else Modifier,
                    // The band animates its own height: the content only fades.
                    transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) using null },
                    label = "band content"
                ) { shown ->
                    when (shown) {
                        BandContent.Summary -> Summary(state, actions, links)
                        BandContent.Search -> search?.list?.invoke()
                    }
                }
                SearchBar(search)
            }
        }
        BcBandSheet(
            visible = sheetOpen,
            onClose = { actions.onDrawerExpandedChange(false) },
            modifier = Modifier.align(Alignment.BottomCenter),
            frost = frost
        ) {
            Essentials(state, actions, links)
        }
    }
}

/** The system back gesture closes the search; a closed search gives the keyboard back. */
@Composable
private fun ClosesSearch(search: HomeSearch?) {
    val focus = LocalFocusManager.current
    val open = search?.open == true
    BackHandler(enabled = open) { search?.onOpenChange?.invoke(false) }
    LaunchedEffect(open) { if (!open) focus.clearFocus() }
}

/**
 * The folded band above the search bar: the summary line (who wrote, then what is on in the house; touching it
 * opens the sheet, sliding it to the right shows the messaging app at once), then the dock.
 */
@Composable
private fun Summary(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        SummaryLine(
            state = state,
            onClick = { actions.onDrawerExpandedChange(true) },
            onClickLabel = OPEN_SHEET,
            onOpenApp = links::onOpenMessagesApp
        )
        if (state.dock.isNotEmpty()) {
            Dock(state.dock, state.dockIconPack, state.dockIcons, state.dockMenu, actions, actions::onOpenApp)
        }
    }
}

/** The sheet over the band: the favorite people and "Tous", the home controls and "Tout". */
@Composable
private fun Essentials(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        BcSectionLabel("Gens")
        PeopleRow(state, actions, links)
        BcSectionLabel("Maison")
        BcRingRow {
            state.controls.forEach { control ->
                BcRing(
                    name = control.name,
                    state = control.state,
                    mark = BcRingMark.Icon(control.icon),
                    pastel = control.pastel,
                    on = control.isOn,
                    onToggle = { actions.onToggleControl(control.id) }
                )
            }
            BcRing("Tout", BcRingMark.Icon(BcIcons.House), onClick = links::onOpenHomeApp, tone = BcRingTone.Off)
        }
    }
}

private const val OPEN_SHEET = "Ouvrir la feuille"
private val ROW_GAP = 14.dp

/**
 * The search bar, which is the field itself: touching it opens the search. Focus alone does not, because Android
 * may hand the focus back to the field once the search is closed. The search opens when the finger lifts, so that
 * the field still receives the whole touch.
 */
@Composable
private fun SearchBar(search: HomeSearch?) {
    val open by rememberUpdatedState { if (search?.open == false) search.onOpenChange(true) }
    BcSearchField(
        value = search?.query.orEmpty(),
        onValueChange = { search?.onQueryChange?.invoke(it) },
        onSearch = { search?.onSubmit?.invoke() },
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (waitForUpOrCancellation(PointerEventPass.Initial) != null) open()
            }
        },
        enabled = search != null
    )
}
