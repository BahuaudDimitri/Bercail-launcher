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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBand
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcBandSheet
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRing
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingMark
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRingTone
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSearchField
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcSectionLabel
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.asTrackTime
import io.github.bahuauddimitri.bercail.core.domain.media.progress
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

/** What the band holds above the search bar. */
private enum class BandContent { Summary, Remote, Search }

/**
 * The bottom of the home: the flat band (the summary line and the search bar; the remote on the Listen screen; the
 * search list when the bar is touched), and the sheet of people and house that rises over it and covers it, search
 * bar included. The search bar never moves.
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
                onOpen = if (listening) null else ({ actions.onDrawerExpandedChange(true) }),
                searching = searching
            ) {
                val content = when {
                    searching -> BandContent.Search
                    listening -> BandContent.Remote
                    else -> BandContent.Summary
                }
                AnimatedContent(
                    targetState = content,
                    modifier = if (searching) Modifier.weight(1f) else Modifier,
                    // The band animates its own height: the content only fades.
                    transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) using null },
                    label = "band content"
                ) { shown ->
                    when (shown) {
                        BandContent.Summary -> Summary(state, links, onOpen = { actions.onDrawerExpandedChange(true) })
                        BandContent.Remote -> Remote(state.media, position, actions)
                        BandContent.Search -> search?.list?.invoke()
                    }
                }
                SearchBar(search)
            }
        }
        BcBandSheet(
            visible = sheetOpen,
            onClose = { actions.onDrawerExpandedChange(false) },
            modifier = Modifier.align(Alignment.BottomCenter)
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
 * The summary line of the band: who wrote, then what is on in the house. Touching it opens the sheet; sliding it to
 * the right shows the messaging app at once.
 */
@Composable
private fun Summary(state: HomeUiState, links: HomeLinks, onOpen: () -> Unit) {
    SummaryLine(state, onClick = onOpen, onClickLabel = OPEN_SHEET, onOpenApp = links::onOpenMessagesApp)
}

/** The sheet over the band: the favorite people and "Tous", the home controls and "Tout". */
@Composable
private fun Essentials(state: HomeUiState, actions: HomeActions, links: HomeLinks) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        BcSectionLabel("Gens")
        PeopleRow(state, actions, links)
        BcSectionLabel("Maison")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

/**
 * The band as a remote: where the content is (touch to move), previous, play or pause, next. Only the buttons
 * the app accepts are there: YouTube, for one, has no "previous".
 */
@Composable
private fun Remote(media: MediaState, position: StateFlow<Duration>, actions: HomeActions) {
    val loaded = media as? MediaState.Loaded ?: return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BcSpacing.m)) {
        // A live stream has no end: nothing to measure the progress against.
        if (loaded.content.duration > Duration.ZERO) Progress(loaded, position, actions)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = BcSpacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quiet = BcButtonVariant.Ghost
            // A missing button leaves its room empty: play and pause stay in the middle.
            Box(Modifier.size(SIDE_BUTTON)) {
                if (MediaAction.Previous in loaded.actions) {
                    BcIconButton(
                        BcIcons.Previous,
                        "Précédent",
                        actions::onPrevious,
                        variant = quiet,
                        size = SIDE_BUTTON
                    )
                }
            }
            BcIconButton(
                icon = if (loaded.playing) BcIcons.Pause else BcIcons.Play,
                contentDescription = if (loaded.playing) "Pause" else "Lecture",
                onClick = actions::onPlayPause,
                variant = BcButtonVariant.Light,
                size = MAIN_BUTTON
            )
            Box(Modifier.size(SIDE_BUTTON)) {
                if (MediaAction.Next in loaded.actions) {
                    BcIconButton(BcIcons.Next, "Suivant", actions::onNext, variant = quiet, size = SIDE_BUTTON)
                }
            }
        }
    }
}

/** Where the content is. Touching the bar moves in the content, if its app lets the remote do it. */
@Composable
private fun Progress(loaded: MediaState.Loaded, position: StateFlow<Duration>, actions: HomeActions) {
    // Followed here only: the position costs nothing while the remote is not on screen.
    val now by position.collectAsStateWithLifecycle()
    val seek: ((Float) -> Unit)? = if (MediaAction.Seek in loaded.actions) actions::onSeek else null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TIME_GAP)) {
        BcText(now.asTrackTime(), style = BcTextStyle.LabelSmall, color = BcTextColor.Muted)
        BcProgress(progress(now, loaded.content.duration), onSeek = seek, modifier = Modifier.weight(1f))
        BcText(loaded.content.duration.asTrackTime(), style = BcTextStyle.LabelSmall, color = BcTextColor.Muted)
    }
}

private const val OPEN_SHEET = "Ouvrir la feuille"
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
