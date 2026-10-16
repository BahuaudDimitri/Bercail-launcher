package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut
import io.github.bahuauddimitri.bercail.core.domain.apps.AppShortcut.Kind
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Moment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The menu of shortcuts of a dock app, as the home screen keeps it. Every source is a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeDockTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val world = FakeWorld(Moment.Matin)

    // Made once the main dispatcher is the test one.
    private val home by lazy { world.homeViewModel() }

    private fun TestScope.show(): Job = backgroundScope.launch(dispatcher) { home.state.collect {} }

    private fun shortcut(id: String, kind: Kind = Kind.Dynamic, rank: Int = 0) =
        AppShortcut("chrome", id, id, kind, rank)

    @Test
    fun `un toucher long ouvre le menu de l'app avec ses raccourcis dans l'ordre, quatre au plus`() =
        runTest(dispatcher) {
            world.apps.offeredShortcuts["chrome"] =
                (1..5).map { shortcut("r$it", rank = it) } + shortcut("m", Kind.Manifest)
            show()

            home.onDockLongPress("chrome")

            val menu = home.state.value.dockMenu
            assertThat(menu?.appId).isEqualTo("chrome")
            assertThat(menu?.shortcuts.orEmpty().map { it.id }).containsExactly("m", "r1", "r2", "r3")
        }

    @Test
    fun `une app sans raccourci lisible n'ouvre pas de menu`() = runTest(dispatcher) {
        show()

        home.onDockLongPress("beeper")

        assertThat(home.state.value.dockMenu).isNull()
    }

    @Test
    fun `le menu se referme quand on le demande, par le geste retour ou le bouton Accueil`() = runTest(dispatcher) {
        world.apps.offeredShortcuts["chrome"] = listOf(shortcut("onglet"))
        show()
        home.onDockLongPress("chrome")

        home.onCloseDockMenu()

        assertThat(home.state.value.dockMenu).isNull()
    }

    @Test
    fun `ouvrir un raccourci le lance et referme le menu`() = runTest(dispatcher) {
        val tab = shortcut("onglet")
        world.apps.offeredShortcuts["chrome"] = listOf(tab)
        show()
        home.onDockLongPress("chrome")

        home.onOpenShortcut(tab)

        assertThat(world.apps.openedShortcuts).containsExactly(tab)
        assertThat(home.state.value.dockMenu).isNull()
    }
}
