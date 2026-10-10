package io.github.bahuauddimitri.bercail.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.bahuauddimitri.bercail.core.designsystem.brume.rememberBcBrumeState
import io.github.bahuauddimitri.bercail.core.designsystem.brume.wakesBrume
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

const val HOME_TAG = "home"
internal const val TIMELINE_TAG = "home-timeline"
internal const val MIDDLE_TAG = "home-middle"
internal const val DRAWER_TAG = "home-drawer"
internal const val BRUME_TAG = "home-brume"

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

/** The home screen plugged into its sources. They are only listened to while the screen is visible. */
@Composable
fun HomeRoute(viewModel: HomeViewModel, modifier: Modifier = Modifier, links: HomeLinks = HomeLinks.None) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel.position, viewModel, modifier, links)
}

/**
 * The two screens of the home (Accueil, Écoute) over their background, and the glass drawer at the bottom.
 * Nothing moves from one moment to the next: only the content changes.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    position: StateFlow<Duration>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    links: HomeLinks = HomeLinks.None
) {
    val brume = rememberBcBrumeState()
    Box(modifier.fillMaxSize().testTag(HOME_TAG).wakesBrume(brume)) {
        if (state.loaded) {
            HomeBackdrop(state, brume)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize()) {
                        HomeHeader(state, onOpenDay = links::onOpenDay)
                        HomeMiddle(state, resting = !brume.isAnimating, actions, links, Modifier.weight(1f))
                    }
                    VerticalTimeline(state, Modifier.align(Alignment.TopEnd))
                }
                HomeDrawer(state, position, actions, links)
            }
        }
    }
}
