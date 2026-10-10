package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcDrawer
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcHomeToggle
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPersonTile
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryLeading
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSummaryRow
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcPastel
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.asTrackTime
import io.github.bahuauddimitri.bercail.core.domain.media.progress
import io.github.bahuauddimitri.bercail.core.domain.messages.allMessagesDescription
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

/** What the drawer holds above the search bar. */
private enum class DrawerContent { Summary, Essentials, Remote, Search }

/**
 * The single glass drawer of the home: two summary lines when folded, people and house when open, the remote
 * on the Listen screen, and the search list when the bar is focused. Its height follows its content; the search
 * bar never moves.
 */
@Composable
internal fun HomeDrawer(
    state: HomeUiState,
    position: StateFlow<Duration>,
    actions: HomeActions,
    links: HomeLinks,
    search: HomeSearch?,
    modifier: Modifier = Modifier
) {
    val listening = state.page == HomePage.Listen
    val searching = search?.open == true
    val fade = BcTheme.motion.fadeMillis
    ClosesSearch(search)
    BcDrawer(
        expanded = state.drawerExpanded && !listening,
        onExpandedChange = actions::onDrawerExpandedChange,
        modifier = modifier.testTag(DRAWER_TAG),
        searching = searching,
        handle = !listening
    ) {
        val content = when {
            searching -> DrawerContent.Search
            listening -> DrawerContent.Remote
            state.drawerExpanded -> DrawerContent.Essentials
            else -> DrawerContent.Summary
        }
        AnimatedContent(
            targetState = content,
            modifier = if (searching) Modifier.weight(1f) else Modifier,
            // The drawer animates its own height: the content only fades.
            transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) using null },
            label = "drawer content"
        ) { shown ->
            when (shown) {
                DrawerContent.Summary -> Summary(state, onExpand = { actions.onDrawerExpandedChange(true) })
                DrawerContent.Essentials -> Essentials(state, actions, links)
                DrawerContent.Remote -> Remote(state.media, position, actions)
                DrawerContent.Search -> search?.list?.invoke()
            }
        }
        SearchBar(search)
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

/** Folded drawer: who wrote, and what is on in the house. Touching a line opens the drawer. */
@Composable
private fun Summary(state: HomeUiState, onExpand: () -> Unit) {
    val favoriteIds = state.favorites.map { it.id }
    Column(Modifier.fillMaxWidth()) {
        BcSummaryRow(
            text = state.messages.detail,
            strong = state.messages.headline,
            leading = BcSummaryLeading.Dots(state.messages.senders.map { pastelOf(it.id, favoriteIds) }),
            onClick = onExpand,
            onClickLabel = OPEN_DRAWER
        )
        BcSummaryRow(
            text = state.homeSummary,
            leading = BcSummaryLeading.Icon(BcIcons.House),
            onClick = onExpand,
            onClickLabel = OPEN_DRAWER
        )
    }
}

/** Open drawer: the favorite people and "Tous", the home controls and "Tout". */
@Composable
private fun Essentials(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    val favoriteIds = state.favorites.map { it.id }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.favorites.forEach { person ->
                BcPersonTile(
                    name = person.name,
                    initials = person.initials,
                    pastel = pastelOf(person.id, favoriteIds),
                    unread = person.unread,
                    onClick = { links.onOpenConversation(person.id) },
                    description = person.description
                )
            }
            BcPersonTile(
                name = "Tous",
                initials = "",
                pastel = null,
                unread = state.otherUnread,
                onClick = links::onOpenMessagesApp,
                description = allMessagesDescription(state.otherUnread),
                icon = BcIcons.Chat
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.controls.forEach { control ->
                BcHomeToggle(
                    name = control.name,
                    state = control.state,
                    icon = control.icon,
                    pastel = control.pastel,
                    on = control.isOn,
                    onToggle = { actions.onToggleControl(control.id) }
                )
            }
            BcHomeToggle("Tout", "l'app", BcIcons.House, BcPastel.Apricot, on = false, onToggle = links::onOpenHomeApp)
        }
    }
}

/** The drawer as a remote: where the content is (touch to move), previous, play or pause, next. */
@Composable
private fun Remote(media: MediaState, position: StateFlow<Duration>, actions: HomeActions) {
    val loaded = media as? MediaState.Loaded ?: return
    // Followed here only: the position costs nothing while the remote is not on screen.
    val now by position.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TIME_GAP)) {
            BcText(now.asTrackTime(), style = BcTextStyle.LabelSmall, color = BcTextColor.Muted)
            BcProgress(progress(now, loaded.content.duration), onSeek = actions::onSeek, modifier = Modifier.weight(1f))
            BcText(loaded.content.duration.asTrackTime(), style = BcTextStyle.LabelSmall, color = BcTextColor.Muted)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = BcSpacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quiet = BcButtonVariant.Ghost
            BcIconButton(BcIcons.Previous, "Précédent", actions::onPrevious, variant = quiet, size = SIDE_BUTTON)
            BcIconButton(
                icon = if (loaded.playing) BcIcons.Pause else BcIcons.Play,
                contentDescription = if (loaded.playing) "Pause" else "Lecture",
                onClick = actions::onPlayPause,
                variant = BcButtonVariant.Light,
                size = MAIN_BUTTON
            )
            BcIconButton(BcIcons.Next, "Suivant", actions::onNext, variant = quiet, size = SIDE_BUTTON)
        }
    }
}

private const val OPEN_DRAWER = "Ouvrir le tiroir"
private val ROW_GAP = 14.dp
private val TIME_GAP = 10.dp
private val SIDE_BUTTON = 52.dp
private val MAIN_BUTTON = 66.dp

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
