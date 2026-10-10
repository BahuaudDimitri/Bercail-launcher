package io.github.bahuauddimitri.bercail

import io.github.bahuauddimitri.bercail.core.domain.settings.Settings
import io.github.bahuauddimitri.bercail.core.domain.settings.SettingsSource
import io.github.bahuauddimitri.bercail.core.testing.FakeAgendaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeMediaSource
import io.github.bahuauddimitri.bercail.core.testing.FakeWorld
import io.github.bahuauddimitri.bercail.core.testing.Samples
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * In the debug app, the sources that are not plugged in yet show the prototype's data: every state of the home
 * screen can be seen and tried on the phone. The released app shows empty states instead.
 */
internal fun waiting(): Waiting {
    val prototype = FakeWorld()
    return Waiting(
        // The prototype's day, replayed every day.
        agenda = FakeAgendaSource(Samples.eventsOn(Samples.day), everyDay = true),
        weather = prototype.weather,
        messages = prototype.messages,
        home = prototype.home,
        // A player whose time moves, so that the remote can be tried.
        media = FakeMediaSource(Samples.spotify, playing = true, position = Samples.position, ticking = true)
    )
}

/** The prototype's people are the favorites until real ones can be chosen (wave 8). */
internal fun SettingsSource.forThisBuild(): SettingsSource = object : SettingsSource {
    override val settings: Flow<Settings> = this@forThisBuild.settings.map {
        if (it.favoritePeople.isEmpty()) it.copy(favoritePeople = Samples.settings.favoritePeople) else it
    }

    override suspend fun update(transform: (Settings) -> Settings) = this@forThisBuild.update(transform)
}
