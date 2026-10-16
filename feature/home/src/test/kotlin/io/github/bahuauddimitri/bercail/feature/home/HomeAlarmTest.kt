package io.github.bahuauddimitri.bercail.feature.home

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.bahuauddimitri.bercail.core.domain.access.Access
import io.github.bahuauddimitri.bercail.core.domain.time.WakeUp
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

/** The alarm of the phone at the end of the line of the day, as the home screen shows it. Every source is a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeAlarmTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun homeOf(world: FakeWorld) = world.homeViewModel()

    /** The home screen is visible: someone listens to its state. */
    private fun TestScope.show(home: HomeViewModel): Job = backgroundScope.launch(dispatcher) { home.state.collect {} }

    @Test
    fun `le réveil du lendemain arrive sur la ligne avec la température prévue à son heure`() = runTest(dispatcher) {
        val home = homeOf(FakeWorld(Moment.Soir))
        show(home)

        assertThat(home.state.value.wakeUp).isEqualTo(WakeUp("07:15", "réveil · 12°", "Réveil demain à 07:15"))
    }

    @Test
    fun `sans accord pour l'agenda, le réveil reste au bout du fil`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Soir)
        world.access.refuse(Access.Calendar)
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.wakeUp?.time).isEqualTo("07:15")
    }

    @Test
    fun `sans réveil, la ligne finit à minuit sans anneau`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Soir)
        world.alarm.next.value = null
        val home = homeOf(world)
        show(home)

        assertThat(home.state.value.wakeUp).isNull()
    }

    @Test
    fun `toucher le réveil montre les alarmes de l'horloge du téléphone`() = runTest(dispatcher) {
        val world = FakeWorld(Moment.Soir)
        val home = homeOf(world)
        show(home)

        home.onOpenAlarms()

        assertThat(world.alarm.shown).isEqualTo(1)
    }
}
