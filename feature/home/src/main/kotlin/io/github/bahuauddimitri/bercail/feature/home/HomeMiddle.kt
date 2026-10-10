package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcCover
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcMusicPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPageDots
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcPill
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcSpacing
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.math.abs

/**
 * The middle of the home screen: the music pill on Accueil, the cover on Écoute, and the page dots.
 * Sliding left opens Écoute, sliding right comes back. With [resting], the pill's equalizer keeps still.
 */
@Composable
internal fun HomeMiddle(
    state: HomeUiState,
    resting: Boolean,
    actions: HomeActions,
    links: HomeLinks,
    modifier: Modifier = Modifier
) {
    val fade = BcTheme.motion.fadeMillis
    val onSwipe by rememberUpdatedState { left: Boolean ->
        actions.onPageWanted(if (left) HomePage.Listen else HomePage.Home)
    }
    Box(
        modifier.fillMaxWidth().testTag(MIDDLE_TAG).pointerInput(Unit) {
            var travel = 0f
            detectHorizontalDragGestures(
                onDragStart = { travel = 0f },
                onDragEnd = { if (abs(travel) >= SWIPE.toPx()) onSwipe(travel < 0) }
            ) { _, amount -> travel += amount }
        }
    ) {
        AnimatedContent(
            targetState = state.page,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) },
            label = "home page"
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                val loaded = state.media as? MediaState.Loaded
                when (page) {
                    HomePage.Home -> MusicPill(
                        media = loaded,
                        resting = resting,
                        onOpenListen = { actions.onPageWanted(HomePage.Listen) },
                        onOpenMediaApps = links::onOpenMediaApps,
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = SIDE, bottom = PILL_BOTTOM)
                    )

                    HomePage.Listen -> if (loaded != null) {
                        Listening(
                            content = loaded.content,
                            onOpenApp = { links.onOpenMediaApp(loaded.content.app) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = SIDE, end = SIDE, bottom = LISTEN_BOTTOM)
                        )
                    }
                }
            }
        }
        // The dots only exist while there is a second screen to go to.
        AnimatedVisibility(
            visible = state.media.isLoaded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(fade)),
            exit = fadeOut(tween(fade))
        ) {
            BcPageDots(
                pages = PAGES,
                selected = state.page.ordinal,
                onSelect = { actions.onPageWanted(HomePage.entries[it]) }
            )
        }
    }
}

/** With a content: its cover, "Title · Artist" and the equalizer. Without: "Écouter…", which opens the media apps. */
@Composable
private fun MusicPill(
    media: MediaState.Loaded?,
    resting: Boolean,
    onOpenListen: () -> Unit,
    onOpenMediaApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (media == null) {
        BcPill("Écouter…", onClick = onOpenMediaApps, modifier = modifier, icon = BcIcons.Note)
    } else {
        BcMusicPill(
            title = media.content.title,
            artist = media.content.artist,
            cover = null,
            playing = media.playing,
            onClick = onOpenListen,
            modifier = modifier,
            coverColors = media.content.colors.pair,
            still = resting
        )
    }
}

/** The Listen screen: large cover with its halo, title on two lines at most, artist, and the way to its app. */
@Composable
private fun Listening(content: MediaContent, onOpenApp: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BcSpacing.s)
    ) {
        BcCover(image = null, colors = content.colors.pair, contentDescription = "Pochette de ${content.title}")
        BcText(
            content.title,
            modifier = Modifier.padding(top = TITLE_GAP),
            style = BcTextStyle.Display,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        BcText(content.artist, style = BcTextStyle.BodySmall, color = BcTextColor.Muted, maxLines = 1)
        BcButton("Ouvrir dans ${content.app}", onClick = onOpenApp)
    }
}

private val PAGES = listOf("Accueil", "Écoute")
private val SWIPE = 40.dp
private val PILL_BOTTOM = 40.dp
private val LISTEN_BOTTOM = 46.dp
private val TITLE_GAP = 10.dp
