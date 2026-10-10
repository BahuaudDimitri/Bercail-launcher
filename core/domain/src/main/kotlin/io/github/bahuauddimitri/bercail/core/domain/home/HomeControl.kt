package io.github.bahuauddimitri.bercail.core.domain.home

import kotlinx.coroutines.flow.Flow

/** One of the home controls: a lamp, a scene, shutters or heating. */
sealed interface HomeControl {
    val id: String
    val name: String
    val isOn: Boolean

    /** What the control says about itself: "60 %", "ouverts", "active", "21 °C". */
    val state: String

    /** One wording per device, the same in the summary, on its tile and in the search. */
    val label: String get() = "$name $state"

    data class Light(override val id: String, override val name: String, val brightness: Int, val on: Boolean) :
        HomeControl {
        override val isOn get() = on
        override val state get() = if (on) "$brightness %" else "éteint"
    }

    data class Scene(override val id: String, override val name: String, val active: Boolean) : HomeControl {
        override val isOn get() = active
        override val state get() = if (active) "active" else "prête"
    }

    data class Shutters(override val id: String, override val name: String, val open: Boolean) : HomeControl {
        override val isOn get() = open
        override val state get() = if (open) "ouverts" else "fermés"
    }

    data class Heating(override val id: String, override val name: String, val target: Int, val comfort: Boolean) :
        HomeControl {
        override val isOn get() = comfort
        override val state get() = "$target °C"
    }
}

interface HomeSource {
    /** The controls chosen for the home screen, emitted again when one changes. */
    val controls: Flow<List<HomeControl>>

    /** Switches a control on or off. */
    suspend fun toggle(id: String)
}

/** The home line of the folded drawer: what is on, or "Maison au repos". */
fun homeSummary(controls: List<HomeControl>): String =
    controls.filter { it.isOn }.joinToString(", ") { it.label }.ifEmpty { "Maison au repos" }
