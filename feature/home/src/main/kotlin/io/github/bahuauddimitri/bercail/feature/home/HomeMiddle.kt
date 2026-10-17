package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcMusicPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowSwipe
import io.github.bahuauddimitri.bercail.core.designsystem.motion.BcHeldTransition
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage

/**
 * The middle of Accueil: the music pill, with no page dots. While a content is loaded, sliding left brings Écoute in
 * along [transition], as far as the finger goes across the screen; letting go past half way, or with a flick, lets
 * Écoute take the whole screen, otherwise Accueil comes back. The pill leaves to the left with the hero. With
 * [resting], the pill's equalizer keeps still.
 */
@Composable
internal fun HomeMiddle(
    state: HomeUiState,
    resting: Boolean,
    actions: HomeActions,
    transition: BcHeldTransition,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableIntStateOf(1) }
    val onEnter by rememberUpdatedState { actions.onPageWanted(HomePage.Listen) }
    // Leftwards is towards Écoute: the finger's travel, as a share of the width of the screen.
    val slide = rememberDraggableState { delta -> transition.drag(-delta / width) }
    Box(
        modifier
            .fillMaxWidth()
            .testTag(MIDDLE_TAG)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .draggable(
                slide,
                Orientation.Horizontal,
                enabled = state.media is MediaState.Loaded,
                onDragStopped = { velocity -> if (transition.release(-velocity / width) == 1f) onEnter() }
            )
    ) {
        // The pill moves within a layer of its own: the finger keeps sliding over a middle that stays in place.
        Box(Modifier.fillMaxSize().leavesLeft { transition.progress }) {
            MusicPill(
                media = state.media as? MediaState.Loaded,
                resting = resting,
                onOpenListen = { actions.onPageWanted(HomePage.Listen) },
                onOpenMediaApps = { actions.onListenOpenChange(true) },
                onOpenApp = actions::onOpenMediaApp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = SIDE, bottom = PILL_BOTTOM)
            )
        }
    }
}

/**
 * With a content: its cover, "Title · Artist", the equalizer and the chevron; sliding it to the right shows the
 * content in its app. Without: "Écouter…", which opens the media apps.
 */
@Composable
private fun MusicPill(
    media: MediaState.Loaded?,
    resting: Boolean,
    onOpenListen: () -> Unit,
    onOpenMediaApps: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (media == null) {
        BcPill("Écouter…", onClick = onOpenMediaApps, modifier = modifier, icon = BcIcons.Note)
    } else {
        BcMusicPill(
            title = media.content.title,
            artist = media.content.artist,
            cover = media.content.cover.asImage(),
            playing = media.playing,
            onClick = onOpenListen,
            modifier = modifier,
            coverColors = media.content.colors?.pair,
            still = resting,
            swipe = BcRowSwipe("Ouvrir ${media.content.app}", onOpenApp)
        )
    }
}

private val PILL_BOTTOM = 40.dp
