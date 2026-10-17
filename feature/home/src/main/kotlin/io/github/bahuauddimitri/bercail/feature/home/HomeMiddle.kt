package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcMusicPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcRowSwipe
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage

/**
 * The middle of Accueil: the music pill, with no page dots. Sliding left opens Écoute, which then takes the whole
 * screen. With [resting], the pill's equalizer keeps still.
 */
@Composable
internal fun HomeMiddle(state: HomeUiState, resting: Boolean, actions: HomeActions, modifier: Modifier = Modifier) {
    val onSwipeLeft by rememberUpdatedState { actions.onPageWanted(HomePage.Listen) }
    Box(
        modifier.fillMaxWidth().testTag(MIDDLE_TAG).pointerInput(Unit) {
            var travel = 0f
            detectHorizontalDragGestures(
                onDragStart = { travel = 0f },
                onDragEnd = { if (travel <= -SWIPE.toPx()) onSwipeLeft() }
            ) { _, amount -> travel += amount }
        }
    ) {
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

private val SWIPE = 40.dp
private val PILL_BOTTOM = 40.dp
