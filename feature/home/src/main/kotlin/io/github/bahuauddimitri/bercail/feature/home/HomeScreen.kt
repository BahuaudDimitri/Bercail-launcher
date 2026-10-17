package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.brume.rememberBcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.wakesBrume
import io.github.bahuauddimitri.bercail.core.designsystem.component.BcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.component.rememberBcFrost
import io.github.bahuauddimitri.bercail.core.designsystem.motion.BcHeldTransition
import io.github.bahuauddimitri.bercail.core.designsystem.motion.rememberBcHeldTransition
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcMomentScope
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.media.MediaState
import io.github.bahuauddimitri.bercail.core.domain.screen.HomeBackground
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

const val HOME_TAG = "home"
internal const val TIMELINE_TAG = "home-timeline"
internal const val MIDDLE_TAG = "home-middle"
internal const val DRAWER_TAG = "home-drawer"
internal const val BRUME_TAG = "home-brume"
internal const val VEIL_TAG = "home-veil"
internal const val DAY_TAG = "home-day"
internal const val LISTEN_TAG = "home-listen"

/** What the home screen does by itself. */
interface HomeActions :
    ListenActions,
    TalkActions,
    DockActions {
    fun onDrawerExpandedChange(expanded: Boolean)
    fun onPageWanted(page: HomePage)

    /** Opens or closes the « Aujourd'hui » sheet. [focus] names the appointment it opens on, if any. */
    fun onDayOpenChange(open: Boolean, focus: String? = null)

    /** Shows an appointment of the day in the phone's calendar app. */
    fun onOpenEvent(id: String)

    /** Shows today in the phone's calendar app. */
    fun onOpenCalendar()

    /** Shows the alarms in the phone's clock app. */
    fun onOpenAlarms()
    fun onToggleControl(id: String)
}

/** What the home screen asks others to open or to do. The one of the house arrives with its wave. */
interface HomeLinks {
    /** Asks the user, on Android's own screen, to let Bercail read something on the phone. */
    fun onAskAccess(access: Access) = Unit
    fun onOpenMessagesApp() = Unit

    fun onOpenHomeApp() = Unit

    /** The page of Bercail in the phone settings: where a right Android refuses to give can be unlocked. */
    fun onOpenAppSettings() = Unit

    object None : HomeLinks
}

/**
 * The search that lives in the drawer. The bar is the field itself: focusing it asks to open the search, and
 * the drawer then rises over the whole screen around [list], which another module provides.
 */
class HomeSearch(
    val open: Boolean,
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onOpenChange: (Boolean) -> Unit,
    /** The keyboard's search key. */
    val onSubmit: () -> Unit = {},
    val list: @Composable () -> Unit
)

/** The home screen plugged into its sources. They are only listened to while the screen is visible. */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    links: HomeLinks = HomeLinks.None,
    search: HomeSearch? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel.position, viewModel, modifier, links, search)
}

/**
 * The two screens of the home (Accueil, Écoute) over their background, and the glass drawer at the bottom.
 * Nothing moves from one moment to the next: only the content changes. Without [search], the bar is inert.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    position: StateFlow<Duration>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    links: HomeLinks = HomeLinks.None,
    search: HomeSearch? = null
) {
    val brume = rememberBcBrumeState()
    val frost = rememberBcFrost()
    // The sheets frost Brume only: the user's wallpaper is drawn by the system, out of reach. A sheet freezes Brume.
    val sheetFrost = frost.takeIf { state.background is HomeBackground.Brume && !brume.isAnimating }
    // A sheet is tinted glass: what it covers fades away, so that nothing shows through it.
    val covered = state.dayOpen || state.listenOpen || state.talk != null
    val uncovered by animateFloatAsState(
        targetValue = if (covered) 0f else 1f,
        animationSpec = tween(BcTheme.motion.fadeMillis),
        label = "under a sheet"
    )
    val fading = Modifier.graphicsLayer { alpha = uncovered }
    // 0 on Accueil, 1 on Écoute. A touch on the pill, the back gesture, a screen reader or the music stopping change
    // the page: the transition then plays the rest of the way alone.
    val transition = rememberBcHeldTransition(state.page.side)
    LaunchedEffect(state.page) { transition.playTo(state.page.side) }
    Box(modifier.fillMaxSize().testTag(HOME_TAG).wakesBrume(brume)) {
        if (state.loaded) {
            BcMomentScope(state.dayPart.moment) {
                HomeBackdrop(state, brume, frost, searching = search?.open == true)
                // Under a sheet, a screen reader must not read the screen either.
                Box(if (covered) Modifier.clearAndSetSemantics {} else Modifier) {
                    HomeOrPlayer(
                        state = state,
                        transition = transition,
                        home = {
                            Accueil(state, actions, links, search, !brume.isAnimating, sheetFrost, transition) {
                                uncovered
                            }
                        },
                        player = { loaded ->
                            Player(loaded, position, state.dayPart.brume, actions, transition, fading)
                        }
                    )
                }
                DaySheet(state, actions, links, sheetFrost)
                ListenSheet(state, actions, links, sheetFrost)
                TalkSheet(state, actions, links, sheetFrost)
            }
        }
    }
}

/**
 * Accueil or Écoute, and both while one gives way to the other along [transition]: the hero leaves to the left, the
 * line retracts into the top edge and the band goes down, while the arch of the player comes down from that edge.
 * At rest, only one of them is there.
 */
@Composable
private fun HomeOrPlayer(
    state: HomeUiState,
    transition: BcHeldTransition,
    home: @Composable () -> Unit,
    player: @Composable (MediaState.Loaded) -> Unit
) {
    // While the player goes away, it keeps the content it showed, even once nothing is loaded any more.
    val shown = remember { Ref<MediaState.Loaded>() }
    (state.media as? MediaState.Loaded)?.let { shown.value = it }
    // A finger holding the transition at one end keeps both: the gesture goes on.
    val homeThere by remember { derivedStateOf { transition.progress < 1f || transition.isMoving } }
    val playerThere by remember { derivedStateOf { transition.progress > 0f || transition.isMoving } }
    if (homeThere) home()
    if (playerThere) shown.value?.let { player(it) }
}

/**
 * Accueil: the hero, the line of the day, the music pill, and the band with its sheet at the bottom. Under a sheet,
 * they fade by [uncovered], as read at each frame. Towards Écoute ([transition]), the hero and the pill leave to the
 * left, the line retracts into the top edge, and the band goes down off the screen.
 */
@Composable
private fun Accueil(
    state: HomeUiState,
    actions: HomeActions,
    links: HomeLinks,
    search: HomeSearch?,
    still: Boolean,
    sheetFrost: BcFrost?,
    transition: BcHeldTransition,
    uncovered: () -> Float
) {
    var headerHeight by remember { mutableIntStateOf(0) }
    val fading = Modifier.graphicsLayer { alpha = uncovered() }
    val towardsListen = { transition.progress }
    DrawerOverScreen(
        searching = search?.open == true,
        screen = {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    HomeHeader(
                        state = state,
                        onOpenDay = { actions.onDayOpenChange(true, focus = it) },
                        onOpenCalendar = actions::onOpenCalendar,
                        modifier = Modifier.onSizeChanged { headerHeight = it.height }.leavesLeft(towardsListen)
                    )
                    HomeMiddle(state, still, actions, transition, Modifier.weight(1f).then(fading))
                }
                VerticalTimeline(
                    state = state,
                    onOpenDay = { actions.onDayOpenChange(true, focus = it) },
                    onOpenEvent = actions::onOpenEvent,
                    onOpenAlarms = actions::onOpenAlarms,
                    modifier = Modifier.align(Alignment.TopEnd).then(fading).retractsUp(towardsListen),
                    titlesBelow = with(LocalDensity.current) { headerHeight.toDp() }
                )
            }
        },
        drawer = { Box(fading.goesDown(towardsListen)) { HomeDrawer(state, actions, links, search, sheetFrost) } }
    )
}

/** 0 for Accueil, 1 for Écoute: where the transition rests on each page. */
private val HomePage.side: Float get() = if (this == HomePage.Listen) 1f else 0f

/** Moves to the left by a share of its width: by [progress] of it, as read at each frame. */
internal fun Modifier.leavesLeft(progress: () -> Float) = graphicsLayer { translationX = -progress() * size.width }

/** Goes down by its own height: off the bottom of the screen at the end of [progress]. */
private fun Modifier.goesDown(progress: () -> Float) = graphicsLayer { translationY = progress() * size.height }

/** Retracts from its bottom towards its top edge: nothing of it is left at the end of [progress]. */
private fun Modifier.retractsUp(progress: () -> Float) = drawWithContent {
    val left = 1f - progress()
    if (left < 1f) {
        // What it draws beside itself (the titles, the glow) stays, down to where it is cut.
        clipRect(-size.width * BEYOND, -size.height, size.width * BEYOND, size.height * left) {
            this@drawWithContent.drawContent()
        }
    } else {
        drawContent()
    }
}

/** How far beside the line, in widths of the line, what it draws may reach. */
private const val BEYOND = 8f

/**
 * Places the drawer at the bottom and gives the screen the room above it. While the drawer rises for the search,
 * and until it is back down, the screen keeps the room it had: it is covered, not squeezed.
 */
@Composable
private fun DrawerOverScreen(searching: Boolean, screen: @Composable () -> Unit, drawer: @Composable () -> Unit) {
    val rising = updateTransition(searching, label = "search")
    // Gives the transition the duration of the drawer's own movement.
    val rise = rising.animateFloat({ tween(BcTheme.motion.drawerMillis) }, label = "search rise") {
        if (it) 1f else 0f
    }
    val covered = rising.targetState || rising.currentState
    val restingDrawerHeight = remember { IntArray(1) }
    // The glass of the search is not opaque: what it covers fades away, and is no longer read out either.
    val fading: @Composable () -> Unit = {
        Box(
            Modifier
                .graphicsLayer { alpha = 1f - rise.value }
                .then(if (searching) Modifier.clearAndSetSemantics {} else Modifier)
        ) { screen() }
    }
    Layout(contents = listOf(fading, drawer), modifier = Modifier.fillMaxSize()) { (screens, drawers), constraints ->
        val placedDrawers = drawers.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val drawerHeight = placedDrawers.maxOfOrNull { it.height } ?: 0
        if (!covered || restingDrawerHeight[0] == 0) restingDrawerHeight[0] = drawerHeight
        val room = (constraints.maxHeight - restingDrawerHeight[0]).coerceAtLeast(0)
        val placedScreens = screens.map { it.measure(Constraints.fixed(constraints.maxWidth, room)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placedScreens.forEach { it.place(0, 0) }
            placedDrawers.forEach { it.place(0, constraints.maxHeight - it.height) }
        }
    }
}
