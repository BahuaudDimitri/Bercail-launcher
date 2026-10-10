package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.brume.rememberBcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.wakesBrume
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

const val HOME_TAG = "home"
internal const val TIMELINE_TAG = "home-timeline"
internal const val MIDDLE_TAG = "home-middle"
internal const val DRAWER_TAG = "home-drawer"
internal const val BRUME_TAG = "home-brume"
internal const val VEIL_TAG = "home-veil"

/** What the home screen does by itself. */
interface HomeActions {
    fun onDrawerExpandedChange(expanded: Boolean)
    fun onPageWanted(page: HomePage)
    fun onToggleControl(id: String)
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onSeek(fraction: Float)
}

/** What the home screen asks others to open. Each one arrives with its wave; until then, nothing happens. */
interface HomeLinks {
    fun onOpenDay() = Unit
    fun onOpenConversation(id: String) = Unit
    fun onOpenMessagesApp() = Unit
    fun onOpenHomeApp() = Unit
    fun onOpenMediaApps() = Unit
    fun onOpenMediaApp(app: String) = Unit

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
    Box(modifier.fillMaxSize().testTag(HOME_TAG).wakesBrume(brume)) {
        if (state.loaded) {
            HomeBackdrop(state, brume)
            DrawerOverScreen(
                searching = search?.open == true,
                screen = {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            HomeHeader(state, onOpenDay = links::onOpenDay)
                            HomeMiddle(state, resting = !brume.isAnimating, actions, links, Modifier.weight(1f))
                        }
                        VerticalTimeline(state, Modifier.align(Alignment.TopEnd))
                    }
                },
                drawer = { HomeDrawer(state, position, actions, links, search) }
            )
        }
    }
}

/**
 * Places the drawer at the bottom and gives the screen the room above it. While the drawer rises for the search,
 * and until it is back down, the screen keeps the room it had: it is covered, not squeezed.
 */
@Composable
private fun DrawerOverScreen(searching: Boolean, screen: @Composable () -> Unit, drawer: @Composable () -> Unit) {
    val rising = updateTransition(searching, label = "search")
    // Gives the transition the duration of the drawer's own movement.
    rising.animateFloat({ tween(BcTheme.motion.drawerMillis) }, label = "search rise") { if (it) 1f else 0f }
    val covered = rising.targetState || rising.currentState
    val restingDrawerHeight = remember { IntArray(1) }
    Layout(contents = listOf(screen, drawer), modifier = Modifier.fillMaxSize()) { (screens, drawers), constraints ->
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
