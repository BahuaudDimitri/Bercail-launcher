package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgressCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCover
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcon
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
import io.github.bahuauddimitri.bercail.core.designsystem.motion.BcHeldTransition
import io.github.bahuauddimitri.bercail.core.domain.media.MediaAction
import io.github.bahuauddimitri.bercail.core.domain.media.MediaContent
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.media.asTrackTime
import io.github.bahuauddimitri.bercail.core.domain.media.credits
import io.github.bahuauddimitri.bercail.core.domain.media.progress
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

internal const val PLAYER_TAG = "home-player"

/**
 * The Listen screen, the player alone over Brume, laid out from the top edge of the screen as on the reference: the
 * arch of the cover hanging from it, the half circle of the progress around the arch's curve with the times at its
 * centre, the title on two lines at most and « Artiste · Album » under it, then the remote, the way to the app, and
 * the chevron that says sliding up goes back to the home. Nothing moves when the title takes one line or two.
 * Along [transition], the arch and its half circle come down from the top edge, as if lowered, and the rest appears
 * as they arrive. Sliding up rolls the arch back up under the finger: past half way, or with a flick, the home comes
 * back. The system back gesture goes back to the home too.
 * A content without cover takes the colors Brume has at that moment ([fallback]).
 * On a screen smaller than the Pixel 9's, the arch gets shorter or narrower and the gaps tighten ([listenLayout]):
 * every control stays whole on the screen, above the navigation bar.
 */
@Composable
internal fun Player(
    loaded: MediaState.Loaded,
    position: StateFlow<Duration>,
    fallback: BcBrumeColors,
    actions: HomeActions,
    transition: BcHeldTransition,
    modifier: Modifier = Modifier
) {
    // The screen keeps the Pixel 9's layout, or as close to it as a smaller one allows.
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
        val layout = remember(maxWidth, maxHeight, bar, density.fontScale) {
            listenLayout(maxWidth, maxHeight, bar, density.fontScale)
        }
        PlayerContent(loaded, position, fallback, actions, transition, layout)
    }
}

/** The player of [Player], each thing where [layout] puts it. */
@Composable
private fun PlayerContent(
    loaded: MediaState.Loaded,
    position: StateFlow<Duration>,
    fallback: BcBrumeColors,
    actions: HomeActions,
    transition: BcHeldTransition,
    layout: ListenLayout
) {
    val content = loaded.content
    // Sliding up, the system back gesture and a screen reader's action all go back to the home.
    val leave by rememberUpdatedState { actions.onPageWanted(HomePage.Home) }
    BackHandler { leave() }
    val below = Modifier.fillMaxWidth().padding(horizontal = TEXT_SIDE)
    val arrival = { transition.progress }
    val appearing = Modifier.appears(arrival)
    // Sliding up rolls the arch back up under the finger: the whole way is the height of the arch.
    val travel = with(LocalDensity.current) { layout.archHeight.toPx() }
    val slide = rememberDraggableState { delta -> transition.drag(delta / travel) }
    val lowered = Modifier.lowered(travel, arrival)
    Box(
        Modifier.fillMaxSize().testTag(PLAYER_TAG).leavesUpwards(slide, travel, transition) { leave() },
        contentAlignment = Alignment.TopCenter
    ) {
        BcArchCover(
            image = content.cover.asImage(),
            colors = content.colors?.pair ?: (fallback.first to fallback.second),
            contentDescription = "Pochette de ${content.title}",
            modifier = lowered,
            width = layout.archWidth,
            height = layout.archHeight
        )
        val arcTop = layout.curveCentre - BcArcProgressCurveCentre
        ArcProgress(
            loaded,
            position,
            actions,
            layout.arcRadius,
            Modifier.padding(top = arcTop).then(lowered).then(appearing)
        )
        Titles(content, below.padding(top = layout.titlesTop).then(appearing))
        Remote(loaded, actions, Modifier.padding(top = layout.remoteTop).then(appearing))
        BcButton(
            "Ouvrir dans ${content.app}",
            onClick = actions::onOpenMediaApp,
            modifier = Modifier.padding(top = layout.openTop).then(appearing)
        )
        BcIcon(
            BcIcons.ChevronUp,
            contentDescription = null,
            modifier = Modifier.padding(top = layout.chevronTop).then(appearing).semantics {
                contentDescription = BACK_HOME_HINT
                // A screen reader cannot slide: the chevron offers the way back as an action.
                customActions = listOf(
                    CustomAccessibilityAction(BACK_HOME) {
                        leave()
                        true
                    }
                )
            },
            size = CHEVRON,
            tint = BcTextColor.Faint
        )
    }
}

/**
 * Sliding up rolls the arch back up along [transition], by [slide], as far as the finger goes, [travel] being the
 * whole way; letting go past half way, or with a flick upwards, calls [onLeave]. A finger put down on the arc is
 * taken by the arc, to move in the content; sideways, nothing happens.
 */
private fun Modifier.leavesUpwards(
    slide: DraggableState,
    travel: Float,
    transition: BcHeldTransition,
    onLeave: () -> Unit
) = draggable(
    slide,
    Orientation.Vertical,
    onDragStopped = { velocity -> if (transition.release(velocity / travel) == 0f) onLeave() }
)

/** Comes down from above the top edge, by the height of the arch ([travel]), to its place at the end of [arrival]. */
private fun Modifier.lowered(travel: Float, arrival: () -> Float) = graphicsLayer {
    translationY = -(1f - arrival()) * travel
}

/** Shows only as the arch arrives: from half way of [arrival], fully at its end. */
private fun Modifier.appears(arrival: () -> Float) = graphicsLayer {
    alpha = ((arrival() - APPEARS_FROM) / (1f - APPEARS_FROM)).coerceIn(0f, 1f)
}

/**
 * Where the content is, along the half circle under the arch. Touching or dragging along it moves in the content, if
 * its app lets the remote do it. A live stream has no end: « En direct » instead of the times.
 */
@Composable
private fun ArcProgress(
    loaded: MediaState.Loaded,
    position: StateFlow<Duration>,
    actions: HomeActions,
    radius: Dp,
    modifier: Modifier
) {
    // Followed here only: the position costs nothing while the player is not on screen.
    val now by position.collectAsStateWithLifecycle()
    val duration = loaded.content.duration
    BcArcProgress(
        progress = progress(now, duration),
        elapsed = now.asTrackTime(),
        duration = duration.asTrackTime(),
        onSeek = if (MediaAction.Seek in loaded.actions) actions::onSeek else null,
        modifier = modifier,
        live = duration <= Duration.ZERO,
        radius = radius
    )
}

/** The title, on two lines at most, and « Artiste · Album » under it, or whichever of the two is known. */
@Composable
private fun Titles(content: MediaContent, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CREDITS_GAP)
    ) {
        BcText(content.title, style = BcTextStyle.Display, maxLines = 2, textAlign = TextAlign.Center)
        val credits = content.credits
        if (credits.isNotEmpty()) {
            BcText(
                credits,
                style = BcTextStyle.Message,
                color = BcTextColor.Muted,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Previous, play or pause, next. Only the buttons the app accepts are there (YouTube, for one, has no "previous"); a
 * missing one leaves its room empty, so that play and pause stay in the middle.
 */
@Composable
private fun Remote(loaded: MediaState.Loaded, actions: HomeActions, modifier: Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(REMOTE_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(SIDE_BUTTON)) {
            if (MediaAction.Previous in loaded.actions) {
                BcIconButton(BcIcons.Previous, "Précédent", actions::onPrevious, size = SIDE_BUTTON)
            }
        }
        BcIconButton(
            icon = if (loaded.playing) BcIcons.Pause else BcIcons.Play,
            contentDescription = if (loaded.playing) "Pause" else "Lecture",
            onClick = actions::onPlayPause,
            variant = BcIconButtonVariant.Light,
            size = MAIN_BUTTON
        )
        Box(Modifier.size(SIDE_BUTTON)) {
            if (MediaAction.Next in loaded.actions) {
                BcIconButton(BcIcons.Next, "Suivant", actions::onNext, size = SIDE_BUTTON)
            }
        }
    }
}

private const val BACK_HOME_HINT = "Glisser vers le haut pour revenir à l'accueil"
private const val BACK_HOME = "Revenir à l'accueil"

/** From where along [BcHeldTransition] the texts and controls of the player start to show. */
private const val APPEARS_FROM = 0.5f

private val TEXT_SIDE = 24.dp
private val CREDITS_GAP = 5.dp
private val REMOTE_GAP = 30.dp
private val SIDE_BUTTON = 48.dp
private val MAIN_BUTTON = 76.dp
private val CHEVRON = CHEVRON_SIZE
