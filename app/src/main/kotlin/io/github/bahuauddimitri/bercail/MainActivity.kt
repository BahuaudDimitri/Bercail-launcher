package io.github.bahuauddimitri.bercail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.feature.home.HomeLinks
import io.github.bahuauddimitri.bercail.feature.home.HomeRoute
import io.github.bahuauddimitri.bercail.feature.home.HomeSearch
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel
import io.github.bahuauddimitri.bercail.feature.search.SearchLinks
import io.github.bahuauddimitri.bercail.feature.search.SearchList
import io.github.bahuauddimitri.bercail.feature.search.SearchViewModel
import io.github.bahuauddimitri.bercail.feature.settings.SettingsRoute
import io.github.bahuauddimitri.bercail.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

/** The home screen of the phone: the home with its search in the drawer, and the settings over it. */
class MainActivity : ComponentActivity() {
    private val sources get() = (application as BercailApplication).sources
    private val home: HomeViewModel by viewModels { viewModelFactory { initializer { sources.homeViewModel() } } }
    private val search: SearchViewModel by viewModels { viewModelFactory { initializer { sources.searchViewModel() } } }
    private val settings: SettingsViewModel by viewModels {
        viewModelFactory { initializer { sources.settingsViewModel() } }
    }
    private val questions = AccessQuestions(this, access = { sources.access }, phone = { sources.phone })
    private var settingsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The home screen is the bottom of the stack: back has nowhere to go.
        onBackPressedDispatcher.addCallback(this) {}
        setContent { BcTheme { Screen() } }
        // Each time the home screen shows, Bercail may look for its own update; it stops as soon as it is hidden.
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { sources.updater.onHomeShown() } }
    }

    /** The home screen comes back: the user may have changed, in the phone settings, what Bercail may read. */
    override fun onStart() {
        super.onStart()
        sources.access.refresh()
    }

    /** Nobody looks at the home screen anymore: a new version of Bercail may replace this one; no menu stays open. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) sources.onHomeHidden()
        home.onCloseDockMenu()
    }

    /**
     * The Home button always comes back to the plain home screen: no settings, search, sheet or menu of the dock,
     * the Accueil page.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            settingsOpen = false
            search.onOpenChange(false)
            home.onDayOpenChange(false)
            home.onListenOpenChange(false)
            home.onCloseConversation()
            home.onCloseDockMenu()
            home.onPageWanted(HomePage.Home)
        }
    }

    @Composable
    private fun Screen() {
        val searching by search.state.collectAsStateWithLifecycle()
        val searchLinks = object : SearchLinks {
            override fun onOpenSettings() {
                settingsOpen = true
            }

            /** A person found by the search: the search closes, the conversation opens. */
            override fun onOpenConversation(id: String) {
                search.onOpenChange(false)
                home.onOpenConversation(id)
            }

            /** An appointment found by the search: the search closes, the day opens on it. */
            override fun onOpenDay(eventId: String) {
                search.onOpenChange(false)
                home.onDayOpenChange(true, focus = eventId)
            }
        }
        val homeLinks = object : HomeLinks {
            override fun onAskAccess(access: Access) = questions.ask(access)
            override fun onOpenAppSettings() = sources.phone.openAppSettings()
            override fun onOpenMessagesApp() = sources.messages.openApp()
        }
        Box {
            HomeRoute(
                viewModel = home,
                // Under the settings, the home screen is covered: a screen reader must not read it either.
                modifier = if (settingsOpen) Modifier.clearAndSetSemantics {} else Modifier,
                links = homeLinks,
                search = HomeSearch(
                    open = searching.open,
                    query = searching.query,
                    onQueryChange = search::onQueryChange,
                    onOpenChange = search::onOpenChange,
                    onSubmit = search::onSubmit
                ) { SearchList(searching, search, search.icons, links = searchLinks) }
            )
            AnimatedVisibility(visible = settingsOpen, enter = fadeIn(), exit = fadeOut()) {
                SettingsRoute(
                    settings,
                    onBack = { settingsOpen = false },
                    onAskAccess = questions::ask,
                    onOpenAccessPage = questions::openPage
                )
            }
        }
    }
}
