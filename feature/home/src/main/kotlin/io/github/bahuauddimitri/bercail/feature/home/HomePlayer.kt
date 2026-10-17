package io.github.bahuauddimitri.bercail.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.brume.BcBrumeColors
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgress
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArcProgressCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCover
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcArchCoverCurveCentre
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcon
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButton
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIconButtonVariant
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcIcons
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcText
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextColor
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcTextStyle
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
 * the chevron that says sliding up goes back to the home. Nothing moves when the title takes one line or two. The
 * system back gesture goes back to the home.
 * A content without cover takes the colors Brume has at that moment ([fallback]).
 */
@Composable
internal fun Player(
    loaded: MediaState.Loaded,
    position: StateFlow<Duration>,
    fallback: BcBrumeColors,
    actions: HomeActions,
    modifier: Modifier = Modifier
) {
    val content = loaded.content
    // Sliding up, the system back gesture and a screen reader's action all go back to the home.
    val leave by rememberUpdatedState { actions.onPageWanted(HomePage.Home) }
    BackHandler { leave() }
    val below = Modifier.fillMaxWidth().padding(horizontal = TEXT_SIDE)
    Box(
        modifier.fillMaxSize().testTag(PLAYER_TAG).pointerInput(Unit) { leavesUpwards { leave() } },
        contentAlignment = Alignment.TopCenter
    ) {
        BcArchCover(
            image = content.cover.asImage(),
            colors = content.colors?.pair ?: (fallback.first to fallback.second),
            contentDescription = "Pochette de ${content.title}"
        )
        ArcProgress(loaded, position, actions, Modifier.padding(top = ARC_TOP))
        Titles(content, below.padding(top = TITLES_TOP))
        Remote(loaded, actions, Modifier.padding(top = REMOTE_TOP))
        BcButton(
            "Ouvrir dans ${content.app}",
            onClick = actions::onOpenMediaApp,
            modifier = Modifier.padding(top = OPEN_TOP)
        )
        BcIcon(
            BcIcons.ChevronUp,
            contentDescription = null,
            modifier = Modifier.padding(top = CHEVRON_TOP).semantics {
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
 * Calls [onLeave] when a finger slides up far enough. A finger put down on the arc is taken by the arc, to move in
 * the content; sideways, nothing happens.
 */
private suspend fun PointerInputScope.leavesUpwards(onLeave: () -> Unit) {
    var travel = 0f
    detectVerticalDragGestures(
        onDragStart = { travel = 0f },
        onDragEnd = { if (travel <= -LEAVE_SWIPE.toPx()) onLeave() }
    ) { _, amount -> travel += amount }
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
        live = duration <= Duration.ZERO
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

/** How far a finger must slide up to leave the Listen screen: as far as a slide to the left to open it. */
private val LEAVE_SWIPE = 40.dp

// Distances from the top edge of the screen, as on the reference (a 390 × 844 phone, the arch at the top).
private val ARC_TOP = BcArchCoverCurveCentre - BcArcProgressCurveCentre
private val TITLES_TOP = 506.dp
private val REMOTE_TOP = 604.dp

// The pill of the button is drawn at 704 dp, centred in its 40 dp touch target.
private val OPEN_TOP = 702.dp
private val CHEVRON_TOP = 776.dp

private val TEXT_SIDE = 24.dp
private val CREDITS_GAP = 5.dp
private val REMOTE_GAP = 30.dp
private val SIDE_BUTTON = 48.dp
private val MAIN_BUTTON = 76.dp
private val CHEVRON = 28.dp
