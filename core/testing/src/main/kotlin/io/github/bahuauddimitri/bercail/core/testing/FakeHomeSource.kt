package io.github.bahuauddimitri.bercail.core.testing

import io.github.bahuauddimitri.bercail.core.domain.home.HomeControl
import io.github.bahuauddimitri.bercail.core.domain.home.HomeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A house like the prototype's: a lamp, a scene, shutters and heating.
 * Activating a scene dims every lamp to 10 % and closes the shutters, like a "cinema" scene would.
 */
class FakeHomeSource(controls: List<HomeControl> = Samples.home) : HomeSource {
    override val controls = MutableStateFlow(controls)
    private val usualBrightness = controls.filterIsInstance<HomeControl.Light>().associate { it.id to it.brightness }

    override suspend fun toggle(id: String) = controls.update { all ->
        when (val control = all.firstOrNull { it.id == id }) {
            is HomeControl.Scene -> all.map { it.afterScene(control.id, active = !control.active) }
            null -> all
            else -> all.map { if (it.id == id) it.toggled() else it }
        }
    }

    private fun HomeControl.afterScene(sceneId: String, active: Boolean): HomeControl = when (this) {
        is HomeControl.Scene -> if (id == sceneId) copy(active = active) else this
        is HomeControl.Light -> if (active) dimmed() else copy(brightness = usual())
        is HomeControl.Shutters -> if (active) copy(open = false) else this
        is HomeControl.Heating -> this
    }

    private fun HomeControl.toggled(): HomeControl = when (this) {
        is HomeControl.Light -> copy(on = !on)
        is HomeControl.Shutters -> copy(open = !open)
        is HomeControl.Heating -> copy(comfort = !comfort, target = if (comfort) ECO_TARGET else COMFORT_TARGET)
        is HomeControl.Scene -> this
    }

    private fun HomeControl.Light.dimmed() = copy(brightness = SCENE_BRIGHTNESS, on = true)

    private fun HomeControl.Light.usual() = usualBrightness[id] ?: brightness

    private companion object {
        const val SCENE_BRIGHTNESS = 10
        const val ECO_TARGET = 18
        const val COMFORT_TARGET = 21
    }
}
