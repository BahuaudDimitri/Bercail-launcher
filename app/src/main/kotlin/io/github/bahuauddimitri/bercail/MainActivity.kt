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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.bahuauddimitri.bercail.core.designsystem.theme.BcTheme
import io.github.bahuauddimitri.bercail.core.domain.screen.HomePage
import io.github.bahuauddimitri.bercail.feature.home.HomeRoute
import io.github.bahuauddimitri.bercail.feature.home.HomeSearch
import io.github.bahuauddimitri.bercail.feature.home.HomeViewModel
import io.github.bahuauddimitri.bercail.feature.search.SearchLinks
import io.github.bahuauddimitri.bercail.feature.search.SearchList
import io.github.bahuauddimitri.bercail.feature.search.SearchViewModel
import io.github.bahuauddimitri.bercail.feature.settings.SettingsRoute
import io.github.bahuauddimitri.bercail.feature.settings.SettingsViewModel

/** The home screen of the phone: the home with its search in the drawer, and the settings over it. */
class MainActivity : ComponentActivity() {
    private val sources get() = (application as BercailApplication).sources
    private val home: HomeViewModel by viewModels { viewModelFactory { initializer { sources.homeViewModel() } } }
    private val search: SearchViewModel by viewModels { viewModelFactory { initializer { sources.searchViewModel() } } }
    private val settings: SettingsViewModel by viewModels {
        viewModelFactory { initializer { sources.settingsViewModel() } }
    }
    private var settingsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The home screen is the bottom of the stack: back has nowhere to go.
        onBackPressedDispatcher.addCallback(this) {}
        setContent { BcTheme { Screen() } }
    }

    /** The Home button always comes back to the plain home screen: no settings, no search, the Accueil page. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            settingsOpen = false
            search.onOpenChange(false)
            home.onPageWanted(HomePage.Home)
        }
    }

    @Composable
    private fun Screen() {
        val searching by search.state.collectAsStateWithLifecycle()
        val links = object : SearchLinks {
            override fun onOpenSettings() {
                settingsOpen = true
            }
        }
        Box {
            HomeRoute(
                viewModel = home,
                search = HomeSearch(
                    open = searching.open,
                    query = searching.query,
                    onQueryChange = search::onQueryChange,
                    onOpenChange = search::onOpenChange
                ) { SearchList(searching, search, search.icons, links = links) }
            )
            AnimatedVisibility(visible = settingsOpen, enter = fadeIn(), exit = fadeOut()) {
                SettingsRoute(settings, onBack = { settingsOpen = false })
            }
        }
    }
}
